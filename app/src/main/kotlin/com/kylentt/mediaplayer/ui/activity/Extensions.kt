package com.kylentt.mediaplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity

object ActivityExtension {

  /**
   * @receiver [ComponentActivity] [Window] to Ignore System Windows,
   * e.g: Status bar and Bottom Navigation Bar
   */

  @JvmStatic
  fun ComponentActivity.disableWindowFitSystem() {
    requireNotNull(window) {
      "Activity Window cannot be null," +
        "make sure super.onCreate() is called before calling this method"
    }
    WindowCompat.setDecorFitsSystemWindows(window, false)
  }
}

object IntentExtension {

  @JvmStatic
  fun Intent.isActionView() = action == Intent.ACTION_VIEW

  @JvmStatic
  fun Intent.appendMainActivityClass(context: Context) =
    MainActivity.Companion.Defaults.appendClass(context, this)

  @JvmStatic
  fun Intent.appendMainActivityAction() =
    MainActivity.Companion.Defaults.appendAction(this)
}

object CollectionExtension {

  @JvmStatic
  fun <T> MutableList<T>.forEachClear(lock: Any = this, each: (T) -> Unit) {
    synchronized(lock) {
      forEach { each(it) }
      clear()
    }
  }

  @JvmStatic
  fun <T> MutableList< () -> T >.forEachClear(lock: Any = this) {
    synchronized(lock) {
      forEach { it() }
      clear()
    }
  }
}



