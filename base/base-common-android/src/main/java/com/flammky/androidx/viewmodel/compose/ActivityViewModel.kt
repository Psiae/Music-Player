package com.flammky.androidx.viewmodel.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.androidx.content.context.findActivity


/**
 * retrieve or create the specified ViewModel [VM] in Local Activity within current Composition
 *
 * @throws [IllegalArgumentException] if no Local [Activity] that implements [ViewModelStoreOwner]
 * is found within current Composition Context
 *
 * @return [ViewModel] owned by local [Activity]
 */
@Composable
@Throws(IllegalArgumentException::class)
public inline fun <reified VM: ViewModel> activityViewModel(): VM {

	val activity: Activity? = LocalContext.current.findActivity()

	requireNotNull(activity) {
		"activityViewModel must be called from within Activity composition Context"
	}

	return activityViewModel(activity)
}

/**
 * retrieve or create the specified ViewModel [VM] from given [activity]
 *
 * @throws [IllegalArgumentException] if [Activity] does Not implement [ViewModelStoreOwner]
 *
 * @return [ViewModel] owned by given [activity]
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
