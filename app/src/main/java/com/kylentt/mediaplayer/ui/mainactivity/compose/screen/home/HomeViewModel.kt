package com.kylentt.mediaplayer.ui.mainactivity.compose.screen.home

import androidx.lifecycle.ViewModel
import com.kylentt.mediaplayer.disposed.data.repository.SongRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SongRepositoryImpl
) : ViewModel() {



}