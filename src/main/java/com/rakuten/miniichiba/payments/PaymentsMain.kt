package com.rakuten.miniichiba.payments

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
open class PaymentsMain

fun main(args: Array<String>) {
    SpringApplication.run(PaymentsMain::class.java, *args)
}

