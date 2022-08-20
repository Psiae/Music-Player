package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api28

import android.net.Uri

// TODO

data class MediaStoreAudioEntity28(
	val id: String,
	val uri: Uri,
	val file: MediaStoreAudioFile28,
	val metadata: MediaStoreAudioMetadata28
)
data class MediaStoreAudioFile28(
	val dateAdded: Int,
	val dateModified: Int,
	val fileAbsolutePath: String,
	val fileExtension: String,
	val fileName: String,
	val fileSize: Int,
)

data class MediaStoreAudioMetadata28(
	val artist: String,
	val album: String,
	val durationMS: Long,
	val title: String,
	val year: Int
)
