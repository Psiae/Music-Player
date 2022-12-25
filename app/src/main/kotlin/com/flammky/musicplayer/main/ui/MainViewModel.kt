package com.flammky.musicplayer.main.ui

import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.main.ext.IntentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
	private val presenter: MainPresenter
) : ViewModel(), MainPresenter.ViewModel {

	val entryCheckWaiter = mutableStateListOf<() -> Unit>()

	init {
		presenter.initialize(this)
	}

	val intentHandler: IntentHandler = presenter.intentHandler


	override fun loadSaver(): Bundle? = /* TODO: SavedStateHandle */ null

	override fun onCleared() {
		presenter.dispose()
	}
}
