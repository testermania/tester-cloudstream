// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class PixelDrain : ExtractorApi() {
    override val name            = "PixelDrain"
    override val mainUrl         = "https://pixeldrain.com"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val pixelId      = Regex("""([^/]+)(?=\?download)""").find(url)?.groupValues?.get(1)
        val downloadLink = "${mainUrl}/api/file/${pixelId}?download"

        callback.invoke(
            newExtractorLink(
                source  = "pixeldrain - $pixelId",
                name    = "pixeldrain - $pixelId",
                url     = downloadLink,
                type    = INFER_TYPE
            ) {
                this.referer = "${mainUrl}/u/${pixelId}?download"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
