package com.rakuten.miniichiba.payments.paypal

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.*
import com.paypal.api.payments.*
import com.paypal.base.rest.APIContext
import com.rakuten.miniichiba.payments.PaymentData
import com.rakuten.miniichiba.payments.PaymentRequest
import com.rakuten.miniichiba.payments.PaymentResult
import com.rakuten.miniichiba.payments.RedirectPaymentResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import javax.servlet.http.HttpServletResponse

data class PayPalPaymentData(val id: String, val token: String, val successUri: URI, val cancelUri: URI)

interface PayPalPaymentStorage {
    fun registerPayment(data: PayPalPaymentData)
    fun getAndRemovePaymentWithToken(token: String): PayPalPaymentData
    fun hasToken(token: String): Boolean
}

private const val ACCEPT_REQUEST = "/paypal/accept"
private const val CANCEL_REQUEST = "/paypal/cancel"

@Component
class PayPalBeanFactory {
    @Bean
    fun createApiContext(
            @Value("\${payments.paypal.mode}") paypalMode: String,
            @Value("\${payments.paypal.client}") paypalClient: String,
            @Value("\${payments.paypal.secret}") paypalSecret: String): APIContext = APIContext(paypalClient, paypalSecret, paypalMode)
}

@RestController
class PayPalPaymentController @Autowired constructor(
        private val storage: PayPalPaymentStorage,
        private val context: APIContext) {

    @RequestMapping(ACCEPT_REQUEST)
    fun processAcceptedPayment(
            response: HttpServletResponse,
            @RequestParam paymentId: String,
            @RequestParam token: String,
            @RequestParam("PayerID") payer: String) {
        val paymentData = storage.getAndRemovePaymentWithToken(token)
        if (paymentId != paymentData.id)
            throw IllegalAccessException("Accepted payment ID not equal to stored ID")
        val payment = Payment().apply { id = paymentId }
        val execution = PaymentExecution().apply { payerId = payer }
        response.status = HttpServletResponse.SC_FOUND
        try {
            payment.execute(context, execution)
            response.setHeader("Location", paymentData.successUri.toString())
        } catch (e: Throwable) {
            response.setHeader("Location", paymentData.cancelUri.toString())
            // TODO Log the error
        }
    }

    @RequestMapping(CANCEL_REQUEST)
    fun processCancelledPayment(response: HttpServletResponse, @RequestParam token: String) {
        if (storage.hasToken(token)) {
            val paymentData = storage.getAndRemovePaymentWithToken(token)
            response.status = HttpServletResponse.SC_FOUND
            response.setHeader("Location", paymentData.cancelUri.toString())
        }
    }
}

@Component
class PayPalJsonContext @Autowired constructor(
        internal val paymentStorage: PayPalPaymentStorage,
        internal val context: APIContext,
        injectables: InjectableValues.Std,
        @Value("\${payments.servlet.address}") servletAddress: String) {

    internal val fullAcceptAddress = servletAddress + ACCEPT_REQUEST
    internal val fullCancelAddress = servletAddress + CANCEL_REQUEST

    init {
        injectables.addValue(PayPalJsonContext::class.java, this)
    }
}

class PayPalPaymentRequest @JsonCreator constructor(
        @JsonProperty("details", required = true) val details: PaymentData,
        @JsonProperty("successUri", required = true) val successUri: URI,
        @JsonProperty("cancelUri", required = true) val cancelUri: URI,
        @JacksonInject val context: PayPalJsonContext) : PaymentRequest() {

    override fun submit(): PaymentResult {
        val pointsString = if (details.points != null) details.points.value else "0"
        var pointsDiscount = pointsString.toBigDecimal()
        val fixedItems = mutableListOf<Item>()
        var totalPrice = BigDecimal.ZERO

        for (item in details.items) {
            val quantity = if (item.quantity != null) item.quantity.toBigDecimal() else BigDecimal.ONE
            val oldPrice = item.price.value.toBigDecimal().multiply(quantity)
            if (oldPrice <= pointsDiscount) {
                pointsDiscount = pointsDiscount.minus(oldPrice)
                fixedItems.add(Item(item.name, item.quantity ?: "1", "0", item.price.currency))
            } else {
                var newPrice = oldPrice.minus(pointsDiscount)
                pointsDiscount = BigDecimal.ZERO
                val newItemPrice = newPrice.divide(quantity).setScale(2, RoundingMode.CEILING)
                newPrice = newItemPrice.multiply(quantity)
                totalPrice = totalPrice.plus(newPrice)
                fixedItems.add(Item(item.name, item.quantity ?: "1", newItemPrice.toString(), item.price.currency))
            }
        }
        val payment = Payment().apply {
            intent = "sale"
            payer = Payer().apply {
                paymentMethod = "paypal"
            }
            transactions = listOf(Transaction().apply {
                amount = Amount(details.totalPrice.currency, totalPrice.toString())
                itemList = ItemList().apply {
                    items = fixedItems
                }
            })
            redirectUrls = RedirectUrls().apply {
                returnUrl = context.fullAcceptAddress
                cancelUrl = context.fullCancelAddress
            }
        }
        val created = payment.create(context.context)
        val approvalUrl = URI(created.links.find { it.rel == "approval_url" }!!.href)
        val token = approvalUrl.query.split('&').find { it.startsWith("token=") }!!.substring(6)
        context.paymentStorage.registerPayment(PayPalPaymentData(created.id, token, successUri, cancelUri))
        return RedirectPaymentResult(approvalUrl)
    }
}