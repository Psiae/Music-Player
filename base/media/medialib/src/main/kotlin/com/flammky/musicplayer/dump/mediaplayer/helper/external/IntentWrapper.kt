package com.flammky.musicplayer.dump.mediaplayer.helper.external

import android.content.Intent
import com.flammky.musicplayer.dump.mediaplayer.helper.external.providers.ContentProvidersHelper

data class IntentWrapper @JvmOverloads constructor(
  private val intent: Intent,
  private val cancellable: Boolean = true,
  private var handled: Boolean = false
) {

  val action
    get() = intent.action
  val data
    get() = intent.data?.toString() ?: ""
  val scheme
    get() = intent.scheme
      ?: if (ContentProvidersHelper.isSchemeContentUri(data)) {
        ContentProvidersHelper.contentScheme
      } else {
        ""
      }
  val type
    get() = intent.type
      ?: if (ContentProvidersHelper.isMediaAudioUri(data)) {
        "audio/"
      } else {
        ""
      }

  val shouldHandleIntent: Boolean
    get() = !(handled || isActionMain() || isActionEmpty() || (isActionView() && isDataEmpty()))

  val isCancellable = this.cancellable // TODO

  fun isActionEmpty() = action.isNullOrBlank()
  fun isActionMain() = action == Intent.ACTION_MAIN
  fun isActionView() = action == Intent.ACTION_VIEW
  fun isDataEmpty() = data.isBlank()
  fun isSchemeContent() = scheme == ContentProvidersHelper.contentScheme
  fun isTypeAudio() = type.startsWith(typeAudioPrefix)

  fun markHandled() {
    this.handled = true
  }

  override fun toString(): String {
    return "IntentWrapper, intent: $intent" +
      "\n action: $action" +
      "\n data: $data" +
      "\n scheme: $scheme" +
      "\n type: $type"
  }

  companion object {
    @JvmStatic
    val EMPTY = fromIntent(Intent())

		@JvmStatic
    val typeAudioPrefix = "audio/"

    @JvmStatic
    fun fromIntent(intent: Intent) = IntentWrapper(intent)
  }
}
