package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.paypal.api.payments.*
import com.paypal.base.rest.APIContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.net.URI
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletResponse

data class PayPalPaymentData(val id: String, val successUri: URI, val cancelUri: URI)

interface PayPalPaymentStorage {
    fun registerPayment(data: PayPalPaymentData)
    fun getAndRemovePaymentWithId(id: String): PayPalPaymentData
}

private const val ACCEPT_REQUEST = "/paypal/accept"
private const val CANCEL_REQUEST = "/paypal/cancel"

@Component
class PayPalBeanFactory(
        @Value("\${paypal.mode}") private val paypalMode: String,
        @Value("\${paypal.client}") private val paypalClient: String,
        @Value("\${paypal.secret}") private val paypalSecret: String) {

    @Bean
    fun createApiContext(): APIContext = APIContext(paypalClient, paypalSecret, paypalMode)
}

@Controller
class PayPalPaymentController @Autowired constructor(
        private val storage: PayPalPaymentStorage,
        private val context: APIContext) {

    @RequestMapping(ACCEPT_REQUEST)
    fun processAcceptedPayment(
            response: HttpServletResponse, @RequestParam paymentId: String, @RequestParam("PayerID") payer: String) {
        val paymentData = storage.getAndRemovePaymentWithId(paymentId)
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
    fun processCancelledPayment(response: HttpServletResponse, @RequestParam paymentId: String) {
        val paymentData = storage.getAndRemovePaymentWithId(paymentId)
        response.status = HttpServletResponse.SC_FOUND
        response.setHeader("Location", paymentData.cancelUri.toString())
    }
}

@Component
class PayPalJsonContext @Autowired constructor(
        internal val paymentStorage: PayPalPaymentStorage,
        internal val context: APIContext,
        servletContext: ServletContext,
        injectables: InjectableValues.Std) {

    internal val fullAcceptAddress: String
    internal val fullCancelAddress: String

    init {
        val serverAddress = "https://" + servletContext.virtualServerName + servletContext.contextPath
        fullAcceptAddress = serverAddress + ACCEPT_REQUEST
        fullCancelAddress = serverAddress + CANCEL_REQUEST
        injectables.addValue(PayPalJsonContext::class.java, this)
    }
}

class PayPalPaymentMethod @JsonCreator constructor(
        @JsonProperty("details", required = true) val details: PaymentData,
        @JsonProperty("successUri", required = true) val successUri: URI,
        @JsonProperty("cancelUri", required = true) val cancelUri: URI,
        @JacksonInject val context: PayPalJsonContext) : PaymentMethod() {

    override fun submit(): PaymentResult {
        val payment = Payment().apply {
            intent = "sale"
            transactions = details.items.map {
                Transaction().apply {
                    amount = Amount(it.price.currency, it.price.total)
                    description = it.name
                }
            }
            redirectUrls = RedirectUrls().apply {
                returnUrl = context.fullAcceptAddress
                cancelUrl = context.fullCancelAddress
            }
            paymentInstruction = PaymentInstruction(null, null, null, Currency(details.totalPrice.currency, details.totalPrice.total))
        }
        val created = payment.create(context.context)
        context.paymentStorage.registerPayment(PayPalPaymentData(created.id, successUri, cancelUri))
        return RedirectPaymentResult(URI(created.links.find { it.rel == "approval_url" }!!.href))
    }
}