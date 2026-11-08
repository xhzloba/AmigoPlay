package com.example.movie.data.remote

import com.example.movie.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Query

internal interface GithubApi {
    @GET("repos/xhzloba/AmigoPlay/releases/latest")
    suspend fun latest(): GithubReleaseDto

    @GET("repos/xhzloba/AmigoPlay/releases")
    suspend fun releases(@Query("per_page") perPage: Int = 20): List<GithubReleaseDto>
}
