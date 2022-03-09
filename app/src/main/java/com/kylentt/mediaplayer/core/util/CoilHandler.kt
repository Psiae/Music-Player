package com.kylentt.mediaplayer.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.annotation.Px
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import jp.wasabeef.transformers.coil.CropSquareTransformation
import javax.inject.Singleton

@Singleton
class CoilHandler(
    private val context: Context,
    private val coil: ImageLoader
) {

    var sizeForNotif: Int? = null

    init {
        sizeForNotif = if (VersionHelper.isR()) 256 else 512
    }

    suspend fun squareWithCoil(bitmap: Bitmap, @Px size: Int = sizeForNotif!!, ): Bitmap? {
        val req = ImageRequest.Builder(context)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(size)
            .scale(Scale.FILL)
            .data(bitmap)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    suspend fun makeSquaredBitmap(uri: Uri, @Px size: Int = sizeForNotif!!, ): Bitmap? {
        val req = ImageRequest.Builder(context)
            .transformations(CropSquareTransformation())
            .size(size)
            .scale(Scale.FILL)
            .data(uri)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }
}