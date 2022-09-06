package com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.media

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.media3.contract.MediaItemFactoryOf

abstract class MediaItemFactory<
	E : MediaStoreEntity,
	F : MediaStoreFile,
	M : MediaStoreMetadata,
	Q : MediaStoreQuery
	> (context: MediaStoreContext) : MediaItemFactoryOf<E> {

	override fun createMediaItem(
		from: E,
		metadataExtra: Bundle,
		requestMetadataExtra: Bundle
	): MediaItem {
		val mediaItemBuilder = MediaItem.Builder()
		val metadataBuilder = MediaMetadata.Builder()
		val metadataRequestBuilder = RequestMetadata.Builder()

		fillMetadata(from.metadataInfo as M, metadataBuilder, metadataExtra)
		fillRequestMetadata(from.uri, metadataRequestBuilder, requestMetadataExtra)

		mediaItemBuilder.apply {
			setUri(from.uri)
			setMediaMetadata(metadataBuilder.build())
			setRequestMetadata(metadataRequestBuilder.build())
			setMediaId(from.uid)
		}

		return mediaItemBuilder.build()
	}

	open fun fillMetadata(
		metadataInfo: M,
		mediaMetadata: MediaMetadata.Builder,
		extra: Bundle
	) {
		mediaMetadata.apply {
			setTitle(metadataInfo.title)
			setExtras(extra)
		}
	}

	open fun fillRequestMetadata(
		contentUri: Uri,
		requestMetadata: RequestMetadata.Builder,
		extra: Bundle
	) {
		requestMetadata.apply {
			setSearchQuery(null)
			setMediaUri(contentUri)
			setExtras(extra)
		}
	}
}
