package dev.dexsr.klio.media.playlist.realm

import dev.dexsr.klio.media.playlist.PlaylistItem
import io.realm.kotlin.types.RealmList

class PlaylistSynchronizeData(
    val playlistId: String,
    val playlistSnapshotId: String,
    val displayName: String,
    val contents: List<PlaylistItem>,
    val ownerId: String
) {
}

fun PlaylistSynchronizeData.toRealmPlaylist(
    primaryKey: String,
    contents: RealmList<RealmPlaylistItem>,
    snapshotId: String = this.playlistSnapshotId,
) = RealmPlaylist(
   _id = primaryKey,
    playlistId = playlistId,
    snapshotId = snapshotId,
    displayName = displayName,
    contents = contents,
    ownerId = ownerId
)