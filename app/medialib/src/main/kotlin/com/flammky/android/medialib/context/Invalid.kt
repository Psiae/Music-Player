package com.flammky.android.medialib.context

import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.Display
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executor

object InvalidAndroidContext : Context() {
	override fun getAssets(): AssetManager = error("Invalid")

	override fun getResources(): Resources = error("Invalid")

	override fun getPackageManager(): PackageManager = error("Invalid")

	override fun getContentResolver(): ContentResolver = error("Invalid")

	override fun getMainLooper(): Looper = error("Invalid")

	override fun getMainExecutor(): Executor = error("Invalid")

	override fun getApplicationContext(): Context = error("Invalid")

	override fun registerComponentCallbacks(callback: ComponentCallbacks?) = error("Invalid")

	override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) = error("Invalid")

	override fun setTheme(resid: Int) = error("Invalid")

	override fun getTheme(): Resources.Theme = error("Invalid")

	override fun getClassLoader(): ClassLoader = error("Invalid")

	override fun getPackageName(): String = error("Invalid")

	override fun getOpPackageName(): String = error("Invalid")

	override fun getAttributionTag(): String = error("Invalid")

	override fun getAttributionSource(): AttributionSource = error("Invalid")

	override fun getParams(): ContextParams = error("Invalid")

	override fun getApplicationInfo(): ApplicationInfo = error("Invalid")

	override fun getPackageResourcePath(): String = error("Invalid")

	override fun getPackageCodePath(): String = error("Invalid")

	override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = error("Invalid")

	override fun moveSharedPreferencesFrom(sourceContext: Context?, name: String?): Boolean = error("Invalid")

	override fun deleteSharedPreferences(name: String?): Boolean = error("Invalid")

	override fun openFileInput(name: String?): FileInputStream = error("Invalid")

	override fun openFileOutput(name: String?, mode: Int): FileOutputStream = error("Invalid")

	override fun deleteFile(name: String?): Boolean = error("Invalid")

	override fun getFileStreamPath(name: String?): File = error("Invalid")

	override fun getDataDir(): File = error("Invalid")

	override fun getFilesDir(): File = error("Invalid")

	override fun getNoBackupFilesDir(): File = error("Invalid")

	override fun getExternalFilesDir(type: String?): File? = error("Invalid")

	override fun getExternalFilesDirs(type: String?): Array<File> = error("Invalid")

	override fun getObbDir(): File = error("Invalid")

	override fun getObbDirs(): Array<File> = error("Invalid")

	override fun getCacheDir(): File = error("Invalid")

	override fun getCodeCacheDir(): File = error("Invalid")

	override fun getExternalCacheDir(): File? = error("Invalid")

	override fun getExternalCacheDirs(): Array<File> = error("Invalid")

	override fun getExternalMediaDirs(): Array<File> = error("Invalid")

	override fun fileList(): Array<String> = error("Invalid")

	override fun getDir(name: String?, mode: Int): File = error("Invalid")

	override fun openOrCreateDatabase(
		name: String?,
		mode: Int,
		factory: SQLiteDatabase.CursorFactory?
	): SQLiteDatabase = error("Invalid")

	override fun openOrCreateDatabase(
		name: String?,
		mode: Int,
		factory: SQLiteDatabase.CursorFactory?,
		errorHandler: DatabaseErrorHandler?
	): SQLiteDatabase = error("Invalid")

	override fun moveDatabaseFrom(sourceContext: Context?, name: String?): Boolean = error("Invalid")

	override fun deleteDatabase(name: String?): Boolean = error("Invalid")

	override fun getDatabasePath(name: String?): File = error("Invalid")

	override fun databaseList(): Array<String> = error("Invalid")

	override fun getWallpaper(): Drawable = error("Invalid")

	override fun peekWallpaper(): Drawable = error("Invalid")

	override fun getWallpaperDesiredMinimumWidth(): Int = error("Invalid")

	override fun getWallpaperDesiredMinimumHeight(): Int = error("Invalid")

	override fun setWallpaper(bitmap: Bitmap?) = error("Invalid")

	override fun setWallpaper(data: InputStream?) = error("Invalid")

	override fun clearWallpaper() = error("Invalid")

	override fun startActivity(intent: Intent?) = error("Invalid")

	override fun startActivity(intent: Intent?, options: Bundle?) = error("Invalid")

	override fun startActivities(intents: Array<out Intent>?) = error("Invalid")

	override fun startActivities(intents: Array<out Intent>?, options: Bundle?) = error("Invalid")

	override fun startIntentSender(
		intent: IntentSender?,
		fillInIntent: Intent?,
		flagsMask: Int,
		flagsValues: Int,
		extraFlags: Int
	) = error("Invalid")

	override fun startIntentSender(
		intent: IntentSender?,
		fillInIntent: Intent?,
		flagsMask: Int,
		flagsValues: Int,
		extraFlags: Int,
		options: Bundle?
	) = error("Invalid")

	override fun sendBroadcast(intent: Intent?) = error("Invalid")

	override fun sendBroadcast(intent: Intent?, receiverPermission: String?) = error("Invalid")

	override fun sendBroadcastWithMultiplePermissions(
		intent: Intent,
		receiverPermissions: Array<out String>
	) = error("Invalid")

	override fun sendOrderedBroadcast(intent: Intent?, receiverPermission: String?) = error("Invalid")

	override fun sendOrderedBroadcast(
		intent: Intent,
		receiverPermission: String?,
		resultReceiver: BroadcastReceiver?,
		scheduler: Handler?,
		initialCode: Int,
		initialData: String?,
		initialExtras: Bundle?
	) = error("Invalid")

	override fun sendOrderedBroadcast(
		intent: Intent,
		receiverPermission: String?,
		receiverAppOp: String?,
		resultReceiver: BroadcastReceiver?,
		scheduler: Handler?,
		initialCode: Int,
		initialData: String?,
		initialExtras: Bundle?
	) = error("Invalid")

	override fun sendBroadcastAsUser(intent: Intent?, user: UserHandle?) = error("Invalid")

	override fun sendBroadcastAsUser(
		intent: Intent?,
		user: UserHandle?,
		receiverPermission: String?
	) = error("Invalid")

	override fun sendOrderedBroadcastAsUser(
		intent: Intent?,
		user: UserHandle?,
		receiverPermission: String?,
		resultReceiver: BroadcastReceiver?,
		scheduler: Handler?,
		initialCode: Int,
		initialData: String?,
		initialExtras: Bundle?
	) = error("Invalid")

	override fun sendStickyBroadcast(intent: Intent?) = error("Invalid")

	override fun sendStickyBroadcast(intent: Intent, options: Bundle?) = error("Invalid")

	override fun sendStickyOrderedBroadcast(
		intent: Intent?,
		resultReceiver: BroadcastReceiver?,
		scheduler: Handler?,
		initialCode: Int,
		initialData: String?,
		initialExtras: Bundle?
	) = error("Invalid")

	override fun removeStickyBroadcast(intent: Intent?) = error("Invalid")

	override fun sendStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) = error("Invalid")

	override fun sendStickyOrderedBroadcastAsUser(
		intent: Intent?,
		user: UserHandle?,
		resultReceiver: BroadcastReceiver?,
		scheduler: Handler?,
		initialCode: Int,
		initialData: String?,
		initialExtras: Bundle?
	) = error("Invalid")

	override fun removeStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) = error("Invalid")

	override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? = error("Invalid")

	override fun registerReceiver(
		receiver: BroadcastReceiver?,
		filter: IntentFilter?,
		flags: Int
	): Intent = error("Invalid")

	override fun registerReceiver(
		receiver: BroadcastReceiver?,
		filter: IntentFilter?,
		broadcastPermission: String?,
		scheduler: Handler?
	): Intent = error("Invalid")

	override fun registerReceiver(
		receiver: BroadcastReceiver?,
		filter: IntentFilter?,
		broadcastPermission: String?,
		scheduler: Handler?,
		flags: Int
	): Intent = error("Invalid")

	override fun unregisterReceiver(receiver: BroadcastReceiver?) = error("Invalid")

	override fun startService(service: Intent?): ComponentName = error("Invalid")

	override fun startForegroundService(service: Intent?): ComponentName = error("Invalid")

	override fun stopService(service: Intent?): Boolean = error("Invalid")

	override fun bindService(service: Intent?, conn: ServiceConnection, flags: Int): Boolean = error("Invalid")

	override fun bindService(
		service: Intent,
		flags: Int,
		executor: Executor,
		conn: ServiceConnection
	): Boolean = error("Invalid")

	override fun bindIsolatedService(
		service: Intent,
		flags: Int,
		instanceName: String,
		executor: Executor,
		conn: ServiceConnection
	): Boolean = error("Invalid")

	override fun bindServiceAsUser(
		service: Intent,
		conn: ServiceConnection,
		flags: Int,
		user: UserHandle
	): Boolean = error("Invalid")

	override fun updateServiceGroup(conn: ServiceConnection, group: Int, importance: Int) = error("Invalid")

	override fun unbindService(conn: ServiceConnection) = error("Invalid")

	override fun startInstrumentation(
		className: ComponentName,
		profileFile: String?,
		arguments: Bundle?
	): Boolean = error("Invalid")

	override fun getSystemService(name: String): Any = error("Invalid")

	override fun getSystemServiceName(serviceClass: Class<*>): String = error("Invalid")

	override fun checkPermission(permission: String, pid: Int, uid: Int): Int = error("Invalid")

	override fun checkCallingPermission(permission: String): Int = error("Invalid")

	override fun checkCallingOrSelfPermission(permission: String): Int = error("Invalid")

	override fun checkSelfPermission(permission: String): Int = error("Invalid")

	override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) = error("Invalid")

	override fun enforceCallingPermission(permission: String, message: String?) = error("Invalid")

	override fun enforceCallingOrSelfPermission(permission: String, message: String?) = error("Invalid")

	override fun grantUriPermission(toPackage: String?, uri: Uri?, modeFlags: Int) = error("Invalid")

	override fun revokeUriPermission(uri: Uri?, modeFlags: Int) = error("Invalid")

	override fun revokeUriPermission(toPackage: String?, uri: Uri?, modeFlags: Int) = error("Invalid")

	override fun checkUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int): Int = error("Invalid")

	override fun checkUriPermission(
		uri: Uri?,
		readPermission: String?,
		writePermission: String?,
		pid: Int,
		uid: Int,
		modeFlags: Int
	): Int = error("Invalid")

	override fun checkUriPermissions(
		uris: MutableList<Uri>,
		pid: Int,
		uid: Int,
		modeFlags: Int
	): IntArray = error("Invalid")

	override fun checkCallingUriPermission(uri: Uri?, modeFlags: Int): Int = error("Invalid")

	override fun checkCallingUriPermissions(uris: MutableList<Uri>, modeFlags: Int): IntArray = error("Invalid")

	override fun checkCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int): Int = error("Invalid")

	override fun checkCallingOrSelfUriPermissions(uris: MutableList<Uri>, modeFlags: Int): IntArray = error("Invalid")

	override fun enforceUriPermission(
		uri: Uri?,
		pid: Int,
		uid: Int,
		modeFlags: Int,
		message: String?
	) = error("Invalid")

	override fun enforceUriPermission(
		uri: Uri?,
		readPermission: String?,
		writePermission: String?,
		pid: Int,
		uid: Int,
		modeFlags: Int,
		message: String?
	) = error("Invalid")

	override fun enforceCallingUriPermission(uri: Uri?, modeFlags: Int, message: String?) = error("Invalid")

	override fun enforceCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int, message: String?) = error("Invalid")

	override fun createPackageContext(packageName: String?, flags: Int): Context = error("Invalid")

	override fun createContextForSplit(splitName: String?): Context = error("Invalid")

	override fun createConfigurationContext(overrideConfiguration: Configuration): Context = error("Invalid")

	override fun createDisplayContext(display: Display): Context = error("Invalid")

	override fun createWindowContext(type: Int, options: Bundle?): Context = error("Invalid")

	override fun createWindowContext(display: Display, type: Int, options: Bundle?): Context = error("Invalid")

	override fun createContext(contextParams: ContextParams): Context = error("Invalid")

	override fun createAttributionContext(attributionTag: String?): Context = error("Invalid")

	override fun createDeviceProtectedStorageContext(): Context = error("Invalid")

	override fun getDisplay(): Display = error("Invalid")

	override fun isRestricted(): Boolean = error("Invalid")

	override fun isDeviceProtectedStorage(): Boolean = error("Invalid")

	override fun isUiContext(): Boolean = error("Invalid")
}
