package com.kylentt.mediaplayer.app.delegates.image

import android.graphics.Bitmap
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.app.delegates.Synchronize
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RecycleBitmapDelegate(defaultValue: Bitmap?) : ReadWriteProperty <Any, Bitmap?> {

  private var bitmap by Synchronize(defaultValue)

  override fun getValue(thisRef: Any, property: KProperty<*>): Bitmap? {
    checkState(bitmap?.isRecycled != true) {
      "tried to return Recycled Bitmap $bitmap"
    }
    return bitmap
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: Bitmap?) {
    bitmap?.recycle()
    bitmap = value
  }
}

class RecyclePairBitmap<T>(
  defaultValue: Pair<T, Bitmap?>
) : ReadWriteProperty<Any, Pair<T, Bitmap?>> {

  private var pair by Synchronize<Pair<T, Bitmap?>>(defaultValue)

  override fun getValue(thisRef: Any, property: KProperty<*>): Pair<T, Bitmap?> {
    checkState(pair.second?.isRecycled != true) {
      "tried to return Recycled Bitmap ${pair.second}"
    }
    return pair
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: Pair<T, Bitmap?>) {
    if (pair.first is MediaItem) {
      val cast = pair.first as MediaItem
      val valueCast = value.first as MediaItem
      Timber.d("RecycleBitmapDelegate, setValue from ${cast.mediaMetadata.displayTitle} to ${valueCast.mediaMetadata.displayTitle}")
    }
    Timber.d("RecycleBitmapDelegate, setValue from ${pair.second} to ${value.second}")
    pair.second?.recycle()
    pair = value
  }
}
