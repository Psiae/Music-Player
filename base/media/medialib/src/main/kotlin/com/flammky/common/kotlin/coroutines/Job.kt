package com.flammky.common.kotlin.coroutines

import androidx.annotation.GuardedBy
import com.flammky.musicplayer.core.common.sync
import kotlinx.coroutines.Job
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoCancelJob(
	initialValue: Job = Job().apply { cancel() }
) : ReadWriteProperty<Any?, Job> {

	private val lock = Any()

	@GuardedBy("lock")
	private var _value = initialValue

	override fun getValue(thisRef: Any?, property: KProperty<*>): Job {
		return sync(lock) { _value }
	}

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: Job) {
		// no atomic
		sync(lock) { _value.cancel() ; _value = value }
	}
}
