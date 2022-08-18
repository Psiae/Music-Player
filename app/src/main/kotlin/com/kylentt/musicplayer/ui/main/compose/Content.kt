package com.kylentt.musicplayer.ui.main.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityRoot
import com.kylentt.musicplayer.ui.main.MainActivity
import com.kylentt.musicplayer.ui.main.compose.entry.MainEntry
import com.kylentt.musicplayer.ui.main.compose.theme.MainMaterial3Theme

@Composable
fun MainActivity.ComposeContent() {
	MainTheme() {
		MainEntry {
			val mainViewModel: MainViewModel = viewModel()
			val mediaViewModel: MediaViewModel = viewModel()
			mainViewModel.readPermissionGranted()
			mediaViewModel.readPermissionGranted()
			MainActivityRoot(mainViewModel.appSettings.collectAsState().value)
		}
	}
}

@Composable
private fun MainTheme(
	content: @Composable () -> Unit
) = MainMaterial3Theme { MainSurface(content = content) }

@Composable
private fun MainSurface(content: @Composable () -> Unit) {
	Surface(
		modifier = Modifier.fillMaxSize(),
		content = content
	)
}

