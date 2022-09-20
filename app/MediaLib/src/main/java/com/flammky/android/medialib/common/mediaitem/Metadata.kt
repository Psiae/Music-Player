package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle

open class MediaMetadata internal constructor(
	val bundle: Bundle?,
	val title: String?,
	val playable: Boolean?
) {

	open class Builder {
		protected var bundle: Bundle? = null
		protected var title: String? = null
		protected var playable: Boolean? = null

		fun setBundle(bundle: Bundle?) = apply { this.bundle = bundle }
		fun setTitle(title: String?) = apply { this.title = title }
		fun setPlayable(playable: Boolean?) = apply { this.playable = playable }

		open fun build(): MediaMetadata = MediaMetadata(
			bundle = bundle,
			title = title,
			playable = playable
		)
	}

	open val media3 by lazy {
		androidx.media3.common.MediaMetadata.Builder()
			.setExtras(bundle)
			.setTitle(title)
			.setIsPlayable(playable)
			.build()
	}

	companion object {
		val UNSET = Builder().build()
	}
}

open class AudioMetadata internal constructor(
	bundle: Bundle?,
	title: String?,
	playable: Boolean?,
	val artist: String?,
	val album: String?,
	val albumArtist: String?,
	val bitrate: Long?,
	val durationMs: Long?
) : MediaMetadata(bundle, title, playable) {

	open class Builder : MediaMetadata.Builder() {
		protected var artist: String? = null
		protected var album: String? = null
		protected var albumArtist: String? = null
		protected var bitrate: Long? = null
		protected var durationMs: Long? = null

		fun setArtist(artist: String?) = apply {
			this.artist = artist
		}
		fun setAlbum(album: String?) = apply {
			this.album = album
		}
		fun setAlbumArtist(albumArtist: String?) = apply {
			this.albumArtist = albumArtist
		}
		fun setBitrate(bitrate: Long?) = apply {
			this.bitrate = bitrate
		}
		fun setDuration(durationMs: Long?) = apply {
			this.durationMs = durationMs
		}

		override fun build(): AudioMetadata = AudioMetadata(
			bundle = super.bundle,
			title = super.title,
			playable = super.playable,
			artist = artist,
			album = album,
			albumArtist = albumArtist,
			bitrate = bitrate,
			durationMs = durationMs
		)
	}

	override val media3: androidx.media3.common.MediaMetadata by lazy {
		bundle?.apply {
			putString("durationMs", "$durationMs")
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
