package com.example.movie.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.movie.BuildConfig
import com.example.movie.data.remote.GithubApi
import com.example.movie.data.remote.VokinoApi
import com.example.movie.data.repository.MovieRepositoryImpl
import com.example.movie.data.settings.UserSettingsStore
import com.example.movie.data.update.ApkUpdateInstaller
import com.example.movie.data.update.GithubUpdateRepository
import com.example.movie.domain.model.UserSettingsRepository
import com.example.movie.domain.usecase.CheckAppUpdateUseCase
import com.example.movie.domain.usecase.GetHomeShelvesUseCase
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val userSettings: UserSettingsRepository = UserSettingsStore(context)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    private val api: VokinoApi = Retrofit.Builder()
        .baseUrl(BuildConfig.VOKINO_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(VokinoApi::class.java)

    private val movieRepository = MovieRepositoryImpl(
        api = api,
        token = BuildConfig.VOKINO_TOKEN,
    )

    val getHomeShelves: GetHomeShelvesUseCase = GetHomeShelvesUseCase(movieRepository)

    val getShelfMovies: GetShelfMoviesUseCase = GetShelfMoviesUseCase(movieRepository)

    val getRecommendations: GetRecommendationsUseCase = GetRecommendationsUseCase(movieRepository)

    val getMovieDetail: GetMovieDetailUseCase = GetMovieDetailUseCase(movieRepository)

    private val githubApi: GithubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(
            client.newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "AmigoPlay")
                            .header("Accept", "application/vnd.github+json")
                            .build(),
                    )
                }
                .build(),
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GithubApi::class.java)

    val checkAppUpdate: CheckAppUpdateUseCase = CheckAppUpdateUseCase(GithubUpdateRepository(githubApi))

    val apkInstaller: ApkUpdateInstaller = ApkUpdateInstaller(
        client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .build(),
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}
