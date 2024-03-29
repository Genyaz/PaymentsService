package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rakuten.miniichiba.payments.paypal.PayPalPaymentRequest
import java.net.URI

data class Price @JsonCreator constructor(
        @JsonProperty("value", required = true) val value: String,
        @JsonProperty("currency", required = true) val currency: String)

data class Item @JsonCreator constructor(
        @JsonProperty("name", required = true) val name: String,
        @JsonProperty("price", required = true) val price: Price,
        @JsonProperty("quantity", required = false) val quantity: String?)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "paymentResult")
@JsonSubTypes(JsonSubTypes.Type(value = RedirectPaymentResult::class, name = "redirect"),
        JsonSubTypes.Type(value = SuccessPaymentResult::class, name = "success"))
abstract class PaymentResult

class RedirectPaymentResult(@JsonProperty val redirectURI: URI) : PaymentResult()

class SuccessPaymentResult: PaymentResult()

data class PaymentData @JsonCreator constructor(
        @JsonProperty("token", required = true) val token: String,
        @JsonProperty("totalPrice", required = true) val totalPrice: Price,
        @JsonProperty("points", required = false) val points: Price?,
        @JsonProperty("items", required = true) val items: List<Item>)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "paymentMethod")
@JsonSubTypes(JsonSubTypes.Type(value = PayPalPaymentRequest::class, name = "paypal"))
abstract class PaymentRequest {
    abstract fun submit(): PaymentResult
}
