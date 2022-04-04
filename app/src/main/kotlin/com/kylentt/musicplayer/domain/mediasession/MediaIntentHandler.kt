package com.kylentt.musicplayer.domain.mediasession

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.data.entity.Song
import com.kylentt.musicplayer.data.repository.ProtoRepository
import com.kylentt.musicplayer.data.source.local.MediaStoreSource
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.ui.musicactivity.IntentWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.random.Random

internal class MediaIntentHandler(
    private val context: Context,
    private val manager: MediaSessionManager,
    private val mediaStore: MediaStoreSource,
    private val proto: ProtoRepository
) {
    suspend fun handleIntent(intent: IntentWrapper) {
        if (intent.handled) {
            Timber.e("Handled Intent is sent to IntentHandler \n $intent")
            return
        }
        when(intent.action) {
            Intent.ACTION_VIEW -> handleIntentActionView(intent)
        }
    }

    private suspend fun handleIntentActionView(intent: IntentWrapper) {
        check(intent.action == Intent.ACTION_VIEW)
        when(intent.scheme) {
            ContentProviders.contentScheme -> handleIntentSchemeContent(intent)
        }
    }

    private suspend fun handleIntentSchemeContent(intent: IntentWrapper) {
        check(intent.scheme == ContentProviders.contentScheme)
        intent.data?.let { uri ->
            val path = getPathFromUri(uri)
            if (path == ContentProviders.NOT_SUPPORTED) {
                val m = fromMetadata(uri)
                if (m == MediaItem.EMPTY) {
                    Toast.makeText(this.context, "Unable to Play Media", Toast.LENGTH_LONG).show()
                    return
                } else {
                    val list = mutableListOf(
                        ControllerCommand.Stop,
                        ControllerCommand.SetMediaItem(m),
                        ControllerCommand.Prepare,
                        ControllerCommand.SetPlayWhenReady(true)
                    )
                    withContext(Dispatchers.Main) {
                        manager.sendCommand(ControllerCommand.WithFade(ControllerCommand.MultiCommand(list)))
                    }
                }
            }

            val a = findMatching(path)
            a?.let {
                Timber.d("handleIntent found $it.data")

                val a = it.first
                val b = it.second

                val list = mutableListOf(
                    ControllerCommand.Stop,
                    ControllerCommand.SetMediaItems(b.map { item -> item.toMediaItem() }, b.indexOf(a)),
                    ControllerCommand.Prepare,
                    ControllerCommand.SetPlayWhenReady(true)
                )

                withContext(Dispatchers.Main) {
                    manager.sendCommand(ControllerCommand.WithFade(ControllerCommand.MultiCommand(list)))
                }

            } ?: run {
                Timber.d("handleIntent uri not found intent = $intent, path = $path,")
            }
        } ?: run {
            Timber.e("Intent sent to IntentHandler when data is null")
        }
    }

    private suspend fun fromMetadata(uri: Uri): MediaItem {
        return try {
            val mtr = MediaMetadataRetriever()
            mtr.setDataSource(this.context, uri)
            val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "<Unknown>"
            val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "<Unknown>"
            val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: try { val c = context.contentResolver.query(uri, null, null, null, null)
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
                .setMediaId(Random.nextInt(5555, 6000).toString())
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

    private suspend fun findMatching(pred: String): Pair<Song, List<Song>>? {
        val b = mediaStore.getMediaStoreSong().first()
        val a = b.find {
            val env = Environment.getExternalStorageDirectory()
            when {
                pred.startsWith("$env") -> pred == it.data
                else -> pred == it.mediaUri.toString()
            }
        }
        return a?.let { Pair(it, b) }
    }

    private suspend fun getPathFromUri(uri: Uri): String {
        return if (DocumentsContract.isDocumentUri(context, uri)) {
            try {
                ContentProviders.DocumentProviders.getContentMediaUri(context, uri)
            } catch (e: Exception) {
                Timber.e(e)
                ContentProviders.NOT_SUPPORTED
            }
        } else {
            Timber.e("not Document Uri : $uri")
            try {
                val c = context.contentResolver.query(uri, null, null, null, null)
                c?.use {
                    it.moveToFirst()
                    for (i in 0 until it.columnCount) {
                        Timber.d("Unknown Provider. columnName = ${c.getColumnName(i)}, value = ${c.getString(i)}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            ContentProviders.NOT_SUPPORTED
        }
    }
}

sealed class ContentProviders {

    object DocumentProviders : ContentProviders() {
        const val INVALID_PATH = "INVALID_PATH"

        fun getContentMediaUri(context: Context, uri: Uri): String {
            val builder = StringBuilder()
            val uris = uri.toString()
            val split2F = uris.split("/")
            val split3A = uris.split(":")

            when {
                split3A.isNotEmpty() && "primary" == split3A[1] -> {
                    builder.append("${Environment.getExternalStorageDirectory()}/$split3A[2]")
                }
                split2F[2].startsWith(auth_provider_download) -> {
                    val a = getDownloadAudioPath(context, uri)
                        ?: INVALID_PATH
                    Timber.d("audioProviderDownload $a")
                    builder.append(a)
                }
                split2F[2].startsWith(auth_provider_media) -> {
                    val a = getMediaAudioUri(context, uri)?.toString()
                        ?: INVALID_PATH
                    builder.append(a)
                }
            }

            val a = builder.toString().replace("%3A", ":")
            val b = a.replace("%2F", "/")
            val c = b.replace("%20", " ")
            val d = c.replace("primary:", "")
            builder.clear()
            builder.append(d)
            return builder.toString().ifEmpty { INVALID_PATH }
        }

        fun getMediaAudioUri(context: Context, uri: Uri): Uri? {
            val c = context.contentResolver.query(uri, null, null, null, null)
            return c?.use {
                it.moveToFirst()
                val data = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.getString(data).filter(Char::isDigit).toLong())
            }
        }

        fun getDownloadAudioPath(context: Context, uri: Uri): String? {
            val c = context.contentResolver.query(uri, null, null, null, null)
            return c?.use {
                it.moveToFirst()
                val data = it.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                val name = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (data != -1) {
                    it.getString(data)
                } else {
                    "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/${it.getString(name)}"
                }
            }
        }
    }

    companion object {
        const val NOT_SUPPORTED = "NOT_SUPPORTED"

        const val auth_provider_media = "com.android.providers.media"
        const val auth_provider_download = "com.android.providers.downloads"
        const val contentScheme = ContentResolver.SCHEME_CONTENT
    }
}