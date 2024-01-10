package dev.dexsr.klio.media.db.realm

import com.flammky.kotlin.common.lazy.LazyConstructor
import dev.dexsr.klio.media.playlist.realm.RealmPlaylist
import dev.dexsr.klio.media.playlist.realm.RealmPlaylistItem
import dev.dexsr.klio.media.playlist.realm.paging.RealmPlaylistContentBucket
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.atomicfu.atomic

object RealmDB {
    private val INSTANCE = LazyConstructor<Realm>()
    private val _provided = atomic(false)
    private var storagePath: String? = null

    const val DEBUG_SCHEMA_VERSION = Long.MAX_VALUE

    // note: delete the realm folder if there's schema conflict
    val realm
        get() = INSTANCE.construct {
            if (!_provided.value) {
                error("Media RealmDB configuration wasn't provided")
            }
            val config = RealmConfiguration
                .Builder(
                    setOf(
                        RealmPlaylist::class,
                        RealmPlaylistItem::class,
                        IncrementalRealmPrimaryKey::class,
                        RealmPlaylistContentBucket::class
                    )
                )
                .name("realm-media")
                .directory("$storagePath/media/realm/")
                .schemaVersion(DEBUG_SCHEMA_VERSION)
                .build()
            Realm.open(config)
        }

    fun provides(
        storagePath: String
    ): Boolean {
        if (!_provided.compareAndSet(false, true)) {
            return false
        }
        this.storagePath = storagePath
        return true
    }
}