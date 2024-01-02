package dev.dexsr.klio.android.base.resource

import dev.dexsr.klio.base.resource.LocalImage
import java.util.*

sealed class AndroidLocalImage<T>(value: T) : LocalImage<T>(value) {

	class Bitmap(value: android.graphics.Bitmap): AndroidLocalImage<android.graphics.Bitmap>(value) {

		override fun equals(other: Any?): Boolean {
			return this === other
		}

		override fun hashCode(): Int = super.hashCode()

		override fun deepEquals(other: LocalImage<*>): Boolean {
			return other is Bitmap && other.value.sameAs(value)
		}

		override fun deepHashcode(): Int {
			var hash = 0
			for (x in 0 .. value.width) {
				for (y in 0 .. value.height) {
					hash *= 31
					hash += value.getPixel(x, y).hashCode()
				}
			}
			return hash;
		}
	}

	class Resource(id: Int) : LocalImage<Int>(id) {

		override fun equals(other: Any?): Boolean {
			return this === other || other is Resource && other.value == value
		}

		override fun deepEquals(other: LocalImage<*>): Boolean {
			return equals(other)
		}

		override fun deepHashcode(): Int = hashCode()

		override fun hashCode(): Int = value.hashCode()
	}
}
