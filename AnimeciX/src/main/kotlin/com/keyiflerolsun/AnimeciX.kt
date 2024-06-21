// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer

class AnimeciX : MainAPI() {
    override var mainUrl              = "https://animecix.net"
    override var name                 = "AnimeciX"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Anime)

    override val mainPage = mainPageOf(
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=action&onlyStreamable=true"          to "Aksiyon",
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=sci-fi-fantasy&onlyStreamable=true"  to "Bilim Kurgu",
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=drama&onlyStreamable=true"           to "Dram",
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=mystery&onlyStreamable=true"         to "Gizem",
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=comedy&onlyStreamable=true"          to "Komedi",
        "${mainUrl}/secure/titles?type=series&order=user_score:desc&genre=horror&onlyStreamable=true"          to "Korku"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get("${request.data}&page=${page}&perPage=12").parsedSafe<Category>()

        val home     = response?.pagination?.data?.mapNotNull { anime ->
            newAnimeSearchResponse(
                anime.title,
                "${mainUrl}/secure/titles/${anime.id}?titleId=${anime.id}",
                TvType.Anime
            ) {
                this.posterUrl = fixUrlNull(anime.poster)
            }
        } ?: listOf<SearchResponse>()

        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("${mainUrl}/secure/search/${query}?limit=20").parsedSafe<Search>() ?: return listOf<SearchResponse>()

        return response.results.mapNotNull { anime ->
            newAnimeSearchResponse(
                anime.title,
                "${mainUrl}/secure/titles/${anime.id}?titleId=${anime.id}",
                TvType.Anime
            ) {
                this.posterUrl = fixUrlNull(anime.poster)
            }
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val response = app.get(url).parsedSafe<Title>() ?: return null

        val episodes = mutableListOf<Episode>()

        if (response.title.title_type == "anime") {
            for (sezon in 1..response.title.season_count) {
                val sezon_response = app.get("${url}&seasonNumber=${sezon}").parsedSafe<Title>() ?: return null
                for (video in sezon_response.title.videos) {
                    episodes.add(Episode(
                        data    = video.url,
                        name    = "${video.season_num}. Sezon ${video.episode_num}. Bölüm",
                        season  = video.season_num,
                        episode = video.episode_num
                    ))
                }
            }
        } else {
            if (response.title.videos.isNotEmpty() == true) {
                episodes.add(Episode(
                    data    = response.title.videos.first().url,
                    name    = "Filmi İzle",
                    season  = 1,
                    episode = 1
                ))
            }
        }


        return newTvSeriesLoadResponse(
            response.title.title,
            "${mainUrl}/secure/titles/${response.title.id}?titleId=${response.title.id}",
            TvType.Anime,
            episodes
        ) {
            this.posterUrl = fixUrlNull(response.title.poster)
            this.year      = response.title.year
            this.plot      = response.title.description
            this.tags      = response.title.tags.filterNotNull().map { it.name }
            this.rating    = response.title.rating.toRatingInt()
            addActors(response.title.actors.filterNotNull().map { Actor(it.name, fixUrlNull(it.poster)) })
            addTrailer(response.title.trailer)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("ACX", "data » ${data}")
        loadExtractor(data, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}