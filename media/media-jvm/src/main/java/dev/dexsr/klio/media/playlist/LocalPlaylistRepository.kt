package dev.dexsr.klio.media.playlist

import dev.dexsr.klio.media.db.realm.IncrementalRealmPrimaryKey
import dev.dexsr.klio.media.db.realm.RealmDB
import dev.dexsr.klio.media.db.realm.advance
import dev.dexsr.klio.media.db.realm.default
import dev.dexsr.klio.media.db.realm.of
import dev.dexsr.klio.media.db.realm.toInitial
import dev.dexsr.klio.media.playlist.paging.PlaylistContentBucket
import dev.dexsr.klio.media.playlist.realm.PlaylistSynchronizeData
import dev.dexsr.klio.media.playlist.realm.RealmPlaylist
import dev.dexsr.klio.media.playlist.realm.RealmPlaylistItem
import dev.dexsr.klio.media.playlist.realm.paging.RealmPlaylistContentBucket
import dev.dexsr.klio.media.playlist.realm.paging.addContent
import dev.dexsr.klio.media.playlist.realm.paging.advanceSnapshotId
import dev.dexsr.klio.media.playlist.realm.toOriginalType
import dev.dexsr.klio.media.playlist.realm.toRealmPlaylist
import dev.dexsr.klio.media.playlist.realm.toRealmType
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.query.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.ceil

const val DEBUG = true

/*
* TODO:
*  - efficient interface for small operation
*  - keep previous modified entry up to `n` amount
*  - query builder extension function
* */

/*
* fixme:
*  - catch exception in `write` block and close the mutable instance
* */

/*
* fixme (release):
*  - use gradle to change DEBUG property
*  */
class LocalPlaylistRepository() : PlaylistRepository {

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

    // fixme: magic literal
    // fixme: there's a bug in synchronizeEntity
    override fun synchronizeOrCreate(
        synchronizeData: PlaylistSynchronizeData
    ): Deferred<Result<Playlist>> {
        return coroutineScope.async(Dispatchers.IO) {
            if (synchronizeData.playlistId.isBlank()) {
                return@async Result.failure(IllegalArgumentException("playlistId cannot be blank"))
            }
            try {
                val result = RealmDB.realm.write {
                    val current = query(
                        clazz = RealmPlaylist::class,
                        query = "playlistId == $0",
                        synchronizeData.playlistId
                    ).first().find()
                    current
                        ?.let {
                            synchronizeEntity(
                                current,
                                synchronizeData,
                            )
                        }
                        ?: run {
                            val playlistPrimaryKey = advanceIncrementalPrimaryKey("playlist")
                            val playlist = synchronizeData.toRealmPlaylist(
                                primaryKey = playlistPrimaryKey,
                                snapshotId = "0",
                                contents = realmListOf(*Array(synchronizeData.contents.size) { i ->
                                    val element = synchronizeData.contents[i]
                                    RealmPlaylistItem(
                                        _id = advanceIncrementalPrimaryKey("playlist_item"),
                                        // fixme: check for duplicate
                                        id = element.id.ifBlank {
                                            advanceIncrementalPrimaryKey("playlist_${playlistPrimaryKey}_item")
                                        },
                                        contentId = element.contentId,
                                        playlistId = synchronizeData.playlistId,
                                        index = i
                                    )
                                })
                            )
                            copyToRealm(playlist)
                        }
                }
                Result.success(result.toOriginalType())
            } catch (ex: Exception) {
                if (DEBUG) ex.printStackTrace()
                Result.failure(ex)
            }
        }
    }

    override suspend fun getPagedPlaylistItems(
        playlistId: String,
        playlistSnapshotId: String,
        offset: Int,
        limit: Int
    ): Result<PlaylistContentBucket> {
        println("PlaylistDetailLazyLayoutState_DEBUG: getPagedPlaylistItem($playlistId, $playlistSnapshotId, $offset, $limit)")
        return runCatching {
            require(offset >= 0) {
                "offset must be at least 0"
            }
            require(limit >= 0) {
                "limit must be at least 0"
            }
            require(limit <= 50) {
                "limit must be at most 50"
            }
            val contents = RealmDB.realm
                .query(
                    RealmPlaylistContentBucket::class,
                    "playlistId == $0 AND playlistSnapshotId == $1",
                    playlistId, playlistSnapshotId
                )
                .query("bucketFirstIndex >= $0", offset)
                .query("bucketLastIndex <= $0", offset + limit)
                .sort("bucketIndex")
                .find()
                .let { rr ->
                    val buckets = if (rr.isEmpty()) {
                        generatePlaylistBucketsAndGet(playlistId, playlistSnapshotId, offset, limit)
                    } else {
                        rr
                    }

                    if (buckets.isEmpty()) {
                        return@let emptyList()
                    }
                    val firstBucket = buckets.first()
                    val truncatedFirstBucket = firstBucket.contents
                        .let { if (buckets.size == 1) it else it.subList(offset - firstBucket.bucketFirstIndex, firstBucket.contents.size) }
                        .take(limit)
                    if (buckets.size == 1) {
                        return@let truncatedFirstBucket
                    }
                    val lastBucket = buckets.last()
                    if (buckets.size == 2) {
                       return@let ArrayList<String>(limit)
                           .apply {
                               addAll(truncatedFirstBucket)
                               repeat(kotlin.math.min(limit - truncatedFirstBucket.size, lastBucket.contents.size)) {
                                   add(lastBucket.contents[it])
                               }
                           }
                    }
                    val arr = ArrayList<String>(limit)
                    run {
                        truncatedFirstBucket.forEach { arr.add(it) }
                        buckets.subList(1, buckets.size).forEach { bucket -> bucket.contents.forEach {
                            if (arr.size == limit) return@run
                            arr.add(it)
                        } }
                    }
                    arr
                }
            check(contents.size <= limit)
            PlaylistContentBucket(playlistId, playlistSnapshotId, contents)
        }
    }

    // try to generate the buckets
    private suspend fun generatePlaylistBucketsAndGet(
        playlistId: String,
        playlistSnapshotId: String,
        offset: Int,
        limit: Int
    ): List<RealmPlaylistContentBucket> {

        // fixme
        val intervalSize = 10

        require(offset >= 0) {
            "offset must be at least 0"
        }
        require(limit >= 0) {
            "limit must be at least 0"
        }

        if (limit == 0) return emptyList()

        return RealmDB.realm.write {
            // the maximum amount of buckets
            val interval = ceil((limit / intervalSize.toFloat())).toInt()
            val find = this
                .query(
                    RealmPlaylistContentBucket::class,
                    "playlistId == $0 AND playlistSnapshotId == $1",
                    playlistId, playlistSnapshotId
                )
                .query("bucketFirstIndex == $0", offset / intervalSize * intervalSize)
                .query("bucketSize == $0", intervalSize)
                .sort("bucketIndex")
                .limit(interval)
                .find()
            val toFillBucket = find.lastOrNull()
            val toFillBucketContentsSize = toFillBucket?.contents?.size
            if (toFillBucket == null) {
                val itemsToQuery = limit
                this
                    .query(
                        RealmPlaylistItem::class,
                        "playlistId == $0",
                        playlistId
                    )
                    .query("index >= $0", offset)
                    .sort("index")
                    .limit(itemsToQuery)
                    .find()
                    .let { items ->
                        // TODO: fix bucketIndex
                        println("media.DEBUG: generatePlaylistBuckets_noLastBucket_items=$items")
                        val count = items.size
                        if (count == 0) return@write emptyList()
                        val bucketToAllocate = count / intervalSize + 1
                        val buckets = ArrayList<RealmPlaylistContentBucket>(bucketToAllocate)
                        var cBucket = RealmPlaylistContentBucket()
                        items.forEachIndexed { index, realmPlaylistItem ->
                            if (index % intervalSize == 0) {
                                cBucket = RealmPlaylistContentBucket(
                                    _id = advanceIncrementalPrimaryKey("playlist_bucket"),
                                    playlistId = playlistId,
                                    playlistSnapshotId = playlistSnapshotId,
                                    bucketSize = intervalSize,
                                    bucketIndex = index / intervalSize,
                                    bucketFirstIndex = realmPlaylistItem.index
                                )
                                buckets.add(cBucket)
                            }
                            cBucket.addContent(realmPlaylistItem.id)
                        }
                        buckets.forEach(RealmPlaylistContentBucket::advanceSnapshotId)
                        val nearestBucketIndex = query(
                            RealmPlaylistContentBucket::class,
                            "bucketLastIndex < $0",
                            buckets.first().bucketFirstIndex
                        ).max<Int>("bucketIndex").find()
                        if (nearestBucketIndex != null) {
                            buckets.forEach {
                               it.bucketIndex = nearestBucketIndex + 1 + it.bucketIndex
                            }
                        }
                        buckets.forEach { copyToRealm(it) }
                        buckets
                    }
            } else if (toFillBucketContentsSize!! < intervalSize) {
                val itemsToQuery = limit - find.size
                this
                    .query(
                        RealmPlaylistItem::class,
                        "playlistId == $0"/* AND playlistSnapshotId == $1"*/,
                        playlistId, /*playlistSnapshotId*/
                    )
                    .query("index > $0", toFillBucket.bucketLastIndex)
                    .sort("index")
                    .limit(itemsToQuery)
                    .find()
                    .let { items ->
                        val count = items.size
                        if (count == 0) return@write copyFromRealm(listOf(toFillBucket))
                        val newBucketToAllocate = (count - (intervalSize - toFillBucketContentsSize)) / intervalSize
                        val newBuckets = ArrayList<RealmPlaylistContentBucket>(newBucketToAllocate)
                        var cBucket: RealmPlaylistContentBucket = toFillBucket
                        items.forEachIndexed { index, realmPlaylistItem ->
                            if (index < toFillBucket.bucketSize) {
                                toFillBucket.addContent(realmPlaylistItem.id)
                                if (index == toFillBucket.bucketSize - 1) {
                                    toFillBucket.advanceSnapshotId()
                                }
                                return@forEachIndexed
                            }
                            if (index % intervalSize == 0) {
                                cBucket = RealmPlaylistContentBucket(
                                    _id = advanceIncrementalPrimaryKey("playlist_bucket"),
                                    playlistId = playlistId,
                                    playlistSnapshotId = playlistSnapshotId,
                                    bucketSize = intervalSize,
                                    bucketIndex = toFillBucket.bucketIndex + (index / intervalSize),
                                    bucketFirstIndex = cBucket.bucketLastIndex + index
                                )
                                newBuckets.add(cBucket)
                            }
                            cBucket.addContent(realmPlaylistItem.id)
                        }
                        newBuckets.forEach(RealmPlaylistContentBucket::advanceSnapshotId)
                        newBuckets.forEach { copyFromRealm(copyToRealm(it)) }
                        newBuckets.apply { add(0, copyFromRealm(toFillBucket)) }
                    }
            } else if (toFillBucket.contents.size == interval) {
                listOf(copyFromRealm(toFillBucket))
            } else {
                error("unexpected buckets condition")
            }
        }
    }

    // only Ideal for possibly large dataset changes

    // TODO: delete buckets on change
    private fun MutableRealm.synchronizeEntity(
        live: RealmPlaylist,
        update: PlaylistSynchronizeData,
    ): RealmPlaylist {

        val updateContents = update.contents

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
            repeat(updateContents.size) { i ->
                if (pastLiveElements) {
                    liveChanged = true
                    val upElement = updateContents[i]
                    if (upElement.id == "") {
                        appends.add(upElement.toRealmType(update.playlistId))
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
                                    upElement.id,
                                ).first().find()?.id ?: upElement.id,
                                playlistId = update.playlistId
                            ).let { appends.add(it) ; appendsIndexMapping[it.id] = i - pastLiveElementsIndex - 1 }
                        }
                    return@repeat
                }
                val upElement = updateContents[i]
                val liveElement = live.contents[i]
                pastLiveElements = i == live.contents.lastIndex
                if (upElement.id == "") {
                    mightBeMoved[liveElement.id] = liveElement
                    mightBeMovedIndexMapping[liveElement.id] = i
                    elements.add(upElement.toRealmType(update.playlistId))
                    elementsIndexMapping[upElement.id] = i
                    liveChanged = true
                } else {
                    val removedMightBeMoved = mightBeMoved.remove(upElement.id)
                        ?.also { mightBeMovedIndexMapping.remove(upElement.id) }
                    if (removedMightBeMoved != null) {
                        removedMightBeMoved.contentId = upElement.contentId
                        elements.add(upElement.toRealmType(
                            _id = removedMightBeMoved._id,
                            playlistId = update.playlistId
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
                            ?: upElement.toRealmType(update.playlistId)
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
            *Array(updateContents.size) { i ->
                val upElement = updateContents[i]
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
                    element.contentId,
                    index = i,
                    playlistId = update.playlistId
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