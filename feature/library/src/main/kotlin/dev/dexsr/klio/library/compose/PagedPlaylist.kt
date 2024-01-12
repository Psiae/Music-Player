package dev.dexsr.klio.library.compose

@ComposeImmutable
class PagedPlaylistData(
	val playlistId: String,
	val snapshotId: String,
	val offset: Int,
	val data: List<String>,
	val pageMaxSize: Int
){
}
