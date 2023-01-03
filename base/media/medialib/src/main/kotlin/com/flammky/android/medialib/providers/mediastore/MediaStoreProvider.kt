package com.flammky.android.medialib.providers.mediastore

import android.net.Uri
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
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		suspend fun queryById(id: String): MediaStoreEntity?
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		suspend fun queryByUri(uri: Uri): MediaStoreEntity?
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		suspend fun queryUris(): List<Uri>
		fun observe(observer: ContentObserver)
		fun removeObserver(observer: ContentObserver)
		fun rescan(callback: (List<Uri>) -> Unit)

		fun uriFromId(id: String): Uri?
		fun idFromUri(uri: Uri): String?
	}

	/** Audio Interface */
	interface Audio : Media {
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		override suspend fun query(): List<MediaStoreAudioEntity>
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		override suspend fun queryById(id: String): MediaStoreAudioEntity?
		@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
		override suspend fun queryByUri(uri: Uri): MediaStoreAudioEntity?
	}

	/** Image Interface */
	interface Image : Media

	/** Video Interface */
	interface Video : Media

	fun interface ContentObserver {

		fun onChange(id: String, uri: Uri, flag: Flag)

		/**
		 * Flags that indicates the event type
		 */
		/* private */ sealed interface Flag {

			/**
			 * unknown Flag, the reason for this change is unresolved, you might want to do full
			 * synchronization on the given uri
			 */
			object Unknown : Flag

			/**
			 * update Flag, the reason for this change is because there is an update within the Table Row,
			 * Uri sent refer to the updated Row
			 */
			object Update : Flag

			/**
			 * insert Flag, the reason for this change is because there is a new Row insertion,
			 * Uri sent refer to the inserted Row
			 */
			object Insert : Flag

			/**
			 * delete Flag, the reason for this change is because there is a Row deletion,
			 * Uri sent refer to the deleted Row
			 */
			object Delete : Flag

			// Internal use only
			object Unsupported : Flag

			companion object {
				inline val Flag.isUnknown
					get() = this is Unknown
				inline val Flag.isUpdate
					get() = this is Update
				inline val Flag.isInsert
					get() = this is Insert
				inline val Flag.isDelete
					get() = this is Delete
				internal inline val Flag.isUnsupported
					get() = this is Unsupported
			}
		}
	}
}
