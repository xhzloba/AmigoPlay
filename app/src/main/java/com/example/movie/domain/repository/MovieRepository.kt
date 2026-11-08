package com.example.movie.domain.repository

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.Shelf
import com.example.movie.domain.model.WatchCheck

interface MovieRepository {
    suspend fun getShelves(): Result<List<Shelf>>

    suspend fun getMovies(query: CatalogQuery, page: Int = 1, limit: Int? = null): Result<List<Movie>>

    suspend fun getRecommendations(): Result<List<Movie>>

    suspend fun search(name: String): Result<List<Movie>>

    suspend fun getMovie(id: String): Result<MovieDetail>

    suspend fun franchise(id: String, title: String? = null, year: Int? = null): Franchise?

    suspend fun watchCheck(id: String, title: String? = null, year: Int? = null): WatchCheck
}
