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
		MediaColumns._ID
	)

	override val audioFileInfoProjector = arrayOf(
		MediaColumns.DATA,
		MediaColumns.DATE_ADDED,
		MediaColumns.DATE_MODIFIED,
		MediaColumns.DISPLAY_NAME,
		MediaColumns.MIME_TYPE,
		MediaColumns.SIZE
	)

	override val audioEntityMetadataInfoProjector: Array<String> = arrayOf(
		AudioColumns.ARTIST,
		AudioColumns.ALBUM,
		AudioColumns.DURATION,
		AudioColumns.TITLE,
	)
}
