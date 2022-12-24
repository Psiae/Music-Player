package com.flammky.musicplayer.activity

import android.content.Context
import android.content.Intent

abstract class ActivityCompanion() {

	// abstract val instanceCount: Int

	abstract fun launchWithIntent(
		launcherContext: Context,
		intent: Intent
	): Boolean
}
