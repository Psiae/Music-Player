/*
 * @(#)MutableTreeNode.java	1.13 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree

/**
 * Defines the requirements for a tree node object that can change --
 * by adding or removing child nodes, or by changing the contents
 * of a user object stored in the node.
 *
 * @see DefaultMutableTreeNode
 *
 * @see javax.swing.JTree
 *
 *
 * @version 1.13 03/23/10
 * @author Rob Davis
 * @author Scott Violet
 */
interface MutableTreeNode<T> : TreeNode<T> {
	/**
	 * Adds `child` to the receiver at `index`.
	 * `child` will be messaged with `setParent`.
	 */
	fun insert(child: MutableTreeNode<T>, index: Int)

	/**
	 * Removes the child at `index` from the receiver.
	 */
	fun remove(index: Int)

	/**
	 * Removes `node` from the receiver. `setParent`
	 * will be messaged on `node`.
	 */
	fun remove(node: MutableTreeNode<T>)

	/**
	 * Resets the user object of the receiver to `object`.
	 */
	fun setUserObject(`object`: T)

	/**
	 * Removes the receiver from its parent.
	 */
	fun removeFromParent()

	/**
	 * Sets the parent of the receiver to `newParent`.
	 */
	fun setParent(newParent: MutableTreeNode<T>?)
}
