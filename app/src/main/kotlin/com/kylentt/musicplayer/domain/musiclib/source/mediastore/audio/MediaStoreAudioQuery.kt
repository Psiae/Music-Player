package com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio

import com.kylentt.musicplayer.domain.musiclib.source.mediastore.query.MediaStoreQuery

abstract class MediaStoreAudioQuery internal constructor() : MediaStoreQuery() {
	abstract val albumId: Long
	abstract val artistId: Long
	abstract val version: Long
}
