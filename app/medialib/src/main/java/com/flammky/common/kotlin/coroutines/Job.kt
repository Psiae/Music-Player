package com.flammky.common.kotlin.coroutines

import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoCancelJob(
	initialValue: Job = Job().apply { cancel() }
) : ReadWriteProperty<Any?, Job> {

	private var value = initialValue

	override fun getValue(thisRef: Any?, property: KProperty<*>): Job = this.value

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: Job) {
		this.value.cancel()
		this.value = value
	}
}
