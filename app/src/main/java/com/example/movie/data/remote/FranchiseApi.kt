package com.example.movie.data.remote

import com.example.movie.data.remote.dto.FranchiseDto
import retrofit2.http.GET
import retrofit2.http.Query

internal interface FranchiseApi {
    @GET("franchise/details")
    suspend fun details(
        @Query("token") token: String,
        @Query("kinopoisk_id") kinopoiskId: Int,
    ): FranchiseDto
}
