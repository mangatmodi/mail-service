package com.github.mangatmodi.mail.service.sendmail

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mangatmodi.mail.common.*
import com.github.mangatmodi.mail.config.ApplicationConfig
import com.google.inject.Inject
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toCompletable
import io.vertx.core.Future
import io.vertx.ext.mail.MailResult
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import java.util.*

class MailQueueService @Inject constructor(
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val kafkaProducer: KafkaProducer<String, String>,
    private val sendMailConfig: ApplicationConfig.SendMailService,
    private val kafkaConfig: ApplicationConfig.Kafka,
    private val downloadService: DownloadService,
    private val smtpService: SmtpService,
    private val objectMapper: ObjectMapper
) {
    private val logger = logger()
    private val topics = kafkaConfig.consumer!!.topic!!
    private val retryCount = sendMailConfig.retry!!

    fun start(startFuture: Future<Void>) {
        logger.info("Starting Mail Queue Service")

        kafkaConsumer
            .rxSubscribe(topics.toSet())
            .subscribe({
                logger.info("Successfully subscribed to $topics")
                startFuture.complete()
            }, {
                logger.error("Failed to subscribe to $topics", it)
                startFuture.fail(it)
            })

        kafkaConsumer
            .toObservable()
            .observeOn(ioScheduler)
            .map {
                logger.info("Processing record from ${it.topic()}")
                when (KafkaTopic.from(it.topic())) {
                    KafkaTopic.MAIL -> objectMapper.readValue<KafkaRecordInitial<MailResponse>>(it.value())
                    KafkaTopic.REJECTED_MAIL -> objectMapper.readValue<KafkaRecordWithRetry<MailResponse>>(it.value())
                }
            }
            .flatMap { record ->
                MDC.put(logger.REQUEST_KEY, record.requestId.toString())

                val attachments = downloadService.downloadAttachment(record.entity.attachments)
                    .onErrorResumeNext { ex: Throwable ->
                        logger.warn("Unable to download one of the ${record.entity.attachments}", ex)
                        val retryRecord = record.kafkaRecordWithRetry()
                        if (retryRecord.retry < retryCount) {
                            addToRetryQueue(retryRecord)
                        } else {
                            logger.info("Reached retry limit $retryCount for the record: $record")
                        }
                        Observable.just(NoAttachment)
                    }.toList()

                attachments
                    .flatMap { smtpService.sendMail(MailData(record.entity, it)) }
                    .map { Optional.of(it) }
                    .onErrorResumeNext { ex: Throwable ->
                        logger.warn("Unable to download one of the ${record.entity.attachments}", ex)
                        val retryRecord = record.kafkaRecordWithRetry()
                        if (retryRecord.retry < retryCount) {
                            addToRetryQueue(retryRecord)
                        } else {
                            logger.info("Reached retry limit $retryCount for the record: $record")
                        }
                        Single.fromCallable { Optional.empty<MailResult>() }
                    }
                    .toObservable()
            }
            .subscribe(
                { logger.info("Successfully send mail: $it") },
                { logger.warn("Stopped listening to Kafka", it) },
                { logger.warn("Stopped listening to Kafka") }
            )
    }

    private fun <T> addToRetryQueue(record: KafkaRecordWithRetry<T>) {
        val retryRecord = ProducerRecord(
            KafkaTopic.REJECTED_MAIL.value,
            UUID.randomUUID().toString(),
            objectMapper.writeValueAsString(record)
        )

        kafkaProducer.send(retryRecord) { _, exception ->
            if (exception != null) {
                logger.error("Unable to send kafka record: $record for retrying")
            } else {
                logger.info("Sent kafka record: $record for retrying")
            }
        }
            .toCompletable()
            .toSingleDefault(Unit)
            .subscribe()
    }
}
