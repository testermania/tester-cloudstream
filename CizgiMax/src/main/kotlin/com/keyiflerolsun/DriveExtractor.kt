// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLDecoder

open class Drive : ExtractorApi() {
    override var name            = "Drive"
    override var mainUrl         = "https://drive.google.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef  = referer ?: ""

        val docId        = Regex("""file\/d\/(.*)\/preview""").find(url)?.groupValues?.get(1) ?: throw IllegalArgumentException("docId not found")
        val getVideoLink = "https://drive.google.com/get_video_info?docid=${docId}&drive_originator_app=303"
        val iSource      = app.get(getVideoLink, referer=extRef).text

        val bakalim = Regex("""&fmt_stream_map=(.*)&url_encoded_fmt_stream_map""").find(iSource)?.groupValues?.get(1) ?: throw IllegalArgumentException("fmt_stream_map not found")
        val decoded = URLDecoder.decode(bakalim, "utf-8")
        val m3uLink = decoded.split("|").last()
        Log.d("Kekik_${this.name}", "m3uLink » ${m3uLink}")

        callback.invoke(
            ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink ?: throw ErrorLoadingException("m3u link not found"),
                referer = url,
                quality = Qualities.Unknown.value,
                type    = INFER_TYPE
            )
        )
    }
}
