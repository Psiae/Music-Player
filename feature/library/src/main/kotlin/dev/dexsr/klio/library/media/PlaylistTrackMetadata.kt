package dev.dexsr.klio.library.media

import dev.dexsr.klio.base.UNSET
import dev.dexsr.klio.library.compose.ComposeImmutable
import kotlin.time.Duration

@ComposeImmutable
data class PlaylistTrackMetadata(
	val title: String,
	val subtitle: String,
	val duration: Duration,
	val isNone: Boolean
): UNSET<PlaylistTrackMetadata> by Companion {

	companion object : UNSET<PlaylistTrackMetadata> {

		override val UNSET = PlaylistTrackMetadata("", "", Duration.ZERO, false)
	}
}

