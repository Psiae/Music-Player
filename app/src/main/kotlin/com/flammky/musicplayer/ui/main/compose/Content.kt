package com.flammky.musicplayer.ui.main.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.dump.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose.MainActivityRoot
import com.flammky.musicplayer.main.MainActivity
import com.flammky.musicplayer.main.ui.compose.entry.EntryGuard
import com.flammky.musicplayer.main.ui.compose.nav.RootNavigation

@Composable
internal fun MainActivity.ComposeContent() = MainSurface {
	EntryGuard {
		RootNavigation()
		val mainViewModel: MainViewModel = viewModel()
		MainActivityRoot(mainViewModel.appSettings.collectAsState().value)
	}
}

@Composable
private fun MainSurface(content: @Composable () -> Unit) {
	Surface(
		modifier = Modifier.fillMaxSize(),
		content = content
	)
}

