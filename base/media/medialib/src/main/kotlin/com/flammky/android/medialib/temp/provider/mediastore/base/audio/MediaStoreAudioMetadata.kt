package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaStoreMetadata
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
@Immutable
abstract class MediaStoreAudioMetadata internal constructor() : MediaStoreMetadata() {

	abstract val album: String?

	abstract val artist: String?

	@DurationValue(TimeUnit.MILLISECONDS)
	abstract val bookmark: Long?

	abstract val composer: String?

	@DurationValue(TimeUnit.MILLISECONDS)
	abstract val durationMs: Long?

	abstract val year: Int?

	override fun hashCode(): Int =
		Objects.hash(album, artist, bookmark, composer, durationMs, year, super.hashCode())

	override fun equals(other: Any?): Boolean {
		return other is MediaStoreAudioMetadata
			&& album == other.album
			&& artist == other.artist
			&& bookmark == other.bookmark
			&& composer == other.composer
			&& durationMs == other.durationMs
			&& year == other.year
			&& super.equals(other)
	}
}
