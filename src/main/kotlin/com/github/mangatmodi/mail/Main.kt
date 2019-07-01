package com.github.mangatmodi.mail

import com.github.mangatmodi.mail.dependency.MailServiceDependencies

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            MailServiceDependencies.apiService.start()
        }
    }
}
