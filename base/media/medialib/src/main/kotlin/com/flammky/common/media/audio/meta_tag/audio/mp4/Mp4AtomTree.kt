package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NullBoxIdException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4MetaBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4StcoBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.NullPadding
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree.DefaultMutableTreeNode
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree.DefaultTreeModel
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasOreo
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.logging.Logger

/**
 * Tree representing atoms in the mp4 file
 *
 * Note it doesn't create the complete tree it delves into subtrees for atom we know about and are interested in. (Note
 * it would be impossible to create a complete tree for any file without understanding all the nodes because
 * some atoms such as meta contain data and children and therefore need to be specially preprocessed)
 *
 * This class is currently only used when writing tags because it better handles the difficulties of mdat and free
 * atoms being optional/multiple places then the older sequential method. It is expected this class will eventually
 * be used when reading tags as well.
 *
 * Uses a TreeModel for the tree, with convenience methods holding onto references to most common nodes so they
 * can be used without having to traverse the tree again.
 */
class Mp4AtomTree {

	private var rootNode: DefaultMutableTreeNode<*>? = null

	/**
	 *
	 * @return
	 */
	var dataTree: DefaultTreeModel<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var moovNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var mdatNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var ilstNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var metaNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var tagsNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var udtaNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var hdlrWithinMdiaNode: DefaultMutableTreeNode<*>? = null
		private set

	/**
	 *
	 * @return
	 */
	var hdlrWithinMetaNode: DefaultMutableTreeNode<*>? = null
		private set
	private val stcoNodes: MutableList<DefaultMutableTreeNode<*>> = ArrayList()
	private val freeNodes: MutableList<DefaultMutableTreeNode<*>> = ArrayList()
	private val mdatNodes: MutableList<DefaultMutableTreeNode<*>> = ArrayList()
	private val trakNodes: MutableList<DefaultMutableTreeNode<*>> = ArrayList()
	private val stcos: MutableList<Mp4StcoBox> = ArrayList()

	/**
	 *
	 * @return
	 */
	var moovBuffer //Contains all the data under moov
		: ByteBuffer? = null
		private set

	/**
	 *
	 * @return
	 */
	var moovHeader: Mp4BoxHeader? = null
		private set

	/**
	 * Create Atom Tree
	 *
	 * @param  fc
	 * @throws IOException
	 * @throws CannotReadException
	 */
	constructor(fc: FileChannel) {
		buildTree(fc, true)
	}

	constructor(file: File) {
		val fc =
			if (AndroidAPI.hasOreo()) {
				Files.newByteChannel(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)
			} else {
				RandomAccessFile(file, "rw").channel
			}
		buildTree(fc, true)
	}

	constructor(file: File, closeExit: Boolean) {
		val fc =
			if (AndroidAPI.hasOreo()) {
				Files.newByteChannel(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)
			} else {
				RandomAccessFile(file, "rw").channel
			}
		buildTree(fc, closeExit)
	}

	/**
	 * Create Atom Tree and maintain open channel to raf, should only be used if will continue
	 * to use raf after this call, you will have to close raf yourself.
	 *
	 * @param fc
	 * @param closeOnExit to keep randomfileaccess open, only used when randomaccessfile already being used
	 * @throws IOException
	 * @throws CannotReadException
	 */
	constructor(fc: SeekableByteChannel, closeOnExit: Boolean) {
		buildTree(fc, closeOnExit)
	}

	/**
	 * Build a tree of the atoms in the file
	 *
	 * @param fc
	 * @param closeExit false to keep randomfileacces open, only used when randomaccessfile already being used
	 * @return
	 * @throws IOException
	 * @throws CannotReadException
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun buildTree(fc: SeekableByteChannel, closeExit: Boolean): DefaultTreeModel<*>? {
		return try {
			//make sure at start of file
			fc.position(0)

			//Build up map of nodes
			rootNode = DefaultMutableTreeNode<Any?>(null)
			dataTree = DefaultTreeModel<Any?>(rootNode as DefaultMutableTreeNode<Any?>)

			//Iterate though all the top level Nodes
			val headerBuffer = ByteBuffer.allocate(Mp4BoxHeader.HEADER_LENGTH)
			// we need to have at least enough data in the file left
			// to read a box header
			while (fc.position() < fc.size() - Mp4BoxHeader.HEADER_LENGTH) {
				val boxHeader = Mp4BoxHeader()
				headerBuffer.clear()
				fc.read(headerBuffer)
				headerBuffer.rewind()
				try {
					boxHeader.update(headerBuffer)
				} catch (ne: NullBoxIdException) {
					//If we only get this error after all the expected data has been found we allow it
					if ((moovNode != null) and (mdatNode != null)) {
						val np = NullPadding(fc.position() - Mp4BoxHeader.HEADER_LENGTH, fc.size())
						val trailingPaddingNode: DefaultMutableTreeNode<Any?> = DefaultMutableTreeNode<Any?>(np)
						(rootNode as DefaultMutableTreeNode<Any?>).add(trailingPaddingNode)
						logger.warning(ErrorMessage.NULL_PADDING_FOUND_AT_END_OF_MP4.getMsg(np.filePos))
						break
					} else {
						//File appears invalid
						throw ne
					}
				}
				boxHeader.filePos = fc.position() - Mp4BoxHeader.HEADER_LENGTH
				val newAtom: DefaultMutableTreeNode<Any?> = DefaultMutableTreeNode<Any?>(boxHeader)

				//Go down moov
				if (boxHeader.id == Mp4AtomIdentifier.MOOV.fieldName) {
					//A second Moov atom, this is illegal but may just be mess at the end of the file so ignore
					//and finish
					if ((moovNode != null) and (mdatNode != null)) {
						logger.warning(ErrorMessage.ADDITIONAL_MOOV_ATOM_AT_END_OF_MP4.getMsg(fc.position() - Mp4BoxHeader.HEADER_LENGTH))
						break
					}
					moovNode = newAtom
					moovHeader = boxHeader
					val filePosStart = fc.position()
					moovBuffer = ByteBuffer.allocate(boxHeader.dataLength)
					val bytesRead = fc.read(moovBuffer)

					//If Moov atom is incomplete we are not going to be able to read this file properly
					if (bytesRead < boxHeader.dataLength) {
						val msg = ErrorMessage.ATOM_LENGTH_LARGER_THAN_DATA.getMsg(
							boxHeader.id,
							boxHeader.dataLength,
							bytesRead
						)
						throw CannotReadException(msg)
					}
					moovBuffer!!.rewind()
					buildChildrenOfNode(moovBuffer, newAtom)
					fc.position(filePosStart)
				} else if (boxHeader.id == Mp4AtomIdentifier.FREE.fieldName) {
					//Might be multiple in different locations
					freeNodes.add(newAtom)
				} else if (boxHeader.id == Mp4AtomIdentifier.MDAT.fieldName) {
					//mdatNode always points to the last mDatNode, normally there is just one mdatnode but do have
					//a valid example of multiple mdatnode

					//if(mdatNode!=null)
					//{
					//    throw new CannotReadException(ErrorMessage.MP4_FILE_CONTAINS_MULTIPLE_DATA_ATOMS.getMsg());
					//}
					mdatNode = newAtom
					mdatNodes.add(newAtom)
				}
				(rootNode as? DefaultMutableTreeNode<Any?>)?.add(newAtom)

				//64bit data length
				if (boxHeader.length == 1) {
					val data64bitLengthBuffer = ByteBuffer.allocate(Mp4BoxHeader.DATA_64BITLENGTH)
					data64bitLengthBuffer.order(ByteOrder.BIG_ENDIAN)
					val bytesRead = fc.read(data64bitLengthBuffer)
					if (bytesRead != Mp4BoxHeader.DATA_64BITLENGTH) {
						return null
					}
					data64bitLengthBuffer.rewind()
					val length = data64bitLengthBuffer.long
					if (length < Mp4BoxHeader.HEADER_LENGTH) {
						return null
					}
					fc.position(fc.position() + length - Mp4BoxHeader.REALDATA_64BITLENGTH)
				} else {
					fc.position(fc.position() + boxHeader.dataLength)
				}
			}
			val extraDataLength = fc.size() - fc.position()
			if (extraDataLength != 0L) {
				logger.warning(ErrorMessage.EXTRA_DATA_AT_END_OF_MP4.getMsg(extraDataLength))
			}
			dataTree
		} finally {
			//If we cant find the audio then we cannot modify this file so better to throw exception
			//now rather than later when try and write to it.
			if (mdatNode == null) {
				throw CannotReadException(ErrorMessage.MP4_CANNOT_FIND_AUDIO.msg)
			}
			if (closeExit) {
				fc.close()
			}
		}
	}

	/**
	 * Display atom tree
	 */
	fun printAtomTree() {
		val e: Enumeration<out DefaultMutableTreeNode<Any?>> =
			rootNode!!.preorderEnumeration() as Enumeration<out DefaultMutableTreeNode<Any?>>
		var nextNode: DefaultMutableTreeNode<*>
		while (e.hasMoreElements()) {
			nextNode = e.nextElement()
			val header = nextNode.userObject as? Mp4BoxHeader
			if (header != null) {
				var tabbing = ""
				for (i in 1 until nextNode.level) {
					tabbing += "\t"
				}
				if (header is NullPadding) {
					if (header.length == 1) {
						println(tabbing + "Null pad " + " @ " + header.filePos + " 64bitDataSize" + " ,ends @ " + (header.filePos + header.length))
					} else {
						println(tabbing + "Null pad " + " @ " + header.filePos + " of size:" + header.length + " ,ends @ " + (header.filePos + header.length))
					}
				} else {
					if (header.length == 1) {
						println(tabbing + "Atom " + header.id + " @ " + header.filePos + " 64BitDataSize" + " ,ends @ " + (header.filePos + header.length))
					} else {
						println(tabbing + "Atom " + header.id + " @ " + header.filePos + " of size:" + header.length + " ,ends @ " + (header.filePos + header.length))
					}
				}
			}
		}
	}

	/**
	 *
	 * @param moovBuffer
	 * @param parentNode
	 * @throws IOException
	 * @throws CannotReadException
	 */
	@Throws(IOException::class, CannotReadException::class)
	fun buildChildrenOfNode(moovBuffer: ByteBuffer?, parentNode: DefaultMutableTreeNode<Any?>) {
		var boxHeader: Mp4BoxHeader?

		//Preprocessing for nodes that contain data before their children atoms
		val parentBoxHeader = parentNode.userObject as? Mp4BoxHeader

		//We set the buffers position back to this after processing the children
		val justAfterHeaderPos = moovBuffer!!.position()

		//Preprocessing for meta that normally contains 4 data bytes, but doesn't where found under track or tags atom
		if (parentBoxHeader?.id == Mp4AtomIdentifier.META.fieldName) {
			val meta = Mp4MetaBox(parentBoxHeader, moovBuffer)
			meta.processData()
			try {
				boxHeader = Mp4BoxHeader(moovBuffer)
			} catch (nbe: NullBoxIdException) {
				//It might be that the meta box didn't actually have any additional data after it so we adjust the buffer
				//to be immediately after metabox and code can retry
				moovBuffer.position(moovBuffer.position() - Mp4MetaBox.FLAGS_LENGTH)
			} finally {
				//Skip back last header cos this was only a test
				moovBuffer.position(moovBuffer.position() - Mp4BoxHeader.HEADER_LENGTH)
			}
		}

		//Defines where to start looking for the first child node
		val startPos = moovBuffer.position()
		while (moovBuffer.position() < startPos + (parentBoxHeader?.dataLength
				?: 0) - Mp4BoxHeader.HEADER_LENGTH
		) {
			boxHeader = Mp4BoxHeader(moovBuffer)
			if (boxHeader != null) {
				boxHeader.filePos = moovHeader!!.filePos + moovBuffer.position()
				logger.finest("Atom " + boxHeader.id + " @ " + boxHeader.filePos + " of size:" + boxHeader.length + " ,ends @ " + (boxHeader.filePos + boxHeader.length))
				val newAtom: DefaultMutableTreeNode<Any?> = DefaultMutableTreeNode<Any?>(boxHeader)
				parentNode.add(newAtom)
				if (boxHeader.id == Mp4AtomIdentifier.UDTA.fieldName) {
					udtaNode = newAtom
				} else if (boxHeader.id == Mp4AtomIdentifier.META.fieldName && parentBoxHeader?.id == Mp4AtomIdentifier.UDTA.fieldName) {
					metaNode = newAtom
				} else if (boxHeader.id == Mp4AtomIdentifier.HDLR.fieldName && parentBoxHeader?.id == Mp4AtomIdentifier.META.fieldName) {
					hdlrWithinMetaNode = newAtom
				} else if (boxHeader.id == Mp4AtomIdentifier.HDLR.fieldName) {
					hdlrWithinMdiaNode = newAtom
				} else if (boxHeader.id == Mp4AtomIdentifier.TAGS.fieldName) {
					tagsNode = newAtom
				} else if (boxHeader.id == Mp4AtomIdentifier.STCO.fieldName) {
					stcos.add(Mp4StcoBox(boxHeader, moovBuffer))
					stcoNodes.add(newAtom)
				} else if (boxHeader.id == Mp4AtomIdentifier.ILST.fieldName) {
					val parent = parentNode.parent as? DefaultMutableTreeNode<*>
					if (parent != null) {
						val parentsParent = parent.parent as? Mp4BoxHeader
						if (parentsParent != null) {
							if (parentBoxHeader?.id == Mp4AtomIdentifier.META.fieldName && parentsParent.id == Mp4AtomIdentifier.UDTA.fieldName) {
								ilstNode = newAtom
							}
						}
					}
				} else if (boxHeader.id == Mp4AtomIdentifier.FREE.fieldName) {
					//Might be multiple in different locations
					freeNodes.add(newAtom)
				} else if (boxHeader.id == Mp4AtomIdentifier.TRAK.fieldName) {
					//Might be multiple in different locations, although only one should be audio track
					trakNodes.add(newAtom)
				}

				//For these atoms iterate down to build their children
				if (boxHeader.id == Mp4AtomIdentifier.TRAK.fieldName || boxHeader.id == Mp4AtomIdentifier.MDIA.fieldName || boxHeader.id == Mp4AtomIdentifier.MINF.fieldName || boxHeader.id == Mp4AtomIdentifier.STBL.fieldName || boxHeader.id == Mp4AtomIdentifier.UDTA.fieldName || boxHeader.id == Mp4AtomIdentifier.META.fieldName || boxHeader.id == Mp4AtomIdentifier.ILST.fieldName) {
					buildChildrenOfNode(moovBuffer, newAtom)
				}
				//Now  adjust buffer for the next atom header at this level
				moovBuffer.position(moovBuffer.position() + boxHeader.dataLength)
			}
		}
		moovBuffer.position(justAfterHeaderPos)
	}

	/**
	 *
	 * @return
	 */
	fun getStcoNodes(): List<DefaultMutableTreeNode<*>> {
		return stcoNodes
	}

	/**
	 *
	 * @param node
	 * @return
	 */
	fun getBoxHeader(node: DefaultMutableTreeNode<*>?): Mp4BoxHeader? {
		return if (node == null) {
			null
		} else node.userObject as? Mp4BoxHeader
	}

	/**
	 *
	 * @return
	 */
	fun getFreeNodes(): List<DefaultMutableTreeNode<*>> {
		return freeNodes
	}

	/**
	 *
	 * @return
	 */
	fun getTrakNodes(): List<DefaultMutableTreeNode<*>> {
		return trakNodes
	}

	/**
	 *
	 * @return
	 */
	fun getStcos(): List<Mp4StcoBox> {
		return stcos
	}

	companion object {
		//Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.mp4")
	}
}
