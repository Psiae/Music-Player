package dev.dexsr.klio.media

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import dev.dexsr.klio.media.db.realm.RealmDB

class MediaAndroidInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        RealmDB.provides(context.filesDir.canonicalPath)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}