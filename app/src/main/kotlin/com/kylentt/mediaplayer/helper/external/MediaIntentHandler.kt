package com.kylentt.mediaplayer.helper.external

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import com.kylentt.mediaplayer.helper.external.providers.DocumentProviders
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import com.kylentt.disposed.musicplayer.data.entity.SongEntity
import com.kylentt.disposed.musicplayer.domain.mediasession.ContentProviders
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.net.URLDecoder
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

interface MediaIntentHandler {
  suspend fun handleMediaIntent(intent: IntentWrapper)
}

@Singleton
class MediaIntentHandlerImpl(
  private val context: Context,
  private val dispatcher: AppDispatchers,
  private val itemHelper: MediaItemHelper,
  private val protoRepo: ProtoRepository,
  private val mediaRepo: MediaRepository,
  private val sessionManager: MediaSessionManager
) : MediaIntentHandler {

  private val actionViewHandler = ActionViewHandler()

  override suspend fun handleMediaIntent(intent: IntentWrapper) {
    require(intent.shouldHandleIntent)
    when {
      intent.isActionView() -> actionViewHandler.handleIntentActionView(intent)
    }
  }

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun decodeUrl(str: String, encoder: String = "UTF-8") =
    withContext(coroutineContext) { URLDecoder.decode(str, encoder) }

  private inner class ActionViewHandler() {

    private var actionViewJob = Job().job

    suspend fun handleIntentActionView(intent: IntentWrapper): Unit
    = withContext(coroutineContext) {
      require(intent.isActionView())
      actionViewJob.cancel()
      actionViewJob = launch {
        when {
          intent.isSchemeContent() -> intentSchemeContent(intent)
        }
      }
    }

    private suspend fun intentSchemeContent(intent: IntentWrapper) {
      require(intent.isSchemeContent())
      when {
        intent.isTypeAudio() -> intentContentUriAudio(intent)
      }
    }

    private suspend fun intentContentUriAudio(intent: IntentWrapper) {
      require(intent.isTypeAudio())
      withContext(dispatcher.io) {
        val defPath = async { getAudioPathFromContentUri(intent) }
        val defSongs = async { mediaRepo.getMediaStoreSong().first() }

        ensureActive()

        findMatchingMediaStoreData(defPath.await(), defSongs.await())
          ?.let { pair ->

            val (song: MediaStoreSong, list: List<MediaStoreSong>) = pair
            withContext(dispatcher.mainImmediate) {
              ensureActive()
              playMediaItem(song, list, true)
            }
          }
          ?: withContext(dispatcher.mainImmediate) {
            val item = itemHelper.buildFromMetadata(intent.data.toUri())
            if (item == MediaItem.EMPTY) {
              Toast.makeText(context, "Unable To Play Media", Toast.LENGTH_LONG).show()
            } else {
              ensureActive()
              playMediaItem(item, listOf(item), true)
            }
          }
      }
    }

    private fun playMediaItem(
      song: SongEntity,
      list: List<SongEntity>,
      fadeOut: Boolean
    ) {
      verifyMainThread()
      val itemList = list.map { it.toMediaItem() }
      val item = itemList[list.indexOf(song)]
      playMediaItem(item, itemList, fadeOut)
    }

    private fun playMediaItem(
      item: MediaItem,
      list: List<MediaItem>,
      fadeOut: Boolean
    ) {
      verifyMainThread()
      val commandList = listOf(
        ControllerCommand.STOP, ControllerCommand.SetMediaItems(list, list.indexOf(item)),
        ControllerCommand.PREPARE, ControllerCommand.SetPlayWhenReady(true)
      )

      val command = ControllerCommand.MultiCommand(commandList)
      sessionManager.sendControllerCommand(if (fadeOut) command.wrapFadeOut() else command)
    }

    private fun ControllerCommand.wrapFadeOut(
      flush: Boolean = false, duration: Long = 1000, interval: Long = 50L, to: Float = 0F
    ): ControllerCommand {
       return ControllerCommand.WithFadeOut(this, flush, duration, interval, to)
    }

    private suspend fun getAudioPathFromContentUri(
      intent: IntentWrapper
    ): String {
      return try {
        require(intent.isSchemeContent())
        val uriString = intent.data
        val uri = Uri.parse(uriString)
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
                return path
              }
            }
            name
        }

        check(
          !fileName.isNullOrBlank()
            && !fileName.startsWith("/")
            && !fileName.endsWith("/")
        )

        if (DocumentsContract.isDocumentUri(context, uri)) {
          val a = DocumentProviders.getAudioPathFromContentUri(context, uri)
          if (a.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) return a
          if (a.isNotBlank() && a.endsWith(fileName) && File(a).exists()) return a
        }

        if (contentHasFileUri(uri, checkStoragePrefix = true)
          && (dSplit2F.last().endsWith(fileName) || ddSplit2F.last().endsWith(fileName))
        ) {
          val b = tryBuildPathFromFilePrefix(uri, fileName)
          if (b.isNotBlank() && b.endsWith(fileName) && File(b).exists()) return b
        }

        if (uriString.contains(extStorageString)) {
          val c = tryBuildPathContainsExtStorage(fileName, uri)
          if (c.isNotBlank() && c.endsWith(fileName) && File(c).exists()) return c
        }

        if (split2F.last() == fileName || decodeUrl(split2F.last()) == fileName) {
          val d = tryBuildPathEndsWithFileName(fileName, uri)
          if (d.isNotBlank() && d.endsWith(fileName) && File(d).exists()) return d
        }

        if (
          split2F.last().last().isDigit()
          && intent.type.startsWith("audio/")
          && (split2F.last() != fileName || decodeUrl(split2F.last()) != fileName)
        ) {
          val e = tryBuildPathFromUriWithAudioId(context, fileName, uri)
          if (e.isNotBlank() && e.endsWith(fileName) && File(e).exists()) return e
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
  ): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val uriString = uri.toString()
      val decodedUriString = decodeUrl(uriString)
      val file = ContentProviders.fileScheme
      val storage = DocumentProviders.storagePath.filter(Char::isLetter)
      require(uriString.isNotEmpty())
      require(uriString.startsWith(ContentProviders.contentScheme))
      if (!uriString.containsAnyOf(listOf("/file", "/file:", "/file%3A"), true)) {
        return@withContext false
      }
      val starting = uriString.split("/")[4]
      val dStarting = decodedUriString.split("/")[4]
      if (!checkStoragePrefix) {
        starting.startsWith("$file%3A%2F%2F")
          || dStarting.startsWith("$file://")
      } else {
        starting.startsWith("$file%3A%2F%2F%2F$storage")
          || dStarting.startsWith("$file:///$storage")
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
      val externalStorage = ContentProviders.DocumentProviders.extStoragePathString
      val storage = ContentProviders.DocumentProviders.storagePath
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
        split2F.forEachIndexed { i, s -> if (i != split2F.lastIndex) splitted2F.add(stringUri) }
        splitted2F.add(decodeUrl(split2F.last()))
      } else {
        split2F.forEach { splitted2F.add(it) }
      }

      val externalStorageString = ContentProvidersHelper.externalStorageDirString
      val storage = ContentProvidersHelper.storageDirString
      val fStorage = storage.filter(Char::isLetterOrDigit)

      var startIndex = 3
      if (!stringUri.contains(externalStorageString)) {
        if (splitted2F.any { it == fStorage }) {
          stringBuilder.append(storage)
          startIndex = splitted2F.indexOf(fStorage)
        } else {
          stringBuilder.append(externalStorageString)
        }
      }

      splitted2F.forEachIndexed { i, s ->
        if (i > startIndex) {
          when {
            i == startIndex + 1 -> {
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
      if (
        (s.startsWith(storage) || s.startsWith(fStorage))
        && s.endsWith(fileName)
        && File(s).exists()
      ) {
        return s
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
    songs: List<MediaStoreSong>
  ): Pair<MediaStoreSong, List<MediaStoreSong>>? {
    return try {
      val storageString = ContentProvidersHelper.storageDirString
      val contentUris = ContentProvidersHelper.contentScheme
      val songList = songs.ifEmpty { mediaRepo.getMediaStoreSong().first() }

      Timber.d("findMatchingMediaStore with $predicate")

      when {
        predicate.startsWith(contentUris) -> {
          val a = songList.find { it.mediaUri.toString() == predicate }
          if (a != null) return Pair(a, songList)
        }
        !File(predicate).exists() -> return null
        predicate.startsWith(storageString) -> {
          val a = songList.find { it.data == predicate }
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


