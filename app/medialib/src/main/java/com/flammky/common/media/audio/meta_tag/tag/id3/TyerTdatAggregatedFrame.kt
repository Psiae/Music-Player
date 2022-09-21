package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.FrameBodyTDAT

/**
 * For use in ID3 for mapping YEAR field to TYER and TDAT Frames
 */
class TyerTdatAggregatedFrame : AggregatedFrame() {

	override var content: String?
		get() {
			val sb = StringBuilder()
			val i: Iterator<AbstractID3v2Frame> = mFrames.iterator()
			val tyer = i.next()
			sb.append(tyer.content)
			val tdat = i.next()
			if (tdat.content!!.length == FrameBodyTDAT.DATA_SIZE) {
				sb.append("-")
				sb.append(
					tdat.content!!.substring(
						FrameBodyTDAT.MONTH_START,
						FrameBodyTDAT.MONTH_END
					)
				)
				if (!(tdat.body as FrameBodyTDAT?)!!.isMonthOnly) {
					sb.append("-")
					sb.append(
						tdat.content!!.substring(
							FrameBodyTDAT.DAY_START,
							FrameBodyTDAT.DAY_END
						)
					)
				}
			}
			return sb.toString()
		}
		set(content) {
			super.content = content
		}

	companion object {
		const val ID_TYER_TDAT = ID3v23Frames.FRAME_ID_V3_TYER + ID3v23Frames.FRAME_ID_V3_TDAT
	}
}
