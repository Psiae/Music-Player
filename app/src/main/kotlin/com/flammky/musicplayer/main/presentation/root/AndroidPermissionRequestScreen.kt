package com.flammky.musicplayer.main.presentation.root

import androidx.compose.runtime.Composable
import dev.dexsr.klio.core.sdk.AndroidAPI

@Composable
fun AndroidPermissionRequestScreen(
	onCompleted: () -> Unit
) {
	when (AndroidAPI.buildcode.CODE_INT) {
		in 1.. 32 -> {
			AndroidPermissionRequestScreenImpl32(onCompleted)
		}
		33 -> {
			AndroidPermissionRequestScreenImpl33(onCompleted)
		}
		else -> {
			AndroidPermissionRequestScreenNoImpl(
				onCompleted = {
					// Never Complete
				}
			)
		}
	}
}
