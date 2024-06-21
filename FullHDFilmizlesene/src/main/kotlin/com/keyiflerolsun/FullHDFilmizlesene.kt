// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import android.util.Base64
import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class FullHDFilmizlesene : MainAPI() {
    override var mainUrl              = "https://www.fullhdfilmizlesene.de"
    override var name                 = "FullHDFilmizlesene"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/en-cok-izlenen-filmler-izle-hd/"            to "En Çok izlenen Filmler",
        "${mainUrl}/filmizle/imdb-puani-yuksek-filmler-izle-1/" to "IMDB Puanı Yüksek Filmler",
        "${mainUrl}/filmizle/aile-filmleri-izle-2/"             to "Aile Filmleri",
        "${mainUrl}/filmizle/aksiyon-filmler-izle-1/"           to "Aksiyon Filmleri",
        "${mainUrl}/filmizle/animasyon-filmleri-izle-4/"        to "Animasyon Filmleri",
        "${mainUrl}/filmizle/belgesel-filmleri-izle-2/"         to "Belgeseller",
        "${mainUrl}/filmizle/bilim-kurgu-filmleri-izle-1/"      to "Bilim Kurgu Filmleri",
        "${mainUrl}/filmizle/bluray-filmler-izle-1/"            to "Blu Ray Filmler",
        "${mainUrl}/filmizle/cizgi-filmler-izle-1/"             to "Çizgi Filmler",
        "${mainUrl}/filmizle/dram-filmleri-izle/"               to "Dram Filmleri",
        "${mainUrl}/filmizle/fantastik-filmleri-izle-2/"        to "Fantastik Filmler",
        "${mainUrl}/filmizle/gerilim-filmleri-izle-3/"          to "Gerilim Filmleri",
        "${mainUrl}/filmizle/gizem-filmleri-izle/"              to "Gizem Filmleri",
        "${mainUrl}/filmizle/hint-filmler-fh-hd-izle/"          to "Hint Filmleri",
        "${mainUrl}/filmizle/komedi-filmleri-izle-2/"           to "Komedi Filmleri",
        "${mainUrl}/filmizle/korku-filmleri-izle-2/"            to "Korku Filmleri",
        "${mainUrl}/filmizle/macera-filmleri-izle-1/"           to "Macera Filmleri",
        "${mainUrl}/filmizle/muzikal-filmleri-izle/"            to "Müzikal Filmler",
        "${mainUrl}/filmizle/polisiye-filmleri-izle-1/"         to "Polisiye Filmleri",
        "${mainUrl}/filmizle/psikolojik-filmleri-izle/"         to "Psikolojik Filmler",
        "${mainUrl}/filmizle/romantik-filmler-izle-1/"          to "Romantik Filmler",
        "${mainUrl}/filmizle/savas-filmleri-izle-2/"            to "Savaş Filmleri",
        "${mainUrl}/filmizle/suc-filmleri-izle-3/"              to "Suç Filmleri",
        "${mainUrl}/filmizle/tarih-filmleri-izle/"              to "Tarih Filmleri",
        "${mainUrl}/filmizle/western-filmleri-izle/"            to "Western Filmler",
        "${mainUrl}/filmizle/yerli-filmler-izle-3/"             to "Yerli Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home     = document.select("li.film").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title     = this.selectFirst("span.film-title")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document

        return document.select("li.film").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title           = document.selectFirst("div[class=izle-titles]")?.text()?.trim() ?: return null
        val poster          = fixUrlNull(document.selectFirst("div img")?.attr("data-src"))
        val year            = document.selectFirst("div.dd a.category")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val description     = document.selectFirst("div.ozet-ic > p")?.text()?.trim()
        val tags            = document.select("a[rel='category tag']").map { it.text() }
        val rating          = document.selectFirst("div.puanx-puan")?.text()?.split(" ")?.last()?.toRatingInt()
        val duration        = document.selectFirst("span.sure")?.text()?.split(" ")?.get(0)?.trim()?.toIntOrNull()
        val trailer         = Regex("""embedUrl\": \"(.*)\"""").find(document.html())?.groupValues?.get(1)
        val actors          = document.select("div.film-info ul li:nth-child(2) a > span").map {
            Actor(it.text())
        }


        val recommendations = document.selectXpath("//div[span[text()='Benzer Filmler']]/following-sibling::section/ul/li").mapNotNull {
            val recName      = it.selectFirst("span.film-title")?.text() ?: return@mapNotNull null
            val recHref      = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src"))
            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl       = poster
            this.year            = year
            this.plot            = description
            this.tags            = tags
            this.rating          = rating
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
            addTrailer(trailer)
        }
    }

    private fun atob(s: String): String {
        return String(Base64.decode(s, Base64.DEFAULT))
    }

    private fun rtt(s: String): String {
        fun rot13Char(c: Char): Char {
            return when (c) {
                in 'a'..'z' -> ((c - 'a' + 13) % 26 + 'a'.code).toChar()
                in 'A'..'Z' -> ((c - 'A' + 13) % 26 + 'A'.code).toChar()
                else -> c
            }
        }

        return s.map { rot13Char(it) }.joinToString("")
    }

    private fun getVideoLinks(document: Document): List<Map<String, String>> {
        val script_element = document.select("script").firstOrNull { it.data().isNotEmpty() }
        val script_content = script_element?.data()?.trim() ?: return emptyList()

        val scx_data = Regex("scx = (.*?);").find(script_content)?.groupValues?.get(1) ?: return emptyList()
        val scx_map: SCXData = jacksonObjectMapper().readValue(scx_data)
        val keys = listOf("atom", "advid", "advidprox", "proton")

        val linkList = mutableListOf<Map<String, String>>()

        for (key in keys) {
            val t = when (key) {
                "atom" -> scx_map.atom?.sx?.t
                "advid" -> scx_map.advid?.sx?.t
                "advidprox" -> scx_map.advidprox?.sx?.t
                "proton" -> scx_map.proton?.sx?.t
                else -> null
            }

            when (t) {
                is List<*> -> {
                    val links = t.filterIsInstance<String>().map { link -> atob(rtt(link)) }
                    linkList.add(mapOf(key to links.joinToString(",")))
                }
                is Map<*, *> -> {
                    val links = t.mapValues { (_, value) ->
                        if (value is String) atob(rtt(value)) else ""
                    }
                    val safeLinks = links.mapKeys { (key, _) ->
                        key?.toString() ?: "Unknown"
                    }
                    linkList.add(safeLinks)
                }
            }
        }

        return linkList
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("FHD", "data » ${data}")
        val document    = app.get(data).document
        val video_links = getVideoLinks(document)
        Log.d("FHD", "video_links » ${video_links}")
        if (video_links.isEmpty()) return false


        for (video_map in video_links) {
            for ((_, value) in video_map) {
                val video_url = fixUrlNull(value) ?: continue
                loadExtractor(video_url, "${mainUrl}/", subtitleCallback, callback)
            }
        }

        return true
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SCXData(
        @JsonProperty("atom")      val atom: AtomData?      = null,
        @JsonProperty("advid")     val advid: AtomData?     = null,
        @JsonProperty("advidprox") val advidprox: AtomData? = null,
        @JsonProperty("proton")    val proton: AtomData?    = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AtomData(
        @JsonProperty("sx") var sx: SXData
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SXData(
        @JsonProperty("t") var t: Any
    )
}
