package com.flammky.musicplayer.library.root

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Library() {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		LibraryContents()
	}
}

@Composable
private fun LibraryContents() {
	ColumnScrollRoot {

	}
}

@Composable
private fun ColumnScrollRoot(
	scrollState: ScrollState = rememberScrollState(),
	arrangement: Arrangement.Vertical = Arrangement.Top,
	alignment: Alignment.Horizontal = Alignment.Start,
	columnContent: @Composable ColumnScope.() -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(scrollState),
		verticalArrangement = arrangement,
		horizontalAlignment = alignment,
		columnContent
	)
}

