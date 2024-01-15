package dev.dexsr.klio.base

interface NONE <T> where T: Any {

	@Suppress("PropertyName")
	val NONE: T
}

val <T: Any> NONE<T>.isNONE
	get() = this === this.NONE
