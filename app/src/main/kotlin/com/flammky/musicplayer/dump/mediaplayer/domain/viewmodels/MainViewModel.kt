package com.flammky.musicplayer.dump.mediaplayer.domain.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flammky.common.kotlin.collection.mutable.doEachClear
import com.flammky.musicplayer.dump.mediaplayer.data.repository.ProtoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val stateHandle: SavedStateHandle,
  private val protoRepo: ProtoRepository
) : ViewModel() {

	val playbackControlShownHeight = mutableStateOf(0.dp)
	val bottomNavigationHeight = mutableStateOf(0.dp)

	val bottomVisibilityHeight = listOf(
		playbackControlShownHeight,
		bottomNavigationHeight
	).derive { dpValues ->
		dpValues.sum()
	}

  var savedBottomNavIndex = 0

  val appSettings = protoRepo.appSettingSF
  val pendingStorageGranted = mutableListOf<() -> Unit>()
	fun readPermissionGranted() = pendingStorageGranted.doEachClear()

	private fun Iterable<Dp>.sum(): Dp {
		var sum = 0.dp
		for (dp in this) {
			sum += dp
		}
		return sum
	}

	private fun <T> State<T>.derive(
		calculation: (T) -> T = { it }
	): State<T> {
		return derivedStateOf { calculation(value) }
	}

	private fun <T> List<State<T>>.derive(
		calculation: (List<T>) -> T
	): State<T> {
		return derivedStateOf { calculation(map { it.value }) }
	}

	private fun <T> State<T>.deriveWith(
		vararg others: State<T>,
		calculation: (List<T>) -> T
	): State<T> {
		val states = listOf(this, *others)
		return states.derive { calculation(it) }

		/*return derivedStateOf { calculation(states.map { it.value }) }*/
	}
}
