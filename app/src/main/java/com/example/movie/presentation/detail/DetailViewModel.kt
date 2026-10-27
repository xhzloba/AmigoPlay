package com.example.movie.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.DataError
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieId: String,
    private val getMovieDetail: GetMovieDetailUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState(phase = DetailPhase.Loading)
            getMovieDetail(movieId)
                .onSuccess { detail ->
                    _state.value = DetailUiState(detail = detail, phase = DetailPhase.Ready)
                }
                .onFailure { error ->
                    _state.value = DetailUiState(phase = DetailPhase.Failed(error.asDataError()))
                }
        }
    }
}

private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.Unknown()
