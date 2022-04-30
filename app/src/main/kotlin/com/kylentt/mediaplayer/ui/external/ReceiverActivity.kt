package com.kylentt.mediaplayer.ui.external

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity

class ReceiverActivity: ComponentActivity() {

  /**
   * MainActivity can misbehave when the launcher Intent is not its Default,
   * e.g: re-launched with action.VIEW after process-death causes it to re-handle the Intent.
   * There is better workaround with stateHandle but I would rather just keep the same action.MAIN.
   * */

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent != null) {
      launchMainActivity(intent)
      finishAfterTransition()
    }
  }

  private fun launchMainActivity(withIntent: Intent) = with(MainActivity.Companion) {
    startActivity( launcher = this@ReceiverActivity, intent = withIntent)
    if (!wasLaunched) {
      overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
    }
  }
}
