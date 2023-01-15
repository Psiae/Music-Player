package com.flammky.musicplayer.library.dump.localmedia.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object LocalSongNavigator {
	const val localSongListRoute = "library.localSong.list"

	fun NavGraphBuilder.addLocalSongDestinations() {
		composable(localSongListRoute) {
			LocalSongListsLegacy()
		}
	}
}
