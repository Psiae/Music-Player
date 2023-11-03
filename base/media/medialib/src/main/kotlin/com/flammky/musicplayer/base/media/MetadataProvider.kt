package com.flammky.musicplayer.base.media

import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import kotlinx.coroutines.Deferred

interface MetadataProvider {
	fun getCached(id: String): MediaMetadata?
	fun requestFromMediaStoreIdAsync(id: String): Deferred<MediaMetadata?>

	fun requestFromDocumentUriAsync(contentUri: String): Deferred<MediaMetadata?>
}
