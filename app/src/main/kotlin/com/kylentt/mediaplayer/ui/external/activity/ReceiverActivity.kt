package com.kylentt.mediaplayer.ui.external.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.ui.activity.IntentExtension.isActionView
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity
import timber.log.Timber

/**
 * [Activity] Class that filter incoming [Intent] and launch its handler [Activity]
 * @see [MainActivity]
 */

class ReceiverActivity: ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    when {
      intent == null -> Unit
      intent.isActionView() -> launchMainActivity(intent)
    }
    finishAfterTransition()
  }

  private fun launchMainActivity(withIntent: Intent) {
    MainActivity.startActivity(launcher = this@ReceiverActivity, intent = withIntent)
    if (!MainActivity.wasLaunched) {
      overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
      Timber.d("ReceiverActivity pendingTransition override")
    }
  }
}
