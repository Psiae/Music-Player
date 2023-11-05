package dev.dexsr.klio.base.resource

import java.util.*
import java.io.File as jFile

open class LocalImage<T>(val value: T) {

	object None : LocalImage<Int>(-1)

	class File(file: jFile) : LocalImage<java.io.File>(file) {

		override fun equals(other: Any?): Boolean {
			return this === other || other is File && other.value == value
		}

		override fun hashCode(): Int {
			return Objects.hash(value)
		}
	}
	class Resource(id: Int) : LocalImage<Int>(id) {

		override fun equals(other: Any?): Boolean {
			return this === other || other is Resource && other.value == value
		}

		override fun hashCode(): Int {
			return Objects.hash(value)
		}
	}

	class ByteArray(bytes: kotlin.ByteArray) : LocalImage<kotlin.ByteArray>(bytes) {

		override fun equals(other: Any?): Boolean {
			return this === other
		}

		override fun hashCode(): Int {
			return System.identityHashCode(this)
		}
	}

	companion object {
	}
}
