package com.flammky.musicplayer.library.localsong.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object LocalSongNavigator {
	val localSongListRoute = "library.localSong.list"

	fun NavGraphBuilder.addLocalSongDestinations() {
		composable(localSongListRoute) {
			LocalSongListsLegacy()
		}
	}
}
