package dev.dexsr.klio.android.base.resource

import dev.dexsr.klio.base.resource.LocalImage

sealed class AndroidLocalImage<T>(value: T) : LocalImage<T>(value) {

	class Bitmap(value: android.graphics.Bitmap): AndroidLocalImage<android.graphics.Bitmap>(value) {


	}
}
