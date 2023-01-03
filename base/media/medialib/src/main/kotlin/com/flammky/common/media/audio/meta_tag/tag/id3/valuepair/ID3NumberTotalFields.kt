package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import java.util.*

/**
 * Created by Paul on 09/11/2016.
 */
object ID3NumberTotalFields {
	private val numberField = EnumSet.noneOf(
		FieldKey::class.java
	)
	private val totalField = EnumSet.noneOf(
		FieldKey::class.java
	)

	init {
		numberField.add(FieldKey.TRACK)
		numberField.add(FieldKey.DISC_NO)
		numberField.add(FieldKey.MOVEMENT_NO)
		totalField.add(FieldKey.TRACK_TOTAL)
		totalField.add(FieldKey.DISC_TOTAL)
		totalField.add(FieldKey.MOVEMENT_TOTAL)
	}

	fun isNumber(fieldKey: FieldKey): Boolean {
		return numberField.contains(fieldKey)
	}

	fun isTotal(fieldKey: FieldKey): Boolean {
		return totalField.contains(fieldKey)
	}
}
