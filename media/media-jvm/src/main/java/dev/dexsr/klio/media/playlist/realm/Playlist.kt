package dev.dexsr.klio.media.playlist.realm

import dev.dexsr.klio.media.playlist.Playlist
import dev.dexsr.klio.media.playlist.PlaylistItem
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

class RealmPlaylist(

) : RealmObject {

    @PrimaryKey
    var _id: String = ""
    var playlistId: String = ""
    var snapshotId: String = ""
    var contents: RealmList<RealmPlaylistItem> = realmListOf()
    var displayName: String = ""
    var ownerId: String = ""

    constructor(
        _id: String = "",
        playlistId: String,
        snapshotId: String,
        contents: List<PlaylistItem>,
        displayName: String,
        ownerId: String
    ) : this(
        _id,
        playlistId,
        snapshotId,
        realmListOf(*Array(contents.size) { RealmPlaylistItem(contents[it]) }),
        displayName,
        ownerId
    )

    constructor(
        _id: String = "",
        playlistId: String,
        snapshotId: String,
        contents: RealmList<RealmPlaylistItem>,
        displayName: String,
        ownerId: String
    ) : this() {
        this._id = _id
        this.playlistId = playlistId
        this.snapshotId = snapshotId
        this.contents = contents
        this.displayName = displayName
        this.ownerId = ownerId
    }

    fun copy(
        _id: String = this._id,
        playlistId: String = this.playlistId,
        snapshotId: String = this.snapshotId,
        contents: RealmList<RealmPlaylistItem> = this.contents,
        displayName: String = this.displayName,
        ownerId: String = this.ownerId
    ): RealmPlaylist = RealmPlaylist(_id, playlistId, snapshotId, contents, displayName, ownerId)
}

class RealmPlaylistItem(

) : RealmObject {

    @PrimaryKey
    var _id = ""

    // id relative to the [playlistId]
    var id = ""

    // actual content of this item
    var contentId: String = ""

    // playlist this instance belongs to
    var playlistId: String = ""

    // index of this item on the [playlistId]
    var index: Int = -1

    constructor(obj: PlaylistItem) : this(id = obj.id, contentId = obj.contentId)

    constructor(_id: String, obj: PlaylistItem) : this(_id = _id, id = obj.id, contentId = obj.contentId)

    constructor(
        _id: String = "",
        id: String = "",
        contentId: String = "",
        playlistId: String = "",
        index: Int = -1
    ) : this() {
        this._id = _id
        this.id = id
        this.contentId = contentId
        this.playlistId = playlistId
        this.index = index
    }

    override fun equals(other: Any?): Boolean {
        return other is RealmPlaylistItem && other._id == _id && other.id == id && other.contentId == contentId
    }

    override fun hashCode(): Int {
        var hash = _id.hashCode()
        hash *= 31 ; hash += id.hashCode()
        hash *= 31 ; hash += contentId.hashCode()
        return hash
    }
}

fun RealmPlaylist.toOriginalType(
    playlistId: String = this.playlistId,
    snapshotId: String = this.snapshotId,
    contentCount: Int = this.contents.size,
    displayName: String = this.displayName,
    ownerId: String = this.ownerId
): Playlist = Playlist(
    id = playlistId,
    snapshotId = snapshotId,
    contentCount = contentCount,
    displayName = displayName,
    ownerId = ownerId
)

fun Playlist.toRealmType(
    _id: String = "",
    playlistId: String = this.id,
    snapshotId: String = this.snapshotId,
    contents: RealmList<RealmPlaylistItem> = realmListOf(),
    displayName: String = this.displayName,
    creatorId: String = this.ownerId
): RealmPlaylist = RealmPlaylist(
    _id,
    playlistId,
    snapshotId,
    contents,
    displayName,
    creatorId,
)

fun RealmPlaylistItem.toOriginalType(
    id: String = this.id,
    contentId: String = this.contentId
): PlaylistItem = PlaylistItem(id = id, contentId = contentId)

fun PlaylistItem.toRealmType(
    playlistId: String,
    index: Int = -1,
    id: String = this.id,
    contentId: String = this.contentId
) = toRealmType("", playlistId, index, id, contentId)

fun PlaylistItem.toRealmType(
    _id: String,
    playlistId: String,
    index: Int = -1,
    id: String = this.id,
    contentId: String = this.contentId,
) = RealmPlaylistItem(_id, id, contentId, playlistId, index)