package com.kylentt.musicplayer.domain.musiclib.source.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore.Images.ImageColumns
import android.provider.MediaStore.MediaColumns
import com.kylentt.musicplayer.BuildConfig
import com.kylentt.musicplayer.common.android.context.ContextInfo
import com.kylentt.musicplayer.common.android.exception.ReadStoragePermissionException
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.audiofile.AudioFileInfo
import com.kylentt.musicplayer.domain.musiclib.audiofile.AudioFileMetadata
import com.kylentt.musicplayer.domain.musiclib.entity.AudioEntity
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal abstract class MediaStoreImplBase(private val context: Context) {

	protected open val dispatchers: CoroutineDispatchers = CoroutineDispatchers.DEFAULT
	protected open val contextInfo: ContextInfo = ContextInfo(context)

	protected open val audioEntityQueryProjector: Array<String> = arrayOf(
		/* columnId */ BaseColumns._ID
	)

	/**
	 * https://developer.android.com/reference/android/provider/MediaStore.MediaColumns
	 */
	protected open val audioFileInfoProjector: Array<String> = arrayOf(
		MediaColumns.DATA,
		MediaColumns.DATE_ADDED,
		MediaColumns.DATE_MODIFIED,
		MediaColumns.DISPLAY_NAME,
		MediaColumns.MIME_TYPE,
		MediaColumns.SIZE
	)

	protected open val audioEntityMetadataInfoProjector: Array<String> = arrayOf(
		MediaColumns.ARTIST,
		MediaColumns.ALBUM,
		MediaColumns.DURATION,
		MediaColumns.TITLE,
	)

	open suspend fun queryAudioEntity(
		fillFileInfo: Boolean,
		fillMetadata: Boolean
	): List<AudioEntity> = withContext(dispatchers.io) {
		if (!contextInfo.permissionInfo.readExternalStorageAllowed) throw ReadStoragePermissionException()

		val holder = mutableListOf<AudioEntity>()

		val projector = when {
			fillFileInfo && fillMetadata -> arrayOf(
				*audioEntityQueryProjector,
				*audioFileInfoProjector,
				*audioEntityMetadataInfoProjector
			)
			fillFileInfo -> arrayOf(
				*audioEntityQueryProjector,
				*audioFileInfoProjector
			)
			fillMetadata -> arrayOf(
				*audioEntityQueryProjector,
				*audioFileInfoProjector
			)
			else -> audioEntityQueryProjector
		}

		try {
			context.contentResolver.query(
				/* uri = */
				MediaStoreInfo.Audio.EXTERNAL_CONTENT_URI,
				/* projection = */
				projector,
				/* selection = */
				"${android.provider.MediaStore.Audio.Media.IS_MUSIC} == 1",
				/* selectionArgs = */
				null,
				/* sortOrder = */
				android.provider.MediaStore.Audio.Media.DATE_ADDED,
			)?.use { cursor ->
				if (cursor.moveToFirst()) {
					do {
						ensureActive()

						val id = cursor.getColumnIndex(BaseColumns._ID).takeIf { it != -1 }
							?.let { cursor.getLong(it) }.toString()

						val uri = ContentUris.withAppendedId(MediaStoreInfo.Audio.EXTERNAL_CONTENT_URI, id.toLong())

						val fileInfoBuilder = AudioFileInfo.Builder()

						val fileInfo = when {
							fillFileInfo -> {
								fillAudioFileInfoBuilder(
									cursor = cursor,
									builder = fileInfoBuilder,
									fillMetadata = fillMetadata
								)
							}
							fillMetadata -> {
								val metadataBuilder = AudioFileMetadata.Builder()
								fileInfoBuilder.metadata = fillMetadataBuilder(cursor, metadataBuilder).build()
								fileInfoBuilder
							}
							else -> fileInfoBuilder
						}.build()

						val entity = AudioEntity(
							id = id,
							uid = MediaStoreProvider.UID_Audio_Prefix + id,
							fileInfo = fileInfo,
							uri = uri
						)

						holder.add(entity)
					} while (cursor.moveToNext())
				}
			}
		} catch (e: Exception) {
			if (BuildConfig.DEBUG) throw e
		}
		holder
	}

	open suspend fun fillAudioFileInfoBuilder(
		uri: Uri,
		builder: AudioFileInfo.Builder = AudioFileInfo.Builder(),
		fillMetadata: Boolean
	): AudioFileInfo.Builder = withContext(dispatchers.io) {
		if (!contextInfo.permissionInfo.readExternalStorageAllowed) throw ReadStoragePermissionException()

		if (uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_FILE) {
			try {
				context.contentResolver.query(
					/* uri = */
					uri,
					/* projection = */
					audioFileInfoProjector,
					/* selection = */
					null,
					/* selectionArgs = */
					null,
					/* sortOrder = */
					null
				)?.use { cursor ->
					if (cursor.moveToFirst()) {
						fillAudioFileInfoBuilder(cursor, builder, fillMetadata)
					}
				}
			} catch (e: Exception) {
				if (BuildConfig.DEBUG) throw e
			}
		}
		builder
	}

	open suspend fun fillAudioFileInfoBuilder(
		cursor: Cursor,
		builder: AudioFileInfo.Builder,
		fillMetadata: Boolean
	): AudioFileInfo.Builder {
		cursor.getColumnIndex(ImageColumns.BUCKET_DISPLAY_NAME).takeIf { it != -1 }
			?.let { builder.parentFileName = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.DATA).takeIf { it != -1 }
			?.let { builder.absolutePath = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.DATE_ADDED).takeIf { it != -1 }
			?.let { builder.dateAdded = cursor.getInt(it).toLong() }

		cursor.getColumnIndex(MediaColumns.DATE_MODIFIED).takeIf { it != -1 }
			?.let { builder.dateModified = cursor.getInt(it).toLong() }

		cursor.getColumnIndex(MediaColumns.DISPLAY_NAME).takeIf { it != -1 }
			?.let { builder.fileName = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.MIME_TYPE).takeIf { it != -1 }
			?.let { builder.mimeType = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.SIZE).takeIf { it != -1 }
			?.let { builder.fileSize = cursor.getInt(it) }

		val metadataBuilder = AudioFileMetadata.Builder()
		if (fillMetadata) fillMetadataBuilder(cursor, metadataBuilder)

		builder.metadata = metadataBuilder.build()
		return builder
	}

	open suspend fun fillMetadataBuilder(
		uri: Uri,
		builder: AudioFileMetadata.Builder
	): AudioFileMetadata.Builder {
		if (!contextInfo.permissionInfo.readExternalStorageAllowed) throw ReadStoragePermissionException()

		if (uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_FILE) {
			context.contentResolver.query(
				/* uri = */
				uri,
				/* projection = */
				audioEntityMetadataInfoProjector,
				/* selection = */
				null,
				/* selectionArgs = */
				null,
				/* sortOrder = */
				null
			)?.use {
				if (it.moveToFirst()) fillMetadataBuilder(it, builder)
			}
		}
		return builder
	}

	open suspend fun fillMetadataBuilder(
		cursor: Cursor,
		builder: AudioFileMetadata.Builder
	): AudioFileMetadata.Builder {
		cursor.getColumnIndex(MediaColumns.ARTIST).takeIf { it != -1 }
			?.let { builder.artist = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.ALBUM).takeIf { it != -1 }
			?.let { builder.album = cursor.getString(it) }

		cursor.getColumnIndex(MediaColumns.DURATION).takeIf { it != -1 }
			?.let { builder.durationMs = cursor.getLong(it) }

		cursor.getColumnIndex(MediaColumns.TITLE).takeIf { it != -1 }
			?.let { builder.title = cursor.getString(it) }

		builder.playable = builder.durationMs > 0
		return builder
	}

	companion object {
	}
}
