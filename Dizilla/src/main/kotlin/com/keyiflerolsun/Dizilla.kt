// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors

class Dizilla : MainAPI() {
    override var mainUrl              = "https://dizilla.club"
    override var name                 = "Dizilla"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.TvSeries)

    // ! CloudFlare bypass
    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    // override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
    // override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    override val mainPage = mainPageOf(
        "${mainUrl}/tum-bolumler"          to "Altyazılı Bölümler",
        "${mainUrl}/dublaj-bolumler"       to "Dublaj Bölümler",
        "${mainUrl}/dizi-turu/aile"        to "Aile",
        "${mainUrl}/dizi-turu/aksiyon"     to "Aksiyon",
        "${mainUrl}/dizi-turu/bilim-kurgu" to "Bilim Kurgu",
        "${mainUrl}/dizi-turu/romantik"    to "Romantik",
        "${mainUrl}/dizi-turu/komedi"      to "Komedi"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}").document
        val home     = if (request.data.contains("dizi-turu")) { 
            document.select("div.new-added-list a").mapNotNull { it.diziler() }
        } else {
            document.select("div.grid a").mapNotNull { it.sonBolumler() }
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.diziler(): SearchResponse? {
        val title     = this.selectFirst("h2")?.text() ?: return null
        val href      = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    private suspend fun Element.sonBolumler(): SearchResponse? {
        val name     = this.selectFirst("h2")?.text() ?: return null
        val ep_name  = this.selectFirst("div.opacity-80")!!.text().replace(". Sezon ", "x").replace(". Bölüm", "")
        val title    = "${name} - ${ep_name}"

        val ep_doc    = app.get(this.attr("href")).document
        val href      = fixUrlNull(ep_doc.selectFirst("a.relative")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(ep_doc.selectFirst("img.imgt")?.attr("onerror")?.substringAfter("= '")?.substringBefore("';"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    private fun SearchItem.toSearchResponse(): SearchResponse? {
        return newTvSeriesSearchResponse(
            title ?: return null,
            "${mainUrl}/${slug}",
            TvType.TvSeries,
        ) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val main_req  = app.get(mainUrl)
        val main_page = main_req.document
        val c_key     = main_page.selectFirst("input[name='cKey']")?.attr("value") ?: return emptyList()
        val c_value   = main_page.selectFirst("input[name='cValue']")?.attr("value") ?: return emptyList()

        val veriler   = mutableListOf<SearchResponse>()

        Log.d("DZL", "query » ${query}")

        val search_req = app.post(
            "${mainUrl}/bg/searchcontent",
            data = mapOf(
                "cKey"       to c_key,
                "cValue"     to c_value,
                "searchterm" to query
            ),
            headers = mapOf(
                "Accept"           to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest"
            ),
            referer = "${mainUrl}/",
            cookies = mapOf(
                "showAllDaFull"   to "true",
                "PHPSESSID"       to main_req.cookies["PHPSESSID"].toString(),
            )
        ).parsedSafe<SearchResult>()

        Log.d("DZL", "search_req » ${search_req}")

        if (search_req?.data?.state != true) {
            throw ErrorLoadingException("Invalid Json response")
        }

        search_req.data.result?.forEach { search_item ->
            Log.d("DZL", "search_item » ${search_item}")
            veriler.add(search_item.toSearchResponse() ?: return@forEach)
        }

        return veriler     
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title       = document.selectFirst("div.page-top h1")?.text() ?: return null
        val poster      = fixUrlNull(document.selectFirst("div.page-top img")?.attr("src")) ?: fixUrlNull(document.selectFirst("div.page-top img")?.attr("data-src"))
        val year        = document.selectXpath("//span[text()='Yayın tarihi']//following-sibling::span").text().trim().split(" ").last().toIntOrNull()
        val description = document.selectFirst("div.mv-det-p")?.text()?.trim() ?: document.selectFirst("div.w-full div.text-base")?.text()?.trim()
        val tags        = document.select("[href*='dizi-turu']").map { it.text() }
        val rating      = document.selectFirst("a[href*='imdb.com'] span")?.text()?.trim().toRatingInt()
        val duration    = Regex("(\\d+)").find(document.select("div.gap-3 span.text-sm").get(1).text() ?: "")?.value?.toIntOrNull()
        val actors      = document.select("[href*='oyuncu']").map {
            Actor(it.text())
        }

        val episode_list = mutableListOf<Episode>()
        document.selectXpath("//div[contains(@class, 'gap-2')]/a[contains(@href, '-sezon')]").forEach {
            val ep_doc = app.get(fixUrlNull(it.attr("href")) ?: return@forEach).document
        
            ep_doc.select("div.episodes div.cursor-pointer").forEach ep@ { episodeElement ->
                val ep_name        = episodeElement.select("a").last()?.text()?.trim() ?: return@ep
                val ep_href        = fixUrlNull(episodeElement.selectFirst("a.opacity-60")?.attr("href")) ?: return@ep
                val ep_description = episodeElement.selectFirst("span.t-content")?.text()?.trim()
                val ep_poster      = ep_doc.selectFirst("img.object-cover")?.attr("src")
                val ep_episode     = episodeElement.selectFirst("a.opacity-60")?.text()?.toIntOrNull()
        
                val parent_div   = episodeElement.parent()
                val season_class = parent_div?.className()?.split(" ")?.find { it.startsWith("szn") }
                val ep_season    = season_class?.substringAfter("szn")?.toIntOrNull()
        
                episode_list.add(Episode(
                    data        = ep_href,
                    name        = ep_name,
                    season      = ep_season,
                    episode     = ep_episode,
                    description = ep_description,
                    posterUrl   = ep_poster
                ))
            }
        
            ep_doc.select("div.dub-episodes div.cursor-pointer").forEach ep_dub@ { dubEpisodeElement ->
                val ep_name        = dubEpisodeElement.select("a").last()?.text()?.trim() ?: return@ep_dub
                val ep_href        = fixUrlNull(dubEpisodeElement.selectFirst("a.opacity-60")?.attr("href")) ?: return@ep_dub
                val ep_description = dubEpisodeElement.selectFirst("span.t-content")?.text()?.trim()
                val ep_poster      = ep_doc.selectFirst("img.object-cover")?.attr("src")
                val ep_episode     = dubEpisodeElement.selectFirst("a.opacity-60")?.text()?.toIntOrNull()
        
                val parent_div   = dubEpisodeElement.parent()
                val season_class = parent_div?.className()?.split(" ")?.find { it.startsWith("szn") }
                val ep_season    = season_class?.substringAfter("szn")?.toIntOrNull()
        
                episode_list.add(Episode(
                    data        = ep_href,
                    name        = "${ep_name} Dublaj",
                    season      = ep_season,
                    episode     = ep_episode,
                    description = ep_description,
                    posterUrl   = ep_poster
                ))
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episode_list) {
            this.posterUrl = poster
            this.year      = year
            this.plot      = description
            this.tags      = tags
            this.rating    = rating
            this.duration  = duration
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("DZL", "data » ${data}")
        val document = app.get(data).document
        val iframes  = mutableSetOf<String>()

        val alternatifler = document.select("a[href*='player']")
        if (alternatifler.isEmpty()) {
            val iframe = fixUrlNull(document.selectFirst("div#playerLsDizilla iframe")?.attr("src")) ?: return false

            Log.d("DZL", "iframe » ${iframe}")

            loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        } else {
            alternatifler.forEach {
                val player_doc = app.get(fixUrlNull(it.attr("href")) ?: return@forEach).document
                val iframe     = fixUrlNull(player_doc.selectFirst("div#playerLsDizilla iframe")?.attr("src")) ?: return false

                if (iframe in iframes) { return@forEach }
                iframes.add(iframe)

                Log.d("DZL", "iframe » ${iframe}")

                loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            }
        }

        return true
    }
}
