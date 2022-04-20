package com.kylentt.mediaplayer.domain.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val stateHandle: SavedStateHandle,
  private val protoRepo: ProtoRepository
) : ViewModel() {

  val appSettings = protoRepo.appSettingSF
  
  val pendingStorageGranted = mutableStateListOf<() -> Unit>()
}
