package com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.common.kotlin.charsequence.orEmptyString
import com.kylentt.musicplayer.common.kotlin.charsequence.orEmptyUri
import com.kylentt.musicplayer.common.android.intent.putExtraAsString
import com.kylentt.musicplayer.common.android.uri.orEmpty

class MediaItemInfo private constructor(

	/** @see MediaItem.mediaId */
	private val mediaId: String,

	/** @see RequestMetadata.mediaUri */
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

	val mediaItem: MediaItem = with(MediaItemFactory.newBuilder()) {
		val metadata = with(MediaMetadata.Builder()) {
			setArtist(artist)
			setAlbumTitle(album)
			setDisplayTitle(displayTitle)
			setTitle(title)
			build()
		}

		val requestMetadata = with(RequestMetadata.Builder()) {
			setMediaUri(mediaUri)
			build()
		}

		setUri(mediaUri)
		setMediaId(mediaId)
		setMediaMetadata(metadata)
		setRequestMetadata(requestMetadata)
		build()
	}

	class Builder {
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

	class IntentConverter : Converter<Intent> {

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
				val uri = MediaItemFactory.getUri(item)
				putExtraAsString(mediaIdIntentExtraName, item.mediaId)
				putExtraAsString(mediaUriIntentExtraName, uri)
				putExtraAsString(artistIntentExtraName, item.mediaMetadata.artist.orEmptyString())
				putExtraAsString(albumIntentExtraName, item.mediaMetadata.albumTitle.orEmptyString())
				putExtraAsString(
					displayTitleIntentExtraName,
					item.mediaMetadata.displayTitle.orEmptyString()
				)
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

				// maybe Implement these in factory

				val newMetadata = MediaMetadata.Builder()
					.populate(item.mediaMetadata)
					.setExtras(this)
					.build()

				val newMetadataRequest = RequestMetadata.Builder()
					.setMediaUri(item.requestMetadata.mediaUri)
					.setExtras(item.requestMetadata.extras)
					.build()

				val newItem = MediaItem.Builder().apply {
					val uri = getMediaItemUri(item)
					setMediaId(item.mediaId)
					setUri(uri)
					setMediaMetadata(newMetadata)
					setRequestMetadata(newMetadataRequest)
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

		private fun getMediaItemUri(item: MediaItem): Uri? = MediaItemFactory.getUri(item)

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
			const val intentConverterSignatureKey =
				"MediaItemInfo.IntentConverter.intentConverterSignature"
			const val intentConverterSignatureValue = "todoValue"
			const val mediaIdIntentExtraName = "MediaItemInfo.IntentConverter.mediaIdIntentExtraName"
			const val mediaUriIntentExtraName = "MediaItemInfo.IntentConverter.mediaUriIntentExtraName"
			const val artistIntentExtraName = "MediaItemInfo.IntentConverter.artistIntentExtraName"
			const val albumIntentExtraName = "MediaItemInfo.IntentConverter.albumIntentExtraName"
			const val displayTitleIntentExtraName =
				"MediaItemInfo.IntentConverter.displayTitleIntentExtraName"
			const val titleIntentExtraName = "MediaItemInfo.IntentConverter.titleIntentExtraName"
		}
	}

	class MediaItemConverter : Converter<MediaItem> {
		override fun toMediaItemInfo(obj: MediaItem): MediaItemInfo {
			return with(Builder()) {
				mediaId = obj.mediaId
				mediaUri = getMediaItemUri(obj).orEmpty()
				artist = obj.mediaMetadata.artist.orEmptyString()
				album = obj.mediaMetadata.albumTitle.orEmptyString()
				displayTitle = obj.mediaMetadata.displayTitle.orEmptyString()
				title = obj.mediaMetadata.title.orEmptyString()
				build()
			}
		}

		fun getMediaItemUri(item: MediaItem): Uri? = MediaItemFactory.getUri(item)

		override fun fromMediaItemInfo(itemInfo: MediaItemInfo): MediaItem = itemInfo.mediaItem
	}
}
