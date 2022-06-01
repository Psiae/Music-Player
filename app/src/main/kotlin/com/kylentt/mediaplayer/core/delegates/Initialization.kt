package com.kylentt.mediaplayer.core.delegates

import androidx.annotation.GuardedBy
import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface InitializerDelegate <T> : ReadOnlyProperty<Any?, T> {
	fun initializeValue(lazyValue: () -> T ): T
}

interface LateInitializerDelegate <T> : InitializerDelegate<T> {
	val isInitialized: Boolean
}

/**
 * Lateinit val basically, any attempt to update the value is ignored
 * @param lock the Lock
 * @throws IllegalStateException on any attempt to get value when its not Initialized
 * @sample LateLazySample.testCase1
 */

class LateLazy<T>(lock: Any? = null) : LateInitializerDelegate<T> {

	private object EMPTY

	private val lock: Any = lock ?: this

	@GuardedBy("lock")
	private var value: Any? = EMPTY

  override val isInitialized
    get() = value !== EMPTY

  override fun initializeValue(lazyValue: () -> T): T = synchronized(lock) {
    if (!isInitialized) {
      value = lazyValue()
    }
    castValue()
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
		return if (!isInitialized) {
			synchronized(lock) { castValue() }
		} else {
			castValue()
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun castValue(): T = value as T
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

    initializer.initializeValue { any1 }
    initializer.initializeValue { any2 }
    initializer.initializeValue { any3 }

    checkState(myObject === any1)
  }

  private fun testCase2() {
    val initializer = LateLazy<String>()
    val myObject by initializer

    val jobs = mutableListOf<String>()

    val scope = CoroutineScope(AppDispatchers.DEFAULT.io)
    scope.launch {
			val c = async {
				val key = "c"
				delay(100)
				jobs.add(key)
				initializer.initializeValue { key }
			}
			val b = async {
				val key = "b"
				delay(100)
				jobs.add(key)
				initializer.initializeValue { key }
			}
      val a = async {
        val key = "a"
        delay(100)
				jobs.add(key)
        initializer.initializeValue { key }
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
			Timber.d("LateLazy validCase3 Success" +
				"\nresult: $myObject" +
				"\nlist: $jobs")
      scope.cancel()
    }
  }
}
