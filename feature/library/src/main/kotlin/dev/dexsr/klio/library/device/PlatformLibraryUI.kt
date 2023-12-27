package dev.dexsr.klio.library.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasLevel
import com.flammky.musicplayer.core.sdk.tiramisu
import com.flammky.musicplayer.library.presentation.entry.PermGuard

object PlatformLibraryUI

@Composable
/* actual */ fun PlatformLibraryUI.hasQueryAudioFilesPermission(): Boolean {
	val contextHelper = rememberLocalContextHelper()
	val state = remember {
		mutableStateOf(
			if (AndroidAPI.hasLevel(AndroidAPI.tiramisu.BUILD_CODE_INT)) {
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
