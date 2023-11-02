package com.flammky.musicplayer.android.activity

import android.content.Context
import android.content.Intent

interface RequireLauncher {

	abstract fun launchWithIntent(
		launcherContext: Context,
		intent: Intent
	): Boolean
}
