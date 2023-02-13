package com.flammky.musicplayer.dump.mediaplayer.helper.external.providers

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasQ
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasR
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.URLDecoder
import kotlin.coroutines.coroutineContext

// TODO: Organize This Better

object DocumentProviderHelper {
  const val auth_provider_externalStorage = "com.android.externalstorage"
  const val auth_provider_media = "com.android.providers.media"
  const val auth_provider_download = "com.android.providers.downloads"
  const val contentScheme = ContentResolver.SCHEME_CONTENT
  const val fileScheme = ContentResolver.SCHEME_FILE

  @JvmStatic
  val extStoragePath: File?
    get() = Environment.getExternalStorageDirectory()

  @JvmStatic
  val extStoragePathString
    get() = extStoragePath?.toString() ?: "/storage/emulated/0"

  @JvmStatic
  val storagePath
    get() = if (AndroidAPI.hasR()) {
      Environment.getStorageDirectory().toString()
    } else {
      "/storage"
    }

  @Suppress("BlockingMethodInNonBlockingContext")
  suspend fun decodeUrl(url: String, enc: String = "UTF-8"): String {
    return withContext(coroutineContext) { URLDecoder.decode(url, enc) }
  }

  private val dispatchers = com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers.DEFAULT

  suspend fun getAudioPathFromContentUri(
    context: Context,
    uri: Uri
  ): String {
    return try {
      require(DocumentsContract.isDocumentUri(context, uri))
      val fileName = withContext(dispatchers.io) {
        context.contentResolver.query(uri, null, null, null, null)
          ?.use { cursor ->
            cursor.moveToFirst()
            val dataIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
            val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (dataIndex > -1) {
              val data = cursor.getString(dataIndex)
              if (File(data).exists()) return@withContext data
            }
            cursor.getString(nameIndex)
          }
      }

      requireNotNull(fileName)

      if (fileName.startsWith(storagePath) && File(fileName).exists()) {
        return fileName
      }

      val tries = when {
        uri.authority!!.startsWith(auth_provider_externalStorage) -> {
          tryExternalStorageProvider(fileName, uri)
        }
        uri.authority!!.startsWith(auth_provider_download) -> {
          tryDownloadProvider(context, fileName, uri)
        }
        uri.authority!!.startsWith(auth_provider_media) -> {
          tryMediaProvider(context, fileName, uri)
        }
        else -> null
      }

      if (tries != null) {
        if ((tries.endsWith(fileName) && File(tries).exists())
          || tries.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())
        ) {
          Timber.d("DocumentProviders returned $tries")
          return tries
        }
      }
      ""
    } catch (e: Exception) {
      Timber.e(e)
      ""
    }
  }

  private suspend fun tryExternalStorageProvider(fileName: String, uri: Uri): String {
    require(fileName.isNotBlank())
    val builder = StringBuilder()
    val uriString = uri.toString()
    val decodedUriString = decodeUrl(uriString)

    val split2F = uriString.split("/")
    val split3A = uriString.split(":")
    val dSplit2F = decodedUriString.split("/")
    val dSplit3A = decodedUriString.split(":")

    /* primary:Directory is the common result*/
    val prim = run {
      val bPrim = StringBuilder()
      if (dSplit3A.size > 1) {
        // there is ":" other than "content:" in decoded Url
        if (split2F.size > 4) {
          // plain split "/" url has index 4
          // which is likely to be storage path in case of document provider
          if (dSplit3A[1].endsWith(dSplit2F[4].split(":").first())) {
            // decoded split ":" second index which after content://
            // does ends with the starting string of decoded "/"
            bPrim.append(dSplit2F[4])
          }
        }
      }
      bPrim.toString()
    }
    when {
      prim.isNotEmpty() -> {
        val primPrefix = prim.split(":")[0]
        val primSuffix = prim.split(":")[1]
        val directory = dSplit3A[2]
        if (primPrefix == "primary") {
          if (directory.endsWith(fileName)) {
            builder.append("$extStoragePathString/$directory")
          }
        } else {
          if (directory.endsWith(fileName)) {
            builder.append("$storagePath/$primPrefix/$directory")
          }
        }
      }
    }
    return builder.toString()
  }

  private suspend fun tryDownloadProvider(context: Context, fileName: String, uri: Uri): String {
    require(fileName.isNotBlank())
    val stringBuilder = StringBuilder()
    val uriString = uri.toString()
    val decodedUriString = decodeUrl(uriString)
    val dSplit2F = decodedUriString.split("/")
		val dSplit3A = decodedUriString.split(":")

    return withContext(dispatchers.io) {
			if (dSplit3A[2].startsWith(extStoragePathString) && dSplit2F.last() == fileName) {
				return@withContext dSplit3A[2]
			}

			val fUri = when {
				AndroidAPI.hasQ() && dSplit2F.last().startsWith("msf") -> {
					val mime = context.contentResolver.query(uri, null, null, null, null)
						?.use { cursor ->
							cursor.moveToFirst()
							cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
						}
					if (mime?.startsWith("audio") == true) {
						val extContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
						ContentUris.withAppendedId(extContentUri, dSplit2F.last().filter(Char::isDigit).toLong())
					} else {
						uri
					}
				}
				else -> uri
			}

      context.contentResolver.query(fUri, null, null, null, null)
        ?.use { cursor ->
          cursor.moveToFirst()
          val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
          val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
          if (dataIndex > -1) {
            val data = cursor.getString(dataIndex)
            if (data.endsWith(fileName)) stringBuilder.append(data)
          } else {
            val name = cursor.getString(nameIndex)
            if (fileName == name) {
              stringBuilder.append("$extStoragePathString/${Environment.DIRECTORY_DOWNLOADS}/$name")
            }
          }
        }
      stringBuilder.toString()
    }
  }

  private suspend fun tryMediaProvider(context: Context, filename: String, uri: Uri): String {
    val stringBuilder = StringBuilder()
    val id = withContext(dispatchers.io) {
      context.contentResolver.query(uri, null, null, null, null)
        ?.use { cursor ->
          cursor.moveToFirst()
          val i = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
          cursor.getString(i)
        }
    }
    if (id != null) {
      val fUri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        id.filter(Char::isDigit).toLong()
      )
      context.contentResolver.query(fUri, null, null, null, null)
        ?.use { cursor ->
          cursor.moveToFirst()
          val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
          if (name == filename) stringBuilder.append(fUri)
        }
    }
    return stringBuilder.toString()
  }

	@JvmStatic fun isStoragePath(path: String) = path.startsWith("$storagePath/")
	@JvmStatic fun isStoragePathExist(path: String) = isStoragePath(path) && File(path).exists()

	@JvmStatic
	fun isExternalStoragePath(path: String) = path.startsWith("$extStoragePathString/")
	@JvmStatic
	fun isExternalStoragePathExist(path: String) = isExternalStoragePath(path) && File(path).exists()
}
