package com.github.mangatmodi.mail.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.javafaker.Faker
import com.github.mangatmodi.mail.config.ApplicationConfig
import com.github.mangatmodi.mail.dependency.MailServiceModule
import com.google.inject.Guice
import io.kotlintest.Duration
import io.kotlintest.eventually
import io.kotlintest.seconds
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.net.InetAddress
import java.util.Properties

object Fixture {
    val hostname = InetAddress.getLocalHost().hostAddress
    val injector = Guice.createInjector(MailServiceModule())
    val kafkaConfig = injector.getInstance(ApplicationConfig.Kafka::class.java)
    val objectMapper = injector.getInstance(ObjectMapper::class.java)
    val faker = Faker.instance()

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

    fun <T> sendRequest(body: T) = RestAssured.given()
        .baseUri("http://$hostname:8080/mail")
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

    fun <T> eventually(waitDuration: Duration = 10.seconds, sleepMillis: Long = 500, f: () -> T) =
        eventually(waitDuration, Throwable::class.java) {
            try {
                f()
            } catch (e: Throwable) {
                Thread.sleep(sleepMillis)
                throw e
            }
        }
}
