package com.flammky.musicplayer.library.localsong.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LocalSongDisplay(
	navigate: (String) -> Unit
) {
	Column(modifier = Modifier.fillMaxWidth()) {
		DisplayHeader()
		DisplayContent()
	}
}

@Composable
private fun DisplayHeader(
	textStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
) {
	Text(text = "Device Songs")
}

@Composable
private fun DisplayContent() {

}
