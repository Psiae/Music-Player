package com.kylentt.musicplayer.core.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.Px
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.wasabeef.transformers.coil.CropCenterTransformation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoilHelper @Inject constructor(
  @ApplicationContext private val context: Context,
  private val coil: ImageLoader
) {

  suspend fun squareWithCoil(bitmap: Bitmap) = bitmap.squareWithCoil()

  suspend fun Bitmap.squareWithCoil(
    @Px height: Int = this.height,
    @Px width: Int = this.width,
    @Px size: Int = -1,
  ): Bitmap {
    val s = if (size >= 0) PixelSize(size, size) else PixelSize(height, width)
    val c = coil.execute(
      ImageRequest.Builder(context)
        .diskCachePolicy(CachePolicy.ENABLED)
        .data(this)
        .size(s)
        .scale(Scale.FILL)
        .transformations(CropCenterTransformation())
        .build()
    ).drawable as BitmapDrawable
    return c.bitmap
  }

}
