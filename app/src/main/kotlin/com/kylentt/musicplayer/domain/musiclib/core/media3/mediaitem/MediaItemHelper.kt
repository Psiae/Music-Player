package com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.kylentt.mediaplayer.helper.Preconditions
import com.kylentt.musicplayer.core.sdk.VersionHelper
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class MediaItemHelper(
	private val context: Context
) {

	fun buildFromMetadata(uri: Uri): MediaItem = MediaItemFactory.fromMetaData(context, uri)

	fun getEmbeddedPicture(item: MediaItem): ByteArray? =
		item.mediaUri?.let { getEmbeddedPicture(it) }

	private fun getEmbeddedPicture(uri: Uri): ByteArray? =
		MediaItemFactory.getEmbeddedImage(context, uri)

	init {
		Preconditions.checkArgument(context is Application)
	}
}

object MediaItemPropertyHelper {
	inline val MediaItem.fileName
		@JvmStatic get() = mediaMetadata.description

	inline val MediaItem.displayTitle
		@JvmStatic get() = mediaMetadata.displayTitle

	inline val MediaItem.title
		@JvmStatic get() = mediaMetadata.title

	val MediaItem.mediaUri: Uri?
		@JvmStatic get() = localConfiguration?.uri ?: requestMetadata.mediaUri

	@JvmStatic
	fun MediaItem.getDebugDescription(): String {
		return "$this Description:" +
			"\n fileName: $fileName" +
			"\n mediaId: $mediaId" +
			"\n title: $title" +
			"\n localConfig: $localConfiguration"
	}


}

object MediaMetadataHelper {
	private const val STORAGE_PATH_KEY = "STORAGE_PATH"

	@JvmStatic
	fun MediaMetadata.Builder.putDisplayTitle(title: String): MediaMetadata.Builder {
		return this.setDisplayTitle(title)
	}

	@JvmStatic
	fun MediaMetadata.Builder.putFileName(fileName: String): MediaMetadata.Builder {
		return this.setDescription(fileName)
	}

	@JvmStatic
	fun MediaMetadata.Builder.putStoragePath(path: String, bundle: Bundle): MediaMetadata.Builder {
		bundle.putString(STORAGE_PATH_KEY, path)
		return this.setExtras(bundle)
	}

	@JvmStatic
	fun MediaMetadata.getStoragePath(): String? {
		return extras?.getString(STORAGE_PATH_KEY)
	}
}

object RequestMetadataHelper {
	// TODO
}
