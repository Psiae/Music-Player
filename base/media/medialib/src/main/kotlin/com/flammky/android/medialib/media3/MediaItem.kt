package com.flammky.android.medialib.media3

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.InternalMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Extra
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.common.mediaitem.PlaybackMetadata

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

				val internalBundle = Bundle()
				val type = StringBuilder()

				if (metadata is AudioMetadata) type.append("audio;")
				if (metadata is PlaybackMetadata) type.append("playback;")
				type.append("base;")

				internalBundle.putBundle("nest", extra.bundle)
				internalBundle.putString("mediaMetadataType", type.toString())

				setMediaId(mediaId)
				setUri(uri)
				setRequestMetadata(RequestMetadata.Builder().setMediaUri(uri).setExtras(internalBundle).build())
				setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().apply {

					setTitle(metadata.title)
					setExtras(metadata.extra?.bundle)

					if (metadata is PlaybackMetadata) {
						setIsPlayable(metadata.playable)
						metadata.extra?.bundle?.apply {

							if (metadata.duration != null) {
								putString("durationMs", "${metadata.duration.inWholeMilliseconds}")
							}

							if (metadata.bitrate != null) {
								putString("bitrate", "${metadata.bitrate}")
							}
						}
						if (metadata is AudioMetadata) {
							setArtist(metadata.artistName)
							setAlbumArtist(metadata.albumArtistName)
							setAlbumTitle(metadata.albumTitle)
						}
					}
				}.build())
			}.build()
			return Media3Item(item)
		}
	}

	companion object {
		@JvmStatic
		fun build(mediaId: String, apply: Builder.() -> Unit): Media3Item {
			return Builder(mediaId).apply(apply).build()
		}
	}
}
