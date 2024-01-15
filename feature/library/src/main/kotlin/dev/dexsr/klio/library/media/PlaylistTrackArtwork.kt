package dev.dexsr.klio.library.media

import dev.dexsr.klio.base.UNSET
import dev.dexsr.klio.base.resource.LocalImage
import dev.dexsr.klio.library.compose.ComposeImmutable

@ComposeImmutable
data class PlaylistTrackArtwork(
	val localImage: LocalImage<*>,
	val isNone: Boolean
): UNSET<PlaylistTrackArtwork> by Companion {

	companion object : UNSET<PlaylistTrackArtwork> {

		override val UNSET = PlaylistTrackArtwork(LocalImage.None, false)
	}
}
