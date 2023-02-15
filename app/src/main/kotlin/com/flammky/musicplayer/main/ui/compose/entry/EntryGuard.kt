package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.base.compose.NoInline
import com.flammky.musicplayer.main.ui.MainViewModel

@Composable
internal fun EntryGuard(
	content: @Composable () -> Unit
) {
	if (entryGuardState().value == true) {
		content()
	}
}

@Composable
private fun entryGuardState(): State<Boolean?> {
	val entryVM: EntryGuardViewModel = viewModel()
	val mainVM: MainViewModel = viewModel()
	val authAllowState = entryVM.authGuardAllow.collectAsState()
	val permAllowState = entryVM.permGuardAllow.collectAsState()

	AuthGuard(
		mainVM = mainVM,
		entryVM = entryVM,
		allowShowContentState = remember { mutableStateOf(true) }
	)

	NoInline {
		mainVM.firstEntryGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	PermGuard(
		mainVM = mainVM,
		entryVM = entryVM,
		allowShowContentState = remember { derivedStateOf { authAllowState.value == true } }
	)

	NoInline {
		mainVM.intentEntryGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	return remember {
		derivedStateOf {
			val auth = authAllowState.value
			val perm = permAllowState.value
			if (auth == null || perm == null ) null else auth && perm
		}
	}
}

private inline fun invoke(block: () -> Unit) = block.invoke()
