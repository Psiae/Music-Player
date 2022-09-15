package com.flammky.android.x.lifecycle.viewmodel.compose

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * @return [ViewModel] owned by its current [Activity] if implements [ViewModelStoreOwner]
 * @throws [IllegalArgumentException] if not attached to any [Activity] that implements [ViewModelStoreOwner]
 */
@Composable
@Throws(IllegalArgumentException::class)
public inline fun <reified VM: ViewModel> activityViewModel(): VM {
	val context = LocalContext.current

	require(context is Activity) {
		"LocalContext is not an Activity"
	}

	return activityViewModel(context)
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



