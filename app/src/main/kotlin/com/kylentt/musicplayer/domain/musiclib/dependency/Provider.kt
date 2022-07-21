package com.kylentt.musicplayer.domain.musiclib.dependency

interface Provider<T : Any> {
	val value: T
	val valueClass: Class<T>
}

class ValueProvider<T : Any>(override val value: T) : Provider<T> {

	override val valueClass: Class<T> = value.javaClass

	companion object {
		fun args(vararg args: Any): Array<ValueProvider<Any>> {
			val mtb = mutableListOf<ValueProvider<Any>>()
			args.forEach { mtb.add(ValueProvider(it)) }
			return mtb.toTypedArray()
		}
	}
}
