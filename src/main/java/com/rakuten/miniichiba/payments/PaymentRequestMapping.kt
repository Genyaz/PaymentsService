package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Component
class ObjectMappingInjector {
    @Bean
    fun createInjectables(mapper: ObjectMapper): InjectableValues.Std {
        val result = InjectableValues.Std()
        mapper.injectableValues = result
        return result
    }
}

@RestController
class PaymentRequestMapping {
    @PostMapping("/payments/checkout", produces = ["application/json"])
    fun handleCheckoutRequest(@RequestBody request: PaymentRequest) = request.submit()
}