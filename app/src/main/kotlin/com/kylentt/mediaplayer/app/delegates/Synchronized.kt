package com.kylentt.mediaplayer.app.delegates

import androidx.annotation.MainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Synchronize<T>(defaultValue: T, lock: Any? = null): ReadWriteProperty<Any, T> {
  private var value = defaultValue
  private val lock = lock ?: this

  override fun getValue(thisRef: Any, property: KProperty<*>): T {
    return synchronized(lock) { value }
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    synchronized(lock) { this.value = value }
  }
}

class SynchronizeInitOnce<T>(
  private val defaultValue: T,
  lock: Any? = null
): ReadWriteProperty<Any, T> {

  @Volatile private var value = defaultValue
  private val lock = lock ?: this

  override fun getValue(thisRef: Any, property: KProperty<*>): T = value

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) = synchronized(lock) {
    if (this.value === defaultValue) {
      this.value = value
    }
  }
}

@MainThread
class LockMainThread<T>(value: T): ReadWriteProperty<Any,T> {

  private var current = value

  override fun getValue(thisRef: Any, property: KProperty<*>): T {
    checkMainThread()
    return current
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    checkMainThread()
    current = value
  }
}




