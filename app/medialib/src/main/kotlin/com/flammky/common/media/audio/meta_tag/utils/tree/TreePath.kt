/*
 * @(#)TreePath.java	1.32 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.*

/**
 * Represents a path to a node. A TreePath is an array of Objects that are
 * vended from a TreeModel. The elements of the array are ordered such
 * that the root is always the first element (index 0) of the array.
 * TreePath is Serializable, but if any
 * components of the path are not serializable, it will not be written
 * out.
 *
 *
 * For further information and examples of using tree paths,
 * see [How to Use Trees](http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html)
 * in *The Java Tutorial.*
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
 * @version 1.32 03/23/10
 * @author Scott Violet
 * @author Philip Milne
 */
class TreePath<T> : Any, Serializable {
	/**
	 * Returns a path containing all the elements of this object, except
	 * the last path component.
	 */
	/** Path representing the parent, null if lastPathComponent represents
	 * the root.  */
	var parentPath: TreePath<T>? = null
		private set
	/**
	 * Returns the last component of this path. For a path returned by
	 * DefaultTreeModel this will return an instance of TreeNode.
	 *
	 * @return the Object at the end of the path
	 * @see .TreePath
	 */
	/** Last path component.  */
	@Transient
	var lastPathComponent: TreeNode<T>? = null
		private set

	/**
	 * Constructs a path from an array of Objects, uniquely identifying
	 * the path from the root of the tree to a specific node, as returned
	 * by the tree's data model.
	 *
	 *
	 * The model is free to return an array of any Objects it needs to
	 * represent the path. The DefaultTreeModel returns an array of
	 * TreeNode objects. The first TreeNode in the path is the root of the
	 * tree, the last TreeNode is the node identified by the path.
	 *
	 * @param path  an array of Objects representing the path to a node
	 */
	constructor(path: Array<TreeNode<T>>?) {
		require(!(path == null || path.size == 0)) { "path in TreePath must be non null and not empty." }
		lastPathComponent = path[path.size - 1]
		if (path.size > 1) parentPath = TreePath(path, path.size - 1)
	}

	/**
	 * Constructs a TreePath containing only a single element. This is
	 * usually used to construct a TreePath for the the root of the TreeModel.
	 *
	 *
	 * @param singlePath  an Object representing the path to a node
	 * @see .TreePath
	 */
	constructor(singlePath: TreeNode<T>?) {
		requireNotNull(singlePath) { "path in TreePath must be non null." }
		lastPathComponent = singlePath
		parentPath = null
	}

	/**
	 * Constructs a new TreePath, which is the path identified by
	 * `parent` ending in `lastElement`.
	 */
	protected constructor(parent: TreePath<T>?, lastElement: TreeNode<T>?) {
		requireNotNull(lastElement) { "path in TreePath must be non null." }
		parentPath = parent
		lastPathComponent = lastElement
	}

	/**
	 * Constructs a new TreePath with the identified path components of
	 * length `length`.
	 */
	protected constructor(path: Array<TreeNode<T>>, length: Int) {
		lastPathComponent = path[length - 1]
		if (length > 1) parentPath = TreePath(path, length - 1)
	}

	/**
	 * Primarily provided for subclasses
	 * that represent paths in a different manner.
	 * If a subclass uses this constructor, it should also override
	 * the `getPath`,
	 * `getPathCount`, and
	 * `getPathComponent` methods,
	 * and possibly the `equals` method.
	 */
	protected constructor()

	/**
	 * Returns an ordered array of Objects containing the components of this
	 * TreePath. The first element (index 0) is the root.
	 *
	 * @return an array of Objects representing the TreePath
	 * @see .TreePath
	 */
	val path: Array<TreeNode<T>?>
		get() {
			var i = pathCount
			val result: Array<TreeNode<T>?> = arrayOfNulls<TreeNode<T>?>(i--)
			var path: TreePath<T>? = this
			while (path != null) {
				result[i--] = path.lastPathComponent
				path = path.parentPath
			}
			return result
		}

	/**
	 * Returns the number of elements in the path.
	 *
	 * @return an int giving a count of items the path
	 */
	val pathCount: Int
		get() {
			var result = 0
			var path: TreePath<T>? = this
			while (path != null) {
				result++
				path = path.parentPath
			}
			return result
		}

	/**
	 * Returns the path component at the specified index.
	 *
	 * @param element  an int specifying an element in the path, where
	 * 0 is the first element in the path
	 * @return the Object at that index location
	 * @throws IllegalArgumentException if the index is beyond the length
	 * of the path
	 * @see .TreePath
	 */
	fun getPathComponent(element: Int): TreeNode<T>? {
		val pathLength = pathCount
		require(!(element < 0 || element >= pathLength)) { "Index $element is out of the specified range" }
		var path: TreePath<T>? = this
		for (i in pathLength - 1 downTo element + 1) {
			path = path!!.parentPath
		}
		return path!!.lastPathComponent
	}

	/**
	 * Tests two TreePaths for equality by checking each element of the
	 * paths for equality. Two paths are considered equal if they are of
	 * the same length, and contain
	 * the same elements (`.equals`).
	 *
	 * @param o the Object to compare
	 */
	override fun equals(o: Any?): Boolean {
		if (o === this) return true
		if (o is TreePath<*>) {
			var oTreePath = o as TreePath<T>?
			if (pathCount != oTreePath!!.pathCount) return false
			var path: TreePath<T>? = this
			while (path != null) {
				if (path.lastPathComponent != oTreePath!!.lastPathComponent) {
					return false
				}
				oTreePath = oTreePath.parentPath
				path = path.parentPath
			}
			return true
		}
		return false
	}

	/**
	 * Returns the hashCode for the object. The hash code of a TreePath
	 * is defined to be the hash code of the last component in the path.
	 *
	 * @return the hashCode for the object
	 */
	override fun hashCode(): Int {
		return lastPathComponent.hashCode()
	}

	/**
	 * Returns true if `aTreePath` is a
	 * descendant of this
	 * TreePath. A TreePath P1 is a descendant of a TreePath P2
	 * if P1 contains all of the components that make up
	 * P2's path.
	 * For example, if this object has the path [a, b],
	 * and `aTreePath` has the path [a, b, c],
	 * then `aTreePath` is a descendant of this object.
	 * However, if `aTreePath` has the path [a],
	 * then it is not a descendant of this object.  By this definition
	 * a TreePath is always considered a descendant of itself.  That is,
	 * `aTreePath.isDescendant(aTreePath)` returns true.
	 *
	 * @return true if `aTreePath` is a descendant of this path
	 */
	fun isDescendant(aTreePath: TreePath<T>?): Boolean {
		var aTreePath = aTreePath
		if (aTreePath === this) return true
		if (aTreePath != null) {
			val pathLength = pathCount
			var oPathLength = aTreePath.pathCount
			if (oPathLength < pathLength) // Can't be a descendant, has fewer components in the path.
				return false
			while (oPathLength-- > pathLength) aTreePath = aTreePath!!.parentPath
			return equals(aTreePath)
		}
		return false
	}

	/**
	 * Returns a new path containing all the elements of this object
	 * plus `child`. `child` will be the last element
	 * of the newly created TreePath.
	 * This will throw a NullPointerException
	 * if child is null.
	 */
	fun pathByAddingChild(child: TreeNode<T>?): TreePath<T> {
		if (child == null) throw NullPointerException("Null child not allowed")
		return TreePath(this, child)
	}

	/**
	 * Returns a string that displays and identifies this
	 * object's properties.
	 *
	 * @return a String representation of this object
	 */
	override fun toString(): String {
		val tempSpot = StringBuffer("[")
		var counter = 0
		val maxCounter = pathCount
		while (counter < maxCounter) {
			if (counter > 0) tempSpot.append(", ")
			tempSpot.append(getPathComponent(counter))
			counter++
		}
		tempSpot.append("]")
		return tempSpot.toString()
	}

	// Serialization support.
	@Throws(IOException::class)
	private fun writeObject(s: ObjectOutputStream) {
		s.defaultWriteObject()
		val values = Vector<Any>()
		if (lastPathComponent != null &&
			lastPathComponent is Serializable
		) {
			values.addElement("lastPathComponent")
			values.addElement(lastPathComponent)
		}
		s.writeObject(values)
	}

	@Throws(IOException::class, ClassNotFoundException::class)
	private fun readObject(s: ObjectInputStream) {
		s.defaultReadObject()
		val values = s.readObject() as Vector<*>
		var indexCounter = 0
		val maxCounter = values.size
		if (indexCounter < maxCounter && values.elementAt(indexCounter) == "lastPathComponent") {
			lastPathComponent = values.elementAt(++indexCounter) as TreeNode<T>
			indexCounter++
		}
	}

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = -5521484730448766444L
	}
}
