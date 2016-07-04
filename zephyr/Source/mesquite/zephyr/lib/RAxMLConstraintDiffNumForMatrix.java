/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import mesquite.lib.*;

public abstract class RAxMLConstraintDiffNumForMatrix extends ZephyrConstraintDiffNumForMatrix {

	/*.................................................................................................................*/
	public abstract String getRunnerModuleName();
	/*.................................................................................................................*/
	public abstract Class getRunnerClass() ;
	/*.................................................................................................................*/
	public String getProgramName() {
		return "RAxML";
	}
	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://sco.h-its.org/exelixis/web/software/raxml/index.html";
	 }
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return false;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

}
