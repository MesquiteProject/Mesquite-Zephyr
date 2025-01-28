/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.TNTTreesSSH;

import java.util.*;


import mesquite.zephyr.TNTRunnerSSH.TNTRunnerSSH;
import mesquite.zephyr.lib.*;


public class TNTTreesSSH extends TNTTrees {

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2500;  
	}


	public String getExplanation() {
		return "If TNT is installed, will save a copy of a character matrix and script TNT to conduct a parsimony search on a server to which you can connect via SSH, and harvest the resulting trees.";
	}
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.TNTRunnerSSH.TNTRunnerSSH";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return TNTRunnerSSH.class;
	}
	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 if (runner!=null)
			 return runner.getProgramLocation();
		 return "SSH Server";
	}
	/*.................................................................................................................*/

	public String getName() {
		return "TNT Trees [SSH Server]";
	}


}
