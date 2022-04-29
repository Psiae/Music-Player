package com.kylentt.disposed.musicplayer.ui.activity.helper

import android.content.Intent

data class IntentWrapper(
  private val intent: Intent,
  private var handled: Boolean = false
) {

  private val handledState = listOf(
    handled, intent.action.isNullOrEmpty()
  )

  val isHandled get() = handled

  fun getAction() = this.intent.action
  fun getData() = this.intent.data
  fun getIntent() = this.intent
  fun getScheme() = this.intent.scheme
  fun getType() = this.intent.type


  fun isActionView() = getAction() == Intent.ACTION_VIEW

  fun markHandled() = synchronized(this) {
    handled = true
  }

  fun shouldHandleIntent() = !handledState.any { it }

  override fun toString(): String {
    return "${super.toString()}: " +
      "\naction = ${getAction()}" +
      "\ndata = ${getData()}" +
      "\ntype = ${getType()}"
  }

  companion object {
    val EMPTY = IntentWrapper(Intent())

    fun Intent.wrap() = IntentWrapper(this)
    fun IntentWrapper.isEmpty() = this == EMPTY
  }
}
