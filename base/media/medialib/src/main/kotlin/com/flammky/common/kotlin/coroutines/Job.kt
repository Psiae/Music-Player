package com.flammky.common.kotlin.coroutines

import androidx.annotation.GuardedBy
import kotlinx.coroutines.Job
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoCancelJob(
	initialValue: Job = Job().apply { cancel() }
) : ReadWriteProperty<Any?, Job> {

	private val lock = ReentrantReadWriteLock()

	@GuardedBy("lock")
	private var _value = initialValue

	override fun getValue(thisRef: Any?, property: KProperty<*>): Job = lock.read {
		_value
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: Job) = lock.write {
		_value.cancel()
		_value = value
	}
}
