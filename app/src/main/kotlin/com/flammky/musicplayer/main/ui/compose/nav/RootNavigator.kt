package com.flammky.musicplayer.main.ui.compose.nav

import com.flammky.musicplayer.base.nav.compose.ComposeRootNavigator

object RootNavigator {
	val navigators: List<ComposeRootNavigator> = listOf(
		com.flammky.musicplayer.home.nav.compose.HomeRootNavigator,
		com.flammky.musicplayer.search.nav.compose.SearchRootNavigator,
		com.flammky.musicplayer.library.dump.nav.compose.LibraryRootNavigator,
		com.flammky.musicplayer.user.nav.compose.UserRootNavigator
	)
}
