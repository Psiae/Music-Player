package com.flammky.musicplayer.common.media.audio.meta_tag.audio.real

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.KeyNotFoundException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField

class RealTag : GenericTag() {
	override fun toString(): String {
		return "REAL " + super.toString()
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}
}
