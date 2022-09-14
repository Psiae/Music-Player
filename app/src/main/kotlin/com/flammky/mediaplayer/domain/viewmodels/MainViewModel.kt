package com.flammky.mediaplayer.domain.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flammky.mediaplayer.data.repository.ProtoRepository
import com.flammky.common.kotlin.collection.mutable.doEachClear
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val stateHandle: SavedStateHandle,
  private val protoRepo: ProtoRepository
) : ViewModel() {

	val playbackControlShownHeight = mutableStateOf(0.dp)
	val bottomNavigationHeight = mutableStateOf(0.dp)

	val bottomNavigatorHeight = derivedStateOf {
		playbackControlShownHeight.value + bottomNavigationHeight.value
	}

  var savedBottomNavIndex = 0

  val appSettings = protoRepo.appSettingSF
  val pendingStorageGranted = mutableListOf<() -> Unit>()
	fun readPermissionGranted() = pendingStorageGranted.doEachClear()
}
