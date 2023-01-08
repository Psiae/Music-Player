package com.flammky.musicplayer.dump.mediaplayer.helper.external
/*
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity
import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemFactory
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.flammky.musicplayer.dump.mediaplayer.helper.Preconditions.checkArgument
import com.flammky.musicplayer.dump.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.flammky.musicplayer.dump.mediaplayer.helper.external.providers.DocumentProviderHelper
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.net.URLDecoder
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

*/
/**
 * Interface for handling Media Type [android.content.Intent]
 * @see [MediaIntentHandlerImpl]
 * @author Kylentt
 * @since 2022/04/30
 *//*


interface MediaIntentHandler {
	fun handleMediaIntentI(intent: IntentWrapper)
  suspend fun handleMediaIntent(intent: IntentWrapper)
}

*/
/**
 * Base Implementation for [MediaIntentHandler]
 * @see [IntentWrapper]
 * @author Kylentt
 * @since 2022/04/30
 *//*


@Singleton
class MediaIntentHandlerImpl(
	private val artworkProvider: ArtworkProvider,
  private val context: Context,
  private val dispatcher: com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers,
  private val mediaSource: com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider,
	private val mediaConnection: MediaConnectionDelegate,
	private val mediaLib: MediaLibrary
) : MediaIntentHandler {

  private val actionViewHandler = ActionViewHandler()

	override fun handleMediaIntentI(intent: IntentWrapper) {
		if (intent.shouldHandleIntent) {
			actionViewHandler.handleIntentActionViewI(intent)
		}
	}

	override suspend fun handleMediaIntent(intent: IntentWrapper) {
    require(intent.shouldHandleIntent)
    Timber.d("MediaIntentHandler, handleMediaIntent $intent")
    when {
      intent.isActionView() -> actionViewHandler.handleIntentActionView(intent)
    }
  }

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun decodeUrl(str: String, encoder: String = "UTF-8") =
    withContext(dispatcher.io) { URLDecoder.decode(str, encoder) }

  private inner class ActionViewHandler() {

		@Volatile
    private var actionViewJob = Job().job

		fun handleIntentActionViewI(intent: IntentWrapper): Unit {
			checkArgument(intent.isActionView())

			val job: suspend () -> Unit = when {
				intent.isSchemeContent() -> { { intentSchemeContent(intent) } }
				else -> { { Unit } }
			}

			actionViewJob.cancel()

			if (intent.isCancellable) {
				actionViewJob = CoroutineScope(dispatcher.io).launch { job() }
			} else {
				CoroutineScope(dispatcher.io).launch { job() }
			}
		}

    suspend fun handleIntentActionView(intent: IntentWrapper): Unit = withContext(coroutineContext) {
			checkArgument(intent.isActionView())

			val job: suspend () -> Unit = when {
				intent.isSchemeContent() -> { { intentSchemeContent(intent) } }
				else -> { { Unit } }
			}

			actionViewJob.cancel()

			if (intent.isCancellable) {
				actionViewJob = launch { job() }
			} else {
				job()
			}

    }

    private suspend fun intentSchemeContent(intent: IntentWrapper) {
      require(intent.isSchemeContent())
      when {
        intent.isTypeAudio() -> intentContentUriAudio(intent)
      }
    }

    private suspend fun intentContentUriAudio(intent: IntentWrapper) {
      require(intent.isSchemeContent())
      require(intent.isTypeAudio())
      withContext(dispatcher.io) {
        val defPath = async { getAudioPathFromContentUri(intent) }
        val defSongs = async { mediaSource.audio.query() }

        ensureActive()

        findMatchingMediaStoreData(defPath.await(), defSongs.await())
          ?.let { pair ->
            withContext(dispatcher.io) {
              val (song: MediaStoreAudioEntity, list: List<MediaStoreAudioEntity>) = pair
              ensureActive()
              playMediaItem(song, list, true)
							provideMetadata(song.uid, song.uri)
							provideArtwork(song.uid, song.uri)
							list.forEach {
								if (it === song) return@forEach
								launch(dispatcher.io) {
									provideMetadata(it.uid, it.uri)
									provideArtwork(it.uid, it.uri)
								}
							}
            }
          }
          ?: withContext(dispatcher.io) {
            val item = MediaItemFactory.fromMetaData(context, intent.data.toUri())
            if (item == MediaItemFactory.EMPTY) {
              Toast.makeText(context, "Unable To Play Media", Toast.LENGTH_LONG).show()
            } else {
              ensureActive()
              playMediaItem(item)
							provideMetadata(item.mediaId, intent.data.toUri())
							provideArtwork(item.mediaId, intent.data.toUri())
            }
          }
      }
    }

    private fun playMediaItem(
			song: MediaStoreAudioEntity,
			list: List<MediaStoreAudioEntity>,
			fadeOut: Boolean
    ) {
			val factory = mediaSource.audio.mediaItemFactory
      val itemList = list.map { factory.createMediaItem(it, Bundle()) }
      val item = itemList[list.indexOf(song)]
			playMediaItem(item, itemList, fadeOut)
    }

    private fun playMediaItem(
      item: MediaItem,
      list: List<MediaItem>,
      fadeOut: Boolean
    ) {
			with(mediaConnection.playback) {
				stop()
				setMediaItems(list, list.indexOf(item), 0.milliseconds)
				prepare()
				playWhenReady = true
			}
		}

		private suspend fun playMediaItem(
			item: androidx.media3.common.MediaItem
		) {
			val metadata = fillMetadata(item.mediaUri ?: Uri.EMPTY)
			val actual = mediaLib.context.buildMediaItem {
				setMediaId(item.mediaId)
				setMediaUri(item.localConfiguration?.uri ?: item.requestMetadata.mediaUri ?: Uri.EMPTY)
				setExtra(MediaItem.Extra())
				setMetadata(metadata)
			}
			playMediaItem(actual, listOf(actual), true)
		}

		private suspend fun provideMetadata(id: String, uri: Uri) {
			withContext(dispatcher.io) {
				val metadata = fillMetadata(uri)
				mediaConnection.repository.provideMetadata(id, metadata)
			}
		}

		private suspend fun provideArtwork(id: String, uri: Uri) {
			withContext(dispatcher.io) {
				val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setStoreMemoryCacheAllowed(false)
					.setDiskCacheAllowed(false)
					.setStoreDiskCacheAllowed(false)
					.setStoreMemoryCacheAllowed(false)
					.setUri(uri)
					.build()
				val result = artworkProvider.request(req).await()
				if (result.isSuccessful()) mediaConnection.repository.provideArtwork(id, result.get())
			}
		}

		private suspend fun fillMetadata(uri: Uri): MediaMetadata {
			mediaLib.mediaProviders.mediaStore.audio.queryByUri(uri)?.let { from ->
				val audioMetadata = fillAudioMetadata(uri)
				val fileMetadata = VirtualFileMetadata.build {
					setUri(from.uri)
					setScheme(from.uri.scheme)
					setAbsolutePath(from.file.absolutePath)
					setFileName(from.file.fileName)
					setDateAdded(from.file.dateAdded?.seconds)
					setLastModified(from.file.dateModified?.seconds)
					setSize(from.file.size)
				}
				return AudioFileMetadata(audioMetadata, fileMetadata)
			}

			val vfm = VirtualFileMetadata.build {
				setUri(uri)
				setScheme(uri.scheme)
			}

			return AudioFileMetadata(fillAudioMetadata(uri), vfm)
		}

		private fun fillAudioMetadata(uri: Uri): AudioMetadata {
			return AudioMetadata.build {
				try {
					MediaMetadataRetriever().applyUse {
						setDataSource(context, uri)
						setArtist(extractArtist())
						setAlbumArtist(extractAlbumArtist())
						setAlbumTitle(extractAlbum())
						setBitrate(extractBitrate())
						setDuration(extractDuration()?.milliseconds)
						setTitle(extractTitle())
						setPlayable(duration != null)
						setExtra(MediaMetadata.Extra())
					}
				} catch (_: Exception) {}
			}
		}

		private fun MediaMetadataRetriever.applyUse(apply: MediaMetadataRetriever.() -> Unit) {
			try {
				apply(this)
			} finally {
				release()
			}
		}

		private fun MediaMetadataRetriever.extractArtist(): String? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) }
		}

		private fun MediaMetadataRetriever.extractAlbumArtist(): String? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) }
		}

		private fun MediaMetadataRetriever.extractAlbum(): String? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) }
		}

		private fun MediaMetadataRetriever.extractBitrate(): Long? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) }?.toLong()
		}

		private fun MediaMetadataRetriever.extractDuration(): Long? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) }?.toLong()
		}

		private fun MediaMetadataRetriever.extractTitle(): String? {
			return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) }
		}

		private inline fun <R> tryOrNull(block: () -> R): R? {
			return try {
				block()
			} catch (e: Exception) { null }
		}

    private suspend fun getAudioPathFromContentUri(
      intent: IntentWrapper
    ): String = withContext(dispatcher.computation) {
      try {
        require(intent.isSchemeContent())
        val uri = Uri.parse(intent.data)
				val uriString = ContentProvidersHelper.getDecodedContentUri(uri, context, null).toString()
        val decodedUriString = decodeUrl(uriString)
        val dDecodedUriString = decodeUrl(decodedUriString)
        val split2F = uriString
          .split("/")
        val dSplit2F = decodedUriString
          .split("/")
        val ddSplit2F = dDecodedUriString
          .split("/")

        val extStorageString = ContentProvidersHelper.externalStorageDirString
        val storageString = ContentProvidersHelper.storageDirString

        val fileName = context.contentResolver.query(uri, null, null, null, null)
          ?.use { cursor ->
            cursor.moveToFirst()
            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            if (dataIndex > -1) {
              val path = cursor.getString(dataIndex)
              if (File(path).exists() && path.endsWith(name)) {
                return@withContext path
              }
            }
            name
          }

				Timber.d("getAudioPathFromContentUri," +
					"\n uri: $uriString" +
					"\n dUri: $decodedUriString" +
					"\n dDecodedUri: $dDecodedUriString" +
					"\n fileName: $fileName"
				)

        check(
          !fileName.isNullOrBlank()
            && !fileName.startsWith("/")
            && !fileName.endsWith("/")
        )

        ensureActive()

        if (DocumentsContract.isDocumentUri(context, uri)) {
          val a = DocumentProviderHelper.getAudioPathFromContentUri(context, uri)
					Timber.d("MediaIntentHandler tried isDocumentUri," +
						"\n result = $a")
          if (
            a.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())
            || (a.endsWith(fileName) && File(a).exists())
          ) {
            return@withContext a
          }
        }

        if (contentHasFileUri(uri, checkStoragePrefix = true)
          && (dSplit2F.last().endsWith(fileName) || ddSplit2F.last().endsWith(fileName))
        ) {
          val b = tryBuildPathFromFilePrefix(uri, fileName)
					Timber.d("MediaIntentHandler tried hasFileUri," +
						"\n result = $b")
          if (b.endsWith(fileName) && File(b).exists()) {
            return@withContext b
          }
        }

        if (uriString.contains(extStorageString)) {
          val c = tryBuildPathContainsExtStorage(fileName, uri)
					Timber.d("MediaIntentHandler tried contains extStorage," +
						"\n result = $c")
          if (c.isNotBlank() && c.endsWith(fileName) && File(c).exists()) {
            return@withContext c
          }
        }

        if (split2F.last() == fileName) {
          val d = tryBuildPathEndsWithFileName(fileName, uri)
					Timber.d("MediaIntentHandler tried ends with fileName," +
						"\n result = $d")
          if (d.isNotBlank() && d.endsWith(fileName) && File(d).exists()) {
            return@withContext d
          }
        }

        if (
          split2F.last().last().isDigit()
          && intent.type.startsWith("audio/")
          && (split2F.last() != fileName)
        ) {
          val e = tryBuildPathFromUriWithAudioId(context, fileName, uri)
					Timber.d("MediaIntentHandler tried has MediaStore Audio Id")
          if (e.isNotBlank() && e.endsWith(fileName) && File(e).exists()) {
            return@withContext e
          }
        }

        ""
      } catch (e: Exception) {
        Timber.e(e)
        ""
      }
    }
  }

  private suspend fun contentHasFileUri(
    uri: Uri,
    checkStoragePrefix: Boolean
  ): Boolean = withContext(coroutineContext) {
    return@withContext try {
      val uriString = uri.toString()
      require(uriString.isNotEmpty())
      require(uriString.startsWith(ContentProvidersHelper.contentScheme))
      val decodedUriString = decodeUrl(uriString)
      val file = ContentProvidersHelper.fileScheme
      val storage = DocumentProviderHelper.storagePath.filter(Char::isLetter)
      if (!uriString.containsAnyOf(listOf("/file", "/file:", "/file%3A"), true)) {
        return@withContext false
      }
      if (checkStoragePrefix) {
        uriString.contains("$file%3A%2F%2F%2F$storage")
          || decodedUriString.contains("$file:///$storage")
      } else {
        uriString.contains("$file%3A%2F%2F")
          || decodedUriString.contains("$file://")
      }
    } catch (e: Exception) {
      Timber.e(e)
      false
    }
  }

  // TODO fix some holes
  private suspend fun tryBuildPathFromFilePrefix(
    uri: Uri,
    fileName: String,
    start: Int = -1
  ): String {
    return try {
      val builder = StringBuilder()
      val uriString = uri.toString()

      require(fileName.isNotBlank())
      require(uriString.isNotBlank())

      val decodedUriString = decodeUrl(uriString)
      val dSplit2F = decodedUriString.split("/")
      val externalStorage = DocumentProviderHelper.extStoragePathString
      val storage = DocumentProviderHelper.storagePath
      val fileIndex = if (start > 2) {
        start
      } else {
        dSplit2F.indexOf("file:")
      }

      var buildIndex = fileIndex + 2

      if (!uriString.contains(externalStorage)) {

        val storageIndex = dSplit2F.indexOf(storage.filter(Char::isLetter))
        val externalStorageIndex = dSplit2F.indexOf(externalStorage)

        if (storageIndex > -1) {
          buildIndex = storageIndex
          builder.append(storage)
        } else {
          if (externalStorageIndex > -1) {
            buildIndex = externalStorageIndex
          }
          builder.append(externalStorage)
        }
      }

      dSplit2F.forEachIndexed { index, s ->
        if (index > buildIndex) {
          when {
            index == buildIndex + 1 -> {
              builder.append("/$s/")
            }
            index != dSplit2F.lastIndex -> {
              builder.append("$s/")
            }
            index == dSplit2F.lastIndex -> {
              builder.append(fileName)
            }
          }
        }

        Timber.d(
          "BuildingPathFromFileUri:" +
            "\nbuilder = $builder" +
            "\niterator = $s"
        )

      }
      check(builder.startsWith(storage))
      check(builder.endsWith(fileName))
      return builder.toString()
    } catch (e: Exception) {
      Timber.e(e)
      ""
    }
  }

  private suspend fun tryBuildPathContainsExtStorage(fileName: String, uri: Uri): String {
    return try {
      val stringBuilder = StringBuilder()

      val uriString = uri.toString()
      val split2F = uriString.split("/")

      val startIndex = uriString.indexOf(ContentProvidersHelper.externalStorageDirString)
      split2F.forEachIndexed { index, s ->
        if (index > startIndex) {
          stringBuilder.append("/$s")
        }
      }

      if (!stringBuilder.endsWith(fileName)) {
        stringBuilder.replace(0, stringBuilder.length, decodeUrl(stringBuilder.toString()))
        if (!stringBuilder.endsWith(fileName)) {
          stringBuilder.replace(0, stringBuilder.length, decodeUrl(stringBuilder.toString()))
        }
      }
      val s = stringBuilder.toString()
      if (s.endsWith(fileName) && File(s).exists()) return s
      ""
    } catch (e: Exception) {
      Timber.e(e)
      ""
    }
  }

  private suspend fun tryBuildPathEndsWithFileName(fileName: String, uri: Uri): String {
    return try {

      val stringBuilder = StringBuilder()
      val stringUri = uri.toString()
      val split2F = stringUri.split("/")

      val splitted2F = mutableListOf<String>()

      if (decodeUrl(split2F.last()) == fileName) {
        split2F.forEachIndexed { i, s -> if (i != split2F.lastIndex) splitted2F.add(s) }
        splitted2F.add(decodeUrl(split2F.last()))
      } else {
        split2F.forEach { splitted2F.add(it) }
      }

			val downloadDir = Environment.DIRECTORY_DOWNLOADS
			val musicDir = Environment.DIRECTORY_MUSIC
      val externalStorageString = ContentProvidersHelper.externalStorageDirString
      val storage = ContentProvidersHelper.storageDirString
      val fStorage = storage.filter(Char::isLetterOrDigit)

      var startIndex = splitted2F.lastIndex
			when {
				splitted2F.any { it == fStorage } -> {
					stringBuilder.append(fStorage)
					startIndex = splitted2F.indexOf(fStorage) + 1
				}
				with (splitted2F.indexOf(fileName)) { this >= 5 } -> {
					val beforeLast = splitted2F[splitted2F.lastIndex - 1]
					if (beforeLast.first().isUpperCase()) {
						stringBuilder.append(externalStorageString)
						startIndex = splitted2F.indexOf(beforeLast)
					}
				}
			}

      splitted2F.forEachIndexed { i, s ->
        if (i >= startIndex) {
          when {
            i == startIndex -> {
              stringBuilder.append("/$s/")
            }
            i != splitted2F.lastIndex -> {
              stringBuilder.append("$s/")
            }
            i == splitted2F.lastIndex -> {
              stringBuilder.append(fileName)
            }
          }
          Timber.d("MediaIntentDebug building path: $stringBuilder")
        }
      }

      val s = stringBuilder.toString()

      if ((s.startsWith(storage)) && s.endsWith(fileName)) {
        if (File(s).exists()) return s
        s.split("/").forEachIndexed { i, str ->
          if (i != s.lastIndex) if (str.contains("%")) {
            val s2 = decodeUrl(s)
            if (File(s2).exists()) return s2
          }
        }
      }

      ""
    } catch (e: Exception) {
      Timber.e(e)
      ""
    }
  }

  private suspend fun tryBuildPathFromUriWithAudioId(
    context: Context,
    fileName: String,
    uri: Uri
  ): String {
    return try {
      context.contentResolver.query(uri,null, null, null, null)
        ?.use {
          it.moveToFirst()
          val name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
          val size = it.getString(it.getColumnIndexOrThrow(OpenableColumns.SIZE))

          val uriString = uri.toString()
          val split2F = uriString.split("/")

          val mUri = ContentUris.withAppendedId(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
              split2F.last().filter(Char::isDigit).toLong()
            )
          context.contentResolver.query(mUri, null, null, null, null)
            ?.use { cursor ->
							Timber.d("tryBuildPathFromUriWithAudioId querying $mUri")
              cursor.moveToFirst()
              val nName =
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
              val nSize =
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
              val data =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))

              if (name == nName && size == nSize && File(data).exists()){
                return data
              }
            }
        }
      ""
    } catch (e: Exception) {
      Timber.e(e)
      ""
    }
  }

  private suspend fun findMatchingMediaStoreData(
    predicate: String,
    songs: List<MediaStoreAudioEntity>
  ): Pair<MediaStoreAudioEntity, List<MediaStoreAudioEntity>>? {
    return try {
      val storageString = ContentProvidersHelper.storageDirString
      val contentUris = ContentProvidersHelper.contentScheme
      val songList = songs.ifEmpty { mediaSource.audio.query() }

      Timber.d("findMatchingMediaStore with $predicate")

      when {
        predicate.startsWith(contentUris) -> {
          val a = songList.find { it.uri.toString() == predicate }
          if (a != null) return Pair(a, songList)
        }
        !File(predicate).exists() -> return null
        predicate.startsWith(storageString) -> {
          val a = songList.find { it.fileInfo.absolutePath == predicate }
          if (a != null) return Pair(a, songList)
        }
      }
      null
    } catch (e: Exception) {
      Timber.e(e)
      null
    }
  }

  private fun String.containsAnyOf(
    list: List<String>,
    ignoreCase: Boolean
  ): Boolean {
    return try {
      list.any { this.contains(it, ignoreCase) }
    } catch (e: Exception) {
      Timber.e(e)
      false
    }
  }

  init {
    check(context is Application)
  }
}


*/
