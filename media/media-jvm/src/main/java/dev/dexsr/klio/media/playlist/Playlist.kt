package dev.dexsr.klio.media.playlist

class Playlist(
    val id: String,
    val snapshotId: String,
    val contentCount: Int,
    val displayName: String,
    val ownerId: String
) {

    fun copy(
        id: String = this.id,
        snapshotId: String = this.snapshotId,
        contentCount: Int,
        displayName: String = this.displayName,
        creatorId: String = this.ownerId
    ): Playlist = Playlist(id, snapshotId, contentCount, displayName, creatorId)

    override fun equals(other: Any?): Boolean {
        if (other !is Playlist) return false

        return id == other.id && snapshotId == other.snapshotId && contentCount == other.contentCount &&
                displayName == other.displayName && ownerId == other.ownerId
    }

    override fun hashCode(): Int {
        var hash = id.hashCode()
        hash *= 31 ; hash += snapshotId.hashCode()
        hash *= 31 ; hash += contentCount.hashCode()
        hash *= 31 ; hash += displayName.hashCode()
        hash *= 31 ; hash += ownerId.hashCode()
        return hash
    }
}

class PlaylistItem(
    // Id of this Item, relative to the playlist
    val id: String,
    val contentId: String
) {

    override fun equals(other: Any?): Boolean {
        if (other !is PlaylistItem) return false
        return id == other.id && contentId == other.contentId
    }

    override fun hashCode(): Int {
        var hash = id.hashCode()
        hash *= 31 + contentId.hashCode()
        return hash
    }
}