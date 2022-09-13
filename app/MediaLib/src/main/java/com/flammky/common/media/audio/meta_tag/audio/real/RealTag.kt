package com.kylentt.musicplayer.common.media.audio.meta_tag.audio.real

import com.kylentt.musicplayer.common.media.audio.meta_tag.audio.generic.GenericTag
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.KeyNotFoundException
import com.kylentt.musicplayer.common.media.audio.meta_tag.tag.TagField

class RealTag : GenericTag() {
	override fun toString(): String {
		return "REAL " + super.toString()
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return createField(FieldKey.IS_COMPILATION, value.toString())
	}
}
