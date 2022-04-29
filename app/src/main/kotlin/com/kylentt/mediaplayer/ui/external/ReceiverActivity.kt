package com.kylentt.mediaplayer.ui.external

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.ui.activity.IntentExtension.isActionView
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity

class ReceiverActivity: ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireNotNull(intent)
    if (intent.isActionView()) {
      launchMainActivity()
      finish()
    }
  }

  private fun launchMainActivity() {
    MainActivity.startActivity(launcher = this, intent)
    if (!MainActivity.wasLaunched) {
      overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
    }
  }

}
