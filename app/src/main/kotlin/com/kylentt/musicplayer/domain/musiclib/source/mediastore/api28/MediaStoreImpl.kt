package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import android.content.Context
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreImplBase
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.BaseColumns

internal class MediaStoreImpl28(private val context: Context) : MediaStoreImplBase(context) {

	override val audioEntityQueryProjector: Array<String> = arrayOf(
		BaseColumns._ID
	)

	override val audioFileInfoProjector: Array<String> = arrayOf(
		MediaStore28.Files.FileColumns.DATA,
		MediaStore28.Files.FileColumns.DATE_ADDED,
		MediaStore28.Files.FileColumns.DATE_MODIFIED,
		MediaStore28.Files.FileColumns.DISPLAY_NAME,
		MediaStore28.Files.FileColumns.MIME_TYPE,
		MediaStore28.Files.FileColumns.SIZE
	)

	override val audioEntityMetadataInfoProjector: Array<String> = arrayOf(
		MediaStore28.Audio.AudioColumns.ALBUM,
		MediaStore28.Audio.AudioColumns.ARTIST,
		MediaStore28.Audio.AudioColumns.DURATION,
		MediaStore28.Audio.AudioColumns.TITLE
	)
}
