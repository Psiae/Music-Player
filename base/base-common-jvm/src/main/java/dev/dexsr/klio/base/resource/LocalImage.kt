package dev.dexsr.klio.base.resource

import java.util.*
import java.io.File as jFile

abstract class LocalImage<T>(val value: T) {

	object None : LocalImage<Int>(-1) {

		override fun deepEquals(other: LocalImage<*>) = other is None

		override fun deepHashcode(): Int = hashCode()
	}

	class File(file: jFile) : LocalImage<java.io.File>(file) {

		override fun equals(other: Any?): Boolean {
			return this === other || other is File && other.value == value
		}

		override fun deepEquals(other: LocalImage<*>): Boolean {
			return equals(other)
		}

		override fun deepHashcode(): Int {
			return hashCode()
		}

		override fun hashCode(): Int {
			return Objects.hash(value)
		}
	}

	class ByteArray(bytes: kotlin.ByteArray) : LocalImage<kotlin.ByteArray>(bytes) {

		override fun equals(other: Any?): Boolean {
			return this === other
		}

		override fun deepEquals(other: LocalImage<*>): Boolean {
			return other is ByteArray && value.contentEquals(other.value)
		}

		override fun deepHashcode(): Int {
			return value.contentHashCode()
		}

		override fun hashCode(): Int = super.hashCode()
	}

	abstract fun deepEquals(other: LocalImage<*>): Boolean

	abstract fun deepHashcode(): Int

	companion object {
	}
}
