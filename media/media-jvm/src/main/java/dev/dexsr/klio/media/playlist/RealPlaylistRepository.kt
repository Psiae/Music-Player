package dev.dexsr.klio.media.playlist

import dev.dexsr.klio.media.db.realm.IncrementalRealmPrimaryKey
import dev.dexsr.klio.media.db.realm.RealmDB
import dev.dexsr.klio.media.db.realm.advance
import dev.dexsr.klio.media.db.realm.default
import dev.dexsr.klio.media.db.realm.of
import dev.dexsr.klio.media.db.realm.toInitial
import dev.dexsr.klio.media.playlist.realm.RealmPlaylist
import dev.dexsr.klio.media.playlist.realm.RealmPlaylistItem
import dev.dexsr.klio.media.playlist.realm.toOriginalType
import dev.dexsr.klio.media.playlist.realm.toRealmType
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking

const val DEBUG = false

/*
* TODO:
*  - efficient interface for small operation
*  - keep previous modified entry up to `n` amount
* */

/*
* fixme:
*  - catch exception in `write` block and close the mutable instance
* */

/*
* fixme (release):
*  - use gradle to change DEBUG property
*  */
class RealPlaylistRepository() : PlaylistRepository {

    private val coroutineScope = CoroutineScope(SupervisorJob())

    // TODO: we can conflate the observe
    override fun observeChanges(
        playlistId: String
    ): Flow<Playlist> {
        return RealmDB.realm.query(
            clazz = RealmPlaylist::class,
            query = "playlistId == $0",
            playlistId
        )
            .first()
            .asFlow()
            .mapNotNull { it.obj?.toOriginalType() }
    }

    override fun create(new: Playlist): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                val create = RealmDB.realm.write {
                    val playlistPrimaryKey = advanceIncrementalPrimaryKey("playlist")
                    val realmObj = RealmPlaylist(
                        _id = playlistPrimaryKey,
                        playlistId = playlistPrimaryKey,
                        snapshotId = "0",
                        contents = realmListOf(
                            *Array(new.contents.size) { i ->
                                val primaryKey = advanceIncrementalPrimaryKey("playlist_item")
                                val itemKey = advanceIncrementalPrimaryKey("playlist_${playlistPrimaryKey}_item")
                                val item = new.contents[i]
                                RealmPlaylistItem(
                                    _id = primaryKey,
                                    id = itemKey,
                                    contentId = item.contentId
                                )
                            }
                        ),
                        displayName = new.displayName,
                        ownerId = new.ownerId
                    )
                    copyToRealm(realmObj)
                }
                Result.success(create.toOriginalType())
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    // TODO custom return type
    override fun update(new: Playlist): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                val updated = RealmDB.realm.write {
                    val current = query(
                        clazz = RealmPlaylist::class,
                        query = "playlistId == $0",
                        new.id
                    ).first().find()
                        ?: run {
                            cancelWrite()
                            error("entry does not exist")
                        }
                    applyProposedUpdate(current, new)
                }
                Result.success(updated.toOriginalType())
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    override fun updateOrCreate(new: Playlist): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                RealmDB.realm.write {
                    val current = query(
                        clazz = RealmPlaylist::class,
                        query = "playlistId == $0",
                        new.id
                    ).first().find()
                    val update = current
                        ?.let {
                            applyProposedUpdate(
                                current,
                                new,
                            ).also {
                                if (DEBUG) {
                                    println("RealPlaylistRepository_updateOrCreate(playlistId=${it.playlistId}, snapshotId=${it.snapshotId}, displayName=${it.displayName}, ownerId=${it.ownerId}, contents=${it.contents.map { "(_id=${it._id}, id=${it.id}, contentId=${it.contentId})" }})")
                                }
                            }
                        }
                        ?: run {
                            val playlistPrimaryKey = advanceIncrementalPrimaryKey("playlist")
                            copyToRealm(
                                new.toRealmType(
                                    _id = playlistPrimaryKey,
                                    snapshotId = "0",
                                    contents = realmListOf(*Array(new.contents.size) {
                                        val element = new.contents[it]
                                        RealmPlaylistItem(
                                            advanceIncrementalPrimaryKey("playlist_item"),
                                            element.id.ifEmpty { advanceIncrementalPrimaryKey("playlist_${playlistPrimaryKey}_item") } ,
                                            element.contentId
                                        )
                                    })
                                )
                            )
                        }
                    Result.success(copyFromRealm(update).toOriginalType())
                }
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    override fun updateOrCreate(
        playlistId: String,
        update: suspend () -> Playlist
    ): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                RealmDB.realm.write {
                    val current = query(
                        clazz = RealmPlaylist::class,
                        query = "playlistId == $0",
                        playlistId
                    ).first().find()
                    val updated = current
                        ?.let {
                            applyProposedUpdate(
                                current,
                                runBlocking { update() },
                            )
                        }
                        ?: run {
                            val new = runBlocking { update() }
                            val playlistPrimaryKey = advanceIncrementalPrimaryKey("playlist")
                            copyToRealm(
                                new.toRealmType(
                                    _id = playlistPrimaryKey,
                                    snapshotId = "0",
                                    contents = realmListOf(*Array(new.contents.size) {
                                        val element = new.contents[it]
                                        RealmPlaylistItem(
                                            advanceIncrementalPrimaryKey("playlist_item"),
                                            element.id.ifEmpty { advanceIncrementalPrimaryKey("playlist_${playlistPrimaryKey}_item") } ,
                                            element.contentId
                                        )
                                    })
                                )
                            )
                        }
                    Result.success(copyFromRealm(updated).toOriginalType())
                }
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    override fun get(
        id: String
    ): Deferred<Result<Playlist?>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                val get = RealmDB.realm
                    .query(
                        clazz = RealmPlaylist::class,
                        query = "playlistId == $0",
                        id
                    )
                    .first().find()
                    ?: return@async Result.success(null)
                Result.success(get.toOriginalType())
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    override fun getOrCreate(
        id: String,
        create: suspend () -> Playlist
    ): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            try {
                val realm = RealmDB.realm
                realm.write {
                    val get = RealmDB.realm
                        .query(
                            clazz = RealmPlaylist::class,
                            query = "playlistId == $0",
                            id
                        )
                        .first().find()
                    if (get != null) {
                        cancelWrite()
                        return@write Result.success(realm.copyFromRealm(get).toOriginalType())
                    }
                    val put = run {
                        val new = runBlocking { create() }
                        val playlistPrimaryKey = advanceIncrementalPrimaryKey("playlist")
                        copyToRealm(
                            new.toRealmType(
                                _id = playlistPrimaryKey,
                                snapshotId = "0",
                                contents = realmListOf(*Array(new.contents.size) {
                                    val element = new.contents[it]
                                    RealmPlaylistItem(
                                        advanceIncrementalPrimaryKey("playlist_item"),
                                        element.id.ifEmpty { advanceIncrementalPrimaryKey("playlist_${playlistPrimaryKey}_item") } ,
                                        element.contentId
                                    )
                                })
                            )
                        )
                    }
                    Result.success(put.toOriginalType())
                }
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    // only Ideal for possibly large dataset changes
    private fun MutableRealm.applyProposedUpdate(
        live: RealmPlaylist,
        update: Playlist,
    ): RealmPlaylist {

        var liveChanged = false

        // only Ideal for really large dataset changes
        /*val mut = copyFromRealm(live)*/

        val appends = mutableListOf<RealmPlaylistItem>()
        val appendsIndexMapping = mutableMapOf<String, Int>()
        val elements = mutableListOf<RealmPlaylistItem>()
        val elementsIndexMapping = mutableMapOf<String, Int>()
        // fixme
        run {
            val mightBeMoved = mutableMapOf<String, RealmPlaylistItem>()
            val mightBeMovedIndexMapping = mutableMapOf<String, Int>()
            var pastLiveElements = live.contents.isEmpty()
            var pastLiveElementsIndex = live.contents.lastIndex
            repeat(update.contents.size) { i ->
                if (pastLiveElements) {
                    liveChanged = true
                    val upElement = update.contents[i]
                    if (upElement.id == "") {
                        appends.add(upElement.toRealmType())
                        appendsIndexMapping[upElement.id] = i - pastLiveElementsIndex - 1
                        return@repeat
                    }
                    mightBeMoved.remove(upElement.id)
                        ?.also { mightBeMovedIndexMapping.remove(upElement.id) }
                        ?.let { it.contentId = upElement.contentId ; appends.add(it) ; appendsIndexMapping[it.id] = i - pastLiveElementsIndex - 1 }
                        ?: run {
                            upElement.toRealmType(
                                id = query(
                                    RealmPlaylistItem::class,
                                    "id == $0",
                                    upElement.id
                                ).first().find()?.id ?: upElement.id
                            ).let { appends.add(it) ; appendsIndexMapping[it.id] = i - pastLiveElementsIndex - 1 }
                        }
                    return@repeat
                }
                val upElement = update.contents[i]
                val liveElement = live.contents[i]
                pastLiveElements = i == live.contents.lastIndex
                if (upElement.id == "") {
                    mightBeMoved[liveElement.id] = liveElement
                    mightBeMovedIndexMapping[liveElement.id] = i
                    elements.add(upElement.toRealmType())
                    elementsIndexMapping[upElement.id] = i
                    liveChanged = true
                } else {
                    val removedMightBeMoved = mightBeMoved.remove(upElement.id)
                        ?.also { mightBeMovedIndexMapping.remove(upElement.id) }
                    if (removedMightBeMoved != null) {
                        removedMightBeMoved.contentId = upElement.contentId
                        elements.add(upElement.toRealmType(
                            _id = removedMightBeMoved._id
                        ))
                        liveChanged = true
                    } else if (liveElement.id != upElement.id) {
                        mightBeMoved[liveElement.id] = liveElement
                        mightBeMovedIndexMapping[liveElement.id] = i
                        // fixme if the Id is still present, don't advance the key
                        val element = query(
                            RealmPlaylistItem::class,
                            "id == $0",
                            upElement.id
                        ).first().find()
                            ?.apply { contentId = upElement.contentId }
                            ?: upElement.toRealmType()
                        elements.add(element)
                        liveChanged = true
                    } else if (liveElement.contentId != upElement.contentId) {
                        liveElement.contentId = upElement.contentId
                        liveChanged = true
                        elements.add(liveElement)
                    } else {
                        elements.add(liveElement)
                    }
                    elementsIndexMapping[upElement.id] = i
                }
            }
        }

        val toDelete = if (liveChanged) {
            live.contents.filter { item ->
                !elementsIndexMapping.contains(item.id) && !appendsIndexMapping.contains(item.id)
            }
        } else {
            emptyList()
        }
        live.contents = realmListOf(
            *Array(update.contents.size) {
                val upElement = update.contents[it]
                val element = elementsIndexMapping[upElement.id]
                    ?.let { index -> elements[index] }
                    ?: checkNotNull(appendsIndexMapping[upElement.id]) {
                        "update element is not present in either elements or appends"
                    }.let { index -> appends[index] }
                val primaryKey =
                    if (element._id == "") advanceIncrementalPrimaryKey("playlist_item")
                    else element._id
                val id =
                    if (element.id == "") advanceIncrementalPrimaryKey("playlist_${live.playlistId}_item")
                    else element.id
                RealmPlaylistItem(
                    primaryKey,
                    id,
                    element.contentId
                )
            }
        )

        if (live.displayName != update.displayName) {
            liveChanged = true
            live.displayName = update.displayName
        }

        if (liveChanged) {
            live.snapshotId = live.snapshotId.toLong().inc().toString()
        }

        toDelete.forEach { delete(it) }

        return live
    }

    private fun MutableRealm.advanceIncrementalPrimaryKey(): String {
        return advanceIncrementalPrimaryKey(IncrementalRealmPrimaryKey.default())
    }

    private fun MutableRealm.advanceIncrementalPrimaryKey(name: String): String {
        return advanceIncrementalPrimaryKey(IncrementalRealmPrimaryKey.of(name))
    }

    fun MutableRealm.advanceIncrementalPrimaryKey(schema: IncrementalRealmPrimaryKey): String {
        val live = query<IncrementalRealmPrimaryKey>(
            "_id == $0",
            schema._id
        ).first().find()
            ?: copyToRealm(schema.toInitial())
        return live.advance().incrementValue
    }

    override fun dispose() {
        coroutineScope.cancel()
    }
}