package com.kylentt.musicplayer.domain.musiclib.source.mediastore

import android.content.Context
import com.kylentt.musicplayer.core.sdk.VersionHelper
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28.MediaStoreImpl28
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.api29.MediaStoreImpl29
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.api30.MediaStoreImpl30
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class Representing [android.provider.MediaStore]
 */
@Singleton
class MediaStoreProvider @Inject constructor(
	@ApplicationContext private val context: Context
) {

	/**
	 * The actual implementation on different android APIs
	 */
	private val impl: MediaStoreImplBase = when {
		VersionHelper.hasR() -> MediaStoreImpl30(context)
		VersionHelper.hasQ() -> MediaStoreImpl29(context)
		else -> MediaStoreImpl28(context)
	}

	suspend fun queryAudioEntity(
		fillFileInfo: Boolean,
		fillMetadata: Boolean
	) = impl.queryAudioEntity(fillFileInfo, fillMetadata)

	companion object {
		const val UID_Prefix = "MediaStore_"
		const val UID_Audio_Prefix = "${UID_Prefix}Audio_"
	}
}
