/*
 * @(#)DefaultMutableTreeNode.java	1.25 10/03/23
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

// ISSUE: this class depends on nothing in AWT -- move to java.util?
/**
 * A `DefaultMutableTreeNode` is a general-purpose node in a tree data
 * structure.
 * For examples of using default mutable tree nodes, see
 * [How to Use Trees](http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html)
 * in *The Java Tutorial.*
 *
 *
 *
 *
 * A tree node may have at most one parent and 0 or more children.
 * `DefaultMutableTreeNode` provides operations for examining and modifying a
 * node's parent and children and also operations for examining the tree that
 * the node is a part of.  A node's tree is the set of all nodes that can be
 * reached by starting at the node and following all the possible links to
 * parents and children.  A node with no parent is the root of its tree; a
 * node with no children is a leaf.  A tree may consist of many subtrees,
 * each node acting as the root for its own subtree.
 *
 *
 * This class provides enumerations for efficiently traversing a tree or
 * subtree in various orders or for following the path between two nodes.
 * A `DefaultMutableTreeNode` may also hold a reference to a user object, the
 * use of which is left to the user.  Asking a `DefaultMutableTreeNode` for its
 * string representation with `toString()` returns the string
 * representation of its user object.
 *
 *
 * **This is not a thread safe class.**If you intend to use
 * a DefaultMutableTreeNode<T> (or a tree of TreeNodes) in more than one thread, you
 * need to do your own synchronizing. A good convention to adopt is
 * synchronizing on the root node of a tree.
</T> *
 *
 * While DefaultMutableTreeNode<T> implements the MutableTreeNode<T> interface and
 * will allow you to add in any implementation of MutableTreeNode<T> not all
 * of the methods in DefaultMutableTreeNode<T> will be applicable to all
 * MutableTreeNodes implementations. Especially with some of the enumerations
 * that are provided, using some of these methods assumes the
 * DefaultMutableTreeNode<T> contains only DefaultMutableNode instances. All
 * of the TreeNode/MutableTreeNode<T> methods will behave as defined no
 * matter what implementations are added.
 *
</T></T></T></T></T></T> *
 *
 * **Warning:**
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the `java.beans` package.
 *
 * @see org.jaudiotagger.utils.tree.MutableTreeNode
 *
 *
 * @version 1.25 03/23/10
 * @author Rob Davis
 */
class DefaultMutableTreeNode<T> @JvmOverloads constructor(
	userObject: T,
	/** true if the node is able to have children  */
	protected var mAllowsChildren: Boolean = true
) : Cloneable, MutableTreeNode<T>, Serializable {
	/** this node's parent, or null if this node has no parent  */
	protected var mParent: MutableTreeNode<T>? = null

	/** array of children, may be null if this node has no children  */
	protected var children: Vector<TreeNode<T>>? = null

	/** optional user object  */
	@Transient
	protected var mUserObject: T
	/**
	 * Creates a tree node with no parent, no children, initialized with
	 * the specified user object, and that allows children only if
	 * specified.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 * the node's data
	 * @param mAllowsChildren if true, the node is allowed to have child
	 * nodes -- otherwise, it is always a leaf node
	 */
	/**
	 * Creates a tree node with no parent, no children, but which allows
	 * children, and initializes it with the specified user object.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 * the node's data
	 */
	/**
	 * Creates a tree node that has no parent and no children, but which
	 * allows children.
	 */
	init {
		this.mUserObject = userObject
	}
	//
	//  Primitives
	//
	/**
	 * Removes `newChild` from its present parent (if it has a
	 * parent), sets the child's parent to this node, and then adds the child
	 * to this node's child array at index `childIndex`.
	 * `newChild` must not be null and must not be an ancestor of
	 * this node.
	 *
	 * @param    newChild    the MutableTreeNode<T> to insert under this node
	 * @param    childIndex    the index in this node's child array
	 * where this node is to be inserted
	 * @exception    ArrayIndexOutOfBoundsException    if
	 * `childIndex` is out of bounds
	 * @exception    IllegalArgumentException    if
	 * `newChild` is null or is an
	 * ancestor of this node
	 * @exception    IllegalStateException    if this node does not allow
	 * children
	 * @see .isNodeDescendant
	</T> */
	override fun insert(newChild: MutableTreeNode<T>, childIndex: Int) {
		check(mAllowsChildren) { "node does not allow children" }
		requireNotNull(newChild) { "new child is null" }
		val oldParent = newChild.parent as? MutableTreeNode<T>
		oldParent?.remove(newChild)
		newChild.setParent(this)
		if (children == null) {
			children = Vector()
		}
		children!!.insertElementAt(newChild, childIndex)
	}

	/**
	 * Removes the child at the specified index from this node's children
	 * and sets that node's parent to null. The child node to remove
	 * must be a `MutableTreeNode`.
	 *
	 * @param    childIndex    the index in this node's child array
	 * of the child to remove
	 * @exception    ArrayIndexOutOfBoundsException    if
	 * `childIndex` is out of bounds
	 */
	override fun remove(childIndex: Int) {
		val child = getChildAt(childIndex) as? MutableTreeNode<T>
		children?.removeElementAt(childIndex)
		child?.setParent(null)
	}

	/**
	 * Sets this node's parent to `newParent` but does not
	 * change the parent's child array.  This method is called from
	 * `insert()` and `remove()` to
	 * reassign a child's parent, it should not be messaged from anywhere
	 * else.
	 *
	 * @param    newParent    this node's new parent
	 */
	override fun setParent(newParent: MutableTreeNode<T>?) {
		mParent = newParent
	}

	override val parent: TreeNode<T>?
		/**
		 * Returns this node's parent or null if this node has no parent.
		 *
		 * @return    this node's parent TreeNode, or null if this node has no parent
		 */
		get() = mParent

	/**
	 * Returns the child at the specified index in this node's child array.
	 *
	 * @param    index    an index into this node's child array
	 * @exception    ArrayIndexOutOfBoundsException    if `index`
	 * is out of bounds
	 * @return    the TreeNode<T> in this node's child array at  the specified index
	</T> */
	override fun getChildAt(index: Int): TreeNode<T> {
		if (children == null) {
			throw ArrayIndexOutOfBoundsException("node has no children")
		}
		return children!!.elementAt(index)
	}

	override val childCount: Int
		/**
		 * Returns the number of children of this node.
		 *
		 * @return    an int giving the number of children of this node
		 */
		get() = children?.size ?: 0

	/**
	 * Returns the index of the specified child in this node's child array.
	 * If the specified node is not a child of this node, returns
	 * `-1`.  This method performs a linear search and is O(n)
	 * where n is the number of children.
	 *
	 * @param    aChild    the TreeNode<T> to search for among this node's children
	 * @exception    IllegalArgumentException    if `aChild`
	 * is null
	 * @return    an int giving the index of the node in this node's child
	 * array, or `-1` if the specified node is a not
	 * a child of this node
	</T> */
	override fun getIndex(aChild: TreeNode<T>): Int {
		return if (!isNodeChild(aChild)) -1 else children?.indexOf(aChild) ?: -1
		// linear search
	}

	/**
	 * Creates and returns a forward-order enumeration of this node's
	 * children.  Modifying this node's child array invalidates any child
	 * enumerations created before the modification.
	 *
	 * @return    an Enumeration of this node's children
	 */
	override fun children(): Enumeration<TreeNode<T>> {
		return if (children == null) {
			emptyEnumeration()
		} else {
			children!!.elements()
		}
	}

	/**
	 * Determines whether or not this node is allowed to have children.
	 * If `allows` is false, all of this node's children are
	 * removed.
	 *
	 *
	 * Note: By default, a node allows children.
	 *
	 * @param    allows    true if this node is allowed to have children
	 */
	fun setAllowsChildren(allows: Boolean) {
		if (allows != mAllowsChildren) {
			mAllowsChildren = allows
			if (!mAllowsChildren) {
				removeAllChildren()
			}
		}
	}

	override val allowsChildren: Boolean
		/**
		 * Returns true if this node is allowed to have children.
		 *
		 * @return    true if this node allows children, else false
		 */
		get() = mAllowsChildren

	/**
	 * Sets the user object for this node to `userObject`.
	 *
	 * @param    userObject    the Object that constitutes this node's
	 * user-specified data
	 * @see .getUserObject
	 *
	 * @see .toString
	 */
	override fun setUserObject(userObject: T) {
		this.mUserObject = userObject
	}

	override val userObject: T
		/**
		 * Returns this node's user object.
		 *
		 * @return    the Object stored at this node by the user
		 * @see .setUserObject
		 *
		 * @see .toString
		 */
		get() = mUserObject

	//
	//  Derived methods
	//
	/**
	 * Removes the subtree rooted at this node from the tree, giving this
	 * node a null parent.  Does nothing if this node is the root of its
	 * tree.
	 */
	override fun removeFromParent() {
		mParent?.remove(this)
	}

	/**
	 * Removes `aChild` from this node's child array, giving it a
	 * null parent.
	 *
	 * @param    aChild    a child of this node to remove
	 * @exception    IllegalArgumentException    if `aChild`
	 * is null or is not a child of this node
	 */
	override fun remove(aChild: MutableTreeNode<T>) {
		requireNotNull(aChild) { "argument is null" }
		require(isNodeChild(aChild)) { "argument is not a child" }
		remove(getIndex(aChild)) // linear search
	}

	/**
	 * Removes all of this node's children, setting their parents to null.
	 * If this node has no children, this method does nothing.
	 */
	fun removeAllChildren() {
		for (i in childCount - 1 downTo 0) {
			remove(i)
		}
	}

	/**
	 * Removes `newChild` from its parent and makes it a child of
	 * this node by adding it to the end of this node's child array.
	 *
	 * @see .insert
	 *
	 * @param    newChild    node to add as a child of this node
	 * @exception    IllegalArgumentException    if `newChild`
	 * is null
	 * @exception    IllegalStateException    if this node does not allow
	 * children
	 */
	fun add(newChild: MutableTreeNode<T>) {
		if (newChild != null && newChild.parent === this) insert(
			newChild,
			childCount - 1
		) else insert(newChild, childCount)
	}
	//
	//  Tree Queries
	//
	/**
	 * Returns true if `anotherNode` is an ancestor of this node
	 * -- if it is this node, this node's parent, or an ancestor of this
	 * node's parent.  (Note that a node is considered an ancestor of itself.)
	 * If `anotherNode` is null, this method returns false.  This
	 * operation is at worst O(h) where h is the distance from the root to
	 * this node.
	 *
	 * @see .isNodeDescendant
	 *
	 * @see .getSharedAncestor
	 *
	 * @param    anotherNode    node to test as an ancestor of this node
	 * @return    true if this node is a descendant of `anotherNode`
	 */
	fun isNodeAncestor(anotherNode: TreeNode<T>?): Boolean {
		if (anotherNode == null) {
			return false
		}
		var ancestor: TreeNode<T>? = this
		do {
			if (ancestor === anotherNode) {
				return true
			}
		} while (ancestor?.parent.also { ancestor = it } != null)
		return false
	}

	/**
	 * Returns true if `anotherNode` is a descendant of this node
	 * -- if it is this node, one of this node's children, or a descendant of
	 * one of this node's children.  Note that a node is considered a
	 * descendant of itself.  If `anotherNode` is null, returns
	 * false.  This operation is at worst O(h) where h is the distance from the
	 * root to `anotherNode`.
	 *
	 * @see .isNodeAncestor
	 *
	 * @see .getSharedAncestor
	 *
	 * @param    anotherNode    node to test as descendant of this node
	 * @return    true if this node is an ancestor of `anotherNode`
	 */
	fun isNodeDescendant(anotherNode: DefaultMutableTreeNode<T>?): Boolean {
		return anotherNode?.isNodeAncestor(this) ?: false
	}

	/**
	 * Returns the nearest common ancestor to this node and `aNode`.
	 * Returns null, if no such ancestor exists -- if this node and
	 * `aNode` are in different trees or if `aNode` is
	 * null.  A node is considered an ancestor of itself.
	 *
	 * @see .isNodeAncestor
	 *
	 * @see .isNodeDescendant
	 *
	 * @param    aNode    node to find common ancestor with
	 * @return    nearest ancestor common to this node and `aNode`,
	 * or null if none
	 */
	fun getSharedAncestor(aNode: DefaultMutableTreeNode<T>?): TreeNode<T>? {
		if (aNode === this) {
			return this
		} else if (aNode == null) {
			return null
		}
		val level1: Int
		val level2: Int
		var diff: Int
		var node1: TreeNode<T>?
		var node2: TreeNode<T>?
		level1 = level
		level2 = aNode.level
		if (level2 > level1) {
			diff = level2 - level1
			node1 = aNode
			node2 = this
		} else {
			diff = level1 - level2
			node1 = this
			node2 = aNode
		}

		// Go up the tree until the nodes are at the same level
		while (diff > 0) {
			node1 = node1!!.parent
			diff--
		}

		// Move up the tree until we find a common ancestor.  Since we know
		// that both nodes are at the same level, we won't cross paths
		// unknowingly (if there is a common ancestor, both nodes hit it in
		// the same iteration).
		do {
			if (node1 === node2) {
				return node1
			}
			node1 = node1!!.parent
			node2 = node2!!.parent
		} while (node1 != null) // only need to check one -- they're at the
		// same level so if one is null, the other is
		if (node1 != null || node2 != null) {
			throw Error("nodes should be null")
		}
		return null
	}

	/**
	 * Returns true if and only if `aNode` is in the same tree
	 * as this node.  Returns false if `aNode` is null.
	 *
	 * @see .getSharedAncestor
	 *
	 * @see .getRoot
	 *
	 * @return    true if `aNode` is in the same tree as this node;
	 * false if `aNode` is null
	 */
	fun isNodeRelated(aNode: DefaultMutableTreeNode<T>?): Boolean {
		return aNode != null && this.root === aNode.root
	}

	/**
	 * Returns the depth of the tree rooted at this node -- the longest
	 * distance from this node to a leaf.  If this node has no children,
	 * returns 0.  This operation is much more expensive than
	 * `getLevel()` because it must effectively traverse the entire
	 * tree rooted at this node.
	 *
	 * @see .getLevel
	 *
	 * @return    the depth of the tree whose root is this node
	 */
	val depth: Int
		get() {
			var last: TreeNode<T>? = null
			val enum_ = breadthFirstEnumeration()
			while (enum_.hasMoreElements()) {
				last = enum_.nextElement()
			}
			if (last == null) {
				throw Error("nodes should be null")
			}
			return (last as DefaultMutableTreeNode<T>).level - level
		}

	/**
	 * Returns the number of levels above this node -- the distance from
	 * the root to this node.  If this node is the root, returns 0.
	 *
	 * @see .getDepth
	 *
	 * @return    the number of levels above this node
	 */
	val level: Int
		get() {
			var ancestor: TreeNode<T>?
			var levels = 0
			ancestor = this
			while (ancestor?.parent?.also { ancestor = it } != null) {
				levels++
			}
			return levels
		}

	/**
	 * Returns the path from the root, to get to this node.  The last
	 * element in the path is this node.
	 *
	 * @return an array of TreeNode<T> objects giving the path, where the
	 * first element in the path is the root and the last
	 * element is this node.
	</T> */
	val path: Array<TreeNode<T>>?
		get() = getPathToRoot(this, 0)?.filterNotNull()?.toTypedArray()

	/**
	 * Builds the parents of node up to and including the root node,
	 * where the original node is the last element in the returned array.
	 * The length of the returned array gives the node's depth in the
	 * tree.
	 *
	 * @param aNode  the TreeNode<T> to get the path for
	 * @param depth  an int giving the number of steps already taken towards
	 * the root (on recursive calls), used to size the returned array
	 * @return an array of TreeNodes giving the path from the root to the
	 * specified node
	</T> */
	protected fun getPathToRoot(aNode: TreeNode<T>?, depth: Int): Array<TreeNode<T>?>? {
		var depth = depth
		val retNodes: Array<TreeNode<T>?>?

		/* Check for null, in case someone passed in a null node, or
 they passed in an element that isn't rooted at root. */if (aNode == null) {
			if (depth == 0) return null else retNodes = arrayOfNulls<TreeNode<T>>(depth)
		} else {
			depth++
			retNodes = getPathToRoot(aNode.parent!!, depth)
			retNodes!![retNodes.size - depth] = aNode
		}
		return retNodes
	}

	/**
	 * Returns the user object path, from the root, to get to this node.
	 * If some of the TreeNodes in the path have null user objects, the
	 * returned path will contain nulls.
	 */
	val userObjectPath: Array<Any?>
		get() {
			val realPath = path
			val retPath = arrayOfNulls<Any>(realPath!!.size)
			for (counter in realPath.indices) retPath[counter] =
				(realPath[counter] as DefaultMutableTreeNode<T>)
					.userObject
			return retPath
		}

	/**
	 * Returns the root of the tree that contains this node.  The root is
	 * the ancestor with a null parent.
	 *
	 * @see .isNodeAncestor
	 *
	 * @return    the root of the tree that contains this node
	 */
	val root: TreeNode<T>?
		get() {
			var ancestor: TreeNode<T>? = this
			var previous: TreeNode<T>?
			do {
				previous = ancestor
				ancestor = ancestor!!.parent
			} while (ancestor != null)
			return previous
		}

	/**
	 * Returns true if this node is the root of the tree.  The root is
	 * the only node in the tree with a null parent; every tree has exactly
	 * one root.
	 *
	 * @return    true if this node is the root of its tree
	 */
	fun isRoot(): Boolean {
		return parent == null
	}// No children, so look for nextSibling

	/**
	 * Returns the node that follows this node in a preorder traversal of this
	 * node's tree.  Returns null if this node is the last node of the
	 * traversal.  This is an inefficient way to traverse the entire tree; use
	 * an enumeration, instead.
	 *
	 * @see .preorderEnumeration
	 *
	 * @return    the node that follows this node in a preorder traversal, or
	 * null if this node is last
	 */
	val nextNode: DefaultMutableTreeNode<T>?
		get() {
			if (childCount == 0) {
				// No children, so look for nextSibling
				var nextSibling = nextSibling
				if (nextSibling == null) {
					var aNode = parent as? DefaultMutableTreeNode<T>
					do {
						if (aNode == null) {
							return null
						}
						nextSibling = aNode.nextSibling
						if (nextSibling != null) {
							return nextSibling
						}
						aNode = aNode.parent as DefaultMutableTreeNode<T>
					} while (true)
				} else {
					return nextSibling
				}
			} else {
				return getChildAt(0) as DefaultMutableTreeNode<T>
			}
		}

	/**
	 * Returns the node that precedes this node in a preorder traversal of
	 * this node's tree.  Returns `null` if this node is the
	 * first node of the traversal -- the root of the tree.
	 * This is an inefficient way to
	 * traverse the entire tree; use an enumeration, instead.
	 *
	 * @see .preorderEnumeration
	 *
	 * @return    the node that precedes this node in a preorder traversal, or
	 * null if this node is the first
	 */
	val previousNode: DefaultMutableTreeNode<T>?
		get() {
			val previousSibling: DefaultMutableTreeNode<T>?
			val myParent = parent as? DefaultMutableTreeNode<T>
				?: return null
			previousSibling = this.previousSibling
			return if (previousSibling != null) {
				if (previousSibling.childCount == 0) previousSibling else previousSibling.lastLeaf
			} else {
				myParent
			}
		}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in preorder.  The first node returned by the enumeration's
	 * `nextElement()` method is this node.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see .postorderEnumeration
	 *
	 * @return    an enumeration for traversing the tree in preorder
	</P> */
	fun preorderEnumeration(): Enumeration<TreeNode<T>> {
		return PreorderEnumeration(this)
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in postorder.  The first node returned by the enumeration's
	 * `nextElement()` method is the leftmost leaf.  This is the
	 * same as a depth-first traversal.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see .depthFirstEnumeration
	 *
	 * @see .preorderEnumeration
	 *
	 * @return    an enumeration for traversing the tree in postorder
	</P> */
	fun postorderEnumeration(): Enumeration<TreeNode<T>?> {
		return PostorderEnumeration<T>(this)
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in breadth-first order.  The first node returned by the
	 * enumeration's `nextElement()` method is this node.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see .depthFirstEnumeration
	 *
	 * @return    an enumeration for traversing the tree in breadth-first order
	</P> */
	fun breadthFirstEnumeration(): Enumeration<TreeNode<T>> {
		return BreadthFirstEnumeration<T>(this)
	}

	/**
	 * Creates and returns an enumeration that traverses the subtree rooted at
	 * this node in depth-first order.  The first node returned by the
	 * enumeration's `nextElement()` method is the leftmost leaf.
	 * This is the same as a postorder traversal.<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see .breadthFirstEnumeration
	 *
	 * @see .postorderEnumeration
	 *
	 * @return    an enumeration for traversing the tree in depth-first order
	</P> */
	fun depthFirstEnumeration(): Enumeration<TreeNode<T>?> {
		return postorderEnumeration()
	}

	/**
	 * Creates and returns an enumeration that follows the path from
	 * `ancestor` to this node.  The enumeration's
	 * `nextElement()` method first returns `ancestor`,
	 * then the child of `ancestor` that is an ancestor of this
	 * node, and so on, and finally returns this node.  Creation of the
	 * enumeration is O(m) where m is the number of nodes between this node
	 * and `ancestor`, inclusive.  Each `nextElement()`
	 * message is O(1).<P>
	 *
	 * Modifying the tree by inserting, removing, or moving a node invalidates
	 * any enumerations created before the modification.
	 *
	 * @see .isNodeAncestor
	 *
	 * @see .isNodeDescendant
	 *
	 * @exception    IllegalArgumentException if `ancestor` is
	 * not an ancestor of this node
	 * @return    an enumeration for following the path from an ancestor of
	 * this node to this one
	</P> */
	fun pathFromAncestorEnumeration(ancestor: TreeNode<T>?): Enumeration<TreeNode<T>?> {
		return PathBetweenNodesEnumeration<T>(ancestor, this)
	}
	//
	//  Child Queries
	//
	/**
	 * Returns true if `aNode` is a child of this node.  If
	 * `aNode` is null, this method returns false.
	 *
	 * @return    true if `aNode` is a child of this node; false if
	 * `aNode` is null
	 */
	fun isNodeChild(aNode: TreeNode<T>?): Boolean {
		val retval: Boolean
		if (aNode == null) {
			retval = false
		} else {
			if (childCount == 0) {
				retval = false
			} else {
				retval = aNode.parent === this
			}
		}
		return retval
	}

	/**
	 * Returns this node's first child.  If this node has no children,
	 * throws NoSuchElementException.
	 *
	 * @return    the first child of this node
	 * @exception    NoSuchElementException    if this node has no children
	 */
	val firstChild: TreeNode<T>
		get() {
			if (childCount == 0) {
				throw NoSuchElementException("node has no children")
			}
			return getChildAt(0)
		}

	/**
	 * Returns this node's last child.  If this node has no children,
	 * throws NoSuchElementException.
	 *
	 * @return    the last child of this node
	 * @exception    NoSuchElementException    if this node has no children
	 */
	val lastChild: TreeNode<T>
		get() {
			if (childCount == 0) {
				throw NoSuchElementException("node has no children")
			}
			return getChildAt(childCount - 1)
		}

	/**
	 * Returns the child in this node's child array that immediately
	 * follows `aChild`, which must be a child of this node.  If
	 * `aChild` is the last child, returns null.  This method
	 * performs a linear search of this node's children for
	 * `aChild` and is O(n) where n is the number of children; to
	 * traverse the entire array of children, use an enumeration instead.
	 *
	 * @see .children
	 *
	 * @exception    IllegalArgumentException if `aChild` is
	 * null or is not a child of this node
	 * @return    the child of this node that immediately follows
	 * `aChild`
	 */
	fun getChildAfter(aChild: TreeNode<T>?): TreeNode<T>? {
		requireNotNull(aChild) { "argument is null" }
		val index = getIndex(aChild) // linear search
		require(index != -1) { "node is not a child" }
		return if (index < childCount - 1) {
			getChildAt(index + 1)
		} else {
			null
		}
	}

	/**
	 * Returns the child in this node's child array that immediately
	 * precedes `aChild`, which must be a child of this node.  If
	 * `aChild` is the first child, returns null.  This method
	 * performs a linear search of this node's children for `aChild`
	 * and is O(n) where n is the number of children.
	 *
	 * @exception    IllegalArgumentException if `aChild` is null
	 * or is not a child of this node
	 * @return    the child of this node that immediately precedes
	 * `aChild`
	 */
	fun getChildBefore(aChild: TreeNode<T>?): TreeNode<T>? {
		requireNotNull(aChild) { "argument is null" }
		val index = getIndex(aChild) // linear search
		require(index != -1) { "argument is not a child" }
		return if (index > 0) {
			getChildAt(index - 1)
		} else {
			null
		}
	}
	//
	//  Sibling Queries
	//
	/**
	 * Returns true if `anotherNode` is a sibling of (has the
	 * same parent as) this node.  A node is its own sibling.  If
	 * `anotherNode` is null, returns false.
	 *
	 * @param    anotherNode    node to test as sibling of this node
	 * @return    true if `anotherNode` is a sibling of this node
	 */
	fun isNodeSibling(anotherNode: TreeNode<T>?): Boolean {
		val retval: Boolean
		if (anotherNode == null) {
			retval = false
		} else if (anotherNode === this) {
			retval = true
		} else {
			val myParent = parent
			retval = myParent != null && myParent === anotherNode.parent

			if (retval && !(parent as DefaultMutableTreeNode<T>)
					.isNodeChild(anotherNode)
			) {
				throw Error("sibling has different parent")
			}
		}
		return retval
	}

	/**
	 * Returns the number of siblings of this node.  A node is its own sibling
	 * (if it has no parent or no siblings, this method returns
	 * `1`).
	 *
	 * @return    the number of siblings of this node
	 */
	val siblingCount: Int
		get() {
			val myParent = parent
			return myParent?.childCount ?: 1
		}// linear search

	/**
	 * Returns the next sibling of this node in the parent's children array.
	 * Returns null if this node has no parent or is the parent's last child.
	 * This method performs a linear search that is O(n) where n is the number
	 * of children; to traverse the entire array, use the parent's child
	 * enumeration instead.
	 *
	 * @see .children
	 *
	 * @return    the sibling of this node that immediately follows this node
	 */
	val nextSibling: DefaultMutableTreeNode<T>?
		get() {
			val retval: DefaultMutableTreeNode<T>?
			val myParent = parent as? DefaultMutableTreeNode<T>
			retval = if (myParent == null) {
				null
			} else {
				myParent.getChildAfter(this) as DefaultMutableTreeNode<T>? // linear search
			}
			if (retval != null && !isNodeSibling(retval)) {
				throw Error("child of parent is not a sibling")
			}
			return retval
		}// linear search

	/**
	 * Returns the previous sibling of this node in the parent's children
	 * array.  Returns null if this node has no parent or is the parent's
	 * first child.  This method performs a linear search that is O(n) where n
	 * is the number of children.
	 *
	 * @return    the sibling of this node that immediately precedes this node
	 */
	val previousSibling: DefaultMutableTreeNode<T>?
		get() {
			val retval: DefaultMutableTreeNode<T>?
			val myParent = parent as? DefaultMutableTreeNode<T>
			retval = if (myParent == null) {
				null
			} else {
				myParent.getChildBefore(this) as DefaultMutableTreeNode<T>? // linear search
			}
			if (retval != null && !isNodeSibling(retval)) {
				throw Error("child of parent is not a sibling")
			}
			return retval
		}

	//
	//  Leaf Queries
	//
	override val isLeaf: Boolean
		/**
		 * Returns true if this node has no children.  To distinguish between
		 * nodes that have no children and nodes that *cannot* have
		 * children (e.g. to distinguish files from empty directories), use this
		 * method in conjunction with `getAllowsChildren`
		 *
		 * @see .getAllowsChildren
		 *
		 * @return    true if this node has no children
		 */
		get() = childCount == 0

	/**
	 * Finds and returns the first leaf that is a descendant of this node --
	 * either this node or its first child's first leaf.
	 * Returns this node if it is a leaf.
	 *
	 * @see .isLeaf
	 *
	 * @see .isNodeDescendant
	 *
	 * @return    the first leaf in the subtree rooted at this node
	 */
	val firstLeaf: DefaultMutableTreeNode<T>
		get() {
			var node = this
			while (!node.isLeaf) {
				node = node.firstChild as DefaultMutableTreeNode<T>
			}
			return node
		}

	/**
	 * Finds and returns the last leaf that is a descendant of this node --
	 * either this node or its last child's last leaf.
	 * Returns this node if it is a leaf.
	 *
	 * @see .isLeaf
	 *
	 * @see .isNodeDescendant
	 *
	 * @return    the last leaf in the subtree rooted at this node
	 */
	val lastLeaf: DefaultMutableTreeNode<T>
		get() {
			var node = this
			while (!node.isLeaf) {
				node = node.lastChild as DefaultMutableTreeNode<T>
			}
			return node
		}// linear search
	// tail recursion
	/**
	 * Returns the leaf after this node or null if this node is the
	 * last leaf in the tree.
	 *
	 *
	 * In this implementation of the `MutableNode` interface,
	 * this operation is very inefficient. In order to determine the
	 * next node, this method first performs a linear search in the
	 * parent's child-list in order to find the current node.
	 *
	 *
	 * That implementation makes the operation suitable for short
	 * traversals from a known position. But to traverse all of the
	 * leaves in the tree, you should use `depthFirstEnumeration`
	 * to enumerate the nodes in the tree and use `isLeaf`
	 * on each node to determine which are leaves.
	 *
	 * @see .depthFirstEnumeration
	 *
	 * @see .isLeaf
	 *
	 * @return    returns the next leaf past this node
	 */
	val nextLeaf: DefaultMutableTreeNode<T>?
		get() {
			val nextSibling: DefaultMutableTreeNode<T>?
			val myParent = parent as? DefaultMutableTreeNode<T>
				?: return null
			nextSibling = this.nextSibling // linear search
			return nextSibling?.firstLeaf ?: myParent.nextLeaf
			// tail recursion
		}// linear search
	// tail recursion
	/**
	 * Returns the leaf before this node or null if this node is the
	 * first leaf in the tree.
	 *
	 *
	 * In this implementation of the `MutableNode` interface,
	 * this operation is very inefficient. In order to determine the
	 * previous node, this method first performs a linear search in the
	 * parent's child-list in order to find the current node.
	 *
	 *
	 * That implementation makes the operation suitable for short
	 * traversals from a known position. But to traverse all of the
	 * leaves in the tree, you should use `depthFirstEnumeration`
	 * to enumerate the nodes in the tree and use `isLeaf`
	 * on each node to determine which are leaves.
	 *
	 * @see .depthFirstEnumeration
	 *
	 * @see .isLeaf
	 *
	 * @return    returns the leaf before this node
	 */
	val previousLeaf: DefaultMutableTreeNode<T>?
		get() {
			val previousSibling: DefaultMutableTreeNode<T>?
			val myParent = parent as? DefaultMutableTreeNode<T>
				?: return null
			previousSibling = this.previousSibling // linear search
			return previousSibling?.lastLeaf ?: myParent.previousLeaf
			// tail recursion
		}// order matters not

	/**
	 * Returns the total number of leaves that are descendants of this node.
	 * If this node is a leaf, returns `1`.  This method is O(n)
	 * where n is the number of descendants of this node.
	 *
	 * @see .isNodeAncestor
	 *
	 * @return    the number of leaves beneath this node
	 */
	val leafCount: Int
		get() {
			var count = 0
			var node: TreeNode<T>
			val enum_ = breadthFirstEnumeration() // order matters not
			while (enum_.hasMoreElements()) {
				node = enum_.nextElement()
				if (node.isLeaf) {
					count++
				}
			}
			if (count < 1) {
				throw Error("tree has zero leaves")
			}
			return count
		}
	//
	//  Overrides
	//
	/**
	 * Returns the result of sending `toString()` to this node's
	 * user object, or null if this node has no user object.
	 *
	 * @see .getUserObject
	 */
	override fun toString(): String {
		return if (mUserObject == null) {
			null.toString()
		} else {
			mUserObject.toString()
		}
	}

	/**
	 * Overridden to make clone public.  Returns a shallow copy of this node;
	 * the new node has no parent or children and has a reference to the same
	 * user object, if any.
	 *
	 * @return    a copy of this node
	 */
	public override fun clone(): DefaultMutableTreeNode<T> {
		var newNode: DefaultMutableTreeNode<T>? = null
		try {
			newNode = super.clone() as DefaultMutableTreeNode<T>

			// shallow copy -- the new node has no parent or children
			newNode.children = null
			newNode.mParent = null
		} catch (e: CloneNotSupportedException) {
			// Won't happen because we implement Cloneable
			throw Error(e.toString())
		}
		return newNode
	}

	// Serialization support.
	@Throws(IOException::class)
	private fun writeObject(s: ObjectOutputStream) {
		val tValues: Array<Any?>
		s.defaultWriteObject()
		// Save the userObject, if its Serializable.
		if (mUserObject != null && mUserObject is Serializable) {
			tValues = arrayOfNulls(2)
			tValues[0] = "userObject"
			tValues[1] = mUserObject
		} else tValues = arrayOfNulls(0)
		s.writeObject(tValues)
	}

	@Throws(IOException::class, ClassNotFoundException::class)
	private fun readObject(s: ObjectInputStream) {
		val tValues: Array<T>?
		s.defaultReadObject()
		tValues = s.readObject() as? Array<T>
		if (!tValues.isNullOrEmpty() && tValues[0] == "userObject") mUserObject = tValues[1]
	}

	internal inner class PreorderEnumeration(rootNode: TreeNode<T>) : Enumeration<TreeNode<T>> {
		protected var stack: Stack<Enumeration<TreeNode<T>>>

		init {
			val v = Vector<TreeNode<T>>(1)
			v.addElement(rootNode) // PENDING: don't really need a vector
			stack = Stack()
			stack.push(v.elements())
		}

		override fun hasMoreElements(): Boolean {
			return !stack.empty() &&
				stack.peek().hasMoreElements()
		}

		override fun nextElement(): TreeNode<T> {
			val enumer = stack.peek()
			val node = enumer.nextElement()
			val children = node.children()
			if (!enumer.hasMoreElements()) {
				stack.pop()
			}
			if (children.hasMoreElements()) {
				stack.push(children)
			}
			return node
		}
	} // End of class PreorderEnumeration

	internal inner class PostorderEnumeration<X>(protected var root: TreeNode<X>?) :
		Enumeration<TreeNode<X>?> {
		protected var children: Enumeration<TreeNode<X>>
		protected var subtree: Enumeration<TreeNode<X>?>

		init {
			children = root!!.children()
			subtree = emptyEnumeration()
		}

		override fun hasMoreElements(): Boolean {
			return root != null
		}

		override fun nextElement(): TreeNode<X>? {
			val retval: TreeNode<X>?
			if (subtree.hasMoreElements()) {
				retval = subtree.nextElement()
			} else if (children.hasMoreElements()) {
				subtree = PostorderEnumeration<X>(children.nextElement())
				retval = subtree.nextElement()
			} else {
				retval = root
				root = null
			}
			return retval
		}
	} // End of class PostorderEnumeration

	internal inner class BreadthFirstEnumeration<X>(rootNode: TreeNode<X>) :
		Enumeration<TreeNode<X>> {
		protected var queue: Queue<Enumeration<TreeNode<X>>>

		init {
			val v = Vector<TreeNode<X>>(1)
			v.addElement(rootNode) // PENDING: don't really need a vector
			queue = Queue<Enumeration<TreeNode<X>>>()
			queue.enqueue(v.elements())
		}

		override fun hasMoreElements(): Boolean {
			return !queue.isEmpty &&
				queue.firstObject().hasMoreElements()
		}

		override fun nextElement(): TreeNode<X> {
			val enumer: Enumeration<TreeNode<X>> = queue.firstObject()
			val node = enumer.nextElement()
			val children = node.children()
			if (!enumer.hasMoreElements()) {
				queue.dequeue()
			}
			if (children.hasMoreElements()) {
				queue.enqueue(children)
			}
			return node
		}

		// A simple queue with a linked list data structure.
		internal inner class Queue<Y> {
			var head // null if empty
				: QNode<Y>? = null
			var tail: QNode<Y>? = null

			internal inner class QNode<Z>(var `object`: Z, next: QNode<Z>?) {
				var next // null if end
					: QNode<Z>?

				init {
					this.next = next
				}
			}

			fun enqueue(anObject: Y) {
				if (head == null) {
					tail = QNode<Y>(anObject, null)
					head = tail
				} else {
					tail?.next = QNode<Y>(anObject, null)
					tail = tail?.next
				}
			}

			fun dequeue(): Any {
				return this.head
					?.let { head ->
						val retval: Y = head.`object`
						val oldHead: QNode<Y> = head
						this.head = head.next
						if (this.head == null) tail = null else oldHead.next = null
						retval
					}
					?: throw NoSuchElementException("No more elements")
			}

			fun firstObject(): Y {
				return head?.`object` ?: throw NoSuchElementException("No more elements")
			}

			val isEmpty: Boolean
				get() = head == null
		} // End of class Queue
	} // End of class BreadthFirstEnumeration

	internal inner class PathBetweenNodesEnumeration<X>(
		ancestor: TreeNode<X>?,
		descendant: TreeNode<X>?
	) : Enumeration<TreeNode<X>?> {
		protected var stack: Stack<TreeNode<X>?>

		init {
			require(!(ancestor == null || descendant == null)) { "argument is null" }
			var current: TreeNode<X>?
			stack = Stack()
			stack.push(descendant)
			current = descendant
			while (current !== ancestor) {
				current = current!!.parent
				require(!(current == null && descendant !== ancestor)) {
					"node " + ancestor +
						" is not an ancestor of " + descendant
				}
				stack.push(current)
			}
		}

		override fun hasMoreElements(): Boolean {
			return stack.size > 0
		}

		override fun nextElement(): TreeNode<X>? {
			return try {
				stack.pop()
			} catch (e: EmptyStackException) {
				throw NoSuchElementException("No more elements")
			}
		}
	} // End of class PathBetweenNodesEnumeration

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 7195119412898901913L

		/**
		 * An enumeration that is always empty. This is used when an enumeration
		 * of a leaf node's children is requested.
		 */
		fun <X> emptyEnumeration(): Enumeration<X> {
			return object : Enumeration<X> {
				override fun hasMoreElements(): Boolean {
					return false
				}

				override fun nextElement(): X {
					throw NoSuchElementException("No more elements")
				}
			}
		}
	}
} // End of class DefaultMutableTreeNode
