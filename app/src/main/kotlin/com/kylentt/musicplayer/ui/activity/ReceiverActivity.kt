package com.kylentt.musicplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kylentt.mediaplayer.R
import com.kylentt.musicplayer.ui.activity.musicactivity.IntentValidator.fixIntent
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity

class ReceiverActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireNotNull(intent)
    fixIntent {
      startMainActivity(intent) { finish() }
    }
  }

  // Todo : do something after postCreate() or onResume()
  private inline fun startMainActivity(
    withIntent: Intent,
    afterActive: () -> Unit
  ) {
    MainActivity.startActivity(this, withIntent)
    if (isFirstLaunch()) {
      overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
    }
    afterActive()
  }

  companion object {
    private var first = true
    fun isFirstLaunch(): Boolean {
      return if (first) {
        first = !first
        !first
      } else {
        first
      }
    }

  }
}
