package com.github.mangatmodi.mail.common

import java.lang.IllegalStateException

class InvalidTopicName(value: String) : IllegalArgumentException("Kafka topic:$value do not exists")
class DownloadError(url: String) : IllegalStateException("Unable to download link:$url")
