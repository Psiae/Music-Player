package dev.dexsr.klio.library.compose

@ComposeImmutable
class PagedPlaylistData(
	val id: String,
	val snapshotId: String,
	val offset: Int,
	val data: List<String>,
	val pageMaxSize: Int
){
}
