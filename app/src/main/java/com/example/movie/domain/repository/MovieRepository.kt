package com.example.movie.domain.repository

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.Shelf

interface MovieRepository {
    suspend fun getShelves(): Result<List<Shelf>>

    suspend fun getMovies(query: CatalogQuery, page: Int = 1): Result<List<Movie>>

    suspend fun getRecommendations(): Result<List<Movie>>

    suspend fun getMovie(id: String): Result<MovieDetail>
}
