package com.github.mangatmodi.mail.validation

import com.github.mangatmodi.mail.common.MailRequest
import org.apache.commons.validator.routines.EmailValidator

object MailRequestValidation {
    fun validate(mail: MailRequest): Boolean {
        val validEmails = (mail.to + mail.cc + mail.bcc + mail.from)
            .fold(true) { acc, el ->
                acc && EmailValidator.getInstance().isValid(el)
            }

        val validSubject = mail.subject.isNotBlank()
        val validContent = mail.content.isNotBlank()

        return validEmails && validSubject && validContent
    }
}
