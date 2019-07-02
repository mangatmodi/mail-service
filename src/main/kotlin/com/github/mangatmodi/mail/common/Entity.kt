package com.github.mangatmodi.mail.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.vertx.core.buffer.Buffer
import java.util.*

data class MailRequest(
    val from: String,
    val to: List<String>,
    val cc: List<String> = listOf(),
    val bcc: List<String> = listOf(),
    val subject: String,
    val content: String,
    val attachments: List<String> = listOf()
) {
    fun toMailResponse(id: UUID) = MailResponse(id, from, to, cc, bcc, subject, content, attachments)
}

data class RequestError(val message: String)

data class MailResponse(
    val id: UUID,
    val from: String,
    val to: List<String>,
    val cc: List<String> = listOf(),
    val bcc: List<String> = listOf(),
    val subject: String,
    val content: String,
    val attachments: List<String> = listOf()
) {
    fun toMailRequest() = MailRequest(from, to, cc, bcc, subject, content, attachments)
}

sealed class KafkaRecord<T> {
    abstract val eventName: String
    abstract val entity: T
    abstract val requestId: UUID // Distributed log tracing

    fun kafkaRecordWithRetry(): KafkaRecordWithRetry<T> = when (this) {
        is KafkaRecordInitial -> KafkaRecordWithRetry(entity, requestId, 0)
        is KafkaRecordWithRetry -> KafkaRecordWithRetry(entity, requestId, retry + 1)
    }
}

data class KafkaRecordInitial<T>(
    override val entity: T,
    @JsonProperty("request_id") override val requestId: UUID
) : KafkaRecord<T>() {
    @JsonProperty("event_name")
    override val eventName: String = EventName.MAIL_CREATED.value
}

data class KafkaRecordWithRetry<T>(
    override val entity: T,
    @JsonProperty("request_id") override val requestId: UUID,
    @JsonProperty("retry") val retry: Int = 0
) : KafkaRecord<T>() {
    @JsonProperty("event_name")
    override val eventName: String = EventName.MAIL_RETRY.value
}

enum class EventName(val value: String) {
    MAIL_CREATED("mail_created"), MAIL_RETRY("mail-retry")
}

enum class KafkaTopic(val value: String) {
    MAIL("mail"), REJECTED_MAIL("rejected-mail");

    companion object {
        fun from(value: String): KafkaTopic =
            values().firstOrNull { it.value == value } ?: throw InvalidTopicName(value)
    }
}

open class Attachment(
    val buffer: Buffer,
    val name: String? = null,
    val contentType: String? = null
)

object NoAttachment : Attachment(Buffer.buffer())

data class MailData(
    val entity: MailResponse,
    val attachments: List<Attachment>
)
