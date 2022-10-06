package com.flammky.android.medialib.providers.mediastore

import com.flammky.android.io.exception.ReadExternalStoragePermissionException
import com.flammky.android.medialib.providers.MediaProvider
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreEntity

/**
 * Interface for MediaStoreProvider, our wrapper for MediaStore API
 */
interface MediaStoreProvider : MediaProvider {
	val audio: Audio
	val image: Image
	val video: Video

	/** Base Media Interface */
	sealed interface Media {
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		suspend fun query(): List<MediaStoreEntity>
	}

	/** Audio Interface */
	interface Audio : Media {
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		override suspend fun query(): List<MediaStoreAudioEntity>
	}

	/** Image Interface */
	interface Image : Media

	/** Video Interface */
	interface Video : Media


	interface ContentObserver {




		sealed interface Flag {
			object Unknown : Flag
			object Update : Flag
			object Insert : Flag
			object Delete : Flag
			// Internal use only
			object Unsupported : Flag
		}
	}
}
