package com.kylentt.musicplayer.common.media.audio.meta_tag.audio.iff

class PaddingChunkSummary(fileStartLocation: Long, chunkSize: Long) :
	ChunkSummary("    ", fileStartLocation, chunkSize)
