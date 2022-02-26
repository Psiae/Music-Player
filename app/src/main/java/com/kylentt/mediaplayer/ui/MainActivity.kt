package com.kylentt.mediaplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.util.removeSuffix
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

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
        Timber.d("MainActivity onNewIntent")
        intent?.let { handleIntent(intent) }
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> handleIntentActionView(intent)
        }
    }

    private fun handleIntentActionView(intent: Intent) {
        intent.data?.let { uri ->
            Timber.d("Activity Intent $uri")
            when (uri.scheme) {
                "content" -> when {
                    uri.toString().startsWith("content://com.android.providers")
                    -> handlePlayWithIntent(uri)
                    uri.toString().startsWith("content://com.google.android.apps.docs.storage.legacy/")
                    -> handlePlayWithIntent(uri)
                }
            }
        } ?: Timber.e("Activity Intent ActionView null Data")
    }

    /*private fun handlePlayDriveIntent(uri: Uri) {
        val res = contentResolver.openInputStream(uri)
        res?.let { inputStream ->
            val buffer = ByteArray(8192)
            val output = FileOutputStream(filesDir.absolutePath + "")
            var length = inputStream.read(buffer)
            output.use { outStream ->
                while (length >= 0) {
                    length = inputStream.read(buffer)
                    outStream.write(buffer, 0, length)
                }
                outStream.flush()
            }
        }
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

