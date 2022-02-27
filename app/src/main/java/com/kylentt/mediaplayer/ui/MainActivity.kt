package com.kylentt.mediaplayer.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_MEDIA
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.core.util.removeSuffix
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

    @Inject
    lateinit var coil: ImageLoader

    // List
    private var mediaItems = listOf<MediaItem>()
    private val controller: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true

        controller // simple call so viewModel init is called

        handleIntent(intent)

        if (checkPermission()) {
            // TODO: Permission Screen
        }

        setContent {

        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("IntentHandler MainActivity NewIntent")
        intent?.let {
            Timber.d("IntentHandler Forwarding Intent")
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> handleIntentActionView(intent)
        }
    }

    private fun handleIntentActionView(intent: Intent) {
        intent.data?.let { uri ->
            Timber.d("IntentHandler Activity Intent $uri")
            when (uri.scheme) {
                "content" -> {
                    Timber.d("IntentHandler scheme : ${uri.scheme}")
                    when {
                        uri.toString()
                            .startsWith("content://com.android.providers") -> handlePlayWithIntent(uri)
                        uri.toString()
                            .startsWith("content://com.google.android.apps.docs.storage.legacy/") -> handlePlayDriveIntent(uri)
                        else -> Toast.makeText(this, "unsupported, please inform us", Toast.LENGTH_LONG)
                    }
                }
                else -> Toast.makeText(this, "unsupported, please inform us", Toast.LENGTH_LONG)
            }
        } ?: Timber.e("IntentHandler ActionView null Data")
    }

    private fun handlePlayDriveIntent(uri: Uri) {
        Timber.d("IntentHandler Handling PlayDriveIntent...")
        lifecycleScope.launch(Dispatchers.Default) {
            val mtr = MediaMetadataRetriever()
            mtr.setDataSource(applicationContext, uri)
            val artist = mtr.extractMetadata(METADATA_KEY_ARTIST)
            val album = mtr.extractMetadata(METADATA_KEY_ALBUM)
            val title = mtr.extractMetadata(METADATA_KEY_TITLE)
            val pict = mtr.embeddedPicture

            val art = pict?.let {
                BitmapFactory.decodeByteArray(pict, 0, pict.size).squareWithCoil()
            }
            val bArr = art?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            val item = MediaItem.Builder().setUri(uri).setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkData(bArr, PICTURE_TYPE_MEDIA)
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setSubtitle(artist ?: album)
                    .setMediaUri(uri)
                    .build()
            ).build()
            Timber.d("IntentHandler PlayDriveIntent forwarded ${item.getDisplayTitle})")
            controller.handleItemIntent(item)
        }
    }

    private suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(this@MainActivity)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(256)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    /*// Legit just uploaded the files to G-Drive LOL
    private fun handlePlayDriveIntent(uri: Uri) {
        val res = contentResolver.openInputStream(uri)
        val ofd = contentResolver.openFileDescriptor(uri, "rw")
        res?.let { inputStream ->
            val buffer = ByteArray(8192)
            val output = FileOutputStream(ofd!!.fileDescriptor)
            output.use { outStream ->
                while (true) {
                    val length = inputStream.read(buffer)
                    if (length <= 0) break
                    outStream.write(buffer, 0, length)
                }
                outStream.flush()
                outStream.close()
            }
        }
        handlePlayWithIntent(uri)
    }*/

    private fun handlePlayWithIntent(uri: Uri) {
        lifecycleScope.launch(Dispatchers.Default) {
            contentResolver.query(uri,
                null, null, null, null
            )?.use { cursor ->
                while(cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val byteIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val lastIndex = cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    )
                    val name = cursor.getString(nameIndex)
                    val byte = cursor.getLong(byteIndex)
                    val last = cursor.getLong(lastIndex)
                    controller.handlePlayIntent(name, byte, last.removeSuffix("000"), uri)
                    Timber.d("MainActivity intent handler $name $byte $last ")
                }
            }
        }
    }

        override fun onResume() {
            super.onResume()
        }

        private fun checkPermission(): Boolean {
            return true
        }

        override fun onDestroy() {
            isActive = false
            super.onDestroy()
        }
}

