package com.kylentt.musicplayer.domain.musiclib.source.mediastore.api29

import android.net.Uri

// TODO

data class MediaStoreAudioEntity29(
	val id: Long,
	val uri: Uri,
	val file: MediaStoreAudioFile29,
	val metadata: MediaStoreAudioMetadata29
)



data class MediaStoreAudioMetadata29(
	val artist: String,
	val album: String,
	val durationMS: Long,
	val title: String,
	val year: Int
)
