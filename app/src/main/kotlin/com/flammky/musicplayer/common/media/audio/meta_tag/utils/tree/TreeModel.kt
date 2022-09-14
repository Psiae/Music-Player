/*
 * @(#)TreeModel.java	1.27 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree

/**
 * The model used by `JTree`.
 *
 *
 * `JTree` and its related classes make extensive use of
 * `TreePath`s for indentifying nodes in the `TreeModel`.
 * If a `TreeModel` returns the same object, as compared by
 * `equals`, at two different indices under the same parent
 * than the resulting `TreePath` objects will be considered equal
 * as well. Some implementations may assume that if two
 * `TreePath`s are equal, they identify the same node. If this
 * condition is not met, painting problems and other oddities may result.
 * In other words, if `getChild` for a given parent returns
 * the same Object (as determined by `equals`) problems may
 * result, and it is recommended you avoid doing this.
 *
 *
 * Similarly `JTree` and its related classes place
 * `TreePath`s in `Map`s.  As such if
 * a node is requested twice, the return values must be equal
 * (using the `equals` method) and have the same
 * `hashCode`.
 *
 *
 * For further information on tree models,
 * including an example of a custom implementation,
 * see [How to Use Trees](http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html)
 * in *The Java Tutorial.*
 *
 * @see TreePath
 *
 *
 * @version 1.27 03/23/10
 * @author Rob Davis
 * @author Ray Ryan
 */
interface TreeModel<T> {
	/**
	 * Returns the root of the tree.  Returns `null`
	 * only if the tree has no nodes.
	 *
	 * @return  the root of the tree
	 */
	val root: TreeNode<T>?

	/**
	 * Returns the child of `parent` at index `index`
	 * in the parent's
	 * child array.  `parent` must be a node previously obtained
	 * from this data source. This should not return `null`
	 * if `index`
	 * is a valid index for `parent` (that is `index >= 0 &&
	 * index < getChildCount(parent`)).
	 *
	 * @param   parent  a node in the tree, obtained from this data source
	 * @return  the child of `parent` at index `index`
	 */
	fun getChild(parent: TreeNode<T>, index: Int): TreeNode<T>?

	/**
	 * Returns the number of children of `parent`.
	 * Returns 0 if the node
	 * is a leaf or if it has no children.  `parent` must be a node
	 * previously obtained from this data source.
	 *
	 * @param   parent  a node in the tree, obtained from this data source
	 * @return  the number of children of the node `parent`
	 */
	fun getChildCount(parent: TreeNode<T>): Int

	/**
	 * Returns `true` if `node` is a leaf.
	 * It is possible for this method to return `false`
	 * even if `node` has no children.
	 * A directory in a filesystem, for example,
	 * may contain no files; the node representing
	 * the directory is not a leaf, but it also has no children.
	 *
	 * @param   node  a node in the tree, obtained from this data source
	 * @return  true if `node` is a leaf
	 */
	fun isLeaf(node: TreeNode<T>): Boolean

	/**
	 * Messaged when the user has altered the value for the item identified
	 * by `path` to `newValue`.
	 * If `newValue` signifies a truly new value
	 * the model should post a `treeNodesChanged` event.
	 *
	 * @param path path to the node that the user has altered
	 * @param newValue the new value from the TreeCellEditor
	 */
	fun valueForPathChanged(path: TreePath<T>, newValue: T)

	/**
	 * Returns the index of child in parent.  If either `parent`
	 * or `child` is `null`, returns -1.
	 * If either `parent` or `child` don't
	 * belong to this tree model, returns -1.
	 *
	 * @param parent a node in the tree, obtained from this data source
	 * @param child the node we are interested in
	 * @return the index of the child in the parent, or -1 if either
	 * `child` or `parent` are `null`
	 * or don't belong to this tree model
	 */
	fun getIndexOfChild(parent: TreeNode<T>?, child: TreeNode<T>?): Int
	//
	//  Change Events
	//
	/**
	 * Adds a listener for the `TreeModelEvent`
	 * posted after the tree changes.
	 *
	 * @param   l       the listener to add
	 * @see .removeTreeModelListener
	 */
	fun addTreeModelListener(l: TreeModelListener)

	/**
	 * Removes a listener previously added with
	 * `addTreeModelListener`.
	 *
	 * @see .addTreeModelListener
	 *
	 * @param   l       the listener to remove
	 */
	fun removeTreeModelListener(l: TreeModelListener)
}
