package com.kylentt.mediaplayer.app.delegates.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import kotlin.reflect.KProperty

object StoragePermissionDelegate {

  /**
   * Delegate to check Storage Permission, gets the context from AppDelegate Class
   * Which is the requirement for the App to function properly
   * */

  const val Read_External_Storage = Manifest.permission.READ_EXTERNAL_STORAGE
  const val Write_External_Storage = Manifest.permission.WRITE_EXTERNAL_STORAGE

  fun checkReadStoragePermission(context: Context): Boolean {
    return ContextCompat
      .checkSelfPermission(context, Read_External_Storage) == PackageManager.PERMISSION_GRANTED
  }

  fun checkWriteStoragePermission(context: Context): Boolean {
    return ContextCompat
      .checkSelfPermission(context, Write_External_Storage) == PackageManager.PERMISSION_GRANTED
  }

  operator fun getValue(appDelegate: AppDelegate, property: KProperty<*>): Boolean {
    return checkReadStoragePermission(appDelegate.base) and
      checkWriteStoragePermission(appDelegate.base)
  }

  /** can be Delegated inside Composable */
  operator fun getValue(any: Any?, property: KProperty<*>): Boolean {
    return AppDelegate.hasStoragePermission
  }
}
