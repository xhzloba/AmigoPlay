package com.example.movie.domain.repository

import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.UpdateProbe

interface AppUpdateRepository {
    suspend fun newerThan(versionCode: Int): AppRelease?

    suspend fun lookup(versionCode: Int): UpdateProbe
}
