package com.flammky.musicplayer.common.media.audio.meta_tag.utils

/**
 * Collected methods which allow easy implementation of `equals`.
 *
 * Example use case in a class called Car:
 * <pre>
 * public boolean equals(Object aThat){
 * if ( this == aThat ) return true;
 * if ( !(aThat instanceof Car) ) return false;
 * Car that = (Car)aThat;
 * return
 * EqualsUtil.areEqual(this.fName, that.fName) &&
 * EqualsUtil.areEqual(this.fNumDoors, that.fNumDoors) &&
 * EqualsUtil.areEqual(this.fGasMileage, that.fGasMileage) &&
 * EqualsUtil.areEqual(this.fColor, that.fColor) &&
 * Arrays.equals(this.fMaintenanceChecks, that.fMaintenanceChecks); //array!
 * }
</pre> *
 *
 * *Arrays are not handled by this class*.
 * This is because the `Arrays.equals` methods should be used for
 * array fields.
 */
object EqualsUtil {
	fun areEqual(aThis: Boolean, aThat: Boolean): Boolean {
		//System.out.println("boolean");
		return aThis == aThat
	}

	fun areEqual(aThis: Char, aThat: Char): Boolean {
		//System.out.println("char");
		return aThis == aThat
	}

	fun areEqual(aThis: Long, aThat: Long): Boolean {
		/*
		* Implementation Note
		* Note that byte, short, and int are handled by this method, through
		* implicit conversion.
		*/
		//System.out.println("long");
		return aThis == aThat
	}

	fun areEqual(aThis: Float, aThat: Float): Boolean {
		//System.out.println("float");
		return java.lang.Float.floatToIntBits(aThis) == java.lang.Float.floatToIntBits(aThat)
	}

	fun areEqual(aThis: Double, aThat: Double): Boolean {
		//System.out.println("double");
		return java.lang.Double.doubleToLongBits(aThis) == java.lang.Double.doubleToLongBits(aThat)
	}

	/**
	 * Possibly-null object field.
	 *
	 * Includes type-safe enumerations and collections, but does not include
	 * arrays. See class comment.
	 */
	@JvmStatic
	fun areEqual(aThis: Any?, aThat: Any?): Boolean {
		//System.out.println("Object");
		return if (aThis == null) aThat == null else aThis == aThat
	}
}
