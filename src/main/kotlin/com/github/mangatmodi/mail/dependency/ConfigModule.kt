package com.github.mangatmodi.mail.dependency

import com.github.mangatmodi.mail.config.ApplicationConfig
import com.google.inject.AbstractModule
import com.typesafe.config.ConfigBeanFactory
import com.typesafe.config.ConfigFactory

class ConfigModule : AbstractModule() {
    lateinit var service: ApplicationConfig.Service
    lateinit var deployment: ApplicationConfig.Deployment
    override fun configure() {
        val config = ConfigFactory.defaultApplication().resolve()
        service = ConfigBeanFactory.create(
            config.getConfig("ktor.service"),
            ApplicationConfig.Service::class.java
        )
        deployment = ConfigBeanFactory.create(
            config.getConfig("ktor.deployment"),
            ApplicationConfig.Deployment::class.java
        )

        bind(ApplicationConfig.Kafka::class.java).toInstance(service.kafka)
        bind(ApplicationConfig.Deployment::class.java).toInstance(deployment)
    }
}
