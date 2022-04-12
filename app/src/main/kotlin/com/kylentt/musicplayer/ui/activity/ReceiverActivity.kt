package com.kylentt.musicplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.ui.activity.musicactivity.Hidden.fixIntent
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReceiverActivity : ComponentActivity() {

  private var keepSplashScreen = true
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    installSplashScreen().setKeepOnScreenCondition { keepSplashScreen }
    disableFitWindow()
    return run {
      requireNotNull(intent)
      fixIntent(intent) {
        startMainActivity(intent) { finish() }
      }
    }
  }

  private inline fun startMainActivity(
    withIntent: Intent,
    crossinline afterActive: () -> Unit
  ) {
    with(Intent()) {
      setClass(this@ReceiverActivity, MainActivity::class.java)
      action = Intent.ACTION_MAIN
      startActivity(this)
    }
    with(withIntent) {
      setClass(this@ReceiverActivity, MainActivity::class.java)
      startActivity(this)
      if (!MainActivity.isAlive) {
        overridePendingTransition(0, 0)
      }
    }
    lifecycleScope.launch {
      while (!MainActivity.isAlive) {
        delay(25)
      }
      keepSplashScreen = false
      afterActive()
    }
  }

  companion object {
    private var firstLaunch = true
  }
}
