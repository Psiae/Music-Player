package com.kylentt.mediaplayer.helper.external.providers

import android.content.ContentResolver
import android.os.Environment
import com.kylentt.mediaplayer.helper.VersionHelper
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import kotlin.coroutines.coroutineContext

object ContentProvidersHelper {
  const val storagePath = "/storage"
  const val exteralStoragePath = "/storage/emulated/0"
  const val contentScheme = ContentResolver.SCHEME_CONTENT
  const val fileScheme = ContentResolver.SCHEME_FILE

  val storageDir
    get() = if (VersionHelper.hasR()) Environment.getStorageDirectory() else null
  val externalStorageDir
    get() = Environment.getExternalStorageDirectory()

  val storageDirString
    get() = (storageDir ?: storagePath).toString()
  val externalStorageDirString
    get() = (externalStorageDir ?: exteralStoragePath).toString()

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun decodeUrl(str: String, encoder: String = "UTF-8") =
    withContext(coroutineContext) { URLDecoder.decode(str, encoder) }

}


