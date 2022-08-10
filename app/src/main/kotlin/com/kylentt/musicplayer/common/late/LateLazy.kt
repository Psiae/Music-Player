package com.kylentt.musicplayer.common.late

import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.musicplayer.BuildConfig
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lazy delegate, but with initializer function instead of constructor
 */

class LateLazy<T> @JvmOverloads constructor(lock: Any = Any()) : ReadOnlyProperty<Any?, T> {

	/** Placeholder Object, because the initialized value may be null  */
	private object EMPTY

	/** The lock to synchronize initialization check between threads */
	private val localLock: Any = lock

	/** The value holder. [EMPTY] if not initialized  */
	@Volatile private var localValue: Any? = EMPTY
		set(value) {
			require(field === EMPTY) {
				"Latelazy failed, localValue was $field when trying to set $value"
			}
			field = value
		}

	/** Whether [localValue] is already initialized */
	val isInitialized
		get() = localValue !== EMPTY

	/**
	 * The value.
	 *
	 * @throws ClassCastException if [localValue] is [EMPTY]
	 */
	val value: T
		@Throws(ClassCastException::class)
		get() {
			if (!isInitialized) {
				// The value is not yet initialized, check if its still being initialized.
				// If not then ClassCastException will be thrown
				sync()
			}
			@Suppress("UNCHECKED_CAST")
			return localValue as T
		}

	/**
	 * Try to get the value.
	 *
	 * Null if not yet initialized
	 */
	val valueOrNull: T?
		get() = try { value } catch (e: ClassCastException) { null }

	fun construct(lazyValue: () -> T): T {
		if (!isInitialized) sync {
			// check again if value is already initialized by the time it enters the lock
			if (!isInitialized) {
				// was not initialized, should be safe to invoke
				localValue = lazyValue()
			}
		}
		return value
	}

	override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

	private fun sync(): Unit = sync { }
	private fun <T> sync(block: () -> T): T = synchronized(localLock) { block() }
}

object LateLazySample {

	fun runTestCase(times: Int = 5) {
		if (!BuildConfig.DEBUG) return
		repeat(times) {
			testCase1()
			testCase2()
		}
	}

	private fun testCase1() {
		val initializer = LateLazy<Any>()
		val myObject by initializer

		val any1 = Any()
		val any2 = Any()
		val any3 = Any()

		initializer.construct { any1 }
		initializer.construct { any2 }
		initializer.construct { any3 }

		checkState(myObject === any1)
	}

	private fun testCase2() {
		val initializer = LateLazy<String>()
		val myObject by initializer

		val jobs = mutableListOf<String>()

		val scope = CoroutineScope(CoroutineDispatchers.DEFAULT.io)
		scope.launch {
			val c = async {
				val key = "c"
				delay(100)
				jobs.add(key)
				initializer.construct { key }
			}
			val b = async {
				val key = "b"
				delay(100)
				jobs.add(key)
				initializer.construct { key }
			}
			val a = async {
				val key = "a"
				delay(100)
				jobs.add(key)
				initializer.construct { key }
			}

			a.await()
			b.await()
			c.await()

			checkState(myObject === jobs.first()) {
				"LateLazy validCase3 failed." +
					"\nresult: $myObject" +
					"\nexpected ${jobs.first()}" +
					"\nlist: $jobs"
			}
			Timber.d(
				"LateLazy validCase3 Success" +
					"\nresult: $myObject" +
					"\nlist: $jobs"
			)
			scope.cancel()
		}
	}
}
