package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Component
class ObjectMappingInjector {
    @Bean
    fun createObjectMapper(@Autowired injectables: InjectableValues.Std) = ObjectMapper().apply {
        injectableValues = injectables
    }

    @Bean
    fun createInjectables() = InjectableValues.Std()
}

@RestController
class PaymentRequestMapping @Autowired constructor(val mapper: ObjectMapper) {
    @PostMapping("/checkout", produces = ["application/json"])
    fun handleCheckoutRequest(@RequestBody request: PaymentRequest) = request.submit()
}