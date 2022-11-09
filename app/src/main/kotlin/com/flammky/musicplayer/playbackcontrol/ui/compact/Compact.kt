@file:OptIn(ExperimentalMaterial3Api::class)

package com.flammky.musicplayer.playbackcontrol.ui.compact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flammky.musicplayer.R
import com.flammky.musicplayer.playbackcontrol.domain.model.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.ui.PlaybackControlViewModel

@Composable
internal fun CompactPlaybackControl(
	vm: PlaybackControlViewModel
) {

}




@Composable
private fun ArtworkCard(
	artworkState: State<Any?>,
	onClick: () -> Unit
) {
	Card(
		modifier = Modifier.clickable(onClick = onClick),
		shape = RoundedCornerShape(5.dp),
		colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceVariant)
	) {
		AsyncImage(
			modifier = Modifier.fillMaxSize(),
			model = artworkState.value,
			contentDescription = "ART",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun DescriptionPager(
	playlistState: State<PlaybackInfo.Playlist>
) {

}

@Composable
private fun <T, R> State<T>.rememberDerive(derive: (T) -> R): State<R> {
	return remember { derivedStateOf { derive(this.value) } }
}
