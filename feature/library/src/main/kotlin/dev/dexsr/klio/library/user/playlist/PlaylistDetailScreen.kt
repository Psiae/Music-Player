package dev.dexsr.klio.library.user.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Surface
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAtElevation

@Composable
fun PlaylistDetailScreen(
	modifier: Modifier = Modifier,
	playlistId: String
) { key(playlistId) { Box(
	modifier
		.fillMaxSize()
		.localMaterial3Surface()
) { Column {
	val state = rememberPlaylistDetailScreenState(playlistId = playlistId)

	run {
		val lazyLayoutState = rememberPlaylistDetailLazyLayoutState(
			playlistId = playlistId,
			orderedMeasure = true
		)
		when (val type = "list") {
			"list" -> PlaylistDetailScreenLazyColumn(
				Modifier,
				contentPadding = PaddingValues(top = LocalLayoutVisibility.Top.current, bottom = LocalLayoutVisibility.Bottom.current),
				state = lazyLayoutState
			)
			"grid" -> PlaylistDetailScreenLazyGrid(
				Modifier,
				contentPadding = PaddingValues(),
			)
		}
	}
} }}}

@Composable
private fun  PlaylistDetailScreenLazyColumn(
	modifier: Modifier = Modifier,
	state: PlaylistDetailLazyLayoutState,
	layoutState: LazyListState = rememberLazyListState(),
	contentPadding: PaddingValues,
) {
	val renderData = state.data
		?: return
	LazyColumn(
		modifier = modifier
			.fillMaxSize(),
		state = layoutState,
		contentPadding = contentPadding,
		content = {
			items(
				renderData.contentCount,
				key = { i -> renderData.getContent(i) },
				contentType = { "A" }
			) { i ->
				val id = renderData.getContent(i)
				Box(modifier = Modifier.height(56.dp)) {
					PlaylistDetailScreenLazyListItem(
						id = id
					)
				}
			}
		}
	)
}

@Composable
private fun PlaylistDetailScreenLazyGrid(
	modifier: Modifier = Modifier,
	state: LazyGridState = rememberLazyGridState(),
	contentPadding: PaddingValues,
) {
	// TODO: impl
}



@Composable
private fun PlaylistDetailScreenLazyListItem(
	modifier: Modifier = Modifier,
	id: String,
) {
	Box(
		modifier
			.background(MD3Theme.surfaceColorAtElevation(elevation = 1.dp))
			.padding(4.dp)
			.fillMaxHeight()
			.fillMaxWidth()
			/*.aspectRatio(1f, true)*/
	) {
		Text(modifier= Modifier.align(Alignment.Center), text = id, style = MaterialTheme3.typography.bodyMedium)
	}
}
