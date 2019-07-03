package com.github.mangatmodi.mail.service.sendmail

import com.github.mangatmodi.mail.common.MailData
import com.google.inject.Inject
import io.reactivex.Single
import io.vertx.ext.mail.MailAttachment
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.mail.MailResult
import io.vertx.reactivex.ext.mail.MailClient

class SmtpService @Inject constructor(
    private val smtpClient: MailClient
) {
    fun sendMail(mailData: MailData): Single<MailResult>? {
        val message = MailMessage().apply {
            from = mailData.entity.from
            to = mailData.entity.to
            bcc = mailData.entity.bcc
            cc = mailData.entity.cc
            text = mailData.entity.content
            subject = mailData.entity.subject
        }

        mailData.attachments.forEach {
            val toAttach = MailAttachment().apply {
                data = it.buffer
                name = it.name
                contentType = it.contentType
            }
            message.setAttachment(toAttach)
        }

        return smtpClient.rxSendMail(message)
    }
}
