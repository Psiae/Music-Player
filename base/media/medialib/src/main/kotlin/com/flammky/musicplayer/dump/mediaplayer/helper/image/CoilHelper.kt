package com.flammky.musicplayer.dump.mediaplayer.helper.image

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Bitmap.createScaledBitmap
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
import com.flammky.common.kotlin.coroutine.ensureNotCancelled
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.wasabeef.transformers.coil.CenterBottomCropTransformation
import jp.wasabeef.transformers.coil.CenterCropTransformation
import jp.wasabeef.transformers.coil.CenterTopCropTransformation
import kotlinx.coroutines.ensureActive
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

private object CoilBitmapTransformer {

	enum class CenterCropTransform {
		TOP,
		CENTER,
		BOTTOM
	}

	suspend fun createSquaredBitmap(
		source: Any,
		@Px size: Int,
		scale: Scale?,
		context: Context,
		imageLoader: ImageLoader,
		type: CenterCropTransform,
		cachePolicy: CachePolicy,
		config: Bitmap.Config,
	): Bitmap? {
		coroutineContext.ensureActive()

		val centerCropType = when (type) {
			CenterCropTransform.TOP -> CenterTopCropTransformation()
			CenterCropTransform.CENTER -> CenterCropTransformation()
			CenterCropTransform.BOTTOM -> CenterBottomCropTransformation()
		}

		val req = ImageRequest.Builder(context)
			.bitmapConfig(config)
			.data(source)
			.size(size)
			.transformations(centerCropType)
			.diskCachePolicy(cachePolicy)
			.precision(Precision.EXACT)

		if (scale != null) req.scale(scale)

		coroutineContext.ensureActive()
		val queued = imageLoader.enqueue(req.build())

		coroutineContext.ensureNotCancelled {
			queued.dispose()
		}

		return queued.job.await().drawable?.toBitmap()
	}

	private fun Drawable.toNewScaledBitmap(
		@Px height: Int = intrinsicHeight,
		@Px width: Int = intrinsicWidth,
		config: Bitmap.Config? = null,
	): Bitmap {
		val bitmap = Bitmap
			.createBitmap(intrinsicWidth, intrinsicHeight, config ?: Bitmap.Config.ARGB_8888)
		val (oldLeft, oldTop, oldRight, oldBottom) = bounds
		setBounds(0, 0, intrinsicWidth, intrinsicHeight)
		draw(Canvas(bitmap))
		setBounds(oldLeft, oldTop, oldRight, oldBottom)
		return createScaledBitmap(bitmap, width, height, true)
	}
}

/**
 * Singleton Helper for ImageLoading Task with [coil.Coil.imageLoader] from Application Context
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
class CoilHelper constructor(
  @ApplicationContext private val context: Context,
  private val imageLoader: ImageLoader
) {

  init {
    check(context is Application)
  }


	suspend fun loadBitmap(
		source: Any,
		@Px width: Int? = null,
		@Px height: Int? = null,
		scale: Scale? = null,
		context: Context = this.context,
		loader: ImageLoader = this.imageLoader,
		config: Config = Config.ARGB_8888
	): Bitmap? {
		val req = ImageRequest.Builder(context)
			.bitmapConfig(config)
			.data(source)


		if (width != null && height != null) {
			req.size(width, height)
			req.precision(Precision.EXACT)
		}
		if (scale != null) req.scale(scale)

		coroutineContext.ensureActive()
		val queued = loader.enqueue(req.build())

		coroutineContext.ensureNotCancelled {
			queued.dispose()
		}

		return queued.job.await().drawable?.toBitmap()
	}

  enum class CenterCropTransform {
    TOP,
    CENTER,
    BOTTOM
  }

  suspend fun loadSquaredBitmap(
		source: Any,
		@Px size: Int,
		scale: Scale? = null,
		context: Context = this.context,
		loader: ImageLoader = this.imageLoader,
		cache: CachePolicy = CachePolicy.DISABLED,
		config: Config = Config.ARGB_8888,
		centerType: CenterCropTransform = CenterCropTransform.CENTER,
  ): Bitmap? {

		val type = when (centerType) {
			CenterCropTransform.TOP -> CoilBitmapTransformer.CenterCropTransform.TOP
			CenterCropTransform.CENTER -> CoilBitmapTransformer.CenterCropTransform.CENTER
			CenterCropTransform.BOTTOM -> CoilBitmapTransformer.CenterCropTransform.BOTTOM
		}

		return CoilBitmapTransformer
			.createSquaredBitmap(source, size, scale, context, loader, type, cache,  config)
	}
}
