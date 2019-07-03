package com.github.mangatmodi.mail.dependency

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.mangatmodi.mail.config.ApplicationConfig
import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Provides
import io.vertx.core.http.HttpClientOptions
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mail.MailClient
import io.vertx.reactivex.ext.web.client.WebClient
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import javax.inject.Singleton

class MailServiceModule : AbstractModule() {
    override fun configure() {
        install(ConfigModule())
    }

    @Provides
    @Singleton
    fun provideKafkaProducer(
        kafkaConfig: ApplicationConfig.Kafka
    ): KafkaProducer<String, String> {
        val properties = Properties().apply {
            put("bootstrap.servers", kafkaConfig.bootstrapServers!!.joinToString(","))
            put("key.serializer", StringSerializer::class.qualifiedName)
            put("value.serializer", StringSerializer::class.qualifiedName)
            put("client.id", kafkaConfig.producer?.clientId)
            put("max.block.ms", kafkaConfig.producer?.maxBlockMs.toString())
            put("request.timeout.ms", kafkaConfig.producer?.reqTimeoutMs.toString())
        }
        return KafkaProducer(properties)
    }

    @Provides
    @Singleton
    @Inject
    fun provideKafkaConsumer(
        kafkaConfig: ApplicationConfig.Kafka,
        vertx: Vertx
    ): KafkaConsumer<String, String> {
        val props = mapOf(
            "bootstrap.servers" to kafkaConfig.bootstrapServers!!.joinToString(","),
            "key.deserializer" to StringDeserializer::class.qualifiedName,
            "value.deserializer" to StringDeserializer::class.qualifiedName,
            "group.id" to kafkaConfig.consumer?.groupId,
            "auto.offset.reset" to "latest",
            "enable.auto.commit" to "true"
        )

        return KafkaConsumer.create(vertx, props)
    }

    @Provides
    @Singleton
    fun provideObjectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .apply {
            propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        }

    @Provides
    @Singleton
    fun provideVertx(): Vertx = Vertx.vertx()

    @Provides
    @Singleton
    @Inject
    fun provideWebClient(vertx: Vertx, sendMailConfig: ApplicationConfig.SendMailService): WebClient =
        WebClient.create(
            vertx,
            WebClientOptions(
                HttpClientOptions().setMaxPoolSize(sendMailConfig.connectionPoolSize!!)
            )
                .setFollowRedirects(true)
                .setTryUseCompression(true)
                .setVerifyHost(false) // don't do this on prod
                .setTrustAll(true)

        )

    @Provides
    @Singleton
    @Inject
    fun provideMailClient(vertx: Vertx, sendMailConfig: ApplicationConfig.SendMailService): MailClient =
        MailClient.createShared(
            vertx,
            MailConfig().apply {
                hostname = sendMailConfig.smtp!!.host!!
                port = sendMailConfig.smtp!!.port!!
                isTrustAll = true
            },
            "mail-service"
        )
}
