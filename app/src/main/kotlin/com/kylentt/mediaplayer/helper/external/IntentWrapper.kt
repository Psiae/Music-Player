package com.kylentt.mediaplayer.helper.external

import android.content.ContentResolver
import android.content.Intent

data class IntentWrapper @JvmOverloads constructor(
  private val intent: Intent,
  private val cancellable: Boolean = true,
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

  @JvmField
  val isCancellable = this.cancellable // TODO

  fun isActionEmpty() = action.isEmpty()
  fun isActionMain() = action == Intent.ACTION_MAIN
  fun isActionView() = action == Intent.ACTION_VIEW
  fun isDataEmpty() = data.isEmpty()
  fun isSchemeContent() = scheme == ContentResolver.SCHEME_CONTENT
  fun isTypeAudio() = type.startsWith("audio/")

  fun markHandled() {
    this.handled = true
  }

  companion object {
    @JvmStatic val EMPTY = fromIntent(Intent())
    @JvmStatic fun fromIntent(intent: Intent) = IntentWrapper(intent)
  }
}
