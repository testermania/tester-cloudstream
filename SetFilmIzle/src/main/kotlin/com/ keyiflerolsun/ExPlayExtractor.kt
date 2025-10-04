// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class ExPlay : ExtractorApi() {
    override val name            = "ExPlay"
    override val mainUrl         = "https://explay.store"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        val partKey  = url.substringAfter("?partKey=").substringAfter("turkce").uppercase()
        @Suppress("NAME_SHADOWING") val url      = url.substringBefore("?partKey=")
        val iSource  = app.get(url, referer=extRef).text

        val videoUrl    = Regex("""videoUrl":"([^",]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("videoUrl not found")
        val videoServer = Regex("""videoServer":"([^",]+)""").find(iSource)?.groupValues?.get(1) ?: throw ErrorLoadingException("videoServer not found")
        val title       = if (partKey != "") partKey else Regex("""title":"([^",]+)""").find(iSource)?.groupValues?.get(1)?.split(".")?.last() ?: "Unknown"
        val m3uLink     = "${mainUrl}${videoUrl.replace("\\", "")}?s=${videoServer}"
        Log.d("Kekik_${this.name}", "m3uLink » $m3uLink")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = "${this.name} - $title",
                url     = m3uLink,
                ExtractorLinkType.M3U8
            ) {
                this.referer = url
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
