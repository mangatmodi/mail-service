package com.github.mangatmodi.mail.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mangatmodi.mail.common.*
import com.github.mangatmodi.mail.service.Fixture.assertMailSent
import com.github.mangatmodi.mail.service.Fixture.fakeEmail
import com.github.mangatmodi.mail.service.Fixture.objectMapper
import com.github.mangatmodi.mail.service.Fixture.testRecordFromTopic
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.ShouldSpec
import java.util.*

class TestSendMailService : ShouldSpec() {
    override val oneInstancePerTest = true

    init {

        "SendMailService" {
            "When reading a correct mail request" {
                should("send the mail with attachments") {
                    val mail = MailRequest(
                        from = fakeEmail(),
                        to = listOf(fakeEmail(), fakeEmail()),
                        subject = UUID.randomUUID().toString(),
                        content = UUID.randomUUID().toString(),
                        attachments = listOf("https://www.w3.org/TR/PNG/iso_8859-1.txt") // TODO: Use mockserver
                    )

                    Fixture.sendToKafka(
                        KafkaTopic.MAIL.value,
                        KafkaRecordInitial(mail.toMailResponse(UUID.randomUUID()), UUID.randomUUID())
                    )

                    Thread.sleep(1000)
                    assertMailSent(mail)
                }
            }
            "When the download couldn't be processed" {
                should("add the message in retry queue") {
                    val mail = MailRequest(
                        from = fakeEmail(),
                        to = listOf(fakeEmail(), fakeEmail()),
                        subject = "Test Mail",
                        content = "Test Mail",
                        attachments = listOf("https://ey8eh389d38yd3") // dns will fail
                    )

                    Fixture.sendToKafka(
                        KafkaTopic.MAIL.value,
                        KafkaRecordInitial(mail.toMailResponse(UUID.randomUUID()), UUID.randomUUID())
                    )

                    testRecordFromTopic(KafkaTopic.REJECTED_MAIL.value) {
                        with(objectMapper.readValue<KafkaRecordWithRetry<MailResponse>>(it)) {
                            this.entity.id shouldNotBe null
                            this.requestId shouldNotBe null
                            this.entity.toMailRequest() shouldBe mail
                            this.eventName shouldBe EventName.MAIL_RETRY.value
                        }
                    }
                }
            }
            "When the retry queue has new records" {
                should("read the records and retry") {
                    val mail = MailRequest(
                        from = fakeEmail(),
                        to = listOf(fakeEmail(), fakeEmail()),
                        subject = "Test Mail",
                        content = "Test Mail",
                        attachments = listOf("https://ey8eh389d38yd3") // dns will fail
                    )

                    Fixture.sendToKafka(
                        KafkaTopic.REJECTED_MAIL.value,
                        KafkaRecordWithRetry(mail.toMailResponse(UUID.randomUUID()), UUID.randomUUID(), 0)
                    )
                    testRecordFromTopic(KafkaTopic.REJECTED_MAIL.value) {
                        with(objectMapper.readValue<KafkaRecordWithRetry<MailResponse>>(it)) {
                            this.entity.id shouldNotBe null
                            this.requestId shouldNotBe null
                            this.entity.toMailRequest() shouldBe mail
                            this.eventName shouldBe EventName.MAIL_RETRY.value
                            this.retry shouldBe 1
                        }
                    }
                }
            }
        }
    }
}
