package com.flammky.android.medialib.providers.mediastore.api28.audio

import android.content.ContentUris
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.io.exception.ReadExternalStoragePermissionException
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.BuildConfig
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext.Companion.android
import com.flammky.android.medialib.providers.mediastore.api28.MediaStore28


internal class AudioEntityProvider28 (private val context: MediaStoreContext) {
	private val contextHelper = ContextHelper(context.android)
	private val contentResolver = context.android.contentResolver

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	fun query(): List<MediaStoreAudioEntity28> {
		checkReadExternalStoragePermission()

		val holder = mutableListOf<MediaStoreAudioEntity28>()

		try {
			contentResolver.query(
				/* uri = */ uri_audio_external,
				/* projection = */ entityDefaultProjector,
				/* selection = */ null,
				/* selectionArgs = */ null,
				/* sortOrder = */ null
			)?.use {  cursor ->
				if (cursor.moveToFirst()) {
					do {
						val entity = fillAudioEntityBuilder(cursor, MediaStoreAudioEntity28.Builder()).build()
						holder.add(entity)
					} while (cursor.moveToNext())
				}
			}
		} catch (se: SecurityException) {
			checkReadExternalStoragePermission()
			if (BuildConfig.DEBUG) throw se
		}
		return holder.toList().also {
			// ensure there's no corrupt scan
			/*rescanFiles(it.mapNotNull { entity -> entity.file.absolutePath }.toTypedArray())*/
		}
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	fun queryById(id: String): MediaStoreAudioEntity28? {
		if (!id.startsWith("MediaStore_")) return null
		val num = id.takeLastWhile { it.isDigit() }
		return queryByUri(ContentUris.withAppendedId(uri_audio_external, num.toLong()))
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	fun queryByUri(uri: Uri): MediaStoreAudioEntity28? {
		checkReadExternalStoragePermission()
		return try {
			contentResolver.query(
				/* uri = */ uri,
				/* projection = */ entityDefaultProjector,
				/* selection = */ null,
				/* selectionArgs = */ null,
				/* sortOrder = */ null
			)?.use {  cursor ->
				if (cursor.moveToFirst()) {
					return fillAudioEntityBuilder(cursor, MediaStoreAudioEntity28.Builder()).build()
				}
			}
			null
		} catch (se: SecurityException) {
			checkReadExternalStoragePermission()
			if (BuildConfig.DEBUG) throw se
			null
		}
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	fun queryUris(): List<Uri> {
		checkReadExternalStoragePermission()
		return try {
			val holder = mutableListOf<Uri>()
			contentResolver.query(
				/* uri = */ uri_audio_external,
				/* projection = */ entityDefaultProjector,
				/* selection = */ null,
				/* selectionArgs = */ null,
				/* sortOrder = */ null
			)?.use { cursor ->
				if (cursor.moveToFirst()) {
					do {
						val queryInfo = fillAudioQueryInfoBuilder(cursor, MediaStoreAudioQuery28.Builder())
						holder.add(queryInfo.uri)
					} while (cursor.moveToNext())
				}
			}
			holder.toList()
		} catch (se: SecurityException) {
			checkReadExternalStoragePermission()
			if (BuildConfig.DEBUG) throw se
			emptyList()
		}
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	private fun checkReadExternalStoragePermission() {
		if (!contextHelper.permissions.common.hasReadExternalStorage) {
			throw ReadExternalStoragePermissionException()
		}
	}

	private fun fillAudioEntityBuilder(
		cursor: Cursor,
		builder: MediaStoreAudioEntity28.Builder
	): MediaStoreAudioEntity28.Builder = builder.apply {
		val file = MediaStoreAudioFile28
			.Builder().apply { fillAudioFileBuilder(cursor,this) }.build()
		val metadata = MediaStoreAudioMetadataEntry28Entry
			.Builder().apply { fillAudioMetadataBuilder(cursor, this) }.build()
		val queryInfo = MediaStoreAudioQuery28
			.Builder().apply { fillAudioQueryInfoBuilder(cursor, this) }.build()

		/* Include Generation? */
		val id = queryInfo.id
		val uid = "MediaStore_28_AUDIO_${queryInfo.id}"
		val uri = ContentUris.withAppendedId(uri_audio_external, id)

		setFile(file)
		setMetadata(metadata)
		setQueryInfo(queryInfo)
		setUID(uid)
		setUri(uri)
	}

	private fun fillAudioFileBuilder(
		cursor: Cursor,
		builder: MediaStoreAudioFile28.Builder
	): MediaStoreAudioFile28.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATA)
			.takeIf { it > -1 }
			?.let { i -> setAbsolutePath(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATE_ADDED)
			.takeIf { it > -1 }
			?.let { i -> setDateAdded(cursor.getLong(i)) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DATE_MODIFIED)
			.takeIf { it > -1 }
			?.let { i -> setDateModified(cursor.getLong(i)) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.DISPLAY_NAME)
			.takeIf { it > -1 }
			?.let { i -> setFileName(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.MIME_TYPE)
			.takeIf { it > -1 }
			?.let { i -> setMimeType(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Files.FileColumns.SIZE)
			.takeIf { it > -1 }
			?.let { i -> setSize(cursor.getLong(i)) }
	}

	private fun fillAudioMetadataBuilder(
		cursor: Cursor,
		builder: MediaStoreAudioMetadataEntry28Entry.Builder
	): MediaStoreAudioMetadataEntry28Entry.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ALBUM)
			.takeIf { it > -1 }
			?.let { i -> setAlbum(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ARTIST)
			.takeIf { it > -1 }
			?.let { i -> setArtist(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.BOOKMARK)
			.takeIf { it > -1 }
			?.let { i -> setBookmark(cursor.getLong(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.COMPOSER)
			.takeIf { it > -1 }
			?.let { i -> setComposer(cursor.getString(i))}
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.DURATION)
			.takeIf { it > -1 }
			?.let { i -> setDurationMs(cursor.getLong(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.TITLE)
			.takeIf { it > -1 }
			?.let { i -> setTitle(cursor.getString(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.YEAR)
			.takeIf { it > -1 }
			?.let { i -> setYear(cursor.getInt(i)) }
	}

	private fun fillAudioQueryInfoBuilder(
		cursor: Cursor,
		builder: MediaStoreAudioQuery28.Builder
	): MediaStoreAudioQuery28.Builder = builder.apply {
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns._ID)
			.takeIf { it > -1 }
			?.let { i ->
				setId(cursor.getLong(i))
				setUri(ContentUris.withAppendedId(uri_audio_external, this.id))
			}
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ALBUM_ID)
			.takeIf { it > -1 }
			?.let { i -> setAlbumId(cursor.getLong(i)) }
		cursor
			.getColumnIndex(MediaStore28.Audio.AudioColumns.ARTIST_ID)
			.takeIf { it > -1 }
			?.let { i -> setArtistId(cursor.getLong(i)) }
	}

	private fun rescanFiles(paths: Array<String>) {
		MediaScannerConnection.scanFile(context.android, paths, null, null)
	}

	companion object {
		private val uri_audio_external: Uri = MediaStore28.Audio.EXTERNAL_CONTENT_URI
		private val AndroidDispatchers = AndroidCoroutineDispatchers.DEFAULT

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
		 * Projector to fill [MediaStoreAudioEntity28.file]
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
		 * Projector to fill [MediaStoreAudioEntity28.metadata]
		 *
		 * @see [MediaStoreAudioMetadataEntry28Entry]
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
