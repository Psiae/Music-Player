package com.flammky.musicplayer.user.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState

@Composable
internal fun UserScreen() = UserScreenSurface {
	Column(modifier = Modifier
		.fillMaxSize()
		.statusBarsPadding()
	) {
		Spacer(modifier = Modifier.height(20.dp))
		UserProfileCard(
			modifier = Modifier
				.height(70.dp)
				.padding(horizontal = 10.dp)
		)
		Spacer(modifier = Modifier.height(20.dp))
	}
}

@Composable
private inline fun UserScreenSurface(
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(color = Theme.backgroundColorAsState().value)
	) {
		content()
	}
}


@Composable
private fun UserScreenPreview() {

}
