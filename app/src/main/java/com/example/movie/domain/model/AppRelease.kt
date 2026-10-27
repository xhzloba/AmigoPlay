package com.example.movie.domain.model

data class AppRelease(
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkUrl: String,
)
