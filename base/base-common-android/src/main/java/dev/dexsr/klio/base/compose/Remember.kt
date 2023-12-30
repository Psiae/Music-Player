package dev.dexsr.klio.base.compose

import androidx.compose.runtime.*
import dev.dexsr.klio.base.kt.cast

@Composable
inline fun <T, R> T.rememberSelf(
    vararg keys: Any,
    crossinline init: @DisallowComposableCalls T.() -> R
) = remember(this, *keys) { init() }

@Composable
inline fun <T, R> T.runRemember(
    vararg keys: Any,
    crossinline init: @DisallowComposableCalls T.() -> R
) = remember(keys) { init() }

@Composable
inline fun <T, R> T.runRemember(
    crossinline init: @DisallowComposableCalls T.() -> R
) = remember() { init() }


@Composable
fun <T> rememberUpdatedStateWithCustomEquality(
	key: Any?,
	equality: (old: Any?, new: Any?) -> Boolean,
	calculation: @DisallowComposableCalls () -> T
): State<T> {
	return remember {
		object {
			var latestKey: Any? = RememberKtObj
			val state = mutableStateOf<T>(calculation())
		}
	}.apply {
		if (latestKey == RememberKtObj) {
			latestKey = key
		} else if (!equality.invoke(latestKey, key)) {
			state.value = calculation()
		}
	}.state
}

@Composable
fun <T> rememberUpdatedStateWithKey(
	key: Any?,
	value: T
): State<T> {
	return remember {
		object {
			var latestKey: Any? = RememberKtObj
			val state = mutableStateOf<T>(value)
		}
	}.apply {
		if (latestKey == RememberKtObj) {
			latestKey = key
		} else if (latestKey != key) {
			state.value = value
		}
	}.state
}

@Composable
inline fun <K, reified V> rememberWithCustomEquality(
	key: K,
	keyEquality: (old: K, new: K) -> Boolean,
	crossinline init: @DisallowComposableCalls () -> V
): V {
	return remember {
		object {
			var _key: Any? = RememberKtObj
			var _value: Any? = init()
		}
	}.apply {
		if (_value == RememberKtObj) {
			_key = key
		} else if (!keyEquality.invoke(_key as K, key)) {
			_key = key
			_value = init()
		}
	}._value.cast()
}

object RememberKtObj
