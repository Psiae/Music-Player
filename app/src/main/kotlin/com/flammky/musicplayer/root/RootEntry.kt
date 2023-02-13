package com.flammky.musicplayer.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.flammky.android.manifest.permission.AndroidPermission
import com.flammky.musicplayer.base.compose.rememberLocalContextHelper
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.main.ext.IntentHandler
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.main.ui.compose.entry.EntryPermissionPager
import com.flammky.musicplayer.main.ui.compose.nav.RootNavigation

@Composable
fun BoxScope.RootEntry(
	viewModel: MainViewModel,
) {
	rememberRootEntryGuardState<MainViewModel>(
		handle = rememberUpdatedState(viewModel).value,
		viewModelStoreOwner = LocalViewModelStoreOwner.current!!,
		intentHandler = { it.intentHandler },
		onAllowContent = {
			RootNavigation()
		},
		onFirstEntryGuardLayout = {
			Snapshot.observe {
				it.firstEntryGuardWaiter
					.apply {
						if (!isEmpty()) {
							forEach { it() }
							clear()
						}
					}
			}
		},
		onAuthGuarded = {
			Snapshot.observe {
				it.authGuardWaiter
					.apply {
						if (!isEmpty()) {
							forEach { it() }
							clear()
						}
					}
			}
		},
		onRuntimePermissionGuarded = {
			Snapshot.observe {
				it.permGuardWaiter
					.apply {
						if (!isEmpty()) {
							forEach { it() }
							clear()
						}
					}
			}
		},
	).run {
		GuardLayout()
	}
}

@Composable
private fun <HANDLE> rememberRootEntryGuardState(
	handle: HANDLE,
	viewModelStoreOwner: ViewModelStoreOwner,
	intentHandler: (handle: HANDLE) -> IntentHandler,
	onFirstEntryGuardLayout: (handle: HANDLE) -> Unit,
	onAuthGuarded: (handle: HANDLE) -> Unit,
	onRuntimePermissionGuarded: (handle: HANDLE) -> Unit,
	onAllowContent: @Composable () -> Unit
): RootEntryGuardState<HANDLE> {
	return remember(handle, viewModelStoreOwner) {
		RootEntryGuardState(
			handle,
			viewModelStoreOwner,
			intentHandler(handle),
			onFirstEntryGuardLayout,
			onAuthGuarded,
			onRuntimePermissionGuarded,
			onAllowContent
		)
	}
}


private class RootEntryGuardState <HANDLE> (
	private val handle: HANDLE,
	private val viewModelStoreOwner: ViewModelStoreOwner,
	private val intentHandler: IntentHandler,
	private val onFirstEntryGuardLayout: (handle: HANDLE) -> Unit,
	private val onAuthGuarded: (handle: HANDLE) -> Unit,
	private val onRuntimePermissionGuarded: (handle: HANDLE) -> Unit,
	private val onAllowContent: @Composable () -> Unit
) {
	private val authAllowState = mutableStateOf<Boolean>(false)
	private val permAllowState = mutableStateOf<Boolean>(false)

	@Composable
	fun BoxScope.GuardLayout() {
		AuthGuard {
			PermGuard {
				onAllowContent()
			}
		}
	}

	@Composable
	private fun AuthGuard(
		onAllowContent: @Composable () -> Unit
	) {
		val vm = hiltViewModel<AuthGuardViewModel>(viewModelStoreOwner)
		var showLoading by remember { mutableStateOf(false) }

		if (!authAllowState.value) {
			val interceptor = remember {
				intentHandler.createInterceptor()
					.apply {
						setFilter { target ->
							intentHandler.intentRequireAuthPermission(target.cloneActual())
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
			onAllowContent()
		}

		LaunchedEffect(
			key1 = null,
			block = {
				onFirstEntryGuardLayout(handle)
				onAuthGuarded(handle)
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
		val vm = hiltViewModel<RuntimePermissionGuardViewModel>(viewModelStoreOwner)
		val contextHelper = rememberLocalContextHelper()
		var persistPager by rememberSaveable() { mutableStateOf(false) }
		val permissionGrantedState = remember {
			mutableStateOf(contextHelper.permissions.common.hasReadExternalStorage ||
				contextHelper.permissions.common.hasWriteExternalStorage
			)
		}

		permAllowState.value = remember {
			derivedStateOf { !persistPager && permissionGrantedState.value }
		}.value

		if (!permAllowState.value) {
			persistPager = true
			val intentInterceptor = remember {
				intentHandler.createInterceptor()
					.apply {
						setFilter { target ->
							intentHandler.intentRequireAndroidPermission(
								intent = target.cloneActual(),
								permission = AndroidPermission.Read_External_Storage
							)
						}
						start()
					}
			}
			DisposableEffect(
				// wait to be removed from composition tree
				key1 = null
			) {
				onDispose {
					if (permAllowState.value) {
						intentInterceptor
							.apply {
								dispatchAllInterceptedIntent()
								dispose()
							}
						persistPager = false
					} else {
						// Probably config change, should find a way to retain it
						intentInterceptor
							.apply {
								dropAllInterceptedIntent()
								dispose()
							}
					}
				}
			}
		}
		if (!permAllowState.value) {
			Box(modifier = Modifier
				.fillMaxSize()
				.background(Theme.backgroundColorAsState().value)) {
				EntryPermissionPager(
					contextHelper = contextHelper,
					onGranted = {
						permissionGrantedState.value = true
						persistPager = false
					}
				)
			}
		} else {
			onAllowContent()
		}
		LaunchedEffect(
			key1 = Unit,
			block = {
				onRuntimePermissionGuarded(handle)
			}
		)
	}
}
