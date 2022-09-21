package com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff

class BadChunkSummary(fileStartLocation: Long, chunkSize: Long) :
	ChunkSummary("BAD-DATA", fileStartLocation, chunkSize)
