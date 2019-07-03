package com.github.mangatmodi.mail.service.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mangatmodi.mail.common.*
import com.github.mangatmodi.mail.config.ApplicationConfig
import com.github.mangatmodi.mail.validation.MailRequestValidation
import com.google.common.io.Resources
import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callIdMdc
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.TimeUnit

internal const val SWAGGER_FILE_NAME = "swagger.yml"

class MailApiService @Inject constructor(
    private val kafkaProducer: KafkaProducer<String, String>,
    private val objectMapper: ObjectMapper,
    private val kafkaConfig: ApplicationConfig.Kafka,
    private val deployment: ApplicationConfig.Deployment
) {
    private val logger = logger()
    fun start() {
        val server = embeddedServer(Netty, port = deployment.port!!) {
            install(CallLogging) {
                callIdMdc(this@MailApiService.logger.REQUEST_KEY)
            }
            install(CallId) {
                generate {
                    it.request.header(logger.REQUEST_KEY) ?: UUID.randomUUID().toString()
                }
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
                routing {
                    post("/mail") {
                        val input = call.receive<MailRequest>()
                        logger.info("Request received to send mail ${MDC.get(logger.REQUEST_KEY)}:")
                        if (!MailRequestValidation.validate(input)) {
                            logger.warn("Invalid data format in the request $input:")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                RequestError("Invalid data in request")
                            )
                            this.finish()
                        } else {
                            val response = input.toMailResponse(UUID.randomUUID())
                            call.respond(HttpStatusCode.Accepted, response)
                            launch {
                                val record = ProducerRecord(
                                    kafkaConfig.producer!!.topic!!,
                                    UUID.randomUUID().toString(), // random key
                                    objectMapper.writeValueAsString(
                                        KafkaRecordInitial(response, MDC.get(logger.REQUEST_KEY).toUUID())
                                    )
                                )
                                kafkaProducer.send(record) { _, exception ->
                                    if (exception != null) {
                                        logger.error("Unable to send kafka record $record:")
                                    } else {
                                        logger.debug("Saved kafka record $record:")
                                    }
                                }
                            }
                        }
                    }

                    get("/health") {
                        call.respond(HttpStatusCode.OK)
                    }

                    get("/doc") {
                        call.respondText(swaggerText(), ContentType.Text.Plain)
                    }
                }
            }
        }.start(wait = true)

        Runtime.getRuntime().addShutdownHook(Thread {
            server.stop(1, 5, TimeUnit.SECONDS)
        })
    }

    private fun swaggerText(): String {
        return try {
            val url = Resources.getResource(SWAGGER_FILE_NAME)
            Resources.toString(url, Charsets.UTF_8) ?: "$SWAGGER_FILE_NAME not found in resources"
        } catch (e: Throwable) {
            logger.warn("Unable to read $SWAGGER_FILE_NAME", e)
            "Unable to read $SWAGGER_FILE_NAME"
        }
    }
}
