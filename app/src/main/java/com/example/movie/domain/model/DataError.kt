package com.example.movie.domain.model

sealed class DataError : Exception() {
    class Network : DataError()
    class Http(val code: Int) : DataError()
    class Parsing : DataError()
    class Unknown : DataError()
}
