package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle
import javax.annotation.concurrent.Immutable
import kotlin.time.Duration

@Immutable
open class MediaMetadata internal constructor(
	val bundle: Bundle?,
	val title: String?,
) {

	open class Builder {
		protected var bundle: Bundle? = null
		protected var title: String? = null

		fun setBundle(bundle: Bundle?) = apply { this.bundle = bundle }
		fun setTitle(title: String?) = apply { this.title = title }

		open fun build(): MediaMetadata = MediaMetadata(
			bundle = bundle,
			title = title,
		)
	}

	open val media3 by lazy {
		androidx.media3.common.MediaMetadata.Builder()
			.setExtras(bundle)
			.setTitle(title)
			.build()
	}

	companion object {
		val UNSET = Builder().build()
	}
}

// Better name?
@Immutable
open class PlaybackMetadata(
	bundle: Bundle?,
	title: String?,
	val bitrate: Long?,
	val duration: Duration?,
	val playable: Boolean?
) : MediaMetadata(bundle, title) {

	open class Builder : MediaMetadata.Builder() {
		protected var bitrate: Long? = null
		protected var playable: Boolean? = null
		protected var duration: Duration? = null

		fun setBitrate(bitrate: Long?) = apply {
			this.bitrate = bitrate
		}
		fun setPlayable(playable: Boolean?) = apply {
			this.playable = playable
		}
		fun setDuration(duration: Duration?) = apply {
			this.duration = duration
		}

		override fun build(): PlaybackMetadata = PlaybackMetadata(
			bundle = super.bundle,
			title = super.title,
			bitrate = bitrate,
			duration = duration,
			playable = playable
		)
	}
}

open class AudioMetadata internal constructor(
	bundle: Bundle?,
	title: String?,
	bitrate: Long?,
	duration: Duration?,
	playable: Boolean?,
	val artist: String?,
	val album: String?,
	val albumArtist: String?,
) : PlaybackMetadata(bundle, title, bitrate, duration, playable) {

	open class Builder : PlaybackMetadata.Builder() {
		protected var artist: String? = null
		protected var album: String? = null
		protected var albumArtist: String? = null

		fun setArtist(artist: String?) = apply {
			this.artist = artist
		}
		fun setAlbum(album: String?) = apply {
			this.album = album
		}
		fun setAlbumArtist(albumArtist: String?) = apply {
			this.albumArtist = albumArtist
		}

		override fun build(): AudioMetadata = AudioMetadata(
			bundle = super.bundle,
			title = super.title,
			duration = super.duration,
			playable = super.playable,
			artist = artist,
			album = album,
			albumArtist = albumArtist,
			bitrate = bitrate,
		)
	}

	override val media3: androidx.media3.common.MediaMetadata by lazy {
		bundle?.apply {
			putString("durationMs", "${duration?.inWholeSeconds}")
			putString("bitrate", "$bitrate")
		}
		androidx.media3.common.MediaMetadata.Builder()
			.setExtras(bundle)
			.setTitle(title)
			.setIsPlayable(playable)
			.setArtist(artist)
			.setAlbumTitle(album)
			.setAlbumArtist(artist)
			.build()
	}

	companion object {
		val UNSET = Builder().build()
	}
}
