package com.example.movie.data.remote

import com.example.movie.data.remote.dto.CatalogResponseDto
import com.example.movie.data.remote.dto.MainResponseDto
import com.example.movie.data.remote.dto.ViewResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface VokinoApi {
    @GET("v2/main")
    suspend fun main(@Query("token") token: String): MainResponseDto

    @GET("v2/list")
    suspend fun list(
        @Query("sort") sort: String,
        @Query("type") type: String?,
        @Query("page") page: Int,
        @Query("token") token: String,
        @Query("uhd") uhd: Int? = null,
        @Query("dolbyvision") dolbyVision: Int? = null,
    ): CatalogResponseDto

    @GET("v2/recommendations/list")
    suspend fun recommendations(@Query("token") token: String): CatalogResponseDto

    @GET("v2/compilations/content/{id}")
    suspend fun compilation(
        @Path("id") id: String,
        @Query("token") token: String,
    ): CatalogResponseDto

    @GET("v2/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("token") token: String,
    ): CatalogResponseDto

    @GET("v2/view/{id}")
    suspend fun view(
        @Path("id") id: String,
        @Query("token") token: String,
    ): ViewResponseDto
}
