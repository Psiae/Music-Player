package com.kylentt.musicplayer.domain.musiclib.source.mediastore.audio

import com.kylentt.musicplayer.domain.musiclib.annotation.DurationValue
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.media.MediaStoreMetadata
import java.util.Objects
import java.util.concurrent.TimeUnit

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
abstract class MediaStoreAudioMetadata internal constructor() : MediaStoreMetadata() {

	abstract val album: String

	abstract val artist: String

	@DurationValue(TimeUnit.MILLISECONDS)
	abstract val bookmark: Long

	abstract val composer: String

	@DurationValue(TimeUnit.MILLISECONDS)
	abstract val durationMs: Long

	abstract val genre: String

	abstract val track: String

	abstract val year: Int

	override fun hashCode(): Int = Objects.hash(album, artist, bookmark, composer, durationMs, track, year, super.hashCode())

	override fun equals(other: Any?): Boolean {
		return other is MediaStoreAudioMetadata
			&& album == other.album
			&& artist == other.artist
			&& bookmark == other.bookmark
			&& composer == other.composer
			&& durationMs == other.durationMs
			&& track == other.track
			&& year == other.year
			&& super.equals(other)
	}
}
