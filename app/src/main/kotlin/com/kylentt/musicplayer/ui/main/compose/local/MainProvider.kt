package com.kylentt.musicplayer.ui.main.compose.local

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel

object MainProvider {
	val mainViewModel: MainViewModel
		@ReadOnlyComposable
		@Composable
		get() = LocalMainViewModel.current

	val mediaViewModel: MediaViewModel
		@ReadOnlyComposable
		@Composable
		get() = LocalMediaViewModel.current

	@Composable
	fun ProvideViewModels(
		mainViewModel: MainViewModel = viewModel(),
		mediaViewModel: MediaViewModel = viewModel(),
		content: @Composable () -> Unit
	) {
		CompositionLocalProvider(
			LocalMainViewModel provides mainViewModel,
			LocalMediaViewModel provides mediaViewModel,
			content = content
		)
	}

	private val LocalMainViewModel = staticCompositionLocalOf<MainViewModel> {
		error("no MainViewModel provided")
	}

	private val LocalMediaViewModel = staticCompositionLocalOf<MediaViewModel> {
		error("no MediaViewModel provided")
	}
}




