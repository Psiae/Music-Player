package com.flammky.android.medialib.providers.mediastore.api28.audio

import com.flammky.android.medialib.providers.mediastore.api28.MediaStore28
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioMetadataEntryEntry
import javax.annotation.concurrent.Immutable

/**
 * class representing Audio File Metadata Information on MediaStore API 29 / Android 9.0 / Pie
 * @see MediaStore28.MediaColumns
 * @see MediaStore28.Audio
 */

@Immutable
class MediaStoreAudioMetadataEntry28Entry private constructor(
	album: String?,
	artist: String?,
	bookmark: Long?,
	composer: String?,
	durationMs: Long?,
	year: Int?,
	title: String?,
) : MediaStoreAudioMetadataEntryEntry(
	album,
	artist,
	bookmark,
	composer,
	durationMs,
	year,
	title,
) {

	class Builder internal constructor() {
		var album: String? = null
			private set
		var artist: String? = null
			private set
		var bookmark: Long? = null
			private set
		var composer: String? = null
			private set
		var durationMs: Long? = null
			private set
		var title: String? = null
			private set
		var year: Int? = null
			private set

		fun setAlbum(album: String?) = apply {
			this.album = album
		}

		fun setArtist(artist: String?) = apply {
			this.artist = artist
		}

		fun setBookmark(bookmark: Long?) = apply {
			this.bookmark = bookmark
		}

		fun setComposer(composer: String?) = apply {
			this.composer = composer
		}

		fun setDurationMs(durationMs: Long?) = apply {
			this.durationMs = durationMs
		}

		fun setTitle(title: String?) = apply {
			this.title = title
		}

		fun setYear(year: Int?) = apply {
			this.year = year
		}

		internal fun build(): MediaStoreAudioMetadataEntry28Entry {
			return MediaStoreAudioMetadataEntry28Entry(
				album = album,
				artist = artist,
				bookmark = bookmark,
				composer = composer,
				durationMs = durationMs,
				title = title,
				year = year
			)
		}

	}

	companion object {
		val empty = Builder().build()
	}
}
