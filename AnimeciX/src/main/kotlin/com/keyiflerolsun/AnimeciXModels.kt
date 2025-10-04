// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.fasterxml.jackson.annotation.JsonProperty


data class Category(
    @JsonProperty("pagination") val pagination: Pagination,
)

data class Search(
    @JsonProperty("results") val results: List<AnimeSearch>,
)

data class Title(
    @JsonProperty("title") val title: Anime,
)

data class Pagination(
    @JsonProperty("current_page") val currentPage: Int,
    @JsonProperty("last_page") val lastPage: Int,
    @JsonProperty("per_page") val perPage: Int,
    @JsonProperty("data") val data: List<AnimeSearch>,
    @JsonProperty("total") val total: Int,
)

data class AnimeSearch(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title_type") val titleType: String,
    @JsonProperty("name") val title: String,
    @JsonProperty("poster") val poster: String?,
)

data class Anime(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title_type") val titleType: String,
    @JsonProperty("name") val title: String,
    @JsonProperty("poster") val poster: String,
    @JsonProperty("description") val description: String,
    @JsonProperty("year") val year: Int?,
    @JsonProperty("mal_vote_average") val rating: String?,
    @JsonProperty("genres") val tags: List<Genre>,
    @JsonProperty("trailer") val trailer: String?,
    @JsonProperty("credits") val actors: List<Credit>,
    @JsonProperty("season_count") val seasonCount: Int, //Unrealiable?
    @JsonProperty("seasons") val seasons: List<Season>,
    @JsonProperty("videos") val videos: List<Video>
)

data class LastEpisode(
    @JsonProperty("title_id") val titleId: Int,
    @JsonProperty("title_name") val titleName: String,
    @JsonProperty("title_poster") val titlePoster: String?,
    @JsonProperty("season_number") val seasonNumber: Int,
    @JsonProperty("episode_number") val episodeNumber: Int
)

data class LastEpisodesResponse(
    @JsonProperty("data") val data: List<LastEpisode>
)

data class Genre(
    @JsonProperty("display_name") val name: String,
)

data class Credit(
    @JsonProperty("name") val name: String,
    @JsonProperty("poster") val poster: String?,
)

data class Video(
    @JsonProperty("episode_num") val episodeNum: Int?,
    @JsonProperty("season_num") val seasonNum: Int?,
    @JsonProperty("url") val url: String,
)

data class TitleVideos(
    @JsonProperty("videos") val videos: List<Video>
)

data class Season(@JsonProperty("number") val number: Int)
