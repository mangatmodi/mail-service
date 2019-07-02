package com.github.mangatmodi.mail.config

object ApplicationConfig {
    data class KafkaProducer(
        var clientId: String? = null,
        var maxBlockMs: Long? = null,
        var reqTimeoutMs: Long? = null,
        var topic: String? = null
    )

    data class KafkaConsumer(
        var groupId: String? = null,
        var topic: List<String>? = null
    )

    data class Kafka(
        var bootstrapServers: List<String>? = null,
        var producer: KafkaProducer? = null,
        var consumer: KafkaConsumer? = null
    )

    data class Service(
        var kafka: Kafka? = null,
        var sendMailService: SendMailService? = null
    )

    data class Smtp(var host: String? = null, var port: Int? = null)

    data class SendMailService(var smtp: Smtp? = null, var connectionPoolSize: Int? = null, var retry: Int? = null)

    data class Deployment(var port: Int? = null)
}
