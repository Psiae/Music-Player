package dev.dexsr.klio.library.compose

import dev.dexsr.klio.base.UNSET
import dev.dexsr.klio.media.playlist.PlaylistItem

@ComposeImmutable
class Playlist(
	val id: String,
	val snapshotId: String,
	val contents: List<PlaylistItem>,
	val displayName: String,
	val ownerID: String,
): UNSET<Playlist> by Companion {

	companion object : UNSET<Playlist> {

		override val UNSET: Playlist = Playlist(
			id = "",
			snapshotId = "",
			contents = emptyList(),
			displayName = "",
			ownerID = "",
		)
	}
}

fun dev.dexsr.klio.media.playlist.Playlist.toStablePlaylist(): Playlist {
	return Playlist(id, snapshotId, contents, displayName, ownerId)
}
