/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPDistanceTreesLocal;

import mesquite.zephyr.PAUPDistanceRunnerLocal.PAUPDistanceRunnerLocal;
import mesquite.zephyr.lib.PAUPDistanceTrees;

public class PAUPDistanceTreesLocal extends PAUPDistanceTrees {

		/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2000;  
	}


	public String getExplanation() {
		return "If PAUP is installed, will save a copy of a character matrix and script PAUP to conduct a distance search, and harvest the resulting trees.";
	}
	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPDistanceRunnerLocal.PAUPDistanceRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPDistanceRunnerLocal.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}

		/*.................................................................................................................*/

		public String getName() {
			return "PAUP* Trees (Distance) [Local]";
		}

}
