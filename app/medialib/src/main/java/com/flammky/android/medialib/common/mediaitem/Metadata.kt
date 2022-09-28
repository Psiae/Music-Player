package com.flammky.android.medialib.common.mediaitem

import android.os.Bundle
import kotlin.time.Duration

typealias PlaybackMetadata = MediaMetadata.Playback
typealias AudioMetadata = MediaMetadata.Audio

/**
 * Metadata Information Container for general Media Item.
 *
 * This class is sealed but [Audio], [Image], [Video] are open to extend
 */
sealed class MediaMetadata(
	val extra: Extra?,
	val title: String?
) {

	/** Extra configuration regardless of class type */
	class Extra(val bundle: Bundle)

	/** Builder interface for [MediaMetadata] subclass builders */
	interface Builder {
		val extra: Extra?
		val title: String?
		fun setExtra(extra: Extra?): Builder
		fun setTitle(title: String?): Builder

		fun build(): MediaMetadata
	}

	/**
	 * Metadata Information Container for MediaItem capable of Media playback,
	 * i.e Audio's and Video's, both shares this class as base, GIFs does not count in this case
	 */

	// Think of Better name?
	sealed class Playback(
		extra: Extra?,
		title: String?,
		val bitrate: Long?,
		val duration: Duration?,
		val playable: Boolean?
	) : MediaMetadata(extra, title) {

		interface Builder : MediaMetadata.Builder {

			val bitrate: Long?
			val duration: Duration?
			val playable: Boolean?

			// overrides
			override val extra: Extra?
			override val title: String?

			fun setBitrate(bitrate: Long?): Builder

			fun setDuration(duration: Duration?): Builder

			fun setPlayable(playable: Boolean?): Builder

			// overrides
			override fun setExtra(extra: Extra?): Builder
			override fun setTitle(title: String?): Builder
			override fun build(): Playback
		}

		companion object {
			val UNSET: Playback = build {  }
			fun build(apply: Builder.() -> Unit): Playback {
				return DefaultPlaybackMediaMetadata.Builder().apply(apply).build()
			}
		}
	}

	/**
	 * Metadata Information Container for Audio MediaItem.
	 *
	 * @see [Companion.build] to build instances
	 */
	open class Audio protected constructor(
		extra: Extra?,
		title: String?,
		bitrate: Long?,
		duration: Duration?,
		playable: Boolean?,
		val artistName: String?,
		val albumTitle: String?,
		val albumArtistName: String?
	) : Playback(extra, title, bitrate, duration, playable) {

		interface Builder : Playback.Builder {

			val artistName: String?
			val albumTitle: String?
			val albumArtist: String?

			// overrides
			override val extra: Extra?
			override val title: String?
			override val bitrate: Long?
			override val duration: Duration?
			override val playable: Boolean?

			fun setArtist(artistName: String?): Builder
			fun setAlbumTitle(albumTitle: String?): Builder
			fun setAlbumArtist(albumArtist: String?): Builder

			// overrides
			override fun setExtra(extra: Extra?): Builder
			override fun setTitle(title: String?): Builder
			override fun setBitrate(bitrate: Long?): Builder
			override fun setDuration(duration: Duration?): Builder
			override fun setPlayable(playable: Boolean?): Builder
			override fun build(): Audio
		}
		companion object {
			val UNSET = build {  }
			fun build(apply: Builder.() -> Unit) = DefaultAudioMetadata.Builder().apply(apply).build()
		}
	}

	companion object {
		val UNSET = build { }

		fun build(apply: MediaMetadata.Builder.() -> Unit): MediaMetadata {
			return DefaultMediaMetadata.Builder().apply(apply).build()
		}

		fun buildPlayback(apply: Playback.Builder.() -> Unit): Playback {
			return Playback.build(apply)
		}

		fun buildAudio(apply: Audio.Builder.() -> Unit): Audio {
			return Audio.build(apply)
		}
	}
}

//
// DEFAULTS
//

/**
 * Default Implementation for [MediaMetadata]
 */
internal class DefaultMediaMetadata private constructor(
	extra: MediaMetadata.Extra?,
	title: String?
) : MediaMetadata(extra, title) {

	class Builder() : MediaMetadata.Builder {

		override var extra: MediaMetadata.Extra? = null
			private set

		override var title: String? = null
			private set

		override fun setExtra(extra: MediaMetadata.Extra?): Builder = apply {
			this.extra = extra
		}

		override fun setTitle(title: String?): Builder = apply {
			this.title = title
		}

		override fun build(): DefaultMediaMetadata = DefaultMediaMetadata(extra, title)
	}
}

/**
 * Default Implementation for [MediaMetadata.Playback]
 */
internal class DefaultPlaybackMediaMetadata private constructor(
	extra: MediaMetadata.Extra?,
	title: String?,
	bitrate: Long?,
	duration: Duration?,
	playable: Boolean?
) : MediaMetadata.Playback(extra, title, bitrate, duration, playable) {

	class Builder() : MediaMetadata.Playback.Builder {

		override var extra: MediaMetadata.Extra? = null
			private set

		override var title: String? = null
			private set

		override var bitrate: Long? = null
			private set

		override var duration: Duration? = null
			private set

		override var playable: Boolean? = null
			private set

		override fun setExtra(extra: MediaMetadata.Extra?): Builder = apply {
			this.extra = extra
		}

		override fun setTitle(title: String?): Builder = apply {
			this.title = title
		}

		override fun setBitrate(bitrate: Long?): Builder = apply {
			this.bitrate = bitrate
		}

		override fun setDuration(duration: Duration?): Builder = apply {
			this.duration = duration
		}

		override fun setPlayable(playable: Boolean?): Builder = apply {
			this.playable = playable
		}

		override fun build(): DefaultPlaybackMediaMetadata = DefaultPlaybackMediaMetadata(
			extra = extra,
			title = title,
			bitrate = bitrate,
			duration = duration,
			playable = playable
		)
	}
}

/**
 * Default Implementations for [MediaMetadata.Audio]
 */
internal class DefaultAudioMetadata(
	extra: Extra?,
	title: String?,
	bitrate: Long?,
	duration: Duration?,
	playable: Boolean?,
	artistName: String?,
	albumTitle: String?,
	albumArtist: String?
) : MediaMetadata.Audio(extra, title, bitrate, duration, playable, artistName, albumTitle, albumArtist) {

	class Builder : MediaMetadata.Audio.Builder {

		override var artistName: String? = null
		override var albumTitle: String? = null
		override var albumArtist: String? = null
		override var extra: Extra? = null
		override var title: String? = null
		override var bitrate: Long? = null
		override var duration: Duration? = null
		override var playable: Boolean? = null

		override fun setArtist(artistName: String?): Builder = apply {
			this.artistName = artistName
		}

		override fun setAlbumTitle(albumTitle: String?): Builder = apply {
			this.albumTitle = albumTitle
		}

		override fun setAlbumArtist(albumArtist: String?): Builder = apply {
			this.albumArtist = albumArtist
		}

		override fun setExtra(extra: Extra?): Builder = apply {
			this.extra = extra
		}

		override fun setTitle(title: String?): Builder = apply {
			this.title = title
		}

		override fun setBitrate(bitrate: Long?): Builder = apply {
			this.bitrate = bitrate
		}

		override fun setDuration(duration: Duration?): Builder = apply {
			this.duration = duration
		}

		override fun setPlayable(playable: Boolean?): Builder = apply {
			this.playable = playable
		}

		override fun build(): Audio = DefaultAudioMetadata(
			extra = extra,
			title = title,
			bitrate = bitrate,
			duration = duration,
			playable = playable,
			artistName = artistName,
			albumTitle = albumTitle,
			albumArtist = albumArtist
		)
	}
}
