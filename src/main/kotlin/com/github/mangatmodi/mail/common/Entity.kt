package com.github.mangatmodi.mail.common

import com.fasterxml.jackson.annotation.JsonProperty
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
    fun toMailResponse(id: String) = MailResponse(UUID.fromString(id), from, to, cc, bcc, subject, content, attachments)
}

data class RequestError(val message: String)

data class MailResponse(
    @JsonProperty("request_id") val requestId: UUID,
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

data class KafkaRecord<T>(
    @JsonProperty("event_name") val eventName: String,
    val entity: T
)

enum class EventName(val value: String) {
    MAIL_CREATED("mail_created")
}
