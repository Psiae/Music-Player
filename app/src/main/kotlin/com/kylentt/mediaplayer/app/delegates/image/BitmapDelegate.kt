package com.kylentt.mediaplayer.app.delegates.image

import android.graphics.Bitmap
import androidx.annotation.GuardedBy
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.app.delegates.Synchronize
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class RecycleBitmap(defaultValue: Bitmap?) : ReadWriteProperty <Any, Bitmap?> {

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

  private val lock = Any()

  @GuardedBy("lock")
  @Volatile private var pair = defaultValue

  override fun getValue(thisRef: Any, property: KProperty<*>): Pair<T, Bitmap?> =
    synchronized(lock) {
      checkState(pair.second?.isRecycled != true) {
        "tried to return Recycled Bitmap ${pair.second}"
      }
      return pair
    }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: Pair<T, Bitmap?>) =
    synchronized(lock) {
      pair.second?.recycle()
      pair = value
    }
}
