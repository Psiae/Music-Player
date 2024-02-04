package dev.dexsr.klio.library.user.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.dexsr.klio.android.base.resource.AndroidLocalImage
import dev.dexsr.klio.base.composeui.ImmutableAny
import dev.dexsr.klio.base.composeui.heightAspectRatio
import dev.dexsr.klio.base.composeui.nonScaledFontSize
import dev.dexsr.klio.base.resource.LocalImage
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.blackOrWhiteContent
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.primaryColorAsState
import dev.dexsr.klio.library.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// TODO 2024-02-04 2:41 PM: impl
@Composable
internal fun PlaylistDetailScreenDescription(
	modifier: Modifier,
	playlistId: String
) {
	PlaylistDetailScreenDescription(
		modifier = modifier,
		imgKey = ImmutableAny(LocalImage.None),
		img = ImmutableAny(LocalImage.None),
		displayName = "Local Files",
		description = "tracks in this device",
		ownerDisplayName = "Device",
		ownerImgKey = ImmutableAny(1),
		ownerImg = ImmutableAny(AndroidLocalImage.Resource(R.drawable.touchscreen_96)),
		trackCount = 100,
		visibility = "private",
		visibilityImgKey = ImmutableAny(1),
		visibilityImg = ImmutableAny(AndroidLocalImage.Resource(R.drawable.vpn_lock_fill0_wght400_grad0_opsz24)),
		trackImgKey = ImmutableAny(LocalImage.None),
		trackImg = ImmutableAny(LocalImage.None),
		duration = ImmutableAny(10.hours + 5.minutes + 3.seconds),
		durationImgKey = ImmutableAny(LocalImage.None),
		durationImg = ImmutableAny(LocalImage.None),
	)
}

@Composable
internal fun PlaylistDetailScreenDescription(
	modifier: Modifier,
	imgKey: ImmutableAny<Any>,
	img: ImmutableAny<LocalImage<*>>,
	displayName: String,
	description: String,
	ownerDisplayName: String,
	ownerImgKey: ImmutableAny<Any>,
	ownerImg: ImmutableAny<LocalImage<*>>,
	trackCount: Int,
	trackImgKey: ImmutableAny<Any>,
	trackImg: ImmutableAny<LocalImage<*>>,
	visibility: String,
	visibilityImgKey: ImmutableAny<Any>,
	visibilityImg: ImmutableAny<LocalImage<*>>,
	duration: ImmutableAny<Duration>,
	durationImg: ImmutableAny<LocalImage<*>>,
	durationImgKey: ImmutableAny<Any>,
) {
	val duration = duration.actual
	Column(modifier.fillMaxWidth()) {
		PlaylistDetailScreenDescriptionArtwork(
			modifier = Modifier
				.padding(bottom = MD3Theme.dpPaddingIncrementsOf(4))
				.size(200.dp)
				.align(Alignment.CenterHorizontally),
			imgKey = imgKey,
			img = img,
		)

		BasicText(
			text = displayName,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = MaterialTheme3.typography.titleLarge.let { style ->
				style.copy(
					fontWeight = FontWeight.Bold,
					fontSize = style.nonScaledFontSize(),
					color = MD3Theme.blackOrWhiteContent()
				)
			}
		)
		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(1)))

		BasicText(
			text = description,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = MaterialTheme3.typography.labelSmall.let { style ->
				style.copy(
					fontWeight = FontWeight.SemiBold,
					fontSize = style.nonScaledFontSize(),
					color = MD3Theme.blackOrWhiteContent().copy(alpha = 0.8f)
				)
			}
		)
		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(1)))

		PlaylistDetailScreenDescriptionOwner(
			modifier = Modifier.height(20.dp),
			displayName = ownerDisplayName,
			imgKey = ownerImgKey,
			img = ownerImg
		)
		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))

		PlaylistDetailScreenDescriptionMetadata(
			modifier = Modifier.height(20.dp),
			visibility = visibility,
			visibilityImgKey = visibilityImgKey,
			visibilityImg = visibilityImg,
			trackCount = trackCount,
			trackImgKey = trackImgKey,
			trackImg = trackImg,
			duration = remember(duration) {
				playlistDetailScreenDescriptionFormatDuration(duration, compact = true)
			},
			durationImgKey = durationImgKey,
			durationImg = durationImg
		)
	}
}


@Composable
private fun PlaylistDetailScreenDescriptionArtwork(
	modifier: Modifier,
	imgKey: ImmutableAny<Any>,
	img: ImmutableAny<LocalImage<*>>,
) {
	val ctx = LocalContext.current
	val coilReq = remember(ctx, imgKey) {
		if (img.actual is LocalImage.None) return@remember null
		ImageRequest.Builder(ctx)
			.data(img.actual.value)
			.build()
	}
	if (coilReq != null) {
		AsyncImage(
			modifier = modifier,
			model = coilReq,
			contentDescription = null
		)
	}
}

@Composable
private fun PlaylistDetailScreenDescriptionOwner(
	modifier: Modifier,
	displayName: String,
	imgKey: ImmutableAny<Any>,
	img: ImmutableAny<LocalImage<*>>
) {
	Row(modifier) {
		val ctx = LocalContext.current
		val coilReq = remember(ctx, imgKey) {
			if (img.actual is LocalImage.None) return@remember null
			ImageRequest.Builder(ctx)
				.data(img.actual.value)
				.build()
		}
		if (coilReq != null) {
			AsyncImage(
				modifier = Modifier
					.fillMaxHeight()
					.heightAspectRatio(1f),
				model = coilReq,
				contentDescription = null,
				colorFilter = if (img.actual is AndroidLocalImage.Resource) {
					ColorFilter.tint(MD3Theme.primaryColorAsState().value)
				} else null
			)
			Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
		}
		BasicText(
			modifier = Modifier.align(Alignment.CenterVertically),
			text = displayName,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = MaterialTheme3.typography.labelMedium.let { style ->
				style.copy(
					fontWeight = FontWeight.SemiBold,
					fontSize = style.nonScaledFontSize(),
					color = MD3Theme.blackOrWhiteContent().copy(alpha = 0.9f)
				)
			}
		)
	}
}

// fixme: use AnnotatedString API instead
@Composable
private fun PlaylistDetailScreenDescriptionMetadata(
	modifier: Modifier,
	visibility: String,
	visibilityImgKey: ImmutableAny<Any>,
	visibilityImg: ImmutableAny<LocalImage<*>>,
	trackCount: Int,
	trackImgKey: ImmutableAny<Any>,
	trackImg: ImmutableAny<LocalImage<*>>,
	duration: String,
	durationImgKey: ImmutableAny<Any>,
	durationImg: ImmutableAny<LocalImage<*>>
) {
	val separator = remember { String("\u00b7".toByteArray(Charsets.UTF_8)) }
	val style = MaterialTheme3.typography.labelMedium.let { style ->
		style.copy(
			fontWeight = FontWeight.SemiBold,
			fontSize = style.nonScaledFontSize(),
			color = MD3Theme.blackOrWhiteContent()
		)
	}
	val ctx = LocalContext.current
	val imgSize = 16.dp

	Row(
		modifier
	) {
		run {
			Row {
				if (visibilityImg.actual !is LocalImage.None) {
					AsyncImage(
						modifier = Modifier
							.size(imgSize)
							.align(Alignment.CenterVertically),
						model = remember(visibilityImgKey) {
							ImageRequest.Builder(ctx)
								.data(visibilityImg.actual.value)
								.build()
						},
						contentDescription = null,
						colorFilter = if (visibilityImg.actual is AndroidLocalImage.Resource) {
							ColorFilter.tint(MD3Theme.blackOrWhiteContent())
						} else null,
						contentScale = ContentScale.FillBounds
					)
					Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
				}

				BasicText(
					text = visibility,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = style
				)
			}
		}

		Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
		BasicText(
			text = separator,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = style
		)
		Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))

		run {
			Row {
				if (trackImg.actual !is LocalImage.None) {
					AsyncImage(
						modifier = Modifier
							.sizeIn(maxWidth = imgSize, maxHeight = imgSize)
							.align(Alignment.CenterVertically),
						model = remember(trackImgKey) {
							ImageRequest.Builder(ctx)
								.data(trackImg.actual.value)
								.build()
						},
						contentDescription = null,
						colorFilter = if (trackImg.actual is AndroidLocalImage.Resource) {
							ColorFilter.tint(MD3Theme.blackOrWhiteContent())
						} else null,
						contentScale = ContentScale.FillBounds
					)
					Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
				}
				BasicText(
					text = if (trackCount > 1) "$trackCount tracks" else "$trackCount track",
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = style
				)
			}
		}

		Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
		BasicText(
			text = separator,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			style = style
		)
		Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))

		run {
			Row {
				if (durationImg.actual !is LocalImage.None) {
					AsyncImage(
						modifier = Modifier
							.sizeIn(maxWidth = imgSize, maxHeight = imgSize),
						model = remember(durationImgKey) {
							ImageRequest.Builder(ctx)
								.data(durationImg.actual.value)
								.build()
						},
						contentDescription = null,
						colorFilter = if (durationImg.actual is AndroidLocalImage.Resource) {
							ColorFilter.tint(MD3Theme.blackOrWhiteContent())
						} else null,
						contentScale = ContentScale.FillBounds
					)
					Spacer(modifier = Modifier.width(MD3Theme.dpPaddingIncrementsOf(1)))
				}
				BasicText(
					text = duration,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = style
				)
			}
		}
	}
}


/*
	If we follow spotify rule that I know
	this can take up to YEAR unit resolution, but realistically we can only show up to WEEK as it's
	only constant up to that unit inclusive.
 */
private fun playlistDetailScreenDescriptionFormatDuration(
	duration: Duration,
	compact: Boolean,
	separator: String = " "
): String {
	val stb = StringBuilder()

	listOf<Pair<String, Long>>(
		(if (compact) "s" else "second") to 1,
		(if (compact) "min" else "minute") to 60,
		(if (compact) "h" else "hour") to 60 * 60,
		(if (compact) "d" else "day") to 3600 * 24,
		(if (compact) "w" else "week") to 86400 * 7
	).asReversed().run {
		val seconds = duration.inWholeSeconds
		var lastFactor = 0L
		fastForEachIndexed { i, (name, factor) ->
			val x = run {
				if (i == 0) seconds / factor
				else seconds % lastFactor / factor
			}.coerceAtLeast(0L)
			lastFactor = factor
			if (x == 0L) {
				// keep last element
				if (i < lastIndex) return@fastForEachIndexed
			}
			if (compact) stb.append("$x$name") else stb.append("$x $name")
			// pluralization
			if (!compact) if (x > 1L) stb.append('s')
			// separator
			if (i < lastIndex) stb.append(separator)
		}
	}

	return stb.toString()
}
