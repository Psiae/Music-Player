package dev.dexsr.klio.media.playlist.paging

class PlaylistContentBucket(
    val playlistId: String,
    val playlistSnapshotId: String,
    val contents: List<String>,
)