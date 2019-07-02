package com.github.mangatmodi.mail.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import com.github.mangatmodi.mail.common.MailRequest
import com.github.mangatmodi.mail.config.ApplicationConfig
import com.github.mangatmodi.mail.dependency.MailServiceModule
import com.google.inject.Guice
import io.kotlintest.Duration
import io.kotlintest.eventually
import io.kotlintest.matchers.shouldBe
import io.kotlintest.seconds
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.net.InetAddress
import java.util.*

object Fixture {
    val hostname = InetAddress.getLocalHost().hostAddress
    val injector = Guice.createInjector(MailServiceModule())
    val kafkaConfig = injector.getInstance(ApplicationConfig.Kafka::class.java)
    val objectMapper = injector.getInstance(ObjectMapper::class.java)
    val faker = Faker.instance()
    val port = System.getenv("PORT") ?: 8080

    fun fakeEmail() = "${Fixture.faker.name().username()}@${Fixture.faker.name().username()}.com"

    internal fun <T> testRecordFromTopic(testFn: (String) -> T) {
        eventually {
            with(kafkaConsumer()) {
                subscribe(listOf(kafkaConfig.producer!!.topic!!))
                val record = poll(java.time.Duration.ofSeconds(10))
                    .records(kafkaConfig.producer!!.topic!!)
                    .first()
                    .value()
                    .orEmpty()
                close()
                testFn(record)
            }
        }
    }

    internal fun <T> testRecordFromTopic(topic: String, testFn: (String) -> T) {
        eventually(waitDuration = 20.seconds) {
            with(kafkaConsumer()) {
                subscribe(listOf(topic))
                val record = poll(java.time.Duration.ofSeconds(10))
                    .records(topic)
                    .first()
                    .value()
                    .orEmpty()
                close()
                testFn(record)
            }
        }
    }

    fun <T> sendRequest(body: T) = RestAssured.given()
        .baseUri("http://$hostname:$port/mail")
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(objectMapper.writeValueAsString(body))
        .post()

    fun kafkaConsumer(): KafkaConsumer<String, String> {
        val kafkaConsumerConfig = Properties().apply {
            put("bootstrap.servers", "$hostname:9092")
            put("group.id", "master-data-service-test")
            put("key.deserializer", StringDeserializer::class.java.name)
            put("value.deserializer", StringDeserializer::class.java.name)
            put("enable.auto.commit", "true")
            put("auto.offset.reset", "earliest")
            put("max.poll.records", 1)
        }
        return KafkaConsumer(kafkaConsumerConfig)
    }

    fun kafkaProducer(): KafkaProducer<String, String> {
        val kafkaProducerConfig = Properties().apply {
            put("bootstrap.servers", "$hostname:9092")
            put("client.id", "master-data-service-test")
            put("key.serializer", StringSerializer::class.java.name)
            put("value.serializer", StringSerializer::class.java.name)
        }
        return KafkaProducer(kafkaProducerConfig)
    }

    fun <T> eventually(waitDuration: Duration = 10.seconds, sleepMillis: Long = 500, f: () -> T) =
        eventually(waitDuration, Throwable::class.java) {
            try {
                f()
            } catch (e: Throwable) {
                Thread.sleep(sleepMillis)
                throw e
            }
        }

    fun <T> sendToKafka(topic: String, record: T) {
        kafkaProducer().send(
            ProducerRecord<String, String>(topic, UUID.randomUUID().toString(), objectMapper.writeValueAsString(record))
        )
        kafkaProducer().close()
    }

    // relies on randomness of test, rather than json parsing
    fun assertMailSent(mail: MailRequest) {
        eventually {
            val emails = RestAssured.given()
                .baseUri("http://$hostname:1080/api/emails")
                .get()
                .body.asString()

            emails.contains(mail.from) shouldBe true
            emails.contains(mail.subject) shouldBe true
            emails.contains(mail.content) shouldBe true
            }
        }
}
