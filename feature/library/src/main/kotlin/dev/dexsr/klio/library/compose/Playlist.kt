package dev.dexsr.klio.library.compose

import dev.dexsr.klio.base.UNSET

@ComposeImmutable
class Playlist(
	val id: String,
	val snapshotId: String,
	val contentCount: Int,
	val displayName: String,
	val ownerID: String,
): UNSET<Playlist> by Companion {

	companion object : UNSET<Playlist> {

		override val UNSET: Playlist = Playlist(
			id = "",
			snapshotId = "",
			contentCount = 0,
			displayName = "",
			ownerID = "",
		)
	}
}

fun dev.dexsr.klio.media.playlist.Playlist.toStablePlaylist(): Playlist {
	return Playlist(id, snapshotId, contentCount, displayName, ownerId)
}
