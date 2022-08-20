package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import android.content.Context
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreImplBase
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.api1.BaseColumns

internal class MediaStoreImpl28(private val context: Context) : MediaStoreImplBase(context) {
	override val audioEntityQueryProjector: Array<String> by lazy {
		arrayOf(BaseColumns._ID)
	}

	override val audioFileInfoProjector: Array<String> by lazy {
		arrayOf(
			FileColumns.DATA,
			FileColumns.DATE_ADDED,
			FileColumns.DATE_MODIFIED,
			FileColumns.DISPLAY_NAME,
			FileColumns.MIME_TYPE,
			FileColumns.SIZE
		)
	}

	override val audioEntityMetadataInfoProjector: Array<String> by lazy {
		arrayOf(
			AudioColumns.ALBUM,
			AudioColumns.ARTIST,
			AudioColumns.DURATION,
			AudioColumns.TITLE
		)
	}
}
