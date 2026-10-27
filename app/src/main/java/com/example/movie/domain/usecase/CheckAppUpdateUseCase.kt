package com.example.movie.domain.usecase

import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.repository.AppUpdateRepository

class CheckAppUpdateUseCase(
    private val repository: AppUpdateRepository,
) {
    suspend operator fun invoke(versionCode: Int): AppRelease? = repository.newerThan(versionCode)
}
