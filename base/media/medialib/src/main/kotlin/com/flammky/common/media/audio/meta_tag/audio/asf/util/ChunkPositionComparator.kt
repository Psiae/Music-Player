/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import java.io.Serializable

/**
 * This class is needed for ordering all types of
 * [Chunk]s ascending by their Position. <br></br>
 *
 * @author Christian Laireiter
 */
class ChunkPositionComparator : Comparator<Chunk>, Serializable {
	/**
	 * {@inheritDoc}
	 */
	override fun compare(first: Chunk, second: Chunk): Int {
		return java.lang.Long.valueOf(first.position).compareTo(second.position)
	}

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -6337108235272376289L
	}
}
