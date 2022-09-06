package com.kylentt.musicplayer.domain.musiclib.entity

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.domain.musiclib.audiofile.AudioFileInfo
import com.kylentt.musicplayer.domain.musiclib.media3.mediaitem.MediaMetadataHelper.putDisplayTitle
import com.kylentt.musicplayer.domain.musiclib.media3.mediaitem.MediaMetadataHelper.putFileName
import com.kylentt.musicplayer.domain.musiclib.media3.mediaitem.MediaMetadataHelper.putStoragePath
import java.io.File
import java.util.Enumeration

data class AudioEntity(
	val id: String,
	val uid: String,
	val fileInfo: AudioFileInfo,
	val uri: Uri
) {

	val mediaItem by lazy {

		val bundle = Bundle()

		val metadataBuilder = MediaMetadata.Builder()
			.putDisplayTitle(fileInfo.fileName)
			.putFileName(fileInfo.fileName)
			.setArtist(fileInfo.metadata.artist)
			.setAlbumTitle(fileInfo.metadata.album)
			.setSubtitle(fileInfo.metadata.artist)
			.setTitle(fileInfo.metadata.title) // MediaSession automatically use this one for Notification
			.setIsPlayable(fileInfo.metadata.playable)

		val metadataRequestBuilder = MediaItem.RequestMetadata.Builder()
			.setMediaUri(uri)
			.setExtras(Bundle())

		if (fileInfo.absolutePath.isNotBlank() && File(fileInfo.absolutePath).exists()) {
			metadataBuilder.putStoragePath(fileInfo.absolutePath, bundle)
		}

		MediaItem.Builder()
			.setMediaId(uid)
			.setUri(uri)
			.setMediaMetadata(metadataBuilder.build())
			.setRequestMetadata(metadataRequestBuilder.build())
			.build()
	}
}
