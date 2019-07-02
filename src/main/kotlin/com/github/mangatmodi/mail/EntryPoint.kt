package com.github.mangatmodi.mail

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.mangatmodi.mail.EntryPoint.Service.*
import com.github.mangatmodi.mail.dependency.MailServiceDependencies

class EntryPoint : CliktCommand(printHelpOnEmptyArgs = true) {
    enum class Service {
        API, SEND_MAIL
    }

    private val service: String? by option("-s", "--service", help = "Service to start: API | SEND_MAIL")

    override fun run() {
        when (valueOf(this.service!!)) {
            API -> MailServiceDependencies.apiService.start()
            SEND_MAIL -> MailServiceDependencies.sendMailService.start()
        }
    }
}
