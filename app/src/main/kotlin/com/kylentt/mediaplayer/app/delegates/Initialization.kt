package com.kylentt.mediaplayer.app.delegates

import com.kylentt.mediaplayer.BuildConfig
import com.kylentt.mediaplayer.app.annotation.UnsafeClass
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Lateinit val basically, any attempt to update the value is ignored
 * @param lock the Lock
 * @throws IllegalStateException on any attempt to get value when its not Initialized
 * @sample LateLazySample.testCase1
 */

class LateLazy<T>(lock: Any? = null) : ReadOnlyProperty<Any?, T> {
  private object EMPTY

  private val lock = lock ?: this

  private val empty = { EMPTY }
  private var initializer: (() -> Any?)? = empty

  private val value by lazy(lock) { initializer!!() }

  val isInitialized
    get() = initializer === null

  fun init(lazy: () -> T): T = initializeValue { lazy() }

  @Suppress("UNCHECKED_CAST")
  private fun initializeValue(block: () -> T): T = synchronized(lock) {
    if (!isInitialized) {
      initializer = block
      val get = value
      initializer = null
      get
    } else {
      value
    } as T
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    checkState(isInitialized) {
      "LateLazy Failed, initializer is $initializer"
    }
    @Suppress("UNCHECKED_CAST")
    return value as T
  }
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

    initializer.init { any1 }
    initializer.init { any2 }
    initializer.init { any3 }

    checkState(myObject === any1)
  }


  private fun testCase2() {
    val initializer = LateLazy<String>()
    val myObject by initializer

    val jobs = mutableListOf<String>()

    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {

      val a = async {
        val key = "a"
        delay(200)
        initializer.init { key }
        jobs.add(key)
      }
      val b = async {
        val key = "b"
        delay(200)
        initializer.init { key }
        jobs.add(key)
      }
      val c = async {
        val key = "c"
        delay(200)
        initializer.init { key }
        jobs.add(key)
      }

      a.await()
      b.await()
      c.await()

      checkState(myObject === jobs.first()) {
        "LateLazy validCase3 failed." +
          "\nresult: $myObject" +
          "\nexpected ${jobs.first()}" +
          "\nactual: $jobs"
      }
      scope.cancel()
    }
  }
}

@UnsafeClass
class MutableLateLazy<T>(lock: Any? = null) : ReadOnlyProperty<Any, T> {

  private object EMPTY

  private val lock = lock ?: this
  private var value: Any? = EMPTY

  val isInitialized
    get() = value !== EMPTY

  private fun setValue(lazyBlock: () -> T) = synchronized(lock) {
    value = lazyBlock()
  }

  override fun getValue(thisRef: Any, property: KProperty<*>): T {
    checkState(isInitialized) {
      "MutableLateLazy Failed, value $value is EMPTY"
    }
    @Suppress("UNCHECKED_CAST")
    return value as T
  }
}
