package com.kylentt.musicplayer.ui.main.compose

import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityRoot
import com.kylentt.musicplayer.common.kotlin.mutablecollection.doEachClear
import com.kylentt.musicplayer.common.kotlin.mutablecollection.forEachClear
import com.kylentt.musicplayer.ui.main.MainActivity
import com.kylentt.musicplayer.ui.main.compose.theme.MainMaterial3Theme

@Composable
fun MainActivity.MainContent() {
	MainTheme {
		Surface {
			MainEntry {
				with(viewModels<MainViewModel>().value) {
					pendingStorageGranted.doEachClear()
					pendingStorageIntent.forEachClear { intent ->
						viewModels<MediaViewModel>().value.handleMediaIntent(intent)
					}
					MainActivityRoot(appSettings.collectAsState().value)
				}
			}
		}
	}
}

@Composable
private fun MainTheme(content: @Composable () -> Unit) {
	MainMaterial3Theme(content = content)
}
