package com.kylentt.musicplayer.medialib.internal.android

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.core.content.getSystemService

class AndroidContext internal constructor(context: Context) : ContextWrapper(context) {
	val application = applicationContext as Application
	val audioManager: AudioManager = application.getSystemService()!!
}
