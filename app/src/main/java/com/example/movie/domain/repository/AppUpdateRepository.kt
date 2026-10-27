package com.example.movie.domain.repository

import com.example.movie.domain.model.AppRelease

interface AppUpdateRepository {
    suspend fun newerThan(versionCode: Int): AppRelease?
}
