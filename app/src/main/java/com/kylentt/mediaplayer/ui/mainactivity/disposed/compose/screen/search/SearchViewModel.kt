package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.mediaplayer.disposed.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.disposed.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    val repository: SongRepositoryImpl
) : ViewModel() {

    private val _songList = MutableStateFlow(listOf<Song>())
    val songList get() = _songList.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLocalSongs().collect {
                _songList.value = it
            }
        }
    }
}