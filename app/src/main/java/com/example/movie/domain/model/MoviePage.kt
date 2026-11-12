package com.example.movie.domain.model

data class MoviePage(
    val movies: List<Movie>,
    val hasMore: Boolean,
)
