package dev.dexsr.klio.media.playlist.realm.paging

import io.realm.kotlin.ext.isManaged
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class RealmPlaylistContentBucket(

) : RealmObject {

    @PrimaryKey
    var _id: String = ""
    var playlistId: String = ""
    var playlistSnapshotId: String = ""
    var contents: RealmList<String> = realmListOf()

    // the max size of the bucket
    var bucketSize: Int = -1
    var bucketSnapshotId: Long = -1
    var bucketIndex: Int = -1

    // the slot this bucket occupies
    // bucketSlot: Int = -1

    // the first index this bucket contains on the playlist
    var bucketFirstIndex = -1
    var bucketLastIndex = -1

    companion object {
        val UNSET = RealmPlaylistContentBucket()
    }
}

fun RealmPlaylistContentBucket.addContent(
    content: String
) {
    requireNotUnset {
        "cannot addContent to UNSET RealmPlaylistContentBucket"
    }
    require(contents.size < bucketSize) {
        "cannot add Content to FULL RealmPlaylistContentBucket"
    }
    contents.add(content)
    bucketLastIndex = bucketFirstIndex + contents.lastIndex
}

fun RealmPlaylistContentBucket.advanceSnapshotId(

) {
    bucketSnapshotId++
}

fun RealmPlaylistContentBucket.requireNotUnset(lazyMsg: () -> Any) {
    require(this !== RealmPlaylistContentBucket.UNSET, lazyMsg)
}

fun RealmPlaylistContentBucket(
    _id: String,
    playlistId: String,
    playlistSnapshotId: String,
    bucketSize: Int,
    bucketIndex: Int,
    bucketFirstIndex: Int
) = RealmPlaylistContentBucket().apply {
    this._id = _id
    this.playlistId = playlistId
    this.playlistSnapshotId = playlistSnapshotId
    this.bucketSize = bucketSize
    this.bucketIndex = bucketIndex
    this.bucketFirstIndex = bucketFirstIndex
}