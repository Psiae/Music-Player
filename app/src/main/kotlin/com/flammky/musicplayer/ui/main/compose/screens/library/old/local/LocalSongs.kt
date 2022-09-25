package com.flammky.musicplayer.ui.main.compose.screens.library.old.local

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.musicplayer.R
import com.flammky.musicplayer.ui.main.compose.screens.library.old.LibraryViewModelOld
import com.flammky.musicplayer.ui.main.compose.theme.color.ColorHelper

@Composable
fun LocalSongItem(
	model: LibraryViewModelOld.LocalSongModel,
	onClick: () -> Unit
) {
	Box(
		modifier = Modifier
			.height(60.dp)
			.clickable(onClick = onClick)
			.background(MaterialTheme.colorScheme.surface),
		contentAlignment = Alignment.Center
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.padding(5.dp),
			horizontalArrangement = Arrangement.SpaceAround,
		) {
			ItemArtworkCard(art = model.artState.value)

			Spacer(modifier = Modifier.width(8.dp))

			val metadata = model.mediaItem.metadata as AudioMetadata

			val formattedDuration = remember(metadata.duration) {
				val seconds = metadata.duration?.inWholeSeconds ?: 0
				if (seconds > 3600) {
					String.format(
						"%02d:%02d:%02d",
						seconds / 3600,
						seconds % 3600 / 60,
						seconds % 60
					)
				} else {
					String.format(
						"%02d:%02d",
						seconds / 60,
						seconds % 60
					)
				}
			}

			val separator = String("\u00b7".toByteArray(Charsets.UTF_8))

			ItemTextDescription(
				modifier = Modifier.weight(1f, true),
				title = model.displayName,
				subtitle = "$formattedDuration " + "$separator ${metadata.artist ?: "<unknown artist>"}",
			)

			Spacer(modifier = Modifier.width(8.dp))

			val id =
				if (isSystemInDarkTheme()) {
					R.drawable.more_vert_48px
				} else {
					R.drawable.more_vert_48px_dark
				}

			val context = LocalContext.current

			Box(
				modifier = Modifier
					.align(Alignment.CenterVertically)
					.fillMaxHeight()
					.aspectRatio(1f, true)
					.padding(5.dp)
					.clip(RoundedCornerShape(50))
					.clickable {
						Toast
							.makeText(context.applicationContext, "Coming Soon", Toast.LENGTH_SHORT)
							.show()
					}
					.weight(0.2f, true)
			) {

				Icon(
					modifier = Modifier
						.align(Alignment.Center)
						.fillMaxSize(0.5f),
					painter = painterResource(id = id),
					contentDescription = "More",
				)
			}


		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemArtworkCard(art: Any?) {
	val context = LocalContext.current

	val imageModel = remember(art) {
		if (art == null) return@remember null

		ImageRequest.Builder(context)
			.data(art)
			.crossfade(true)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.build()
	}

	val shape: Shape = remember {
		RoundedCornerShape(5)
	}

	Card(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.clip(shape),
		shape = shape,
		elevation = CardDefaults.elevatedCardElevation(2.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
	) {
		AsyncImage(
			modifier = Modifier
				.fillMaxSize()
				.clip(shape),
			model = imageModel,
			contentDescription = "Artwork",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun ItemTextDescription(
	modifier: Modifier,
	title: String,
	subtitle: String,
	textColor: Color = ColorHelper.textColor()
) {
	Column(
		modifier = modifier
			.fillMaxHeight(),
		verticalArrangement = Arrangement.Center
	) {
		val style = with(MaterialTheme.typography.bodyMedium) {
			copy(
				color = textColor,
				fontWeight = FontWeight.Medium
			)
		}

		val style2 = with(MaterialTheme.typography.bodySmall) {
			copy(
				color = textColor,
				fontWeight = FontWeight.Normal
			)
		}
		Text(
			text = title,
			style = style,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
		Spacer(modifier = Modifier.height(3.dp))
		Text(
			text = subtitle,
			style = style2,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}


@Composable
@Preview
private fun LocalSongItemPreview() {
	LocalSongItem(model = LibraryViewModelOld.LocalSongModel("1", "Preview", MediaItem.UNSET)) {}
}
