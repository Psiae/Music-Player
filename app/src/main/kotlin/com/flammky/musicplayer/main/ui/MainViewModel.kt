package com.flammky.musicplayer.main.ui

import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.main.ext.IntentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val presenter: MainPresenter
) : ViewModel(), MainPresenter.ViewModel {

	private val _intentRequestErrorMessageChannel = Channel<String>(capacity = Channel.CONFLATED)
	private val _playbackErrorMessageChannel = Channel<String>(capacity = Channel.CONFLATED)

	val intentRequestErrorMessageChannel: ReceiveChannel<String> = _intentRequestErrorMessageChannel
	val playbackErrorMessageChannel: ReceiveChannel<String> = _playbackErrorMessageChannel

	val entryCheckWaiter = mutableStateListOf<() -> Unit>()

	override fun showIntentRequestErrorMessage(message: String) {
		_intentRequestErrorMessageChannel.trySend(message)
	}

	override fun showPlaybackErrorMessage(message: String) {
		_playbackErrorMessageChannel.trySend(message)
	}

	init {
		presenter.initialize(this)
	}

	fun rememberAuthAsync(): Deferred<User?> {
		return presenter.auth.rememberAuthAsync(viewModelScope.coroutineContext)
	}



	val currentUserFlow: Flow<User?> = presenter.auth.currentUserFlow

	val currentUser: User?
		get() = presenter.auth.currentUser

	val intentHandler: IntentHandler = presenter.intentHandler

	fun loginLocalAsync() = presenter.auth.loginLocalAsync(viewModelScope.coroutineContext)

	override fun loadSaver(): Bundle? = /* TODO: SavedStateHandle */ null

	override fun onCleared() {
		presenter.dispose()
	}
}
