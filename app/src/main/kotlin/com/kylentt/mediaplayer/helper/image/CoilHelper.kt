package com.kylentt.mediaplayer.helper.image

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.Px
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.kylentt.mediaplayer.helper.image.CoilHelper.CenterCropTransform.*
import com.kylentt.musicplayer.common.extenstions.checkCancellation
import jp.wasabeef.transformers.coil.CenterBottomCropTransformation
import jp.wasabeef.transformers.coil.CenterCropTransformation
import jp.wasabeef.transformers.coil.CenterTopCropTransformation
import kotlinx.coroutines.ensureActive
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

object CoilBitmapTransformer {

	enum class CenterCropTransform {
		TOP,
		CENTER,
		BOTTOM
	}

	suspend fun squareBitmap(
		bitmap: Bitmap,
		@Px size: Int,
		context: Context,
		imageLoader: ImageLoader,
		type: CenterCropTransform,
		cachePolicy: CachePolicy,
		fastPath: Boolean,
	): Bitmap {
		coroutineContext.ensureActive()

		val centerCropType = when (type) {
			CenterCropTransform.TOP -> CenterTopCropTransformation()
			CenterCropTransform.CENTER -> CenterCropTransformation()
			CenterCropTransform.BOTTOM -> CenterBottomCropTransformation()
		}

		val req = ImageRequest.Builder(context)
			.data(bitmap)
			.size(size)
			.transformations(centerCropType)
			.diskCachePolicy(cachePolicy)
			.scale(Scale.FIT)
			.build()

		coroutineContext.ensureActive()
		val drawable = imageLoader.execute(req).drawable!!

		coroutineContext.ensureActive()
		val result =
			if (fastPath) {
				drawable.toBitmap(size, size)
			} else {
				drawable.toNewBitmap(size, size)
			}

		return result
	}

	private fun Drawable.toNewBitmap(
		@Px height: Int = intrinsicHeight,
		@Px width: Int = intrinsicWidth,
		config: Bitmap.Config? = null,
	): Bitmap {
		val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
		val (oldLeft, oldTop, oldRight, oldBottom) = bounds
		setBounds(0, 0, width, height)
		draw(Canvas(bitmap))
		setBounds(oldLeft, oldTop, oldRight, oldBottom)
		return bitmap
	}
}

/**
 * Singleton Helper for ImageLoading Task with [coil.Coil.imageLoader] from Application Context
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
class CoilHelper(
  private val context: Context,
  private val imageLoader: ImageLoader
) {

  init {
    check(context is Application)
  }

  enum class CenterCropTransform {
    TOP,
    CENTER,
    BOTTOM
  }

  suspend fun squareBitmap(
		bitmap: Bitmap,
		@Px size: Int,
		context: Context = this.context,
		loader: ImageLoader = this.imageLoader,
		cache: CachePolicy = CachePolicy.DISABLED,
		fastPath: Boolean = false,
		centerType: CenterCropTransform = CENTER
  ): Bitmap {
		val type = when (centerType) {
			TOP -> CoilBitmapTransformer.CenterCropTransform.TOP
			CENTER -> CoilBitmapTransformer.CenterCropTransform.CENTER
			BOTTOM -> CoilBitmapTransformer.CenterCropTransform.BOTTOM
		}

		val get = CoilBitmapTransformer
			.squareBitmap(bitmap, size, context, loader, type, cache, fastPath)

		coroutineContext.checkCancellation {
			if (!fastPath) get.recycle()
		}
		return get
	}
}
