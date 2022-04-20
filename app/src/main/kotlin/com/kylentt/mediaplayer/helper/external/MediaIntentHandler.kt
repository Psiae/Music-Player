package com.kylentt.mediaplayer.helper.external

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import kotlinx.coroutines.*
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
  private val protoRepo: ProtoRepository
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
      }
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

        val nCursor = context.contentResolver.query(uri, null, null, null, null)

        val fileName = nCursor?.let { cursor ->
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

        }

        nCursor.close()
        ""
      } catch (e: Exception) {
        Timber.e(e)
        ""
      }
    }

  }


  init {
    check(context is Application)
  }
}


