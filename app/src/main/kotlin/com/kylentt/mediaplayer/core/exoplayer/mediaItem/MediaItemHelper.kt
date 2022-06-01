package com.kylentt.mediaplayer.core.exoplayer.mediaItem

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviderHelper

object MediaItemHelper {

  inline val MediaItem.fileName
    @JvmStatic get() = mediaMetadata.description

  inline val MediaItem.displayTitle
    @JvmStatic get() = mediaMetadata.displayTitle

  @JvmStatic
  fun MediaItem.getDebugDescription(): String {
    return "$this Description:" +
      "\n fileName: $fileName" +
      "\n mediaId: $mediaId" +
      "\n displayTitle: $displayTitle"
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
		checkArgument(DocumentProviderHelper.isStoragePathExist(path))
		bundle.putString(STORAGE_PATH_KEY, path)
		return this.setExtras(bundle)
	}

	@JvmStatic
	fun MediaMetadata.getStoragePath(): String? {
		return extras?.getString(STORAGE_PATH_KEY)
	}
}
