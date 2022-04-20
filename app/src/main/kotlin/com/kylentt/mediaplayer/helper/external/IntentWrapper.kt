package com.kylentt.mediaplayer.helper.external

import android.content.ContentResolver
import android.content.Intent

data class IntentWrapper(
  private val intent: Intent,
  private var cancelable: Boolean = false,
  private var handled: Boolean = false
) {

  val action
    get() = intent.action ?: ""
  val data
    get() = intent.data?.toString() ?: ""
  val scheme
    get() = intent.scheme ?: ""
  val type
    get() = intent.type ?: ""

  val shouldHandleIntent: Boolean
    get() = !(handled || isActionEmpty() || (isActionView() && isDataEmpty()))

  fun isActionEmpty() = action.isEmpty()
  fun isActionMain() = action == Intent.ACTION_MAIN
  fun isActionView() = action == Intent.ACTION_VIEW
  fun isDataEmpty() = data.isEmpty()
  fun isSchemeContent() = scheme == ContentResolver.SCHEME_CONTENT
  fun isTypeAudio() = type.startsWith("audio/")

  companion object {
    fun fromIntent(intent: Intent) = IntentWrapper(intent)
  }
}
