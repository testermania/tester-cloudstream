// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import java.util.Locale
import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class BelgeselX : MainAPI() {
    override var mainUrl              = "https://belgeselx.com"
    override var name                 = "BelgeselX"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Documentary)

    override val mainPage = mainPageOf(
        "${mainUrl}/konu/turk-tarihi-belgeselleri&page=" to "Türk Tarihi",
        "${mainUrl}/konu/tarih-belgeselleri&page="		 to "Tarih",
        "${mainUrl}/konu/seyehat-belgeselleri&page="	 to "Seyahat",
        "${mainUrl}/konu/seri-belgeseller&page="		 to "Seri",
        "${mainUrl}/konu/savas-belgeselleri&page="		 to "Savaş",
        "${mainUrl}/konu/sanat-belgeselleri&page="		 to "Sanat",
        "${mainUrl}/konu/psikoloji-belgeselleri&page="	 to "Psikoloji",
        "${mainUrl}/konu/polisiye-belgeselleri&page="	 to "Polisiye",
        "${mainUrl}/konu/otomobil-belgeselleri&page="	 to "Otomobil",
        "${mainUrl}/konu/nazi-belgeselleri&page="		 to "Nazi",
        "${mainUrl}/konu/muhendislik-belgeselleri&page=" to "Mühendislik",
        "${mainUrl}/konu/kultur-din-belgeselleri&page="	 to "Kültür Din",
        "${mainUrl}/konu/kozmik-belgeseller&page="		 to "Kozmik",
        "${mainUrl}/konu/hayvan-belgeselleri&page="		 to "Hayvan",
        "${mainUrl}/konu/eski-tarih-belgeselleri&page="	 to "Eski Tarih",
        "${mainUrl}/konu/egitim-belgeselleri&page="		 to "Eğitim",
        "${mainUrl}/konu/dunya-belgeselleri&page="		 to "Dünya",
        "${mainUrl}/konu/doga-belgeselleri&page="		 to "Doğa",
        "${mainUrl}/konu/bilim-belgeselleri&page="		 to "Bilim"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("div.gen-movie-contain").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun String.toTitleCase(): String {
        val locale = Locale("tr", "TR")
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("h3 a")?.text()?.trim()?.toTitleCase() ?: return null
        val href      = fixUrlNull(this.selectFirst("h3 a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.Documentary) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return mutableListOf<SearchResponse>()
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("h2.gen-title")?.text()?.trim()?.toTitleCase() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.gen-tv-show-top img")?.attr("src")) ?: return null
        val description = document.selectFirst("div.gen-single-tv-show-info p")?.text()?.trim()
        val tags        = document.select("div.gen-socail-share a[href*='belgeselkanali']").map { it.attr("href").split("/").last().replace("-", " ").toTitleCase() }

        var counter  = 0
        val episodes = document.select("div.gen-movie-contain").mapNotNull {
            val epName     = it.selectFirst("div.gen-movie-info h3 a")?.text()?.trim() ?: return@mapNotNull null
            val epHref     = fixUrlNull(it.selectFirst("div.gen-movie-info h3 a")?.attr("href")) ?: return@mapNotNull null

            val seasonName = it.selectFirst("div.gen-single-meta-holder ul li")?.text()?.trim() ?: ""
            var epEpisode  = Regex("""Bölüm (\d+)""").find(seasonName)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            var epSeason   = Regex("""Sezon (\d+)""").find(seasonName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

            if (epEpisode == 0) {
                epEpisode = counter++
            }

            Episode(
                data    = epHref,
                name    = epName,
                season  = epSeason,
                episode = epEpisode
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.Documentary, episodes) {
            this.posterUrl = poster
            this.plot      = description
            this.tags      = tags
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("BLX", "data » ${data}")
        val source = app.get(data)

        Regex("""<iframe\s+[^>]*src=\\\"([^\\\"']+)\\\"""").findAll(source.text).forEach {
            val alternatifUrl  = it.groupValues[1]
            Log.d("BLX", "alternatifUrl » ${alternatifUrl}")
            val alternatifResp = app.get(alternatifUrl, referer=data)

            if (alternatifUrl.contains("new4.php")) {
                Regex("""file:\"([^\"]+)\", label: \"([^\"]+)""").findAll(alternatifResp.text).forEach {
                    var thisName  = this.name
                    var videoUrl  = it.groupValues[1]
                    var quality   = it.groupValues[2]
                    if (quality == "FULL") {
                        quality   = "1080p"
                        thisName  = "Google"
                    }
                    Log.d("BLX", "quality » ${quality}")
                    Log.d("BLX", "videoUrl » ${videoUrl}")

                    callback.invoke(
                        ExtractorLink(
                            source  = thisName,
                            name    = thisName,
                            url     = videoUrl,
                            referer = data,
                            quality = getQualityFromName(quality),
                            type    = INFER_TYPE
                        )
                    )
                }
            } else {
                val iframe = fixUrlNull(alternatifResp.document.selectFirst("iframe")?.attr("src")) ?: return@forEach
                Log.d("BLX", "iframe » ${iframe}")

                loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            }
        }

        return true
    }
}
