// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import android.util.Base64

open class CloseLoad : ExtractorApi() {
    override val name            = "CloseLoad"
    override val mainUrl         = "https://closeload.filmmakinesi.film"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val ext_ref  = referer ?: ""
        Log.d("Kekik_${this.name}", "url » ${url}")

        val i_source = app.get(url, referer=ext_ref)

        i_source.document.select("track").forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = it.attr("label"),
                    url  = fixUrl(it.attr("src"))
                )
            )
        }

        val atob       = Regex("""aHR0[0-9a-zA-Z+\/=]*""").find(i_source.text)?.value ?: throw ErrorLoadingException("atob not found")
        // * Padding kontrolü ve ekleme
        val padding    = 4 - atob.length % 4
        val atobPadded = if (padding < 4) atob.padEnd(atob.length + padding, '=') else atob
        // * Base64 decode ve String'e dönüştürme
        val m3u_link   = String(Base64.decode(atobPadded, Base64.DEFAULT), charset("UTF-8"))

        Log.d("Kekik_${this.name}", "m3u_link » ${m3u_link}")

        callback.invoke(
            ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3u_link,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8  = true
            )
        )
    }
}