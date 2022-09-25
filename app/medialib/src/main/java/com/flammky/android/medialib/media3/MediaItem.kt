package com.flammky.android.medialib.media3

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Extra
import com.flammky.android.medialib.common.mediaitem.MediaMetadata

/**
 * Class that represents [MediaItem] from androidx.media3 package
 */
internal class Media3Item private constructor(internal val item: MediaItem) : InternalMediaItem() {

	class Builder(mediaId: String) : InternalMediaItem.Builder {

		var extra: Extra = Extra.UNSET
			private set

		var mediaId: String = mediaId
			private set

		var uri: Uri = Uri.EMPTY
			private set

		var metadata: MediaMetadata =  MediaMetadata.UNSET
			private set

		fun setExtra(extra: Extra) = apply {
			this.extra = extra
		}

		fun setMediaId(mediaId: String) = apply {
			this.mediaId = mediaId
		}

		fun setUri(uri: Uri) = apply {
			this.uri = uri
		}

		fun setMetadata(metadata: MediaMetadata) = apply {
			this.metadata = metadata
		}

		fun build(): Media3Item = build(extra, mediaId, uri, metadata)

		override fun build(
			extra: Extra,
			mediaId: String,
			uri: Uri,
			metadata: MediaMetadata
		): Media3Item {
			// TODO: Factory
			val item = MediaItem.Builder().apply {
				setMediaId(mediaId)
				setUri(uri)
				setRequestMetadata(RequestMetadata.Builder().setMediaUri(uri).setExtras(extra.bundle).build())
				setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().apply {
					val audioMetadata = metadata as AudioMetadata
					setArtist(audioMetadata.artist)
					setAlbumArtist(audioMetadata.albumArtist)
					setAlbumTitle(audioMetadata.albumTitle)
					setTitle(audioMetadata.title)
					setIsPlayable(audioMetadata.playable)

					metadata.extra?.bundle?.apply {
						putString("durationMs", "${audioMetadata.duration?.inWholeMilliseconds}")
						putString("bitrate", "${audioMetadata.bitrate}")
						setExtras(this)
					}

				}.build())
			}.build()
			return Media3Item(item)
		}
	}

	companion object {
		@JvmStatic
		fun build(mediaId: String, apply: Builder.() -> Media3Item): Media3Item {
			return Builder(mediaId).apply { apply() }.build()
		}
	}
}
