package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle
import kotlin.time.Duration

typealias PlaybackMetadata = MediaMetadata.Playback
typealias AudioMetadata = MediaMetadata.Audio

/**
 * Metadata Information Container for general Media Item
 */
sealed interface MediaMetadata {

	val extra: Extra?

	val title: String?

	class Extra(val bundle: Bundle)


	interface Builder {
		val extra: Extra?
		val title: String?
		fun setExtra(extra: Extra?): Builder
		fun setTitle(title: String?): Builder

		fun build(): MediaMetadata
	}

	open class BaseMediaMetadata internal constructor(
		override val extra: Extra?,
		override val title: String?
	) : MediaMetadata {

		open class Builder : MediaMetadata.Builder {
			override var extra: Extra? = null
				protected set

			override var title: String? = null
				protected set

			override fun setExtra(extra: Extra?): MediaMetadata.Builder = apply {
				this.extra = extra
			}

			override fun setTitle(title: String?): MediaMetadata.Builder = apply {
				this.title = title
			}

			override fun build(): BaseMediaMetadata = BaseMediaMetadata(extra, title)
		}
	}

	/**
	 * Metadata Information Container for MediaItem capable of Media playback
	 */

	// Think of Better name?
	open class Playback internal constructor(
		override val extra: Extra?,
		override val title: String?,
		val bitrate: Long?,
		val duration: Duration?,
		val playable: Boolean?
	) : MediaMetadata {

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

			fun build(): Playback = Playback(
				extra = extra,
				title = title,
				bitrate = bitrate,
				duration = duration,
				playable = playable
			)
		}

		companion object {
			val UNSET = Builder().build()
		}
	}

	/**
	 * Metadata Information Container for Audio MediaItem, this class is open.
	 */
	open class Audio (
		extra: Extra?,
		title: String?,
		bitrate: Long?,
		duration: Duration?,
		playable: Boolean?,
		val artist: String?,
		val albumTitle: String?,
		val albumArtist: String?,
	) : Playback(extra, title, bitrate, duration, playable) {

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

			fun build(): Audio = Audio(
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
			fun build(apply: Builder.() -> Unit) = Builder().apply(apply).build()
		}
	}

	object UNSET : MediaMetadata {
		override val extra: Extra? = null
		override val title: String? = null
	}

	companion object {
		fun build(apply: MediaMetadata.Builder.() -> Unit): MediaMetadata {
			return BaseMediaMetadata.Builder().apply(apply).build()
		}
	}
}
