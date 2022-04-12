package com.kylentt.mediaplayer.core.util.handler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.annotation.Px
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.core.util.helper.VersionHelper
import jp.wasabeef.transformers.coil.CropSquareTransformation
import javax.inject.Singleton

object CoilTarget {
  val notificationlargeIcon = if (VersionHelper.isR()) 256 else 512
}

@Singleton
class CoilHandler(
  private val context: Context,
  private val coil: ImageLoader
) {

  var sizeForNotif: Int = if (VersionHelper.isR()) 256 else 512

  suspend fun makeBitmap(
    uri: Uri
  ): Bitmap? {
    val req = ImageRequest.Builder(context)
      .data(uri)
      .build()
    return ((coil.execute(req).drawable) as? BitmapDrawable)?.bitmap
  }

  suspend fun squareWithCoil(
    bitmap: Bitmap,
    @Px size: Int = CoilTarget.notificationlargeIcon,
  ): Bitmap {
    val req = ImageRequest.Builder(context)
      .diskCachePolicy(CachePolicy.ENABLED)
      .transformations(CropSquareTransformation())
      .size(size)
      .scale(Scale.FILL)
      .data(bitmap)
      .build()
    return ((coil.execute(req).drawable) as BitmapDrawable).bitmap
  }

  suspend fun makeSquaredBitmap(
    uri: Uri,
    @Px size: Int = CoilTarget.notificationlargeIcon,
  ): Bitmap {
    val req = ImageRequest.Builder(context)
      .diskCachePolicy(CachePolicy.ENABLED)
      .transformations(CropSquareTransformation())
      .size(size)
      .scale(Scale.FILL)
      .data(uri)
      .build()
    return ((coil.execute(req).drawable) as BitmapDrawable).bitmap
  }
}
