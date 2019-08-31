package com.rakuten.miniichiba.payments

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import javax.servlet.http.HttpServletRequest

@Component
class ObjectMappingInjector {
    @Bean
    fun createObjectMapper(@Autowired injectables: InjectableValues.Std) = ObjectMapper().apply {
        injectableValues = injectables
    }

    @Bean
    fun createInjectables() = InjectableValues.Std()
}

@Controller
class PaymentRequestMapping @Autowired constructor(val mapper: ObjectMapper) {
    @PostMapping("/checkout", produces = ["application/json"])
    fun handleCheckoutRequest(
            request: HttpServletRequest) = mapper.readValue(request.reader, PaymentMethod::class.java).submit()
}