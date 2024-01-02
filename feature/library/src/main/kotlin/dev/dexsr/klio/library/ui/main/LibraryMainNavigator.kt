package dev.dexsr.klio.library.ui.main

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import com.flammky.musicplayer.library.dump.localmedia.ui.LocalSongListsLegacy
import dev.dexsr.klio.base.compose.SnapshotRead
import dev.dexsr.klio.base.compose.navigation.ComposeDestination
import dev.dexsr.klio.base.kt.cast
import dev.dexsr.klio.library.device.DeviceRootContent
import dev.dexsr.klio.library.spotify.ui.SpotifyUiRoot
import dev.dexsr.klio.library.ui.nav.LibraryUiNavigator
import dev.dexsr.klio.library.user.playlist.YourPlaylistScreen
import dev.dexsr.klio.library.ytm.ui.YTMusicUiRoot
import kotlinx.atomicfu.atomic

@Stable
class LibraryMainNavigator(
	startDestination: String
) : LibraryUiNavigator("main") {

	var startDestination by mutableStateOf(
		resolveDestination(startDestination)
			?: error("LibraryMainNavigator: Unresolved Start Destination")
	)
		private set

	var currentDestination by mutableStateOf(this.startDestination)
		private set

	var currentSubDestination by mutableStateOf<ComposeDestination?>(null)
		private set

	private var restoringInstance = false
	private var savingInstance = false
	private var _hostKey = atomic<Any?>(null)

	fun navigate(route: String): Boolean {

		resolveDestination(route)?.let {
			Snapshot.withoutReadObservation {
				currentDestination = it
			}
			return true
		}

		return false
	}

	fun navigateSub(route: String): Boolean {
		return when (route) {
			"library.localSong.list" -> {
				Snapshot.withoutReadObservation {
					currentSubDestination = ComposeDestination(
						content = @Composable { LocalSongListsLegacy() },
						route = route
					)
				}
				true
			}
			"library.user.playlists" -> {
				Snapshot.withoutReadObservation {
					currentSubDestination = ComposeDestination(
						content = @Composable { YourPlaylistScreen(modifier = Modifier) },
						route = route
					)
				}
				true
			}
			else -> false
		}
	}

	fun pop(): Boolean {
		Snapshot.withoutReadObservation {
			currentSubDestination?.let {
				currentSubDestination = null
				return true
			}
			return false
		}
	}

	@SnapshotRead
	fun canPop(): Boolean {
		return currentSubDestination != null
	}

	private fun restoreInstance(saved: Any) {
		restoringInstance = true
		saved.cast<android.os.Bundle>()
		saved.getString("currentDestination")?.let { route -> navigate(route) }
		saved.getString("currentSubDestination")?.let { route -> navigateSub(route) }
		restoringInstance = false
	}

	private fun saveInstance(): Any {
		savingInstance = true

		val obj = Bundle()
			.apply {
				putString("currentDestination", currentDestination.route)
				putString("currentSubDestination", currentSubDestination?.route)
			}

		savingInstance = false

		return obj
	}

	private fun resolveDestination(route: String): ComposeDestination? {
		return when (route) {
			"device" -> {
				ComposeDestination(
					content = @Composable {
						DeviceRootContent(modifier = Modifier, navigate = { navigateSub(it) })
					},
					route = route
				)
			}
			"spotify" -> {
				ComposeDestination(
					content = @Composable {
						SpotifyUiRoot()
					},
					route = route
				)
			}
			"ytm" -> {
				ComposeDestination(
					content = @Composable {
						YTMusicUiRoot()
					},
					route = route
				)
			}
			else -> null
		}
	}

	companion object {

		fun AndroidSaver(
			startDestination: String
		): Saver<LibraryMainNavigator, Any> {
			return Saver(
				save = { o -> o.saveInstance() },
				restore = { s -> LibraryMainNavigator(startDestination).apply { restoreInstance(s) } }
			)
		}
	}
}

@Composable
fun rememberMainNavigator(): LibraryMainNavigator {

	return rememberSaveable(
		saver = LibraryMainNavigator.AndroidSaver("device")
	) {
		LibraryMainNavigator("device")
	}
}
