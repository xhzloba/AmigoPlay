package com.example.movie.data.remote

import com.example.movie.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET

internal interface GithubApi {
    @GET("repos/xhzloba/AmigoPlay/releases/latest")
    suspend fun latest(): GithubReleaseDto
}
