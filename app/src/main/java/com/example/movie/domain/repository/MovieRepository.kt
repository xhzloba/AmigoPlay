package com.example.movie.domain.repository

import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.MoviePage
import com.example.movie.domain.model.Shelf
import com.example.movie.domain.model.WatchCheck

interface MovieRepository {
    suspend fun getShelves(): Result<List<Shelf>>

    suspend fun getMovies(query: CatalogQuery, page: Int = 1, limit: Int? = null): Result<List<Movie>>

    /** Та же выдача, плюс `page.next`: есть следующая страница или лента кончилась. */
    suspend fun getMoviePage(query: CatalogQuery, page: Int = 1, limit: Int? = null): Result<MoviePage>

    suspend fun getRecommendations(): Result<List<Movie>>

    suspend fun search(name: String, page: Int = 1): Result<MoviePage>

    suspend fun getMovie(id: String): Result<MovieDetail>

    suspend fun franchise(id: String, title: String? = null, year: Int? = null): Franchise?

    suspend fun watchCheck(id: String, title: String? = null, year: Int? = null): WatchCheck
}
