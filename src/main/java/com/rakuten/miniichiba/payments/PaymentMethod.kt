package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI

data class Price(val total: String, val currency: String)

data class Item(val name: String, val price: Price)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "paymentResult")
@JsonSubTypes(JsonSubTypes.Type(value = RedirectPaymentResult::class, name = "redirect"))
abstract class PaymentResult

class RedirectPaymentResult(@JsonProperty val redirectURI: URI) : PaymentResult()

data class PaymentData(
        @JsonProperty(required = true) val totalPrice: Price, @JsonProperty(required = true) val items: List<Item>)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "paymentMethod")
@JsonSubTypes(JsonSubTypes.Type(value = PayPalPaymentMethod::class, name = "paypal"))
abstract class PaymentMethod {
    abstract fun submit(): PaymentResult
}
