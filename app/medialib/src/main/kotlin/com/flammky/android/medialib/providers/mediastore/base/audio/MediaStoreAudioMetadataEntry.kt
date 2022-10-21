package com.flammky.android.medialib.providers.mediastore.base.audio

import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreMetadata
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
@Immutable
abstract class MediaStoreAudioMetadataEntry internal constructor(
	open val album: String?,
	open val artist: String?,
	@DurationValue(TimeUnit.MILLISECONDS)
	open val bookmark: Long?,
	open val composer: String?,
	@DurationValue(TimeUnit.MILLISECONDS)
	open val durationMs: Long?,
	open val year: Int?,
	override val title: String?
) : MediaStoreMetadata(title) {

	override fun hashCode(): Int =
		Objects.hash(album, artist, bookmark, composer, durationMs, year, super.hashCode())

	override fun equals(other: Any?): Boolean {
		return other is MediaStoreAudioMetadataEntry
			&& super.equals(other)
			&& album == other.album
			&& artist == other.artist
			&& bookmark == other.bookmark
			&& composer == other.composer
			&& durationMs == other.durationMs
			&& year == other.year
	}
}
