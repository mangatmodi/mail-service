package com.github.mangatmodi.mail.dependency

import com.github.mangatmodi.mail.service.api.MailApiService
import com.github.mangatmodi.mail.service.sendmail.SendMailService
import com.google.inject.Guice

object MailServiceDependencies {
    private val injector by lazy { Guice.createInjector(MailServiceModule()) }
    val apiService by lazy { injector.getInstance(MailApiService::class.java) }
    val sendMailService by lazy { injector.getInstance(SendMailService::class.java) }
}
