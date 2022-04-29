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

  object DocumentProvider {
    const val auth_provider_externalStorage = "com.android.externalstorage"
    const val auth_provider_media = "com.android.providers.media"
    const val auth_provider_download = "com.android.providers.downloads"


    /*suspend fun getContentMediaUri(context: Context, uri: Uri, fileName: String): String {

      return try {
        val builder = StringBuilder()
        val uriString = uri.toString()
        val dUriString = decodeUrl(uriString)

        require(uriString.isNotEmpty())
        require(dUriString.startsWith(ContentProviders.contentScheme))

        val split2F = uriString
          .split("/")

        val split3A = uriString
          .split(":")

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          cursor.moveToFirst()
          check(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            .equals(fileName)
          )

          val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
          if (data > -1) {
            return cursor.getString(data)
          }
        }


        val tries = when {
          split2F[2].startsWith(ContentProviders.auth_provider_externalStorage) -> ContentProviders.DocumentProviders.tryExternalStorage(
            uri,
            fileName
          )
          split2F[2].startsWith(ContentProviders.auth_provider_media) -> ContentProviders.DocumentProviders.tryMediaProviders(
            uri,
            context
          )
          split2F[2].startsWith(ContentProviders.auth_provider_download) -> ContentProviders.DocumentProviders.tryDownloadProviders(
            uri,
            context,
            fileName
          )
          else -> null
        }

        tries?.let { builder.append(it) }
          ?: builder.append(ContentProviders.DocumentProviders.logUnknownProvider(context, uri))

        Timber.d(
          "getContentMediaUri uri = $uri" +
            "\n2f = $split2F" +
            "\n3a = $split3A" +
            "\ntries = $tries" +
            "\ndecoded uri: ${dUriString}"
        )

        if (builder.startsWith(ContentProviders.DocumentProviders.storagePath)) {
          ContentProviders.DocumentProviders.checkBuildedPath(builder.toString(), fileName)
        }
        return builder.toString()
      } catch (e: Exception) {
        Timber.e(e)
        ""
      }
    }*/
  }
}


