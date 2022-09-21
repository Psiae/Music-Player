/*
 * @(#)TreeModelEvent.java	1.35 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree

import java.util.*

/**
 * Encapsulates information describing changes to a tree model, and
 * used to notify tree model listeners of the change.
 * For more information and examples see
 * [How to Write a Tree Model Listener](http://java.sun.com/docs/books/tutorial/uiswing/events/treemodellistener.html),
 * a section in *The Java Tutorial.*
 *
 *
 * **Warning:**
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the `java.beans` package.
 * Please see [java.beans.XMLEncoder].
 *
 * @version 1.35 03/23/10
 * @author Rob Davis
 * @author Ray Ryan
 * @author Scott Violet
 */
class TreeModelEvent<T> : EventObject {
	/**
	 * For all events, except treeStructureChanged,
	 * returns the parent of the changed nodes.
	 * For treeStructureChanged events, returns the ancestor of the
	 * structure that has changed. This and
	 * `getChildIndices` are used to get a list of the effected
	 * nodes.
	 *
	 *
	 * The one exception to this is a treeNodesChanged event that is to
	 * identify the root, in which case this will return the root
	 * and `getChildIndices` will return null.
	 *
	 * @return the TreePath used in identifying the changed nodes.
	 * @see TreePath.getLastPathComponent
	 */
	/** Path to the parent of the nodes that have changed.  */
	var treePath: TreePath<T>?
		protected set

	/** Indices identifying the position of where the children were.  */
	protected var mChildIndices: IntArray?

	/** Children that have been removed.  */
	protected var mChildren: Array<TreeNode<T>>? = null

	/**
	 * Used to create an event when nodes have been changed, inserted, or
	 * removed, identifying the path to the parent of the modified items as
	 * an array of Objects. All of the modified objects are siblings which are
	 * direct descendents (not grandchildren) of the specified parent.
	 * The positions at which the inserts, deletes, or changes occurred are
	 * specified by an array of `int`. The indexes in that array
	 * must be in order, from lowest to highest.
	 *
	 *
	 * For changes, the indexes in the model correspond exactly to the indexes
	 * of items currently displayed in the UI. As a result, it is not really
	 * critical if the indexes are not in their exact order. But after multiple
	 * inserts or deletes, the items currently in the UI no longer correspond
	 * to the items in the model. It is therefore critical to specify the
	 * indexes properly for inserts and deletes.
	 *
	 *
	 * For inserts, the indexes represent the *final* state of the tree,
	 * after the inserts have occurred. Since the indexes must be specified in
	 * order, the most natural processing methodology is to do the inserts
	 * starting at the lowest index and working towards the highest. Accumulate
	 * a Vector of `Integer` objects that specify the
	 * insert-locations as you go, then convert the Vector to an
	 * array of `int` to create the event. When the postition-index
	 * equals zero, the node is inserted at the beginning of the list. When the
	 * position index equals the size of the list, the node is "inserted" at
	 * (appended to) the end of the list.
	 *
	 *
	 * For deletes, the indexes represent the *initial* state of the tree,
	 * before the deletes have occurred. Since the indexes must be specified in
	 * order, the most natural processing methodology is to use a delete-counter.
	 * Start by initializing the counter to zero and start work through the
	 * list from lowest to higest. Every time you do a delete, add the current
	 * value of the delete-counter to the index-position where the delete occurred,
	 * and append the result to a Vector of delete-locations, using
	 * `addElement()`. Then increment the delete-counter. The index
	 * positions stored in the Vector therefore reflect the effects of all previous
	 * deletes, so they represent each object's position in the initial tree.
	 * (You could also start at the highest index and working back towards the
	 * lowest, accumulating a Vector of delete-locations as you go using the
	 * `insertElementAt(Integer, 0)`.) However you produce the Vector
	 * of initial-positions, you then need to convert the Vector of `Integer`
	 * objects to an array of `int` to create the event.
	 *
	 *
	 * **Notes:**
	 *  * Like the `insertNodeInto` method in the
	 * `DefaultTreeModel` class, `insertElementAt`
	 * appends to the `Vector` when the index matches the size
	 * of the vector. So you can use `insertElementAt(Integer, 0)`
	 * even when the vector is empty.
	 * To create a node changed event for the root node, specify the parent
	 * and the child indices as `null`.
	 *
	 *
	 * @param source the Object responsible for generating the event (typically
	 * the creator of the event object passes `this`
	 * for its value)
	 * @param path   an array of Object identifying the path to the
	 * parent of the modified item(s), where the first element
	 * of the array is the Object stored at the root node and
	 * the last element is the Object stored at the parent node
	 * @param childIndices an array of `int` that specifies the
	 * index values of the removed items. The indices must be
	 * in sorted order, from lowest to highest
	 * @param children an array of Object containing the inserted, removed, or
	 * changed objects
	 * @see TreePath
	 */
	constructor(
		source: Any?, path: Array<TreeNode<T>>?, childIndices: IntArray?,
		children: Array<TreeNode<T>>?
	) : this(source, TreePath<T>(path), childIndices, children)

	/**
	 * Used to create an event when nodes have been changed, inserted, or
	 * removed, identifying the path to the parent of the modified items as
	 * a TreePath object. For more information on how to specify the indexes
	 * and objects, see
	 * `TreeModelEvent(Object,Object[],int[],Object[])`.
	 *
	 * @param source the Object responsible for generating the event (typically
	 * the creator of the event object passes `this`
	 * for its value)
	 * @param path   a TreePath object that identifies the path to the
	 * parent of the modified item(s)
	 * @param childIndices an array of `int` that specifies the
	 * index values of the modified items
	 * @param children an array of Object containing the inserted, removed, or
	 * changed objects
	 *
	 * @see .TreeModelEvent
	 */
	constructor(
		source: Any?, path: TreePath<T>?, childIndices: IntArray?,
		children: Array<TreeNode<T>>?
	) : super(source) {
		treePath = path
		this.mChildIndices = childIndices
		this.mChildren = children
	}

	/**
	 * Used to create an event when the node structure has changed in some way,
	 * identifying the path to the root of a modified subtree as an array of
	 * Objects. A structure change event might involve nodes swapping position,
	 * for example, or it might encapsulate multiple inserts and deletes in the
	 * subtree stemming from the node, where the changes may have taken place at
	 * different levels of the subtree.
	 * <blockquote>
	 * **Note:**<br></br>
	 * JTree collapses all nodes under the specified node, so that only its
	 * immediate children are visible.
	</blockquote> *
	 *
	 * @param source the Object responsible for generating the event (typically
	 * the creator of the event object passes `this`
	 * for its value)
	 * @param path   an array of Object identifying the path to the root of the
	 * modified subtree, where the first element of the array is
	 * the object stored at the root node and the last element
	 * is the object stored at the changed node
	 * @see TreePath
	 */
	constructor(source: Any?, path: Array<TreeNode<T>>?) : this(source, TreePath<T>(path))

	/**
	 * Used to create an event when the node structure has changed in some way,
	 * identifying the path to the root of the modified subtree as a TreePath
	 * object. For more information on this event specification, see
	 * `TreeModelEvent(Object,Object[])`.
	 *
	 * @param source the Object responsible for generating the event (typically
	 * the creator of the event object passes `this`
	 * for its value)
	 * @param path   a TreePath object that identifies the path to the
	 * change. In the DefaultTreeModel,
	 * this object contains an array of user-data objects,
	 * but a subclass of TreePath could use some totally
	 * different mechanism -- for example, a node ID number
	 *
	 * @see .TreeModelEvent
	 */
	constructor(source: Any?, path: TreePath<T>?) : super(source) {
		treePath = path
		mChildIndices = IntArray(0)
	}

	/**
	 * Convenience method to get the array of objects from the TreePath
	 * instance that this event wraps.
	 *
	 * @return an array of Objects, where the first Object is the one
	 * stored at the root and the last object is the one
	 * stored at the node identified by the path
	 */
	fun getPath(): Array<TreeNode<T>?>? {
		return treePath?.path
	}

	/**
	 * Returns the objects that are children of the node identified by
	 * `getPath` at the locations specified by
	 * `getChildIndices`. If this is a removal event the
	 * returned objects are no longer children of the parent node.
	 *
	 * @return an array of Object containing the children specified by
	 * the event
	 * @see .getPath
	 *
	 * @see .getChildIndices
	 */
	fun getChildren(): Array<TreeNode<T>?>? {
		if (mChildren != null) {
			val cCount = mChildren!!.size
			val retChildren: Array<TreeNode<T>?> = arrayOfNulls(cCount)
			System.arraycopy(mChildren!!, 0, retChildren, 0, cCount)
			return retChildren
		}
		return null
	}


	/**
	 * Returns the values of the child indexes. If this is a removal event
	 * the indexes point to locations in the initial list where items
	 * were removed. If it is an insert, the indices point to locations
	 * in the final list where the items were added. For node changes,
	 * the indices point to the locations of the modified nodes.
	 *
	 * @return an array of `int` containing index locations for
	 * the children specified by the event
	 */
	fun getChildIndices(): IntArray? {
		if (mChildIndices != null) {
			val cCount = mChildIndices!!.size
			val retArray = IntArray(cCount)
			System.arraycopy(mChildIndices, 0, retArray, 0, cCount)
			return retArray
		}
		return null
	}

	/**
	 * Returns a string that displays and identifies this object's
	 * properties.
	 *
	 * @return a String representation of this object
	 */
	override fun toString(): String {
		val retBuffer = StringBuffer()
		retBuffer.append(
			javaClass.name + " " +
				Integer.toString(hashCode())
		)
		if (treePath != null) retBuffer.append(" path " + treePath)
		if (mChildIndices != null) {
			retBuffer.append(" indices [ ")
			for (counter in mChildIndices!!.indices) retBuffer.append(
				Integer.toString(
					mChildIndices!![counter]
				) + " "
			)
			retBuffer.append("]")
		}
		if (mChildren != null) {
			retBuffer.append(" children [ ")
			for (counter in mChildren!!.indices) retBuffer.append(mChildren!![counter].toString() + " ")
			retBuffer.append("]")
		}
		return retBuffer.toString()
	}

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 1135112176859241636L
	}
}
