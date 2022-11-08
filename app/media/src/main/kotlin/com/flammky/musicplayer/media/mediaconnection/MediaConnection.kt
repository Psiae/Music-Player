package com.flammky.musicplayer.media.mediaconnection

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.media.mediaconnection.tracks.TracksConnection

interface MediaConnection {
	val tracks: TracksConnection
	val playback : PlaybackConnection
}
