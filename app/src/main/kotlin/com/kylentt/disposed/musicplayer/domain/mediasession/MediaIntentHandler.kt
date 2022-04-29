package com.kylentt.disposed.musicplayer.domain.mediasession

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.URLDecoder
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@UnstableApi // TODO: Flatten Query
class MediaIntentHandler(
  private val context: Context,
  private val manager: MediaSessionManager,
  private val mediaRepo: MediaRepository,
  private val proto: ProtoRepository
) {

  suspend fun handleIntent(intent: IntentWrapper) = withContext(coroutineContext) {
    require(intent.shouldHandleIntent) { "Handled Intent is sent to IntentHandler \n $intent" }
    when (intent.action) {
      Intent.ACTION_VIEW -> handleIntentActionView(intent)
    }
  }

  private suspend fun handleIntentActionView(intent: IntentWrapper) =
    withContext(coroutineContext) {
      require(intent.action == Intent.ACTION_VIEW)
      when (intent.scheme) {
        ContentProviders.contentScheme -> handleIntentSchemeContent(intent)
      }
    }

  @OptIn(ExperimentalTime::class)
  private suspend fun handleIntentSchemeContent(intent: IntentWrapper) =
    withContext(coroutineContext) {
      require(intent.scheme == ContentProviders.contentScheme)
      if (intent.data.isNotEmpty()) {
        intent.data.let { uri ->
          val (pair: Pair<String, List<MediaStoreSong>>, time: Duration) = measureTimedValue {
            val dPath = async { getPathFromUri(uri.toUri(), intent.type ?: "") }
            val dmsSong = async { mediaRepo.getMediaStoreSong().first() }
            Pair(dPath.await(), dmsSong.await())
          }

          Timber.d("MediaIntentHandler GetPath & query done in ${time.inWholeMilliseconds}ms")

          val (path: String, msSong: List<MediaStoreSong>) = pair

          val a = run { findMatchingMediaStore(path, msSong, uri.toUri()) }
          a?.let { pairedSongList ->
            val (song: MediaStoreSong, List: List<MediaStoreSong>) = pairedSongList

            Timber.d("handleIntent found $song")

            val list = mutableListOf(
              ControllerCommand.STOP,
              ControllerCommand.SetMediaItems(
                List.map { item -> item.toMediaItem() },
                List.indexOf(song)
              ),
              ControllerCommand.PREPARE,
              ControllerCommand.SetPlayWhenReady(true)
            )

            withContext(Dispatchers.Main) {
              manager.sendControllerCommand(

                ControllerCommand.WithFadeOut(
                  ControllerCommand.MultiCommand(list),
                  false,
                  1000L,
                  50,
                )
              )
            }

          } ?: run {
            Timber.d("handleIntent uri not found intent = $intent, path = $path,")
            val m = fromMetadata(uri.toUri())
            if (m == MediaItem.EMPTY) {
              withContext(Dispatchers.Main) {
                Toast.makeText(
                  context,
                  "Unable to Play Media",
                  Toast.LENGTH_LONG
                ).show()
              }
            } else {
              prepareWithFade(m)
            }
          }
        }
      }
      Timber.d("Handled Intent with Context: ${kotlin.coroutines.coroutineContext}")
    }

  private suspend fun prepareWithFade(item: MediaItem) = withContext(Dispatchers.Main) {
    val list = mutableListOf(
      ControllerCommand.STOP,
      ControllerCommand.SetMediaItems(listOf(item), 0, 0),
      ControllerCommand.PREPARE,
      ControllerCommand.SetPlayWhenReady(true)
    )
    manager.sendControllerCommand(
      ControllerCommand.WithFadeOut(
        ControllerCommand.MultiCommand(list),
        false, 1000, 50, 0F
      )
    )
  }

  private suspend fun fromMetadata(uri: Uri): MediaItem = withContext(Dispatchers.IO) {
    return@withContext try {
      val mtr = MediaMetadataRetriever()
      mtr.setDataSource(context, uri)
      val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "<Unknown>"
      val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "<Unknown>"
      val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        ?: try {
          val c = context.contentResolver.query(uri, null, null, null, null)
          c?.use {
            it.moveToFirst()
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.getString(i)
          } ?: "Unknown"
        } catch (e: Exception) {
          Timber.e(e)
          "Unknown"
        }

      MediaItem.Builder()
        .setUri(uri)
        .setMediaId((getMediaIdFromHash(uri)).toString())
        .setMediaMetadata(
          MediaMetadata.Builder()
            .setMediaUri(uri)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDisplayTitle(title)
            .build()
        ).build()
    } catch (e: Exception) {
      Timber.e(e)
      MediaItem.EMPTY
    }
  }

  private fun getMediaIdFromHash(uri: Uri): Long {
    return (MediaItem.fromUri(uri).hashCode()).toLong()
  }

  private suspend fun findMatchingMediaStore(
    pred: String,
    songList: List<MediaStoreSong> = emptyList(),
    uri: Uri
  ): Pair<MediaStoreSong, List<MediaStoreSong>>? {
    if (pred.isEmpty()) return null
    Timber.d("MediaIntentDebug findMatchingMediaStore for $pred")

    val envs = Environment.getExternalStorageDirectory().toString()
    val storage = if (VersionHelper.hasR()) {
      Environment.getStorageDirectory().toString()
    } else {
      "/storage"
    }
    val a = songList.ifEmpty { mediaRepo.getMediaStoreSong().first() }
    var b: MediaStoreSong? = null
    try {
      when {
        pred.startsWith(ContentProviders.contentScheme) -> {
          b = a.find { it.mediaUri.toString() == pred }
        }
        pred.startsWith(storage) && File(pred).exists() -> {
          Timber.d("startsWith $storage and File does exist")
          b = a.find { it.data == pred }
        }
        pred.startsWith(storage) && !File(pred).exists() -> {
          Timber.d("startsWith $storage but File $pred doesn't exist")
          // tried this but the file name is not valid in the first place
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
    return b?.let { Pair(it, a) }
  }

  private suspend fun maybeInvalidAudioFileName(
    path1: String,
    path2: List<String?>,
    uri: Uri
  ): String? {
    val storage = ContentProviders.DocumentProviders.storagePath
    val envs = ContentProviders.DocumentProviders.extStoragePathString
    val lastModified = context.contentResolver.query(uri, null, null, null, null)?.use {
      it.moveToFirst()
      val index = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
      val value = if (index > -1) {
        it.getString(index)
      } else {
        null
      }
      value
    }

    return if (path1.startsWith(storage)) {
      val splitDot = path1.split(".")
      if (splitDot.size > 1) {
        val starts = if (path1.startsWith(envs)) {
          envs
        } else {
          storage
        }
        val predSplit2F = path1
          .split("/")
        val a = run {
          var toReturn = ""
          val can = mutableListOf<Pair<String, String>>()
          path2.forEach {
            it?.let { path ->
              val pathSplit2F = path.split("/")
              if (predSplit2F.lastIndex - 1 == pathSplit2F.lastIndex - 1) {
                if (maybeCheckDupe(path1, path, starts)) {
                  Timber.d(
                    "maybeCheckDupe,path1 LastModified = $lastModified path2 LastModified = ${
                      File(
                        path
                      ).lastModified()
                    }"
                  )
                  can.add(Pair(path, File(path).lastModified().toString()))
                }
              }
            }
          }

          var longest = 0
          can.forEach {
            var itLongest = 0
            lastModified?.let { last ->
              last.forEachIndexed { index, c ->
                if (c == it.second[index]) {
                  itLongest = index
                }
              }
            }
            if (itLongest > longest) {
              Timber.d("$itLongest with ${it.first} > $longest")
              longest = itLongest
              toReturn = it.first
            }
          }
          toReturn.ifEmpty { null }
        }
        return a
      }
      null
    } else {
      null
    }
  }

  private suspend fun maybeCheckDupe(
    path1: String,
    path2: String,
    start: String
  ): Boolean = withContext(coroutineContext) {
    val a = path1.split(".")
    val b = path2.split(".")
    if (a.size > 1 && b.size > 1) {
      val aa = a[a.lastIndex - 1]
      val bb = b[b.lastIndex - 1]
      val bbb = bb.split("-")
      if (bbb.size > 1) {
        if (aa == bbb[bbb.lastIndex - 1]
          && ((aa.startsWith(start) && bb.startsWith(start)))
        ) {
          Timber.d(
            "maybeCheckDupe," +
              "\n a = $a" +
              "\n b = $b" +
              "\n aa = $aa" +
              "\n bb = $bb" +
              "\n bbb = $bbb"
          )
          return@withContext true
        }
      }
    }
    false
  }

  @SuppressLint("NewApi")
  private suspend fun getPathFromUri(
    uri: Uri,
    type: String
  ): String {
    Timber.d("MediaIntentDebug getPathFromUri $uri")
    return try {

      val stringUri = uri.toString()
      val decodedStringUri = stringUri.decodeUrl()
      val dDecodedStringUri = decodedStringUri.decodeUrl()

      require(stringUri.isNotEmpty())
      val split2F = stringUri
        .split("/")
      val dSplit2F = decodedStringUri
        .split("/")
      val ddSplit2F = dDecodedStringUri
        .split("/")
      val externalStorageString = ContentProviders.DocumentProviders.extStoragePathString
      val storagePathString = ContentProviders.DocumentProviders.storagePath

      val fileName = context
        .contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          cursor.moveToFirst()
          val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
          val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
          if (BuildConfig.DEBUG) {
            // For Debugging Purposes
            ContentProviders.DocumentProviders.logUnknownProvider(context, uri)
          }
          if (dataIndex > -1) {
            val data = cursor.getString(dataIndex)
            Timber.d("MediaIntentDebug, Uri Contains _data column:\n$data")
            if (data.startsWith(storagePathString) && data.endsWith(name)) {
              return data
            }
          }
          name
        }
        .toString()

      Timber.d("MediaIntentDebug Uri Doesn't contain _data column")

      require(
        fileName.isNotEmpty() && fileName.startsWith("/").not() && fileName.endsWith("/").not()
      )
      if (DocumentsContract.isDocumentUri(context, uri)) {
        Timber.d("MediaIntentDebug, isDocumentUri == true")
        val a = ContentProviders.DocumentProviders.getContentMediaUri(context, uri, fileName)
        if (a.isNotEmpty() || a != ContentProviders.DocumentProviders.INVALID_PATH) {
          return a
        }
      }

      when {
        contentHasFileUri(
          uri,
          true
        ) && (dSplit2F.last() == fileName || ddSplit2F.last() == fileName) -> {
          return tryBuildPathFromFilePrefix(uri, fileName)
            .ifEmpty { ContentProviders.NOT_SUPPORTED }
        }
        stringUri.contains(externalStorageString) -> {
          Timber.d("MediaIntentDebug, Uri Contain $externalStorageString")
          val builder = StringBuilder()

          val start = split2F.indexOf(externalStorageString.split("/").first())
          Timber.d("$start $split2F")
          split2F.forEachIndexed { index, s ->
            Timber.d("forEachIndexed $index, $s, $start")
            if (index > start) {
              builder.append("/$s")
            }
          }

          Timber.d("before: $builder")
          builder.replace(0, builder.length, builder.toString().decodeUrl())

          Timber.d("after: $builder")
          Timber.d(fileName)

          if (!builder.endsWith(fileName)) {
            builder.replace(0, builder.length, builder.toString().decodeUrl())
            if (!builder.endsWith(fileName)) {
              return ContentProviders.NOT_SUPPORTED
            }
          }
          return builder.toString().ifEmpty { ContentProviders.NOT_SUPPORTED }
        }
        split2F.last().decodeUrl() == fileName || split2F.last() == fileName -> {
          val builder = StringBuilder()
          val splitted2F = if (split2F.last().decodeUrl() == fileName) dSplit2F else split2F
          var start = 3
          if (stringUri.contains(externalStorageString).not()) {
            val storage = ContentProviders.DocumentProviders.storagePath
            val fStorage = storage.filter(Char::isLetterOrDigit)
            if (splitted2F.any { it == fStorage }) {
              builder.append(storage)
              start = splitted2F.indexOf(fStorage)
            } else {
              builder.append(externalStorageString)
            }
          }
          splitted2F.forEachIndexed { index, s ->
            if (index > start) {
              when {
                index == start + 1 -> {
                  builder.append("/$s/")
                }
                index != splitted2F.lastIndex -> {
                  builder.append("$s/")
                }
                index == splitted2F.lastIndex -> {
                  builder.append(fileName)
                }
              }
              Timber.d("MediaIntentDebug building path: $builder")
            }
          }
          Timber.d("MediaIntentDebug, Uri Contain filename, returning with $builder,\nfrom $splitted2F,\nwith size ${splitted2F.size}")
          return builder.toString()
        }
        (split2F.last().last()
          .isDigit() && type.startsWith("audio/")) && (dSplit2F.last() != fileName || dSplit2F.last() != fileName.decodeUrl()) -> {
          Timber.d("MediaIntentDebug, Uri ends with\n${split2F.last()}\nand\n$dSplit2F != $fileName")
          val builder = StringBuilder()
          builder.append(tryAudioUri(context, uri))
          return builder.toString()
            .ifEmpty {
              ContentProviders.DocumentProviders.logUnknownProvider(context, uri)
                .ifEmpty { ContentProviders.NOT_SUPPORTED }
            }
        }
        else -> {
          Timber.d("MediaIntentDebug, Uri not recognized Logging...")
          val builder = StringBuilder()
          val l = ContentProviders.DocumentProviders.logUnknownProvider(context, uri)
          builder.append(l)
          builder.toString()
            .ifEmpty { ContentProviders.NOT_SUPPORTED }
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
      ContentProviders.NOT_SUPPORTED
    }
  }

  @Suppress("BlockingMethodInNonBlockingContext")
  private suspend fun String.decodeUrl(enc: String = "UTF-8"): String =
    withContext(Dispatchers.IO) { URLDecoder.decode(this@decodeUrl, enc).toString() }


  private suspend fun contentHasFileUri(
    uri: Uri,
    checkStoragePrefix: Boolean
  ): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val uris = uri.toString()
      if (BuildConfig.DEBUG) {
        require(uris.isNotEmpty())
        require(uris.startsWith(ContentProviders.contentScheme))
      }
      if (uris.containsAnyOf(listOf("/file", "/file:", "/file%3A"), true).not()) {
        return@withContext false
      }
      val dUri = uris.decodeUrl()
      val starting = uris.split("/")[4]
      val dStarting = dUri.split("/")[4]
      val file = ContentProviders.fileScheme
      val storage = ContentProviders.DocumentProviders.storagePath.filter(Char::isLetter)
      if (!checkStoragePrefix) {
        starting.startsWith("$file%3A%2F%2F") || dStarting.startsWith("$file://")
      } else {
        starting.startsWith("$file%3A%2F%2F%2F$storage") || dStarting.startsWith("$file:///$storage")
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
      val stringUri = uri.toString()

      if (BuildConfig.DEBUG) {
        require(fileName.isNotEmpty())
        require(stringUri.isNotEmpty())
      }

      val decodedStringUri = stringUri.decodeUrl()
      val dSplit2F = decodedStringUri.split("/")
      val fileIndex = if (start > 2) {
        start
      } else {
        dSplit2F.indexOf("file:")
      }

      val externalStorage = ContentProviders.DocumentProviders.extStoragePathString
      val storage = ContentProviders.DocumentProviders.storagePath

      var buildIndex = fileIndex + 2

      if (stringUri.contains(externalStorage).not()) {

        val storageIndex = dSplit2F.indexOf(storage.filter(Char::isLetter))
        val extsIndex = dSplit2F.indexOf(externalStorage)

        if (storageIndex > -1) {
          buildIndex = storageIndex
          builder.append(storage)
        } else {
          if (extsIndex > -1) {
            buildIndex = extsIndex
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

  suspend fun tryAudioUri(
    context: Context,
    uri: Uri
  ): String = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use {
        it.moveToFirst()
        val name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        val size = it.getString(it.getColumnIndexOrThrow(OpenableColumns.SIZE))

        val uris = uri.toString()
        val split2F = uris.split("/")
        val split2fLast = split2F.last()

        if ((split2fLast.first().isDigit() && split2fLast.last()
            .isDigit()) || (split2fLast.startsWith("Audio")) && split2fLast.last().isDigit()
        ) {
          Timber.d("MediaIntentHandler tryAudioUri $uri")
          val mUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            split2fLast.filter(Char::isDigit).toLong()
          )
          context.contentResolver.query(mUri, null, null, null, null)?.use { c ->
            c.moveToFirst()
            val nName =
              c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
            val nSize = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE))
            val data = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA))
            if (name == nName && size == nSize) {
              return@withContext data
            }
          }
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
      return@withContext ""
    }
    return@withContext ""
  }
}

sealed class ContentProviders {

  object DocumentProviders : ContentProviders() {
    const val INVALID_PATH = "INVALID_PATH"
    val storagePath = if (VersionHelper.hasR()) {
      Environment.getStorageDirectory().toString()
    } else {
      "/storage"
    }
    val extStoragePath: File? = Environment.getExternalStorageDirectory()
    val extStoragePathString = extStoragePath?.toString() ?: "/storage/emulated/0"

    suspend fun getContentMediaUri(context: Context, uri: Uri, fileName: String): String {
      return try {
        Timber.d("MediaIntentDebug DocumentProviders getContentMediaUri")

        val builder = StringBuilder()
        val uris = uri.toString()
        val dUris = uris.decodeUrl()

        require(uris.isNotEmpty())
        require(dUris.startsWith(contentScheme))

        val split2F = uris
          .split("/")

        val split3A = uris
          .split(":")

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          cursor.moveToFirst()
          require(
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
              .equals(fileName)
          )

          val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
          if (data > -1) {
            return cursor.getString(data)
          }
        }

        val auth = uri.authority ?: ""

        val tries = when {
          auth.startsWith(auth_provider_externalStorage) -> tryExternalStorage(uri, fileName)
          auth.startsWith(auth_provider_media) -> tryMediaProviders(uri, context)
          auth.startsWith(auth_provider_download) -> tryDownloadProviders(
            uri,
            context,
            fileName
          )
          else -> null
        }

        tries?.let { builder.append(it) }
          ?: builder.append(logUnknownProvider(context, uri))

        Timber.d(
          "getContentMediaUri uri = $uri" +
            "\n2f = $split2F" +
            "\n3a = $split3A" +
            "\ntries = $tries" +
            "\ndecoded uri: ${uris.decodeUrl()}"
        )

        if (builder.startsWith(storagePath)) {
          checkBuildedPath(builder.toString(), fileName)
        }
        return builder.toString()
      } catch (e: Exception) {
        Timber.e(e)
        INVALID_PATH
      }
    }

    private fun checkBuildedPath(
      path: String,
      fileName: String
    ) = run {
      Timber.d("checkBuildedPath $path for $fileName")
      check(path.startsWith(storagePath))
      check(path.endsWith(fileName))
    }

    private fun tryExternalStorage(
      uri: Uri,
      fileName: String
    ): String {
      val builder = StringBuilder()
      val uris = uri.toString()
      val dUris = URLDecoder.decode(uris, "UTF-8")


      val split2F = uris
        .split("/")

      val dSplit2F = dUris
        .split("/")

      val dSplit3A = dUris
        .split(":")

      /* primary:Directory is the common result*/
      val prim = run {
        val b = StringBuilder()
        if (dSplit3A.size > 1) {
          // there is ":" other than "content:" in decoded Url
          if (split2F.size > 4) {
            // plain split "/" url has index 4
            // which is likely to be storage path in case of document provider
            if (dSplit3A[1].endsWith(dSplit2F[4].split(":").first())) {
              // decoded split ":" second index which after content://
              // does ends with the starting string of decoded "/"
              b.append(dSplit2F[4])
            }
          }
        }
        b.toString()
      }

      when {
        prim.isNotEmpty() -> {
          val primPrefix = prim.split(":")[0]
          val primSuffix = prim.split(":")[1]
          val directory = dSplit3A[2]
          if (primPrefix == "primary") {
            if (directory.endsWith(fileName)) {
              builder.append("${extStoragePathString}/$directory")
            }
            Timber.d("Document prim == primary, $builder")
          } else {
            if (directory.endsWith(fileName)) {
              builder.append("$storagePath/$primPrefix/$directory")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              Timber.d("getContentMediaUriDebug, root = ${Environment.getRootDirectory()}, storage = ${Environment.getStorageDirectory()}")
            }
            Timber.d("Document prim != primary, $builder, prim = $prim, primPrefix = $primPrefix, primSuffix = $primSuffix")
          }

        }
      }
      return builder.toString()
    }

    private fun tryMediaProviders(
      uri: Uri,
      context: Context
    ): String {
      val builder = StringBuilder()
      val a = getMediaAudioUri(context, uri)?.toString()
        ?: INVALID_PATH
      builder.append(a)
      Timber.d("audioProviderMedia $builder")
      return builder.toString()
    }

    private fun tryDownloadProviders(
      uri: Uri,
      context: Context,
      fileName: String
    ): String {
      val builder = StringBuilder()
      val a = getDownloadAudioPath(context, uri, fileName)
        ?: INVALID_PATH
      Timber.d("audioProviderDownload $a")
      builder.append(a)
      return builder.toString()
    }

    suspend fun logUnknownProvider(
      context: Context,
      uri: Uri
    ): String = withContext(Dispatchers.IO) {
      try {
        val builder = StringBuilder()
        val c = context.contentResolver.query(uri, null, null, null, null)
        c?.use {
          it.moveToFirst()
          for (i in 0 until it.columnCount) {
            try {
              val name = c.getColumnName(i)
              val value = c.getString(i)
              if (name == MediaStore.MediaColumns.DATA) builder.append(value)
              Timber.d(
                "logProvider, i = $i, column = ${it.getColumnName(i)}, value ${
                  it.getString(
                    i
                  )
                }"
              )
            } catch (e: Exception) {
              Timber.e("Exception when looping Media Column: $e")
            }
          }
          Timber.d("from uri: $uri")
        }
        builder.toString()
      } catch (e: Exception) {
        Timber.e(e)
        ""
      }
    }

    private fun getMediaAudioUri(
      context: Context,
      uri: Uri
    ): Uri? {
      val c = context.contentResolver.query(uri, null, null, null, null)
      return c?.use {
        it.moveToFirst()
        val data = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        if (BuildConfig.DEBUG) {
          for (i in 0 until it.columnCount) {
            "getMediaAudioUri, i = $i, column = ${it.getColumnName(i)}, value ${it.getString(i)}"
          }
        }
        ContentUris.withAppendedId(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          it.getString(data).filter(Char::isDigit).toLong()
        )
      }
    }

    fun getDownloadAudioPath(
      context: Context,
      uri: Uri, fileName: String
    ): String? {
      val dc = URLDecoder.decode(uri.toString(), "UTF-8")
        .split("/")

      val cUri = if (VersionHelper.hasQ() && dc.last().startsWith("msf")) {
        val a = context.contentResolver.query(uri, null, null, null, null)?.use {
          it.moveToFirst()
          it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
        }
        val b = when {
          a?.startsWith("audio") == true -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
          else -> null
        }
        b?.let { ContentUris.withAppendedId(it, dc.last().filter(Char::isDigit).toLong()) }
          ?: uri
      } else {
        uri
      }

      val c = context.contentResolver.query(cUri, null, null, null, null)
      return c?.use {
        it.moveToFirst()

        Timber.d("getDownloadAudioPath, uri = $cUri}")
        if (BuildConfig.DEBUG) {
          for (i in 0 until it.columnCount) {
            try {
              Timber.d(
                "getDownloadAudioPath," +
                  " i = $i, column = ${it.getColumnName(i)}, " + "value ${it.getString(i)}"
              )
            } catch (e: Exception) {
              Timber.e("Exception when looping Media Column: $e")
            }
          }
        }

        val data = it.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
        val dName = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val name = c.getString(dName)

        if (fileName.isNotEmpty() && fileName != name) {
          return null
        }

        if (data != -1) {
          it.getString(data)
        } else {
          "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/${name}"
        }
      }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun String.decodeUrl(enc: String = "UTF-8"): String =
      withContext(Dispatchers.IO) { URLDecoder.decode(this@decodeUrl, enc).toString() }
  }

  companion object {
    const val NOT_SUPPORTED = "NOT_SUPPORTED"

    const val auth_provider_externalStorage = "com.android.externalstorage"
    const val auth_provider_media = "com.android.providers.media"
    const val auth_provider_download = "com.android.providers.downloads"
    const val contentScheme = ContentResolver.SCHEME_CONTENT
    const val fileScheme = ContentResolver.SCHEME_FILE
  }
}
