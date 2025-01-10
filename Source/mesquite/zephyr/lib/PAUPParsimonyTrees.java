/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.*;

import mesquite.lib.*;
import mesquite.lib.analysis.ParsimonyAnalysis;
import mesquite.zephyr.PAUPParsimonyRunnerLocal.PAUPParsimonyRunnerLocal;

public abstract class PAUPParsimonyTrees extends PAUPTrees implements ParsimonyAnalysis {

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	public String getMethodNameForMenu() {
		return "Parsimony";
	}
	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return "MP";
	}

	/*.................................................................................................................*/
	public boolean showBranchLengthsProportional(boolean bootstrap, boolean finalTree){
		return false;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}


	public String getExplanation() {
		return "If PAUP is installed, will save a copy of a character matrix and script PAUP to conduct a parsimony search, and harvest the resulting trees.";
	}
	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPParsimonyRunnerLocal.PAUPParsimonyRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPParsimonyRunnerLocal.class;
	}
	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}


}
