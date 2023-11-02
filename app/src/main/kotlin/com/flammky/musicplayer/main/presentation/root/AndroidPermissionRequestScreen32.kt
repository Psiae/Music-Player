package com.flammky.musicplayer.main.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.main.ui.compose.entry.EntryPermissionPager
import com.flammky.musicplayer.main.ui.compose.entry.PermissionPageItem
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.ui.platform.LocalContext as LocalAndroidContext

@Composable
fun AndroidPermissionRequestScreenImpl32(
	onCompleted: () -> Unit
) {
	// TODO: new impl
	val ctx = LocalAndroidContext.current
	EntryPermissionPager(
		pageItems = remember {
			persistentListOf(
				PermissionPageItem(
					permission = AndroidPermission.Other(android.Manifest.permission.READ_EXTERNAL_STORAGE),
					resId = com.flammky.musicplayer.R.drawable.folder_search_base_256_blu_glass,
					optional = false,
					title = "READ EXTERNAL STORAGE PERMISSION"
				),
				PermissionPageItem(
					permission = AndroidPermission.Other(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
					resId = com.flammky.musicplayer.R.drawable.folder_write_base_256,
					optional = true,
					title = "WRITE EXTERNAL STORAGE PERMISSION"
				)
			)
		},
		contextHelper = remember(ctx) {
			ContextHelper(ctx)
		},
		onGranted = onCompleted
	)
}
