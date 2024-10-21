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
    @JsonProperty("current_page") val current_page: Int,
    @JsonProperty("last_page") val last_page: Int,
    @JsonProperty("per_page") val per_page: Int,
    @JsonProperty("data") val data: List<AnimeSearch>,
    @JsonProperty("total") val total: Int,
)

data class AnimeSearch(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title_type") val title_type: String,
    @JsonProperty("name") val title: String,
    @JsonProperty("poster") val poster: String,
)

data class Anime(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title_type") val title_type: String,
    @JsonProperty("name") val title: String,
    @JsonProperty("poster") val poster: String,
    @JsonProperty("description") val description: String,
    @JsonProperty("year") val year: Int?,
    @JsonProperty("mal_vote_average") val rating: String?,
    @JsonProperty("genres") val tags: List<Genre>,
    @JsonProperty("trailer") val trailer: String?,
    @JsonProperty("credits") val actors: List<Credit>,
    @JsonProperty("season_count") val season_count: Int,
    @JsonProperty("videos") val videos: List<Video>
)

data class Genre(
    @JsonProperty("display_name") val name: String,
)

data class Credit(
    @JsonProperty("name") val name: String,
    @JsonProperty("poster") val poster: String?,
)

data class Video(
    @JsonProperty("episode_num") val episode_num: Int?,
    @JsonProperty("season_num") val season_num: Int?,
    @JsonProperty("url") val url: String,
)