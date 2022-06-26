package com.kylentt.mediaplayer.core.media3.mediaitem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.core.extenstions.orEmpty
import com.kylentt.mediaplayer.core.extenstions.orEmptyString
import com.kylentt.mediaplayer.core.extenstions.orEmptyUri
import com.kylentt.mediaplayer.core.extenstions.putExtraAsString
import com.kylentt.mediaplayer.core.media3.MediaItemFactory

class MediaItemInfo private constructor(

	/** @see MediaItem.mediaId */
	private val mediaId: String,

	/** @see MediaMetadata.mediaUri */
	private val mediaUri: Uri,

	/** @see MediaMetadata.artist */
	private val artist: String,

	/** @see MediaMetadata.albumTitle*/
	private val album: String,

	/** @see MediaMetadata.displayTitle */
	private val displayTitle: String,

	/** @see MediaMetadata.title */
	private val title: String
) {

	private val mediaItem: MediaItem = with(MediaItemFactory.newBuilder()) {
		val metadata = with(MediaMetadata.Builder()) {
			setArtist(artist)
			setAlbumTitle(album)
			setDisplayTitle(displayTitle)
			setTitle(title)
			build()
		}

		setUri(mediaUri)
		setMediaId(mediaId)
		setMediaMetadata(metadata)
		build()
	}

	class Builder() {
		var mediaId: String = ""

		var mediaUri: Uri = Uri.EMPTY

		var artist: String = ""

		var album: String = ""

		var displayTitle: String = ""

		var title: String = ""

		fun build(): MediaItemInfo = MediaItemInfo(
			this.mediaId,
			this.mediaUri,
			this.artist,
			this.album,
			this.displayTitle,
			this.title
		)
	}

	interface Converter<T> {
		fun toMediaItemInfo(obj: T): MediaItemInfo
		fun fromMediaItemInfo(itemInfo: MediaItemInfo): T
	}

	class IntentConverter : MediaItemInfo.Converter<Intent> {

		override fun toMediaItemInfo(obj: Intent): MediaItemInfo {
			return with(Builder()) {
				mediaId = obj.getStringExtra(mediaIdIntentExtraName).orEmptyString()
				mediaUri = obj.getStringExtra(mediaUriIntentExtraName).orEmptyUri()
				artist = obj.getStringExtra(artistIntentExtraName).orEmptyString()
				album = obj.getStringExtra(albumIntentExtraName).orEmptyString()
				displayTitle = obj.getStringExtra(displayTitleIntentExtraName).orEmptyString()
				title = obj.getStringExtra(titleIntentExtraName).orEmptyString()

				applyConverterSignature(build())
			}
		}

		override fun fromMediaItemInfo(itemInfo: MediaItemInfo): Intent {
			return with(Intent()) {
				putExtraAsString(mediaIdIntentExtraName, itemInfo.mediaId)
				putExtraAsString(mediaUriIntentExtraName, itemInfo.mediaUri)
				putExtraAsString(artistIntentExtraName, itemInfo.mediaId)
				putExtraAsString(albumIntentExtraName, itemInfo.mediaId)
				putExtraAsString(displayTitleIntentExtraName, itemInfo.mediaId)
				putExtraAsString(titleIntentExtraName, itemInfo.title)

				applyConverterSignature(this)
			}
		}


		fun applyFromMediaItem(intent: Intent, item: MediaItem): Intent {
			return intent.apply {
				val uri = item.localConfiguration?.uri ?: item.mediaMetadata.mediaUri.orEmpty()
				putExtraAsString(mediaIdIntentExtraName, item.mediaId)
				putExtraAsString(mediaUriIntentExtraName, uri)
				putExtraAsString(artistIntentExtraName, item.mediaMetadata.artist.orEmptyString())
				putExtraAsString(albumIntentExtraName, item.mediaMetadata.albumTitle.orEmptyString())
				putExtraAsString(displayTitleIntentExtraName, item.mediaMetadata.displayTitle.orEmptyString())
				putExtraAsString(titleIntentExtraName, item.mediaMetadata.title.orEmptyString())

				applyConverterSignature(this)
			}
		}

		private fun applyConverterSignature(intent: Intent): Intent {
			return intent.putExtra(intentConverterSignatureKey, intentConverterSignatureValue)
		}

		private fun applyConverterSignature(item: MediaItem): MediaItem {
			val eBundle = item.mediaMetadata.extras

			eBundle?.let { existingBundle ->
				existingBundle.putString(intentConverterSignatureKey, intentConverterSignatureValue)
				return item
			}

			with(Bundle()) {
				putString(intentConverterSignatureKey, intentConverterSignatureValue)

				val newMetadata = MediaMetadata.Builder()
					.populate(item.mediaMetadata)
					.setExtras(this)
					.build()

				val newItem = MediaItem.Builder().apply {
					val uri = item.localConfiguration?.uri ?: item.mediaMetadata.mediaUri.orEmpty()
					setMediaId(item.mediaId)
					setUri(uri)
					setMediaMetadata(newMetadata)
				}

				return newItem.build()
			}
		}

		private fun applyConverterSignature(itemInfo: MediaItemInfo): MediaItemInfo {
			applyConverterSignature(itemInfo.mediaItem)
			return itemInfo
		}

		private fun hasConverterSignature(intent: Intent): Boolean =
			intent.getStringExtra(intentConverterSignatureKey) == intentConverterSignatureValue

		private fun hasConverterSignature(item: MediaItem): Boolean =
			item.mediaMetadata.extras?.get(intentConverterSignatureKey) == intentConverterSignatureValue

		private fun hasConverterSignature(item: MediaItemInfo): Boolean =
			hasConverterSignature(item.mediaItem)

		fun isConvertible(intent: Intent): Boolean = hasConverterSignature(intent)

		fun getMediaIdExtra(intent: Intent): String? {
			val get =
				if (hasConverterSignature(intent)) {
					// should not be null even though it might be an empty String
					intent.getStringExtra(mediaUriIntentExtraName)!!
				} else {
					null
				}
			return get
		}

		companion object {
			const val intentConverterSignatureKey = "MediaItemInfo.IntentConverter.intentConverterSignature"
			const val intentConverterSignatureValue = "todoValue"
			const val mediaIdIntentExtraName = "MediaItemInfo.IntentConverter.mediaIdIntentExtraName"
			const val mediaUriIntentExtraName = "MediaItemInfo.IntentConverter.mediaUriIntentExtraName"
			const val artistIntentExtraName = "MediaItemInfo.IntentConverter.artistIntentExtraName"
			const val albumIntentExtraName = "MediaItemInfo.IntentConverter.albumIntentExtraName"
			const val displayTitleIntentExtraName = "MediaItemInfo.IntentConverter.displayTitleIntentExtraName"
			const val titleIntentExtraName = "MediaItemInfo.IntentConverter.titleIntentExtraName"
		}
	}

	class MediaItemConverter : MediaItemInfo.Converter<MediaItem> {
		override fun toMediaItemInfo(obj: MediaItem): MediaItemInfo {
			return with(Builder()) {
				mediaId = obj.mediaId
				mediaUri = obj.localConfiguration?.uri ?: obj.mediaMetadata.mediaUri.orEmpty()
				artist = obj.mediaMetadata.artist.orEmptyString()
				album = obj.mediaMetadata.albumTitle.orEmptyString()
				displayTitle = obj.mediaMetadata.displayTitle.orEmptyString()
				title = obj.mediaMetadata.title.orEmptyString()
				build()
			}
		}

		override fun fromMediaItemInfo(itemInfo: MediaItemInfo): MediaItem = itemInfo.mediaItem
	}
}
