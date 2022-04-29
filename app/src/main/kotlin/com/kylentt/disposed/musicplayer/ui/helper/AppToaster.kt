package com.kylentt.disposed.musicplayer.ui.helper

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.kylentt.disposed.musicplayer.ui.helper.AppToaster.AppToast.Companion.EMPTY
import com.kylentt.disposed.musicplayer.ui.helper.AppToaster.AppToast.Companion.isEmpty
import com.kylentt.disposed.musicplayer.ui.helper.AppToaster.AppToast.Companion.toToastWidget
import kotlinx.coroutines.delay

internal class AppToaster(
  private val base: Application
) {
  private var currentToast = EMPTY

  suspend fun blockIfSameToasting(text: String, short: Boolean) {
    if (!currentToast.isEmpty() && text == currentToast.text) return
    val toast = AppToast(text, short)
    toast.toToastWidget(base)
      .show()
      .also { currentToast = toast }
    if (short) delay(2000) else delay(3500)
    currentToast = EMPTY
  }

  private data class AppToast(
    val text: String,
    val short: Boolean
  ) {
    companion object {
      val EMPTY = AppToast("EMPTY_TOAST", true)
      fun AppToast.isEmpty() = this == EMPTY

      fun AppToast.toToastWidget(base: Context): Toast {
        return Toast.makeText(
          base,
          text,
          if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        )
      }

    }
  }

  companion object {
    private lateinit var instance: AppToaster
    suspend fun blockIfSameToasting(text: String, short: Boolean) =
      instance.blockIfSameToasting(text, short)

    fun provides(base: Application) {
      instance = AppToaster(base)
    }
  }
}
