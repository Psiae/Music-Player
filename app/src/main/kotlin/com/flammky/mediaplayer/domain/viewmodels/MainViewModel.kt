package com.flammky.mediaplayer.domain.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flammky.common.kotlin.collection.mutable.doEachClear
import com.flammky.mediaplayer.data.repository.ProtoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val stateHandle: SavedStateHandle,
  private val protoRepo: ProtoRepository
) : ViewModel() {

	val playbackControlShownHeight = mutableStateOf(0.dp)
	val bottomNavigationHeight = mutableStateOf(0.dp)

	val bottomVisibilityHeight = bottomNavigationHeight.derive(
		{ stateValues ->
			var sum = 0.dp
			for (value in stateValues) {
				sum += value
			}
			sum.also { Timber.d("bottomVisibilityHeight derived to $it") }
		}, playbackControlShownHeight
	)

  var savedBottomNavIndex = 0

  val appSettings = protoRepo.appSettingSF
  val pendingStorageGranted = mutableListOf<() -> Unit>()
	fun readPermissionGranted() = pendingStorageGranted.doEachClear()

	private fun <T> State<T>.derive(
		calculation: (T) -> T = { it }
	): State<T> {
		return derivedStateOf { calculation(value) }
	}

	private fun <T> State<T>.derive(
		calculation: (List<T>) -> T = { it[0] },
		vararg others: State<T>,
	): State<T> {
		return derivedStateOf { calculation(listOf(this, *others).map { it.value }) }
	}
}
