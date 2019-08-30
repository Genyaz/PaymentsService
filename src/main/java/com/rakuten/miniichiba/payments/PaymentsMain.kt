package com.rakuten.miniichiba.payments

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class PaymentsMain

fun main(args: Array<String>) {
    SpringApplication.run(PaymentsMain::class.java, *args)
}

