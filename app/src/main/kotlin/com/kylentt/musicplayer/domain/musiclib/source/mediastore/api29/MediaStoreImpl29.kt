package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api29

import android.content.Context
import android.os.Build
import android.provider.MediaStore.MediaColumns
import androidx.annotation.RequiresApi
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreImplBase

@RequiresApi(Build.VERSION_CODES.Q)
internal class MediaStoreImpl29(private val context: Context) : MediaStoreImplBase(context) {
	override val dispatchers: CoroutineDispatchers = super.dispatchers
	override val contextInfo: ContextInfo = super.contextInfo
	override val audioEntityQueryProjector = super.audioEntityQueryProjector


	override val audioEntityFileInfoProjector = arrayOf(
		*super.audioEntityFileInfoProjector,
		MediaColumns.BUCKET_DISPLAY_NAME,
		MediaColumns.BUCKET_ID,
		MediaColumns.DATE_EXPIRES,
		MediaColumns.DATE_TAKEN,
		MediaColumns.DOCUMENT_ID,
		MediaColumns.INSTANCE_ID,
		MediaColumns.IS_PENDING,
	)

	override val audioEntityMetadataInfoProjector: Array<String> = super.audioEntityMetadataInfoProjector
}
