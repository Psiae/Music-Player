package com.flammky.musicplayer.dump.mediaplayer.helper.external.providers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.flammky.musicplayer.base.BuildConfig
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasR
import com.flammky.musicplayer.dump.mediaplayer.helper.Preconditions.checkArgument
import okio.IOException
import timber.log.Timber
import java.io.File
import java.net.URLDecoder

object ContentProvidersHelper {
  const val storagePath = "/storage"
  const val exteralStoragePath = "/storage/emulated/0"
  const val contentScheme = ContentResolver.SCHEME_CONTENT
  const val fileScheme = ContentResolver.SCHEME_FILE

  @JvmStatic
  val storageDir
    get() = getDeviceStorage()

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
     get() = storageDir.toString()

  @JvmStatic
  val externalStorageDirString
     get() = (externalStorageDir ?: exteralStoragePath).toString()

  @JvmStatic
  val mediaAudioContentUriPrefix
    get() = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ?: "content://media/external/audio/media"

	private fun getDeviceStorage(): File? {
		if (AndroidAPI.hasR()) return Environment.getStorageDirectory()

		val storage = File(storagePath)
		if (storage.exists()) return storage

		val extStorage = Environment.getExternalStorageDirectory() ?: File(exteralStoragePath)
		return if (extStorage.exists()) {
			val split = extStorage.toString().split("/")
			val f = File("/${split.first()}")
			if (f.exists()) f else null
		} else {
			null
		}
	}

	@JvmStatic
	fun isContentUriExist(context: Context, uri: Uri): Boolean {
		Environment.getExternalStorageDirectory()

		return try {
			context.contentResolver.openInputStream(uri)!!.close()
			true
		} catch (e: Exception) {
			when (e) {
				is IOException -> false
				is NullPointerException -> {
					Timber.e("NPE is thrown instead of IOException")
					false
				}
				else -> {
					if (BuildConfig.DEBUG) throw IllegalStateException("Uncaught Exception: \n${e}")
					false
				}
			}
		}
	}

  @JvmStatic
  fun isMediaAudioUri(uri: Uri): Boolean {
    return isMediaAudioUri(uri.toString())
  }

  @JvmStatic
  fun isMediaAudioUri(uri: String): Boolean {
    return uri.startsWith(mediaAudioContentUriPrefix.toString())
  }

  @JvmStatic
  fun isSchemeContentUri(uri: Uri): Boolean {
    return isSchemeContentUri(uri.toString())
  }

  @JvmStatic
  fun isSchemeContentUri(uri: String): Boolean {
    return uri.startsWith(contentScheme)
  }

	@JvmStatic fun getDecodedContentUri(uri: Uri, context: Context, fileName: String?): Uri =
		getDecodedContentUri(uri.toString(), context, fileName).toUri()

	@JvmStatic
	fun getDecodedContentUri(uriString: String, context: Context, fileName: String?): String {
		checkArgument(isSchemeContentUri(uriString))
		val uri = Uri.parse(uriString)
		val nFilename = fileName
			?: context.contentResolver.query(uri, null, null, null ,null)?.use { cursor ->
				cursor.moveToFirst()
				val index = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
				cursor.getString(index)
			}

		var decodeTimes = 0
		var decodedUriString: String = uriString

		when {
			nFilename == null -> {
				throw IllegalStateException("Could not retrieve FileName ($fileName) for $uriString is null")
			}
			nFilename.contains("%") || nFilename.contains("%") -> {
				var times = 0
				if (nFilename.contains("%25") && !nFilename.contains("%25 ")) times++
				decodeTimes += times
			}
			uriString.contains("%") -> {
				var times = 1
				if (nFilename.contains("%25") && !nFilename.contains("%25 ")) times++
				decodeTimes += times
			}
		}
		repeat(decodeTimes) { decodedUriString = decodeUri(decodedUriString) }

		Timber.d("getDecodedContentUri returning $decodedUriString")
		return decodedUriString
	}

	private fun decodeUri(uriString: String) = URLDecoder.decode(uriString, "UTF-8")

}


