package com.github.mangatmodi.mail.service.sendmail

import com.github.mangatmodi.mail.common.Attachment
import com.github.mangatmodi.mail.common.NoAttachment
import com.google.inject.Inject
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.vertx.reactivex.ext.web.client.WebClient

class DownloadService @Inject constructor(
    private val webClient: WebClient
) {
    fun downloadAttachment(urls: List<String>): Observable<Attachment> =
        if (urls.isNotEmpty()) {
            urls.toObservable().flatMap {
                webClient.getAbs(it)
                    .rxSend()
                    .map { res ->
                        val name = res.getHeader("Content-Disposition")
                        val contentType = res.getHeader("content-type")
                        Attachment(res.body().delegate, name, contentType)
                    }
                    .toObservable()
            }
        } else {
            Observable.just(NoAttachment)
        }
}
