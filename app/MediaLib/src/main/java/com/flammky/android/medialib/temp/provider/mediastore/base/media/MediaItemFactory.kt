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
  > internal constructor(context: MediaStoreContext) : MediaItemFactoryOf<E> {

	override fun createMediaItem(source: E): MediaItem = super.createMediaItem(source)

	override fun createMediaItem(
		source: E,
		itemBundle: Bundle?,
		metadataBundle: Bundle?
	): MediaItem {
		val metadata = createMetadata(
			metadataInfo = source.metadataInfo as M,
			bundle = metadataBundle
		)
		return MediaItem.Builder()
			.apply {
				setMediaId(source.uid)
				setMetadata(metadata)
				setUri(source.uri)
				if (itemBundle != null) {
					setBundle(itemBundle)
				}
			}
			.build()
	}

	open fun createMetadata(
		metadataInfo: M,
		bundle: Bundle?
	): MediaMetadata {
		return MediaMetadata.Builder()
			.apply {
				setTitle(metadataInfo.title)
				if (bundle != null) {
					setBundle(bundle)
				}
			}
			.build()
	}
}
