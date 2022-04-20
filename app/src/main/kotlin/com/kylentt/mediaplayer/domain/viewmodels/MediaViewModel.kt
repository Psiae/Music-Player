package com.kylentt.mediaplayer.domain.viewmodels

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.external.MediaIntentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
  /*private val mediaIntentHandler: MediaIntentHandler,*/
  /*private val mediaSessionManager: MediaSessionManager*/
) : ViewModel() {

  @MainThread
  fun connectService() {
    verifyMainThread()
  }

  fun handleMediaIntent(intent: IntentWrapper) {
    viewModelScope.launch {
      /*mediaIntentHandler.handleMediaIntent(intent)*/
    }
  }

}
