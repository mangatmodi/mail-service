package com.github.mangatmodi.mail.config

import com.github.mangatmodi.mail.dependency.ConfigModule
import com.google.inject.Guice
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.ShouldSpec

class ConfigModuleTest : ShouldSpec() {
    init {
        "ConfigModuleTest" {
            "When given the configuration in `application.conf` file" {
                should("read kafka config") {
                    val injector = Guice.createInjector(ConfigModule())
                    val kafkaConfig = injector.getInstance(ApplicationConfig.Kafka::class.java)
                    kafkaConfig.producer!!.topic shouldBe "mail"
                }
            }
        }
    }
}
