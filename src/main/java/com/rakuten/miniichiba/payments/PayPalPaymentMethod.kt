package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.annotation.JsonProperty
import com.paypal.api.payments.*
import com.paypal.base.rest.APIContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.context.support.SpringBeanAutowiringSupport
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

class PayPalPaymentMethod(
        @JsonProperty(required = true) val details: PaymentData,
        @JsonProperty(required = true) val successUri: URI,
        @JsonProperty(required = true) val cancelUri: URI) : PaymentMethod() {

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
                returnUrl = fullAcceptAddress
                cancelUrl = fullCancelAddress
            }
            paymentInstruction = PaymentInstruction(null, null, null, Currency(details.totalPrice.currency, details.totalPrice.total))
        }
        val created = payment.create(context)
        paymentStorage.registerPayment(PayPalPaymentData(created.id, successUri, cancelUri))
        return RedirectPaymentResult(URI(created.links.find { it.rel == "approval_url" }!!.href))
    }

    private companion object {
        @Autowired
        lateinit var paymentStorage: PayPalPaymentStorage

        @Autowired
        lateinit var context: APIContext

        @Autowired
        private lateinit var servletContext: ServletContext

        val fullAcceptAddress: String
        val fullCancelAddress: String

        init {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this)
            val serverAddress = "https://" + servletContext.virtualServerName + servletContext.contextPath
            fullAcceptAddress = serverAddress + ACCEPT_REQUEST
            fullCancelAddress = serverAddress + CANCEL_REQUEST
        }
    }
}