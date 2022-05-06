package com.kylentt.mediaplayer.helper.external.providers

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.kylentt.mediaplayer.helper.VersionHelper
import java.io.File

object ContentProvidersHelper {
  const val storagePath = "/storage"
  const val exteralStoragePath = "/storage/emulated/0"
  const val contentScheme = ContentResolver.SCHEME_CONTENT
  const val fileScheme = ContentResolver.SCHEME_FILE

  @JvmStatic
  val storageDir
    get() = if (VersionHelper.hasR()){
      Environment.getStorageDirectory()
    } else {
      if (File(storagePath).exists()) {
        File(storagePath)
      } else {
        null
      }
    }

  @JvmStatic
  val externalStorageDir
    get() = Environment.getExternalStorageDirectory()
      ?: if (File(exteralStoragePath).exists()) {
        File(exteralStoragePath)
      } else {
        null
      }

  @JvmStatic
  val storageDirString
     get() = (storageDir ?: storagePath).toString()

  @JvmStatic
  val externalStorageDirString
     get() = (externalStorageDir ?: exteralStoragePath).toString()

  @JvmStatic
  val mediaAudioContentUriPrefix
    get() = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ?: "content://media/external/audio/media"

  @JvmStatic
  fun isMediaAudioUri(uri: Uri): Boolean {
    return isMediaAudioUri(uri.toString())
  }

  @JvmStatic
  fun isMediaAudioUri(uri: String): Boolean {
    return uri.startsWith(mediaAudioContentUriPrefix.toString())
  }

  @JvmStatic
  fun inSchemeContentUri(uri: Uri): Boolean {
    return isSchemeContentUri(uri.toString())
  }

  @JvmStatic
  fun isSchemeContentUri(uri: String): Boolean {
    return uri.startsWith(contentScheme)
  }

}


