package com.rakuten.miniichiba.payments.paypal

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisPayPalPaymentStorage @Autowired constructor(connFactory: RedisConnectionFactory) : PayPalPaymentStorage {
    private val ops = RedisTemplate<String, PayPalPaymentData>().apply {
        connectionFactory = connFactory
        keySerializer = RedisSerializer.string()
        afterPropertiesSet()
    }

    private companion object {
        const val KEY_PREFIX = "paypal:token:"
    }

    override fun getAndRemovePaymentWithToken(token: String): PayPalPaymentData? {
        val key = KEY_PREFIX + token
        val result = ops.opsForValue().get(key)
        if (result != null)
            ops.delete(key)
        return result
    }

    override fun registerPayment(data: PayPalPaymentData) {
        ops.opsForValue().set(KEY_PREFIX + data.token, data, Duration.ofMinutes(20))
    }
}