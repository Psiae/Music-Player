package com.kylentt.mediaplayer.helper.image

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.Px
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.helper.image.CoilHelper.CenterCropTransform.*
import jp.wasabeef.transformers.coil.CenterBottomCropTransformation
import jp.wasabeef.transformers.coil.CenterCropTransformation
import jp.wasabeef.transformers.coil.CenterTopCropTransformation
import kotlinx.coroutines.withContext
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class CoilHelper(
  private val context: Context,
  private val imageLoader: ImageLoader
) {

  val storagePermission by StoragePermissionDelegate

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
    cache: CachePolicy = CachePolicy.ENABLED,
    placeHolder: Drawable? = null,
    type: CenterCropTransform,
    bitmap: Bitmap,
    @Px size: Int
  ): Bitmap? = withContext(coroutineContext) {
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
      .size(size)
      .transformations(centerType)
      .build()
    (loader.execute(req).drawable as BitmapDrawable).bitmap
  }

  fun bitmapFromUri(uri: Uri) {

  }

}
