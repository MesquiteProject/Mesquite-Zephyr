/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

/* Adapted from the CIPRES java library class */
package mesquite.zephyr.lib;

public class ZephyrStringUtil {

	public static String escapeBackslashes(final String s) {
		return s.replaceAll("\\\\", "\\\\\\\\");
	}

	public static String[] arrayConcatenate(final String[] f, final String[] s) {
		final int fLen = (f == null ? 0 : f.length);
		final int sLen = (s == null ? 0 : s.length);
		final int len = fLen + sLen;
		final String[] ret = new String[len];
		if (fLen > 0) {
			System.arraycopy(f, 0, ret, 0, fLen);
			if (sLen > 0) {
				System.arraycopy(s, 0, ret, fLen, sLen);
			}
		}
		else if (sLen > 0) {
			System.arraycopy(s, 0, ret, 0, sLen);
		}
		return ret;
	}

	/**
	 * Joins all of the strings in arr into one String with separator added
	 * between each element.
	 */
	public static String join(final String[] arr, final String separator) {
		if (arr.length < 2)
			return (arr.length == 1 ? arr[0] : "");
		final StringBuffer sb = new StringBuffer(arr[0]);
		for (int i = 1; i < arr.length; ++i) {
			sb.append(separator);
			sb.append(arr[i]);
		}
		return sb.toString();
	}


	/**
	 * @return true if el equals any of the elements in the array ar.
	 */
	public static boolean inArray(final String el, final String[] ar) {
		return ZephyrStringUtil.index(el, ar) != -1;
	}

	/**
	 * @return the index of the first element in "ar" that is equals to "el" or
	 *         -1 (if there is no element equal to el).
	 */
	public static int index(final String el, final String[] ar) {
		for (int i = 0; i < ar.length; ++i) {
			if (ar[i].equals(el))
				return i;
		}
		return -1;
	}

}
