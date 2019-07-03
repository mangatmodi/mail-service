package com.github.mangatmodi.mail.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
val Logger.REQUEST_KEY: String get() = "X-Request-ID"

fun String?.toUUID(): UUID = this?.let { UUID.fromString(it) } ?: UUID.randomUUID()
