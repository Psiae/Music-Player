package dev.dexsr.klio.library.compose

import dev.dexsr.klio.base.UNSET

@ComposeImmutable
class PlaylistInfo(
	val id: String,
	val snapshotId: String,
	val contentCount: Int,
	val displayName: String,
	val ownerID: String,
): UNSET<PlaylistInfo> by Companion {

	companion object : UNSET<PlaylistInfo> {

		override val UNSET: PlaylistInfo = PlaylistInfo(
			id = "",
			snapshotId = "",
			contentCount = 0,
			displayName = "",
			ownerID = "",
		)
	}
}

fun dev.dexsr.klio.media.playlist.Playlist.toStablePlaylist(): PlaylistInfo {
	return PlaylistInfo(id, snapshotId, contentCount, displayName, ownerId)
}
