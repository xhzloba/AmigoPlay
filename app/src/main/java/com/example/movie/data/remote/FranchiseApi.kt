package com.example.movie.data.remote

import com.example.movie.data.remote.dto.FranchiseDto
import com.example.movie.data.remote.dto.FranchiseListDto
import retrofit2.http.GET
import retrofit2.http.Query

internal interface FranchiseApi {
    @GET("franchise/details")
    suspend fun details(
        @Query("token") token: String,
        @Query("kinopoisk_id") kinopoiskId: Int,
    ): FranchiseDto

    @GET("franchise/details")
    suspend fun detailsById(
        @Query("token") token: String,
        @Query("id") id: Int,
    ): FranchiseDto

    @GET("list")
    suspend fun list(
        @Query("token") token: String,
        @Query("name") name: String,
    ): FranchiseListDto
}
