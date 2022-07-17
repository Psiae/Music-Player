package com.kylentt.musicplayer.core.app.delegates.device

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.drawable.toBitmap
import com.kylentt.musicplayer.core.app.delegates.AppDelegate
import com.kylentt.musicplayer.core.late.LateLazy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object DeviceWallpaper : ReadOnlyProperty<Any?, Drawable?> {

  private val initializer = LateLazy<WallpaperManager>()
  private val manager by initializer

  override fun getValue(thisRef: Any?, property: KProperty<*>): Drawable? {
		return when (thisRef) {
			is Context -> getDeviceWallpaper(thisRef)
			else -> AppDelegate.deviceWallpaperDrawable
		}
  }

	private fun getDeviceWallpaper(accessor: Context): Drawable? {
		return elseNull(StoragePermissionHelper.checkReadStoragePermission(accessor)) {
			if (!initializer.isInitialized) {
				val context = accessor.applicationContext
				initializer.initializeValue { WallpaperManager.getInstance(context) }
			}
			manager.drawable
		}
	}

	fun getBitmap(
		context: Context? = null,
		fastPath: Boolean = true,
		config: Bitmap.Config? = null
	): Bitmap? {
		return (
			context?.let { acc -> getDeviceWallpaper(acc) } ?: AppDelegate.deviceWallpaperDrawable)
			?.let { drawable: Drawable ->
				with(drawable) { if (fastPath) toBitmap(config = config) else toNewBitmap(config) }
			}
	}

	private fun Drawable.toNewBitmap(config: Bitmap.Config? = null): Bitmap {
		val height = intrinsicHeight
		val width = intrinsicWidth
		val (oldLeft, oldTop, oldRight, oldBottom) = bounds

		val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
		setBounds(0, 0, width, height)
		draw(Canvas(bitmap))

		setBounds(oldLeft, oldTop, oldRight, oldBottom)
		return bitmap
	}

  private inline fun <T> elseNull(condition: Boolean, block: () -> T): T? {
    return if (condition) block() else null
  }
}
