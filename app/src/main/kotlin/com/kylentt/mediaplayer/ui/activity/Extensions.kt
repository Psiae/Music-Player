package com.kylentt.mediaplayer.ui.activity

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity

object ActivityExtension {
  fun ComponentActivity.disableWindowFitSystemDecor() = WindowCompat
    .setDecorFitsSystemWindows(window, false)
}

object IntentExtension {
  fun Intent.isActionView() = action == Intent.ACTION_VIEW

  fun Intent.appendMainActivityClass(context: Context) =
    MainActivity.Companion.Defaults.appendClass(context, this)
  fun Intent.appendMainActivityAction() =
    MainActivity.Companion.Defaults.appendAction(this)
}

object CollectionExtension {
  fun <T> MutableList<T>.forEachClear(lock: Any = this, each: (T) -> Unit) {
    synchronized(lock) {
      forEach { each(it) }
      clear()
    }
  }

  fun <T> MutableList< () -> T >.syncEachClear(lock: Any = this) {
    synchronized(lock) {
      forEach { it() }
      clear()
    }
  }
}



