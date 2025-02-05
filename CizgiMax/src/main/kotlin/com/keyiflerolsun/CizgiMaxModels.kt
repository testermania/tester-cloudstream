// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import com.fasterxml.jackson.annotation.JsonProperty


data class SearchResult(
    @JsonProperty("data") val data: SearchData?
)

data class SearchData(
    @JsonProperty("result") val result: List<SearchItem>? = arrayListOf(),
)

data class SearchItem(
    @JsonProperty("s_link")  val s_link: String,
    @JsonProperty("s_name")  val s_name: String,
    @JsonProperty("s_image") val s_image: String,
)