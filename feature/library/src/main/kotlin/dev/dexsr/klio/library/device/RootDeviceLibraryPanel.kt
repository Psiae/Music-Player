package dev.dexsr.klio.library.device

import androidx.compose.foundation.Indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.library.R
import dev.dexsr.klio.base.compose.clickable
import dev.dexsr.klio.base.compose.nonScaledFontSize
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.components.cards.alpha
import dev.dexsr.klio.base.theme.md3.components.cards.cards
import dev.dexsr.klio.base.theme.md3.components.cards.contentAlpha
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.backgroundContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.blackOrWhiteContent
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.primaryColorAsState
import dev.dexsr.klio.base.theme.md3.compose.shape.toComposeShape
import dev.dexsr.klio.base.theme.md3.compose.surfaceColorAtElevation
import dev.dexsr.klio.base.theme.md3.compose.surfaceContentColorAsState
import dev.dexsr.klio.base.theme.md3.compose.toComposePadding

@Composable
fun RootDeviceLibraryPanel(
	modifier: Modifier = Modifier
) {
	Column(modifier) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(MD3Theme.dpPaddingIncrementsOf(1)),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "Device Library",
				style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
				color = MD3Theme.backgroundContentColorAsState().value
			)
			Icon(
				modifier = Modifier.size(14.dp),
				painter = painterResource(id = R.drawable.touchscreen_96),
				contentDescription = null,
				tint = MD3Theme.primaryColorAsState().value
			)
		}
		Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
		BoxWithConstraints {
			@OptIn(ExperimentalLayoutApi::class)
			FlowRow(
				horizontalArrangement = Arrangement.spacedBy(MD3Spec.cards.SPACING_DP.dp),
				verticalArrangement = Arrangement.spacedBy(MD3Spec.cards.SPACING_DP.dp)
			) {
				DevicePlaylistCard(
					modifier = Modifier
						.defaultMinSize(95.dp, 95.dp)
						.sizeIn(maxHeight = 120.dp, maxWidth = 95.dp),
					onClick = { /*TODO*/ }
				)
				DeviceAlbumsCard(
					modifier = Modifier
						.defaultMinSize(95.dp, 95.dp)
						.sizeIn(maxHeight = 120.dp, maxWidth = 95.dp),
					onClick = { /*TODO*/ }
				)
				DeviceArtists(
					modifier = Modifier
						.defaultMinSize(95.dp, 95.dp)
						.sizeIn(maxHeight = 120.dp, maxWidth = 95.dp),
					onClick = { /*TODO*/ }
				)
				DevicePodcasts(
					modifier = Modifier
						.defaultMinSize(95.dp, 95.dp)
						.sizeIn(maxHeight = 120.dp, maxWidth = 95.dp),
					onClick = { /*TODO*/ }
				)
			}
		}
	}
}

@Composable
private fun DevicePlaylistCard(
	modifier: Modifier,
	onClick: (() -> Unit)?,
	indication: Indication? = rememberRipple()
) {
	RootDeviceLibraryPanelItemCard(
		modifier = modifier,
		onClick = onClick,
		indication = indication,
		iconPainter = painterResource(id = R.drawable.queue_music_opsz24),
		text = "Playlist"
	)
}

@Composable
private fun DeviceAlbumsCard(
	modifier: Modifier,
	onClick: (() -> Unit)?,
	indication: Indication? = rememberRipple()
) {
	RootDeviceLibraryPanelItemCard(
		modifier = modifier,
		onClick = onClick,
		indication = indication,
		iconPainter = painterResource(id = R.drawable.album_fill0_wght400_grad0_opsz24),
		text = "Albums"
	)
}

@Composable
private fun DeviceArtists(
	modifier: Modifier,
	onClick: (() -> Unit)?,
	indication: Indication? = rememberRipple()
) {
	RootDeviceLibraryPanelItemCard(
		modifier = modifier,
		onClick = onClick,
		indication = indication,
		iconPainter = painterResource(id = R.drawable.artist_fill0_wght400_grad0_opsz24),
		text = "Artists"
	)
}

@Composable
private fun DevicePodcasts(
	modifier: Modifier,
	onClick: (() -> Unit)?,
	indication: Indication? = rememberRipple()
) {
	RootDeviceLibraryPanelItemCard(
		modifier = modifier,
		onClick = onClick,
		indication = indication,
		iconPainter = painterResource(id = R.drawable.podcasts_fill0_wght400_grad0_opsz24),
		text = "Podcasts"
	)
}

@Composable
private fun RootDeviceLibraryPanelItemCard(
	modifier: Modifier,
	onClick: (() -> Unit)?,
	indication: Indication? = rememberRipple(),
	iconPainter: Painter,
	text: String
) {
	val upOnClick = rememberUpdatedState(newValue = onClick)
	Box(
		modifier = run {
			val enabled = upOnClick.value != null
			modifier
				.clip(MD3Spec.cards.shapeDp.toComposeShape())
				.composed {
					val color = MD3Theme.surfaceColorAtElevation(elevation = 2.dp)
					val alpha = MD3Spec.cards.alpha(enabled)
					drawBehind { drawRect(color, alpha = alpha) }
				}
				.clickable(
					indication = indication,
					enabled = enabled,
					onClickLabel = null,
					role = Role.Button,
					onClick = { upOnClick.value?.invoke() }
				)
				.padding(MD3Spec.cards.paddingDp.toComposePadding())
				.padding(vertical = 8.dp)
		},
		propagateMinConstraints = true
	) {
		val enabled = upOnClick.value != null
		Column(
			verticalArrangement = Arrangement.SpaceBetween
		) {
			Icon(
				modifier = Modifier
					.alpha(MD3Spec.cards.contentAlpha(enabled = enabled, isText = false))
					.size(MD3Spec.cards.ICON_SIZE_DP.dp),
				painter = iconPainter,
				contentDescription = null,
				tint = MD3Theme.surfaceContentColorAsState().value
			)
			Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(2)))
			BasicText(
				modifier = Modifier
					.alpha(MD3Spec.cards.contentAlpha(enabled = enabled, isText = true))
				/*// should we sacrifice text or icon ?
				.weight(1f, fill = false)*/,
				text = text,
				style = MaterialTheme3.typography.labelLarge.run {
					copy(
						color = MD3Theme.blackOrWhiteContent(),
						fontWeight = FontWeight.SemiBold,
						fontSize = nonScaledFontSize()
					)
				},
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}
