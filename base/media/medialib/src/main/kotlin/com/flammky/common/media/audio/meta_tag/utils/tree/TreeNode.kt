/*
 * @(#)TreeNode.java	1.26 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree

import java.util.*

/**
 * Defines the requirements for an object that can be used as a
 * tree node in a JTree.
 *
 *
 * Implementations of `TreeNode` that override `equals`
 * will typically need to override `hashCode` as well.  Refer
 * to [javax.swing.tree.TreeModel] for more information.
 *
 * For further information and examples of using tree nodes,
 * see [How to Use Tree Nodes](http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html)
 * in *The Java Tutorial.*
 *
 * @version 1.26 03/23/10
 * @author Rob Davis
 * @author Scott Violet
 */
interface TreeNode<T> {
	/**
	 * Returns the child `TreeNode` at index
	 * `childIndex`.
	 */
	fun getChildAt(childIndex: Int): TreeNode<T>?

	/**
	 * Returns the number of children `TreeNode`s the receiver
	 * contains.
	 */
	val childCount: Int

	/**
	 * Returns the parent `TreeNode` of the receiver.
	 */
	val parent: TreeNode<T>?

	/**
	 * Returns the index of `node` in the receivers children.
	 * If the receiver does not contain `node`, -1 will be
	 * returned.
	 */
	fun getIndex(node: TreeNode<T>): Int

	/**
	 * Returns true if the receiver allows children.
	 */
	val allowsChildren: Boolean

	/**
	 * Returns true if the receiver is a leaf.
	 */
	val isLeaf: Boolean

	/**
	 * Returns the children of the receiver as an `Enumeration`.
	 */
	fun children(): Enumeration<TreeNode<T>>
	val userObject: T?
}
