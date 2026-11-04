package com.example.movie.domain.model

sealed interface UpdateProbe {
    data class Available(val release: AppRelease) : UpdateProbe

    data object Current : UpdateProbe

    data object Unavailable : UpdateProbe
}
