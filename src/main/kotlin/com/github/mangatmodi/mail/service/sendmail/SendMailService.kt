package com.github.mangatmodi.mail.service.sendmail

import com.github.mangatmodi.mail.config.ApplicationConfig
import com.google.inject.Inject
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.vertx.core.Future
import java.util.concurrent.TimeUnit

class SendMailService @Inject constructor(
    private val deployment: ApplicationConfig.Deployment,
    private val mailQueueService: MailQueueService
) {
    fun start() {
        val queueStatus = Future.future<Void>()
            mailQueueService.start(queueStatus)

            queueStatus.setHandler {
                if (it.failed()) {
                    System.exit(-1) // Calls shutdown hook
                }
            }

        // Health check endpoints
        val server = embeddedServer(Netty, port = deployment.port!!) {
            install(ContentNegotiation) {
                routing {
                    get("/health") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }.start(wait = true)

        Runtime.getRuntime().addShutdownHook(Thread {
            server.stop(1, 5, TimeUnit.SECONDS)
        })
    }
}
