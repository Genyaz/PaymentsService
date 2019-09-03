package com.rakuten.miniichiba.payments

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalPayPalPaymentStorage: PayPalPaymentStorage {
    private val storage = ConcurrentHashMap<String, PayPalPaymentData>()

    override fun registerPayment(data: PayPalPaymentData) {
        storage[data.token] = data
    }

    override fun getAndRemovePaymentWithToken(token: String): PayPalPaymentData = storage.remove(token)!!
}