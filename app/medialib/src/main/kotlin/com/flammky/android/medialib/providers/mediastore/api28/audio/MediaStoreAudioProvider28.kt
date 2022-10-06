package com.flammky.android.medialib.providers.mediastore.api28.audio

import com.flammky.android.io.exception.ReadExternalStoragePermissionException
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.api28.MediaStoreProvider28

class MediaStoreAudioProvider28(private val context: MediaStoreContext)
	: MediaStoreProvider28.Audio {

	private val entityProvider = AudioEntityProvider28(context)

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	override suspend fun query(): List<MediaStoreAudioEntity28> {
		return entityProvider.query()
	}
}
