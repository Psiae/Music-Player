package com.flammky.musicplayer.root

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.readable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.main.ext.IntentReceiver
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.main.ui.compose.entry.EntryPermissionPager
import com.flammky.musicplayer.main.ui.compose.nav.RootNavigation
import kotlinx.coroutines.launch

@Composable
fun BoxScope.RootEntry(
	viewModel: MainViewModel,
) {
	rememberRootEntryGuardState<MainViewModel>(
		handle = rememberUpdatedState(viewModel).value,
		viewModelStoreOwner = LocalViewModelStoreOwner.current!!,
		intentReceiver = { it.intentHandler },
		onFirstEntryGuardLayout = {
			snapshotFlow(equality = { _, _ -> false }) {
				it.firstEntryGuardWaiter.apply {
					firstStateRecord.readable(this)
				}
			}.collect {
				it.apply {
					if (!isEmpty()) {
						forEach { it() }
						clear()
					}
				}
			}
		},
		onAuthGuarded = {
			snapshotFlow(equality = { _, _ -> false }) {
				it.authGuardWaiter.apply {
					firstStateRecord.readable(this)
				}
			}.collect {
				it.apply {
					if (!isEmpty()) {
						forEach { it() }
						clear()
					}
				}
			}
		},
		onRuntimePermissionGuarded = {
			snapshotFlow(equality = { _, _ -> false }) {
				it.permGuardWaiter.apply {
					firstStateRecord.readable(this)
				}
			}.collect {
				it.apply {
					if (!isEmpty()) {
						forEach { it() }
						clear()
					}
				}
			}
		},
		onAllGuarded = {
			snapshotFlow(equality = { _, _ -> false }) {
				it.intentEntryGuardWaiter.apply {
					firstStateRecord.readable(this)
				}
			}.collect {
				it.apply {
					if (!isEmpty()) {
						forEach { it() }
						clear()
					}
				}
			}
		}
	).run {
		GuardLayout { user ->
			RootNavigation(user = user)
		}
	}
}

@Composable
private fun <HANDLE> rememberRootEntryGuardState(
	handle: HANDLE,
	viewModelStoreOwner: ViewModelStoreOwner,
	intentReceiver: (handle: HANDLE) -> IntentReceiver,
	onFirstEntryGuardLayout: suspend (handle: HANDLE) -> Unit,
	onAuthGuarded: suspend (handle: HANDLE) -> Unit,
	onRuntimePermissionGuarded: suspend (handle: HANDLE) -> Unit,
	onAllGuarded: suspend (handle: HANDLE) -> Unit,
): RootEntryGuardState<HANDLE> {
	return remember(handle, viewModelStoreOwner) {
		RootEntryGuardState(
			handle,
			viewModelStoreOwner,
			intentReceiver(handle),
			onFirstEntryGuardLayout,
			onAuthGuarded,
			onRuntimePermissionGuarded,
			onAllGuarded,
		)
	}
}


private class RootEntryGuardState <HANDLE> (
	val handle: HANDLE,
	private val viewModelStoreOwner: ViewModelStoreOwner,
	private val intentReceiver: IntentReceiver,
	private val onFirstEntryGuardLayout: suspend (handle: HANDLE) -> Unit,
	private val onAuthGuarded: suspend (handle: HANDLE) -> Unit,
	private val onRuntimePermissionGuarded: suspend (handle: HANDLE) -> Unit,
	private val onAllGuarded: suspend (handle: HANDLE) -> Unit,
) {
	private val authAllowState = mutableStateOf<Boolean>(false)
	private val permAllowState = mutableStateOf<Boolean>(false)

	@Composable
	fun BoxScope.GuardLayout(
		// should provide scope instead
		onAllowContent: @Composable (User) -> Unit
	) {
		AuthGuard { user ->
			PermGuard {
				onAllowContent(user)
			}
		}
	}

	@Composable
	private fun AuthGuard(
		onAllowContent: @Composable (User) -> Unit
	) {
		val vm = hiltViewModel<AuthGuardViewModel>(viewModelStoreOwner)
		var showLoading by remember { mutableStateOf(false) }

		if (!authAllowState.value) {
			val interceptor = remember {
				intentReceiver.createInterceptor()
					.apply {
						setFilter { target ->
							intentReceiver.intentRequireAuthPermission(target.cloneActual())
						}
						start()
					}
			}
			DisposableEffect(
				// wait to be removed from composition tree
				key1 = null
			) {
				onDispose {
					if (!authAllowState.value) {
						// should we tho ?
						interceptor.dropAllInterceptedIntent()
					}
					interceptor.dispose()
				}
			}
		}

		if (showLoading) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Theme.absoluteBackgroundContentColorAsState().value.copy(alpha = 0.94f))
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
						onClick = {}
					)
			) {
				// we should probably allow a loading queue
				CircularProgressIndicator(
					modifier = Modifier.align(Alignment.Center),
					color = MaterialTheme.colorScheme.primary,
					strokeWidth = 3.dp
				)
			}
		}

		if (authAllowState.value) {
			vm.currentUser?.let { onAllowContent(it) }
		}

		LaunchedEffect(
			key1 = null,
			block = {
				launch {
					onFirstEntryGuardLayout(handle)
				}
				launch {
					onAuthGuarded(handle)
				}
				if (vm.currentUser == null) {
					val remember = vm.loginRememberAsync()
					showLoading = true
					if (remember.await() == null) {
						vm.loginLocalAsync().await()
					}
				}
				showLoading = false
				vm.currentUserFlow.collect {
					authAllowState.value = it != null
				}
			}
		)
	}

	@Composable
	private fun PermGuard(
		onAllowContent: @Composable () -> Unit
	) {
		val compositionActivity = LocalContext.current as Activity
		// I think it would be better to ask for saver object instead
		val vm = hiltViewModel<RuntimePermissionGuardViewModel>(viewModelStoreOwner)
		val contextHelper = rememberLocalContextHelper()
		var persistPager by remember {
			mutableStateOf(vm.removeSaved("persistPager") as? Boolean ?: false)
		}
		val permissionGrantedState = remember {
			mutableStateOf(
				contextHelper.permissions.common.hasReadExternalStorage ||
				contextHelper.permissions.common.hasWriteExternalStorage
			)
		}

		permAllowState.value = remember {
			derivedStateOf { !persistPager && permissionGrantedState.value }
		}.value

		val intentInterceptor = remember {
			val interceptor = intentReceiver.createInterceptor()
			interceptor
				.apply {
					setFilter { target ->
						if (permAllowState.value) {
							return@setFilter false
						}
						intentReceiver.intentRequireAndroidPermission(
							intent = target.cloneActual(),
							permission = AndroidPermission.Read_External_Storage
						)
					}
					start()
				}
		}

		DisposableEffect(key1 = Unit, effect = {
			onDispose { intentInterceptor.dispose() }
		})

		DisposableEffect(
			key1 = permAllowState.value
		) {
			onDispose {
				if (permAllowState.value) {
					intentInterceptor
						.apply {
							dispatchAllInterceptedIntent()
						}
					persistPager = false
					vm.removeSaved("persistPager")
				} else if (compositionActivity.isChangingConfigurations) {
					intentInterceptor
						.apply {
							pendingAllInterceptedIntent()
							dispose()
						}
				} else {
					intentInterceptor
						.apply {
							dropAllInterceptedIntent()
							dispose()
						}
				}
			}
		}

		if (!permAllowState.value) {
			persistPager = true
			vm.save("persistPager", true)
			Box(modifier = Modifier
				.fillMaxSize()
				.background(Theme.backgroundColorAsState().value)) {
				EntryPermissionPager(
					contextHelper = contextHelper,
					onGranted = {
						permissionGrantedState.value = true
						persistPager = false
						vm.removeSaved("persistPager")
					}
				)
			}
		} else {
			onAllowContent()
		}
		LaunchedEffect(
			key1 = intentInterceptor,
			block = {
				launch {
					onRuntimePermissionGuarded(handle)
				}
				launch {
					onAllGuarded(handle)
				}
			}
		)
	}
}
