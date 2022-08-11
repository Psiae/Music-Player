package com.kylentt.mediaplayer.domain.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.kotlin.mutablecollection.doEachClear
import com.kylentt.musicplayer.common.kotlin.mutablecollection.forEachClear
import com.kylentt.musicplayer.core.app.permission.AndroidPermission
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
