package com.example.movie.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.movie.BuildConfig
import com.example.movie.data.alerts.ReleaseWatch
import com.example.movie.data.alerts.ReleaseWatchStore
import com.example.movie.data.favorites.FavoriteStore
import com.example.movie.data.favorites.WatchedStore
import com.example.movie.data.remote.FranchiseApi
import com.example.movie.data.remote.GithubApi
import com.example.movie.data.remote.VokinoApi
import com.example.movie.data.repository.MovieRepositoryImpl
import com.example.movie.data.history.ResumeStore
import com.example.movie.data.history.ViewHistoryStore
import com.example.movie.data.search.SearchHistoryStore
import com.example.movie.data.settings.UserSettingsStore
import com.example.movie.data.update.ApkUpdateInstaller
import com.example.movie.data.update.GithubUpdateRepository
import com.example.movie.AppForeground
import com.example.movie.domain.model.Franchise
import com.example.movie.domain.model.WatchCheck
import com.example.movie.presentation.alerts.ReleaseNotifier
import com.example.movie.domain.model.UserSettingsRepository
import com.example.movie.domain.usecase.CheckAppUpdateUseCase
import com.example.movie.domain.usecase.GetHomeShelvesUseCase
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import com.example.movie.domain.usecase.GetRecommendationsUseCase
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import com.example.movie.domain.usecase.SearchMoviesUseCase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val userSettings: UserSettingsRepository = UserSettingsStore(context)
    val favorites = FavoriteStore(context)
    val watched = WatchedStore(context)
    val searchHistory = SearchHistoryStore(context)
    val viewHistory = ViewHistoryStore(context)
    val resume = ResumeStore(context)
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

    private val franchiseApi: FranchiseApi = Retrofit.Builder()
        .baseUrl(BuildConfig.FRANCHISE_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FranchiseApi::class.java)

    private val movieRepository = MovieRepositoryImpl(
        api = api,
        franchiseApi = franchiseApi,
        token = BuildConfig.VOKINO_TOKEN,
        franchiseToken = BuildConfig.FRANCHISE_TOKEN,
        http = client,
    )

    val getHomeShelves: GetHomeShelvesUseCase = GetHomeShelvesUseCase(movieRepository)

    val getShelfMovies: GetShelfMoviesUseCase = GetShelfMoviesUseCase(movieRepository)

    val getRecommendations: GetRecommendationsUseCase = GetRecommendationsUseCase(movieRepository)

    val searchMovies: SearchMoviesUseCase = SearchMoviesUseCase(movieRepository)

    val getMovieDetail: GetMovieDetailUseCase = GetMovieDetailUseCase(movieRepository)

    val loadFranchise: suspend (String, String?, Int?) -> Franchise? = { id, title, year ->
        movieRepository.franchise(id, title, year)
    }

    val watchCheck: suspend (String, String?, Int?) -> WatchCheck = { id, title, year ->
        movieRepository.watchCheck(id, title, year)
    }

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

    val releaseWatch = ReleaseWatch(
        store = ReleaseWatchStore(context),
        movies = movieRepository,
        notify = { alert -> ReleaseNotifier.post(context, alert) },
        notifyShelf = { kind, count -> ReleaseNotifier.postShelf(context, kind, count) },
        foreground = { AppForeground.shown.get() },
    )

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
