package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle
import kotlin.time.Duration

open class MediaMetadata internal constructor(
	val extra: Extra?,
	val title: String?,
) {

	class Extra(val bundle: Bundle)

	class Builder {
		protected var extra: Extra? = null
		protected var title: String? = null

		fun setExtra(extra: Extra?) = apply { this.extra = extra }
		fun setTitle(title: String?) = apply { this.title = title }

		fun build(): MediaMetadata = MediaMetadata(
			extra = extra,
			title = title
		)
	}

	companion object {
		val UNSET = Builder().build()
	}
}

// Better name?
open class PlaybackMetadata internal constructor(
	extra: Extra?,
	title: String?,
	val bitrate: Long?,
	val duration: Duration?,
	val playable: Boolean?
) : MediaMetadata(extra, title) {

	class Builder {
		private var extra: Extra? = null
		private var title: String? = null
		private var bitrate: Long? = null
		private var duration: Duration? = null
		private var playable: Boolean? = null

		fun setExtra(extra: Extra?) = apply {
			this.extra = extra
		}
		fun setTitle(title: String?) = apply {
			this.title = title
		}
		fun setBitrate(bitrate: Long?) = apply {
			this.bitrate = bitrate
		}
		fun setDuration(duration: Duration?) = apply {
			this.duration = duration
		}
		fun setPlayable(playable: Boolean?) = apply {
			this.playable = playable
		}

		fun build(): PlaybackMetadata = PlaybackMetadata(
			extra = extra,
			title = title,
			bitrate = bitrate,
			duration = duration,
			playable = playable
		)
	}
}

open class AudioMetadata (
	extra: Extra?,
	title: String?,
	bitrate: Long?,
	duration: Duration?,
	playable: Boolean?,
	val artist: String?,
	val albumTitle: String?,
	val albumArtist: String?,
) : PlaybackMetadata(extra, title, bitrate, duration, playable) {

	class Builder {
		private var extra: Extra? = null
		private var title: String? = null
		private var bitrate: Long? = null
		private var duration: Duration? = null
		private var playable: Boolean? = null
		private var artist: String? = null
		private var albumTitle: String? = null
		private var albumArtist: String? = null

		fun setExtra(extra: Extra) = apply {
			this.extra = extra
		}
		fun setTitle(title: String?) = apply {
			this.title = title
		}
		fun setBitrate(bitrate: Long?) = apply {
			this.bitrate = bitrate
		}
		fun setDuration(duration: Duration?) = apply {
			this.duration = duration
		}
		fun setPlayable(playable: Boolean?) = apply {
			this.playable = playable
		}
		fun setArtist(artist: String?) = apply {
			this.artist = artist
		}
		fun setAlbumTitle(album: String?) = apply {
			this.albumTitle = album
		}
		fun setAlbumArtist(albumArtist: String?) = apply {
			this.albumArtist = albumArtist
		}

		fun build(): AudioMetadata = AudioMetadata(
			extra = extra,
			title = title,
			duration = duration,
			playable = playable,
			artist = artist,
			albumTitle = albumTitle,
			albumArtist = albumArtist,
			bitrate = bitrate,
		)
	}
	companion object {
		val UNSET = Builder().build()
	}
}
