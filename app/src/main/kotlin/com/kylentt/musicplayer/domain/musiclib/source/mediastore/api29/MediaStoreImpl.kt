package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api29

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreImplBase

@RequiresApi(Build.VERSION_CODES.Q)
internal class MediaStoreImpl29(private val context: Context) : MediaStoreImplBase(context) {
	override val dispatchers: CoroutineDispatchers = super.dispatchers
	override val contextInfo: ContextInfo = super.contextInfo

	override val audioEntityQueryProjector = arrayOf(
		MediaStore29.MediaColumns._ID
	)

	override val audioFileInfoProjector = arrayOf(
		MediaStore29.Files.FileColumns.DATA,
		MediaStore29.Files.FileColumns.DATE_ADDED,
		MediaStore29.Files.FileColumns.DATE_MODIFIED,
		MediaStore29.Files.FileColumns.DISPLAY_NAME,
		MediaStore29.Files.FileColumns.MIME_TYPE,
		MediaStore29.Files.FileColumns.SIZE
	)

	override val audioEntityMetadataInfoProjector: Array<String> = arrayOf(
		MediaStore29.Audio.AudioColumns.ARTIST,
		MediaStore29.Audio.AudioColumns.ALBUM,
		MediaStore29.Audio.AudioColumns.DURATION,
		MediaStore29.Audio.AudioColumns.TITLE,
	)
}
