package com.kylentt.mediaplayer.core.exoplayer.mediaItem

import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.data.SongEntity
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong

interface MediaItemFactory {
	fun buildMediaItem(song: SongEntity): MediaItem
}

object MediaItemFactoryImpl : MediaItemFactory {

	override fun buildMediaItem(song: SongEntity): MediaItem {
		return when (song) {
			is MediaStoreSong -> fromMediaStoreSong(song)
			else -> fromSongEntity(song)
		}
	}

	private fun fromSongEntity(song: SongEntity): MediaItem {
		return song.asMediaItem
	}

	private fun fromMediaStoreSong(song: MediaStoreSong): MediaItem {
		return song.asMediaItem
	}
}
