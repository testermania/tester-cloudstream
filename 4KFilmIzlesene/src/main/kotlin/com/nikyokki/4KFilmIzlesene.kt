

package com.nikyokki

import android.util.Log
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.toRatingInt
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class `4KFilmIzlesene` : MainAPI() {
    override var mainUrl = "https://www.4kfilmizlesene.nl"
    override var name = "4KFilmİzlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    override var sequentialMainPageDelay       = 50L  // ? 0.05 saniye
    override var sequentialMainPageScrollDelay = 50L  // ? 0.05 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/"                               to "Yeni Filmler",
        "${mainUrl}/"                               to "Trend Filmler",
        "${mainUrl}/tur/aile-filmleri/"             to "Aile",
        "${mainUrl}/tur/aksiyon-filmleri/"          to "Aksiyon",
        "${mainUrl}/tur/animasyon-filmleri/"        to "Animasyon",
        "${mainUrl}/tur/belgesel/"                  to "Belgesel",
        "${mainUrl}/tur/bilim-kurgu-filmleri/"      to "Bilim Kurgu",
        //"${mainUrl}/tur/biyografi/"                 to "Biyografi",
        "${mainUrl}/tur/dram/"                      to "Dram",
        //"${mainUrl}/tur/editor/"                    to "Editör",
        "${mainUrl}/tur/fantastik-filmler/"         to "Fantastik",
       // "${mainUrl}/tur/genclik/"                   to "Gençlik",
        "${mainUrl}/tur/genel/"                     to "Genel",
        "${mainUrl}/tur/gerilim/"                   to "Gerilim",
        "${mainUrl}/tur/gizem-filmleri/"            to "Gizem",
        "${mainUrl}/tur/komedi-filmleri/"           to "Komedi",
        "${mainUrl}/tur/korku/"                     to "Korku",
        "${mainUrl}/tur/macera-filmleri/"           to "Macera",
       // "${mainUrl}/tur/muzik/"                     to "Müzik",
       // "${mainUrl}/tur/muzikal/"                   to "Müzikal",
        "${mainUrl}/tur/polisiye-filmler/"          to "Polisiye",
       // "${mainUrl}/tur/romantik/"                  to "Romantik",
        "${mainUrl}/tur/savas-filmleri/"            to "Savaş",
       // "${mainUrl}/tur/spor/"                      to "Spor",
        "${mainUrl}/tur/suc/"                       to "Suç",
        "${mainUrl}/tur/tarih/"                     to "Tarih",
        "${mainUrl}/tur/western-kovboy-filmleri/"   to "Western & Kovboy",
       // "${mainUrl}/tur/yerli-film-izle/"           to "Yerli"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}page/${page}").document
        if (request.name == "Trend Filmler"){
            val home = document.select("div.content-inside div").mapNotNull { it.toMainPageResult() }
            return newHomePageResponse(request.name, home)
        }
        val home = document.select("div.film-box").mapNotNull { it.toMainPageResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val title     = this.selectFirst("div.name")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        var score     = this.selectFirst("div.rating span")?.text()
        if (score == null) {
            score =  this.selectFirst("span.icon-star")?.text()?.trim()
        }
        if (this.selectFirst("div.img img")?.attr("data-lazy-src") == "" ||
            this.selectFirst("div.img img")?.attr("data-lazy-src") == null) {
            val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("src"))
            return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl
                this.score = Score.from10(score)
            }
        } else {
            val posterUrl = fixUrlNull(this.selectFirst("div.img img")?.attr("data-lazy-src"))
            return newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.score = Score.from10(score)}
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/?s=${query}").document

        return document.select("div.film-box").mapNotNull { it.toMainPageResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val orgTitle = document.selectFirst("div.film h1")?.text()?.trim() ?: return null
        val altTitle = document.selectFirst("div.original-name span")?.text()?.trim() ?: ""
        val title = if (altTitle.isNotEmpty()) "${orgTitle} - ${altTitle}" else orgTitle
        val poster = fixUrlNull(document.selectFirst("div.img img")?.attr("data-lazy-src"))
        val description = document.selectFirst("div.description")?.text()?.trim()
        val year =
            document.selectFirst("span[itemprop='dateCreated']")?.text()?.trim()?.toIntOrNull()
        val tags = document.select("div.category a[href*='-filmleri/']").map { it.text() }
        val rating =
            document.selectFirst("div.imdb-count")?.text()?.split(" ")?.first()?.trim()
        val actors = document.select("div.actors").map { it.text() }
        val trailer = document.selectFirst("div.container iframe")?.attr("src")

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = Score.from10(rating)
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("4KI", "data » ${data}")
        val document = app.get(data).document
        var iframe   = fixUrlNull(document.selectFirst("div.video-content iframe")?.attr("src")) ?: return false
        Log.d("4KI", "iframe » ${iframe}")

        loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        return true
    }
}
