package com.github.mangatmodi.mail.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mangatmodi.mail.common.EventName
import com.github.mangatmodi.mail.common.KafkaRecordInitial
import com.github.mangatmodi.mail.common.MailRequest
import com.github.mangatmodi.mail.common.MailResponse
import com.github.mangatmodi.mail.service.Fixture.fakeEmail
import com.github.mangatmodi.mail.service.Fixture.objectMapper
import com.github.mangatmodi.mail.service.Fixture.sendRequest
import com.github.mangatmodi.mail.service.Fixture.testRecordFromTopic
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.ShouldSpec

class TestMailAPI : ShouldSpec() {
    init {

        "MailApiService" {
            "When posted a correct message" {
                should("accept the message and write it in Kafka") {
                    val mail = MailRequest(
                        from = fakeEmail(),
                        to = listOf(fakeEmail(), fakeEmail()),
                        subject = "Test Mail",
                        content = "Test Mail",
                        attachments = listOf("http://msldmlwe")
                    )
                    sendRequest(mail).statusCode shouldBe 202
                    testRecordFromTopic {
                        with(objectMapper.readValue<KafkaRecordInitial<MailResponse>>(it)) {
                            this.entity.id shouldNotBe null
                            this.requestId shouldNotBe null
                            this.entity.toMailRequest() shouldBe mail
                            this.eventName shouldBe EventName.MAIL_CREATED.value
                        }
                    }
                }
            }
            "When message can't be parsed" {
                should("return 500 Http code") {
                    sendRequest("Test").statusCode shouldBe 500
                }
            }
            "When incorrect message is posted" {
                should("return 400 Http code") {
                    val mail = MailRequest(
                        from = "wrong email address",
                        to = listOf(fakeEmail(), fakeEmail()),
                        subject = "Test Mail",
                        content = "Test Mail"
                    )
                    sendRequest(mail).statusCode shouldBe 400
                }
            }
        }
    }
}
