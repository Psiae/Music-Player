package com.kylentt.mediaplayer.helper.image

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.Px
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.image.CoilHelper.CenterCropTransform.*
import jp.wasabeef.transformers.coil.CenterBottomCropTransformation
import jp.wasabeef.transformers.coil.CenterCropTransformation
import jp.wasabeef.transformers.coil.CenterTopCropTransformation
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Singleton Helper for ImageLoading Task with [coil.Coil.imageLoader]
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
    context: Context = this.context,
    loader: ImageLoader = this.imageLoader,
    cache: CachePolicy = CachePolicy.DISABLED,
    placeHolder: Drawable? = null,
    recycle: Boolean = false,
    type: CenterCropTransform = CENTER,
    bitmap: Bitmap,
    @Px size: Int
  ): Bitmap = withContext(coroutineContext) {

    Timber.d("squareBitmap $bitmap")

    val centerType = when (type) {
      TOP -> CenterTopCropTransformation()
      CENTER -> CenterCropTransformation()
      BOTTOM -> CenterBottomCropTransformation()
    }

    val req = ImageRequest
      .Builder(context)
      .data(bitmap)
      .diskCachePolicy(cache)
      .placeholder(placeHolder)
      .transformations(centerType)
      .size(size)
      .scale(Scale.FILL)
      .precision(Precision.EXACT)
      .build()

    val result = executeBitmapRequest(loader, req, bitmap, recycle)
    if (recycle) checkState(result !== bitmap)
    Timber.d("squaredBitmap with size: $size?${result.width}:${result.height}, alloc: ${result.allocationByteCount}")
    result
  }

  suspend fun executeBitmapRequest(
    loader: ImageLoader,
    request: ImageRequest,
    bitmap: Bitmap,
    recycle: Boolean
  ): Bitmap = withContext(coroutineContext) {
    checkArgument(request.data === bitmap)
    val drawable = loader.execute(request).drawable!!
    val result =
      if (recycle) {
        val new = drawable.toNewBitmap()
        bitmap.recycle()
        new
      } else {
        drawable.toBitmap()
      }
    ensureActive()
    return@withContext result
  }

  fun Drawable.toNewBitmap(
    @Px width: Int = intrinsicWidth,
    @Px height: Int = intrinsicHeight,
    config: Bitmap.Config? = null,
  ): Bitmap {
    val (oldLeft, oldTop, oldRight, oldBottom) = bounds
    val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    setBounds(oldLeft, oldTop, oldRight, oldBottom)
    return bitmap
  }

  suspend fun resizeBitmap(
    context: Context = this.context,
    loader: ImageLoader = this.imageLoader,
    cache: CachePolicy = CachePolicy.DISABLED,
    placeHolder: Drawable? = null,
    bitmap: Bitmap,
    @Px height: Int,
    @Px width: Int
  ) = withContext(coroutineContext) {
    val req = ImageRequest
      .Builder(context)
      .data(bitmap)
      .diskCachePolicy(cache)
      .placeholder(placeHolder)
      .size(width, height)
      .build()
    ensureActive()
    (loader.execute(req).drawable as BitmapDrawable)
  }

  suspend fun bitmapFromUri(
    context: Context = this.context,
    loader: ImageLoader = this.imageLoader,
    cache: CachePolicy = CachePolicy.DISABLED,
    @Px size: Int = 0,
    uri: Uri
  ): Bitmap? = withContext(coroutineContext) {
    val req = ImageRequest
      .Builder(context)
      .data(uri)
      .diskCachePolicy(cache)
      .size( if (size > 0) PixelSize(size, size) else OriginalSize )
      .build()
    (loader.execute(req).drawable as BitmapDrawable).bitmap
  }

}
