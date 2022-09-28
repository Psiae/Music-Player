package com.flammky.musicplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Entry point for [Library] composable.
 */
@Composable
fun LibraryEntry() {

}


@Composable
private fun Library(
	background: Color = MaterialTheme.colorScheme.background
) {
	ColumnScrollRoot()
}

@Composable
private fun ColumnScrollRoot() {

	val rootScrollState = rememberScrollState()

	Column(
		modifier = Modifier
			.verticalScroll(rootScrollState)
	) {

	}
}
