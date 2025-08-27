package com.keyiflerolsun

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class Jfvid : ExtractorApi() {
    override val name          = "Jfvid"
    override val mainUrl       = "https://cnn-edition.jfvid.com"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val match = Regex("""<iframe src='([^']+)""").find(url)
        val videoUrl = match?.groupValues?.get(1)

        if (videoUrl != null) {
            val streamUrl = videoUrl.replace("/play/", "/stream/")

            callback.invoke(
                newExtractorLink(
                    source = "Jfvid",
                    name   = "Jfvid",
                    url    = streamUrl,
                    type   = INFER_TYPE
                )
            )
        }
    }
}
