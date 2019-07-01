package com.github.mangatmodi.mail.config

object ApplicationConfig {
    data class KafkaProducer(
        var clientId: String? = null,
        var maxBlockMs: Long? = null,
        var reqTimeoutMs: Long? = null,
        var topic: String? = null
    )

    data class Kafka(
        var bootstrapServers: List<String>? = null,
        var producer: KafkaProducer? = null
    )

    data class Service(var kafka: Kafka? = null)

    data class Deployment(var port: Int? = null)
}
