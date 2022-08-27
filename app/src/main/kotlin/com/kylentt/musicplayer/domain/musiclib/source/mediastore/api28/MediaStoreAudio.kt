package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import android.net.Uri
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.audio.MediaStoreAudioEntity
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.audio.MediaStoreAudioFile
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.base.audio.MediaStoreAudioMetadata

/**
 * data class representing an Audio Entity
 * @see MediaStore28.MediaColumns._ID
 */
data class MediaStoreAudioEntity28(
	override val id: Long,
	override val uri: Uri,
	override val fileInfo: MediaStoreAudioFile28,
	override val metadata: MediaStoreAudioMetadata28
) : MediaStoreAudioEntity()

/**
 * data class representing an Audio File
 * @see MediaStore28.Files.FileColumns
 */
data class MediaStoreAudioFile28(
	override val absolutePath: String,
	override val bucketId: Long,
	override val bucketDisplayName: String,
	override val dateAdded: Long,
	override val dateModified: Long,
	override val mediaType: String,
	override val mimeType: String,
	override val fileExtension: String,
	override val fileName: String,
	override val fileNameWithExtension: String,
	override val size: Long,
) : MediaStoreAudioFile()

/**
 * data class representing an Audio File Metadata
 * @see MediaStore28.Audio.AudioColumns
 */
data class MediaStoreAudioMetadata28(
	override val artistId: Long,
	override val artistName: String,
	override val albumId: Long,
	override val albumName: String,
	val durationMS: Long,
	override val title: String,
	val year: Int
) : MediaStoreAudioMetadata()
