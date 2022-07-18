package com.kylentt.musicplayer.common.coroutines

import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoCancelJob(initialValue: Job = Job().job) : ReadWriteProperty<Any?, Job> {

	private var value = initialValue

	override fun getValue(thisRef: Any?, property: KProperty<*>): Job = this.value

	override fun setValue(thisRef: Any?, property: KProperty<*>, value: Job) {
		this.value.cancel()
		this.value = value
	}
}
