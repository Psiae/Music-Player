package com.flammky.musicplayer.main.presentation.root

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.R
import dev.dexsr.klio.core.sdk.AndroidAPI
import com.flammky.musicplayer.main.ui.compose.entry.EntryPermissionPager
import com.flammky.musicplayer.main.ui.compose.entry.PermissionPageItem
import kotlinx.collections.immutable.persistentListOf

@Composable
fun AndroidPermissionRequestScreenImpl33(
	onCompleted: () -> Unit
) {
	if (AndroidAPI.buildcode.CODE_INT < 33) {
		return
	}
	// TODO: new impl
	val ctx = LocalContext.current
	EntryPermissionPager(
		pageItems = remember {
			persistentListOf(
				PermissionPageItem(
					permission = AndroidPermission.Other(Manifest.permission.READ_MEDIA_AUDIO),
					resId = R.drawable.folder_search_base_256_blu_glass,
					optional = false,
					title = "READ EXTERNAL STORAGE AUDIO FILES PERMISSION"
				),
				PermissionPageItem(
					permission = AndroidPermission.Other(Manifest.permission.READ_MEDIA_IMAGES),
					resId = R.drawable.folder_write_base_256,
					optional = true,
					title = "READ EXTERNAL STORAGE IMAGE FILES PERMISSION"
				)
			)
		},
		contextHelper = remember(ctx) {
			ContextHelper(ctx)
		},
		onGranted = onCompleted
	)
}
