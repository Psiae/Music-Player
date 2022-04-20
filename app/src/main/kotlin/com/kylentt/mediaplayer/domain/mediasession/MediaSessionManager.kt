package com.kylentt.mediaplayer.domain.mediasession

import android.app.Application
import android.content.Context
import com.kylentt.mediaplayer.app.AppScope
import javax.inject.Singleton

@Singleton
class MediaSessionManager(
  private val appScope: AppScope,
  private val baseContext: Context
) {

  init {
      check(baseContext is Application)
  }
}
