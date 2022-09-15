package com.flammky.android.medialib.temp.provider.mediastore.api28.audio

import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import com.flammky.android.common.io.exception.NoReadExternalStoragePermissionException
import com.flammky.android.common.kotlin.coroutines.CoroutineDispatchers
import com.flammky.android.medialib.temp.common.context.ContextInfo
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.api28.MediaStore28
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.AudioEntityProvider
import com.flammky.common.kotlin.throwable.throwAll
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.Executors

internal class AudioEntityProvider28 internal constructor(private val context: MediaStoreContext) :
	AudioEntityProvider<MediaStoreAudioEntity28, MediaStoreAudioFile28, MediaStoreAudioMetadata28, MediaStoreAudioQuery28> {

	private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
	private val scope = CoroutineScope(singleDispatcher + SupervisorJob())

	override val mediaItemFactory = MediaItemFactoryAudio28(context)

	private val contextInfo = ContextInfo(context.androidContext)

	private val contentResolver
		get() = context.androidContext.contentResolver

	private var cached = false
	private var cachedQuery: List<MediaStoreAudioEntity28> = emptyList()

	private val mutex = Mutex()

	private val audioContentObserver: ContentObserver

	init {
		audioContentObserver = AudioContentObserver(null)
		contentResolver.registerContentObserver(uri_audio_external, true, audioContentObserver)
	}

	@Throws(NoReadExternalStoragePermissionException::class)
	override suspend fun queryEntity(cacheAllowed: Boolean): List<MediaStoreAudioEntity28> {
		checkReadExternalStoragePermission()
		return mutex.withLock {
			if (!cached) {
				cachedQuery = queryAudioEntity()
				cached = true
			}
			cachedQuery
		}
	}

	private fun onUriContentChanged(uri: Uri) = scope.launch {
		mutex.withLock {
			cached = false
		}
	}

	private suspend fun queryAudioEntity(): List<MediaStoreAudioEntity28> = withContext(
		ioDispatcher
	) {
		val holder = mutableListOf<MediaStoreAudioEntity28>()

		try {
			contentResolver.query(
				/* uri = */ uri_audio_external,
				/* projection = */ entityDefaultProjector,
				/* selection = */ null,
				/* selectionArgs = */ null,
				/* sortOrder = */ null
			)?.use { cursor ->
				if (cursor.moveToFirst()) {
					val throwables: MutableList<Throwable> = mutableListOf()

					do {
						try {
							val builder = MediaStoreAudioEntity28.Builder()
							val entity = fillAudioEntity(cursor, builder).build()
							holder.add(entity)
						} catch (e: Exception) {
							throwables.add(e)
						}
					} while (cursor.moveToNext() && isActive)

					if (throwables.isNotEmpty()) throwables.throwAll()
				}
			}
		} catch (e: SecurityException) {
			checkReadExternalStoragePermission()
		}
		holder
	}

	private fun fillAudioEntity(
		cursor: Cursor,
		builder: MediaStoreAudioEntity28.Builder
	): MediaStoreAudioEntity28.Builder {
		return builder.apply {
			fileInfo = fillAudioFileInfo(cursor, MediaStoreAudioFile28.Builder()).build()
			metadataInfo = fillAudioMetadataInfo(cursor, MediaStoreAudioMetadata28.Builder()).build()
			queryInfo = fillAudioQueryInfo(cursor, MediaStoreAudioQuery28.Builder()).build()
			uid = createUID(queryInfo.id)
			uri = createUri(queryInfo.id)
		}
	}

	/**
	 * @see [MediaStoreAudioFile28]
	 */
	private fun fillAudioFileInfo(
		cursor: Cursor,
		builder: MediaStoreAudioFile28.Builder
	): MediaStoreAudioFile28.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATA)
			.takeIf { it > -1 }
			?.let { i -> absolutePath = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATE_ADDED)
			.takeIf { it > -1 }
			?.let { i -> dateAdded = cursor.getLong(i) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATE_MODIFIED)
			.takeIf { it > -1 }
			?.let { i -> dateModified = cursor.getLong(i) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DISPLAY_NAME)
			.takeIf { it > -1 }
			?.let { i -> fileName = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.MIME_TYPE)
			.takeIf { it > -1 }
			?.let { i -> mimeType = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.SIZE)
			.takeIf { it > -1 }
			?.let { i -> size = cursor.getLong(i) }
	}

	/**
	 * @see [MediaStoreAudioMetadata28]
	 */
	private fun fillAudioMetadataInfo(
		cursor: Cursor,
		builder: MediaStoreAudioMetadata28.Builder
	): MediaStoreAudioMetadata28.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ALBUM)
			.takeIf { it > -1 }
			?.let { i -> album = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ARTIST)
			.takeIf { it > -1 }
			?.let { i -> artist = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.BOOKMARK)
			.takeIf { it > -1 }
			?.let { i -> bookmark = cursor.getLong(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.COMPOSER)
			.takeIf { it > -1 }
			?.let { i -> composer = cursor.getString(i) ?: "" }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.DURATION)
			.takeIf { it > -1 }
			?.let { i -> durationMs = cursor.getLong(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.TITLE)
			.takeIf { it > -1 }
			?.let { i -> title = cursor.getString(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.YEAR)
			.takeIf { it > -1 }
			?.let { i -> year = cursor.getInt(i) }
	}

	/**
	 * @see [MediaStoreAudioQuery28]
	 */
	private fun fillAudioQueryInfo(
		cursor: Cursor,
		builder: MediaStoreAudioQuery28.Builder
	): MediaStoreAudioQuery28.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns._ID)
			.takeIf { it > -1 }
			?.let { i ->
				id = cursor.getLong(i)
				uri = createUri(id)
			}
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ALBUM_ID)
			.takeIf { it > -1 }
			?.let { i -> albumId = cursor.getLong(i) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ARTIST_ID)
			.takeIf { it > -1 }
			?.let { i -> artistId = cursor.getLong(i) }

		version = MediaStore28.getVersion(context.androidContext)
	}


	private fun checkReadExternalStoragePermission(): Boolean {
		if (!contextInfo.permissionInfo.readExternalStorageAllowed) {
			throw NoReadExternalStoragePermissionException()
		}
		return true
	}

	private fun createUID(id: Long): String =
		MediaStoreContract28.audioEntityUID(
			id
		)

	private fun createUri(id: Long): Uri = ContentUris.withAppendedId(uri_audio_external, id)


	private inner class AudioContentObserver(private val handler: Handler?) : ContentObserver(handler) {
		override fun deliverSelfNotifications(): Boolean = true

		override fun onChange(selfChange: Boolean) {
			Timber.d("AudioEntityProvider, onChange(Boolean) \n$selfChange")
			// call super will do nothing
		}

		override fun onChange(selfChange: Boolean, uri: Uri?) {
			Timber.d("AudioEntityProvider, onChange(Boolean, Uri) \n$selfChange\n$uri")
			// call super will call above function without uri
		}

		override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
			Timber.d("AudioEntityProvider, onChange(Boolean, Uri, Int) \n$selfChange\n$uri\n$flags")
			// call super will call above function without flags
		}

		override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
			Timber.d("AudioEntityProvider, onChange(Boolean, MutableCollection<Uri>, Int) \n$selfChange\n${uris.joinToString()}\n$flags")
			// call super will call above function on each uri(s)
			uris.forEach { notifyUriContentChanged(it) }
		}

		private fun notifyUriContentChanged(uri: Uri) {
			if (!uri.toString().startsWith(uri_audio_external.toString())) return
			onUriContentChanged(uri)
		}
	}

	companion object {
		private val uri_audio_external: Uri = MediaStore28.Audio.EXTERNAL_CONTENT_URI

		private val ioDispatcher: CoroutineDispatcher = CoroutineDispatchers.ANDROID.io

		/**
		 * Projector to fill [MediaStoreAudioEntity28.queryInfo]
		 *
		 * @see [MediaStoreAudioQuery28]
		 */
		private val queryInfoProjector: Array<String> = arrayOf(
			MediaStore28.MediaColumns._ID,
			MediaStore28.Audio.AudioColumns.ARTIST_ID,
			MediaStore28.Audio.AudioColumns.ALBUM_ID
		)

		/**
		 * Projector to fill [MediaStoreAudioEntity28.fileInfo]
		 *
		 * @see [MediaStoreAudioFile28]
		 */
		private val fileInfoProjector: Array<String> = arrayOf(
			MediaStore28.Files.FileColumns.DATA,
			MediaStore28.Files.FileColumns.DATE_ADDED,
			MediaStore28.Files.FileColumns.DATE_MODIFIED,
			MediaStore28.Files.FileColumns.DISPLAY_NAME,
			MediaStore28.Files.FileColumns.MIME_TYPE,
			MediaStore28.Files.FileColumns.SIZE
		)

		/**
		 * Projector to fill [MediaStoreAudioEntity28.metadataInfo]
		 *
		 * @see [MediaStoreAudioMetadata28]
		 */
		private val metadataInfoProjector: Array<String> = arrayOf(
			MediaStore28.Audio.AudioColumns.ALBUM,
			MediaStore28.Audio.AudioColumns.ARTIST,
			MediaStore28.Audio.AudioColumns.BOOKMARK,
			MediaStore28.Audio.AudioColumns.COMPOSER,
			MediaStore28.Audio.AudioColumns.DURATION,
			MediaStore28.Audio.AudioColumns.TITLE,
			MediaStore28.Audio.AudioColumns.YEAR
		)

		private val entityDefaultProjector: Array<String> = arrayOf(
			*queryInfoProjector,
			*fileInfoProjector,
			*metadataInfoProjector
		)

		private val entityDefaultSelector: String = "${MediaStore28.Audio.AudioColumns.IS_MUSIC} == 1"

		private val entityDefaultSortOrder: String = MediaStore28.Audio.DEFAULT_SORT_ORDER
	}
}
