package com.flammky.musicplayer.main.ui.compose.entry

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.ui.main.compose.entry.NoInline

@Composable
internal fun EntryGuard(
	content: @Composable () -> Unit
) {
	if (guard().value == true) {
		content()
	}
}

@Composable
private fun guard(): State<Boolean?> {
	val allowState = remember { mutableStateOf<Boolean?>(null) }
	val vm: MainViewModel = viewModel()

	val authGuard = authGuard(
		allowShowContentState = remember { mutableStateOf(true) }
	)

	NoInline {
		vm.authGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	val permGuard = permGuard(
		allowShowContentState = remember { derivedStateOf { authGuard.value == true } }
	)

	NoInline {
		vm.permGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	val auth = authGuard.value
	val perm = permGuard.value
	allowState.value = if (auth == null || perm == null ) null else auth && perm

	NoInline {
		vm.entryGuardWaiter
			.apply {
				// check for size, because `clear` will count as modification regardless of content
				if (!isEmpty()) {
					forEach(::invoke)
					clear()
				}
			}
	}

	return allowState
}

private fun invoke(block: () -> Unit) = block.invoke()
