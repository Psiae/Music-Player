package dev.dexsr.klio.android.base.resource

import dev.dexsr.klio.base.resource.LocalImage

sealed class AndroidLocalImage<T>(value: T) : LocalImage<T>(value) {

	class Bitmap(value: android.graphics.Bitmap): AndroidLocalImage<android.graphics.Bitmap>(value) {

		override fun equals(other: Any?): Boolean {
			return this === other
		}

		override fun hashCode(): Int {
			return System.identityHashCode(this)
		}
	}
}
