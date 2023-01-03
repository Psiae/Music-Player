package com.flammky.android.medialib.providers.mediastore.base.audio

import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreMetadataEntry
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.Immutable

/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-7.0.0_r36/core/java/android/provider/MediaStore.java#1208)
 */
@Immutable
abstract class MediaStoreAudioMetadataEntryEntry internal constructor(
	@JvmField
	val album: String?,
	@JvmField
	val artist: String?,
	@JvmField @DurationValue(TimeUnit.MILLISECONDS)
	val bookmark: Long?,
	@JvmField val
	composer: String?,
	@JvmField @DurationValue(TimeUnit.MILLISECONDS)
	val durationMs: Long?,
	@JvmField
	val year: Int?,
	title: String?
) : MediaStoreMetadataEntry(title) {

	override fun hashCode(): Int =
		Objects.hash(album, artist, bookmark, composer, durationMs, year, super.hashCode())

	override fun equals(other: Any?): Boolean {
		return other is MediaStoreAudioMetadataEntryEntry
			&& super.equals(other)
			&& album == other.album
			&& artist == other.artist
			&& bookmark == other.bookmark
			&& composer == other.composer
			&& durationMs == other.durationMs
			&& year == other.year
	}
}
