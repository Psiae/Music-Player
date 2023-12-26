package dev.dexsr.klio.library.device

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.isTiramisu
import com.flammky.musicplayer.library.presentation.entry.PermGuard

object PlatformLibrary

@Composable
/* actual */ fun PlatformLibrary.hasQueryAudioFilesPermission(): Boolean {
	val contextHelper = rememberLocalContextHelper()
	val state = remember {
		mutableStateOf(
			if (AndroidAPI.isTiramisu()) {
				contextHelper.permissions.hasPermission(android.Manifest.permission.READ_MEDIA_AUDIO)
			} else {
				contextHelper.permissions.common.hasReadExternalStorage ||
					contextHelper.permissions.common.hasWriteExternalStorage
			}
		)
	}
	PermGuard(onPermChanged = {
		it?.let { state.value = it }
	})
	return state.value
}
