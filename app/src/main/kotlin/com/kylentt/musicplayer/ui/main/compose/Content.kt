package com.kylentt.musicplayer.ui.main.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityRoot
import com.kylentt.musicplayer.common.kotlin.mutablecollection.doEachClear
import com.kylentt.musicplayer.common.kotlin.mutablecollection.forEachClear
import com.kylentt.musicplayer.ui.main.MainActivity
import com.kylentt.musicplayer.ui.main.compose.local.MainProvider
import com.kylentt.musicplayer.ui.main.compose.theme.MainMaterial3Theme

@Composable
fun MainActivity.MainContent() {
	MainTheme {
		MainEntry {
			MainProvider.ProvideViewModels {
				val mainViewModel = MainProvider.mainViewModel
				val mediaViewModel = MainProvider.mediaViewModel
				with(mainViewModel) {
					pendingStorageGranted.doEachClear()
					pendingStorageIntent.forEachClear(mediaViewModel::handleMediaIntent)
					MainActivityRoot(appSettings.collectAsState().value)
				}
			}
		}
	}
}

@Composable
private fun MainTheme(content: @Composable () -> Unit) {
	MainMaterial3Theme {
		MainSurface {
			content()
		}
	}
}

@Composable
private fun MainSurface(content: @Composable () -> Unit) {
	Surface(
		modifier = Modifier.fillMaxSize(),
		color = MaterialTheme.colorScheme.surface,
		content = content
	)
}

