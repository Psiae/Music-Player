/*
 * @(#)EventListenerList.java	1.38 10/03/23
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
 * A class that holds a list of EventListeners.  A single instance
 * can be used to hold all listeners (of all types) for the instance
 * using the list.  It is the responsiblity of the class using the
 * EventListenerList to provide type-safe API (preferably conforming
 * to the JavaBeans spec) and methods which dispatch event notification
 * methods to appropriate Event Listeners on the list.
 *
 * The main benefits that this class provides are that it is relatively
 * cheap in the case of no listeners, and it provides serialization for
 * event-listener lists in a single place, as well as a degree of MT safety
 * (when used correctly).
 *
 * Usage example:
 * Say one is defining a class that sends out FooEvents, and one wants
 * to allow users of the class to register FooListeners and receive
 * notification when FooEvents occur.  The following should be added
 * to the class definition:
 * <pre>
 * EventListenerList listenerList = new EventListenerList();
 * FooEvent fooEvent = null;
 *
 * public void addFooListener(FooListener l) {
 * listenerList.add(FooListener.class, l);
 * }
 *
 * public void removeFooListener(FooListener l) {
 * listenerList.remove(FooListener.class, l);
 * }
 *
 *
 * // Notify all listeners that have registered interest for
 * // notification on this event type.  The event instance
 * // is lazily created using the parameters passed into
 * // the fire method.
 *
 * protected void fireFooXXX() {
 * // Guaranteed to return a non-null array
 * Object[] listeners = listenerList.getListenerList();
 * // Process the listeners last to first, notifying
 * // those that are interested in this event
 * for (int i = listeners.length-2; i>=0; i-=2) {
 * if (listeners[i]==FooListener.class) {
 * // Lazily create the event:
 * if (fooEvent == null)
 * fooEvent = new FooEvent(this);
 * ((FooListener)listeners[i+1]).fooXXX(fooEvent);
 * }
 * }
 * }
</pre> *
 * foo should be changed to the appropriate name, and fireFooXxx to the
 * appropriate method name.  One fire method should exist for each
 * notification method in the FooListener interface.
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
 * @version 1.38 03/23/10
 * @author Georges Saab
 * @author Hans Muller
 * @author James Gosling
 */
class EventListenerList : Serializable {
	/**
	 * Passes back the event listener list as an array
	 * of ListenerType-listener pairs.  Note that for
	 * performance reasons, this implementation passes back
	 * the actual data structure in which the listener data
	 * is stored internally!
	 * This method is guaranteed to pass back a non-null
	 * array, so that no null-checking is required in
	 * fire methods.  A zero-length array of Object should
	 * be returned if there are currently no listeners.
	 *
	 * WARNING!!! Absolutely NO modification of
	 * the data contained in this array should be made -- if
	 * any such manipulation is necessary, it should be done
	 * on a copy of the array returned rather than the array
	 * itself.
	 */
	/* The list of ListenerType - Listener pairs */
	@Transient
	var listenerList = NULL_ARRAY
		protected set

	/**
	 * Return an array of all the listeners of the given type.
	 * @return all of the listeners of the specified type.
	 * @exception  ClassCastException if the supplied class
	 * is not assignable to EventListener
	 *
	 * @since 1.3
	 */
	fun <T : EventListener?> getListeners(t: Class<T>): Array<T?> {
		val lList = listenerList
		val n = getListenerCount(lList, t)
		val result = java.lang.reflect.Array.newInstance(t, n) as Array<T?>
		var j = 0
		var i = lList.size - 2
		while (i >= 0) {
			if (lList[i] === t) {
				result[j++] = lList[i + 1] as T?
			}
			i -= 2
		}
		return result
	}

	/**
	 * Returns the total number of listeners for this listener list.
	 */
	val listenerCount: Int
		get() = listenerList.size / 2

	/**
	 * Returns the total number of listeners of the supplied type
	 * for this listener list.
	 */
	fun getListenerCount(t: Class<*>): Int {
		val lList = listenerList
		return getListenerCount(lList, t)
	}

	private fun getListenerCount(list: Array<Any?>, t: Class<*>): Int {
		var count = 0
		var i = 0
		while (i < list.size) {
			if (t == list[i] as Class<*>?) count++
			i += 2
		}
		return count
	}

	/**
	 * Adds the listener as a listener of the specified type.
	 * @param t the type of the listener to be added
	 * @param l the listener to be added
	 */
	@Synchronized
	fun <T : EventListener?> add(t: Class<T>, l: T?) {
		if (l == null) {
			// In an ideal world, we would do an assertion here
			// to help developers know they are probably doing
			// something wrong
			return
		}
		require(t.isInstance(l)) {
			"Listener " + l +
				" is not of type " + t
		}
		if (listenerList == NULL_ARRAY) {
			// if this is the first listener added,
			// initialize the lists
			listenerList = arrayOf(t, l)
		} else {
			// Otherwise copy the array and add the new listener
			val i = listenerList.size
			val tmp = arrayOfNulls<Any>(i + 2)
			System.arraycopy(listenerList, 0, tmp, 0, i)
			tmp[i] = t
			tmp[i + 1] = l
			listenerList = tmp
		}
	}

	/**
	 * Removes the listener as a listener of the specified type.
	 * @param t the type of the listener to be removed
	 * @param l the listener to be removed
	 */
	@Synchronized
	fun <T : EventListener?> remove(t: Class<T>, l: T?) {
		if (l == null) {
			// In an ideal world, we would do an assertion here
			// to help developers know they are probably doing
			// something wrong
			return
		}
		require(t.isInstance(l)) {
			"Listener " + l +
				" is not of type " + t
		}
		// Is l on the list?
		var index = -1
		var i = listenerList.size - 2
		while (i >= 0) {
			if (listenerList[i] === t && listenerList[i + 1] == l == true) {
				index = i
				break
			}
			i -= 2
		}

		// If so,  remove it
		if (index != -1) {
			val tmp = arrayOfNulls<Any>(listenerList.size - 2)
			// Copy the list up to index
			System.arraycopy(listenerList, 0, tmp, 0, index)
			// Copy from two past the index, up to
			// the end of tmp (which is two elements
			// shorter than the old list)
			if (index < tmp.size) System.arraycopy(
				listenerList, index + 2, tmp, index,
				tmp.size - index
			)
			// set the listener array to the new array or null
			listenerList = if (tmp.size == 0) NULL_ARRAY else tmp
		}
	}

	// Serialization support.
	@Throws(IOException::class)
	private fun writeObject(s: ObjectOutputStream) {
		val lList = listenerList
		s.defaultWriteObject()

		// Save the non-null event listeners:
		var i = 0
		while (i < lList.size) {
			val t = lList[i] as Class<*>?
			val l = lList[i + 1] as EventListener?
			if (l != null && l is Serializable) {
				s.writeObject(t!!.name)
				s.writeObject(l)
			}
			i += 2
		}
		s.writeObject(null)
	}

	@Throws(IOException::class, ClassNotFoundException::class)
	private fun readObject(s: ObjectInputStream) {
		listenerList = NULL_ARRAY
		s.defaultReadObject()
		var listenerTypeOrNull: Any?
		while (null != s.readObject().also { listenerTypeOrNull = it }) {
			val cl = Thread.currentThread().contextClassLoader
			val l = s.readObject() as EventListener
			val eventListenerClass =
				Class.forName(listenerTypeOrNull as String?, true, cl) as Class<EventListener>
			add(eventListenerClass, l)
		}
	}

	/**
	 * Returns a string representation of the EventListenerList.
	 */
	override fun toString(): String {
		val lList = listenerList
		var s = "EventListenerList: "
		s += (lList.size / 2).toString() + " listeners: "
		var i = 0
		while (i <= lList.size - 2) {
			s += " type " + (lList[i] as Class<*>?)!!.name
			s += " listener " + lList[i + 1]
			i += 2
		}
		return s
	}

	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 8528835753144198230L

		/* A null array to be shared by all empty listener lists*/
		private val NULL_ARRAY = arrayOfNulls<Any>(0)
	}
}
