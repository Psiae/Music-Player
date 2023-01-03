package com.flammky.musicplayer.base.media.mediaconnection

import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.mediaconnection.tracks.TracksConnection

interface MediaConnection {
	val tracks: TracksConnection
	val playback : PlaybackConnection
}
