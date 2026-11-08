package com.example.movie.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.WatchCheck
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieId: String,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val watchCheck: suspend (String, String?, Int?) -> WatchCheck,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()
    private var watchJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun resolveWatch() {
        val detail = _state.value.detail ?: return
        resolveWatch(detail)
    }

    private fun load() {
        watchJob?.cancel()
        viewModelScope.launch {
            _state.value = DetailUiState(phase = DetailPhase.Loading)
            getMovieDetail(movieId)
                .onSuccess { detail ->
                    _state.value = DetailUiState(detail = detail, phase = DetailPhase.Ready)
                    resolveWatch(detail)
                }
                .onFailure { error ->
                    _state.value = DetailUiState(phase = DetailPhase.Failed(error.asDataError()))
                }
        }
    }

    private fun resolveWatch(detail: MovieDetail) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            _state.value = _state.value.copy(franchiseReady = false, unavailable = false)
            val check = watchCheck(detail.id, detail.title, detail.year)
            val current = _state.value.detail
            if (current?.id != detail.id) return@launch
            val franchise = (check as? WatchCheck.Ready)?.franchise
            _state.value = _state.value.copy(
                detail = current.copy(franchise = franchise),
                franchiseReady = true,
                unavailable = check is WatchCheck.Missing || (franchise != null && !franchise.playable),
            )
        }
    }
}

private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.Unknown()
