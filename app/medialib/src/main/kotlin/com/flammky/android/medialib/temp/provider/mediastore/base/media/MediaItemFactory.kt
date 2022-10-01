package com.flammky.android.medialib.temp.provider.mediastore.base.media

import android.os.Bundle
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItemFactoryOf
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext

abstract class MediaItemFactory<
  E : MediaStoreEntity,
  F : MediaStoreFile,
  M : MediaStoreMetadata,
  Q : MediaStoreQuery
  > internal constructor(private val context: MediaStoreContext) : MediaItemFactoryOf<E> {

	override fun createMediaItem(source: E, metadataBundle: Bundle?): MediaItem {
		val metadata = createMetadata(source.metadataInfo as M, metadataBundle)
		return MediaItem.build(context.library) {
			setMediaId(source.uid)
			setMediaUri(source.uri)
			setMetadata(metadata)
		}
	}

	open fun createMetadata(metadataInfo: M, metadataBundle: Bundle?): MediaMetadata {
		return MediaMetadata.build {
			setTitle(metadataInfo.title)
			if (metadataBundle != null) {
				val extra = MediaMetadata.Extra(metadataBundle)
				setExtra(extra)
			}
		}
	}
}
