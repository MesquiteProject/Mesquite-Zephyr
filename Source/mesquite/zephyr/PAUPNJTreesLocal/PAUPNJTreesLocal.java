/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPNJTreesLocal;

import mesquite.zephyr.PAUPNJRunnerLocal.PAUPNJRunnerLocal;
import mesquite.zephyr.lib.PAUPNJTrees;

public class PAUPNJTreesLocal extends PAUPNJTrees {

	
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2100;  
	}




	public String getExplanation() {
		return "If PAUP is installed, will save a copy of a character matrix and script PAUP to conduct a neighbor-joining or bootstrap neighbor-joining, and harvest the resulting trees.";
	}
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPNJRunnerLocal.PAUPNJRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPNJRunnerLocal.class;
	}
	/*.................................................................................................................*/
	public String getProgramLocation(){
		return "Local";
	}
	/*.................................................................................................................*/

	public String getName() {
		return "PAUP* Trees (NJ) [Local]";
	}


}
