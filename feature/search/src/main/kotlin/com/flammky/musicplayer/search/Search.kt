package com.flammky.musicplayer.search

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun Search(
	navigate: (id: String) -> Unit
) {
	BoxWithConstraints(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.TopCenter
	) {
		Button(
			modifier = Modifier.offset(y = (maxHeight.value * 0.7f).dp),
			onClick = { navigate("root_library") }) {
			Text(text = "To Library")
		}
	}
}
