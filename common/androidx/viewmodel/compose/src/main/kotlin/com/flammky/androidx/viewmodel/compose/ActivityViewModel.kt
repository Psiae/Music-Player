package com.flammky.androidx.viewmodel.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel


/**
 * @return [ViewModel] owned by its current [Activity] if implements [ViewModelStoreOwner]
 * @throws [IllegalArgumentException] if not attached to any [Activity] that implements [ViewModelStoreOwner]
 */
@Composable
@Throws(IllegalArgumentException::class)
public inline fun <reified VM: ViewModel> activityViewModel(): VM {
	val activity = LocalContext.current.takeIf { it is Activity }
		?: LocalLifecycleOwner.current.takeIf { it is Activity }
		?: LocalViewModelStoreOwner.current.takeIf { it is Activity }

	requireNotNull(activity) {
		"Could Not Find Local Activity"
	}

	return activityViewModel(activity as Activity)
}

/**
 * @return [ViewModel] owned by its current [Activity] if implements [ViewModelStoreOwner]
 * @throws [IllegalArgumentException] if not attached to any [Activity] that implements [ViewModelStoreOwner]
 */
@Composable
@Throws(IllegalArgumentException::class)
public inline fun <reified VM: ViewModel> activityViewModel(activity: Activity): VM {

	require(activity is ViewModelStoreOwner) {
		"Activity does Not implement ViewModelStoreOwner"
	}

	// Delegate our call to androidx library
	return viewModel(activity)
}



