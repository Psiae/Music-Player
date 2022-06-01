package com.kylentt.mediaplayer.core.delegates

import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Synchronize<T>(defaultValue: T, lock: Any? = null): ReadWriteProperty<Any, T> {

  private val lock = lock ?: this

  @GuardedBy("lock")
  private var value = defaultValue

  override fun getValue(thisRef: Any, property: KProperty<*>): T {
    return synchronized(lock) { this.value }
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    synchronized(lock) { this.value = value }
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
