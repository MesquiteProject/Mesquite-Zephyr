/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPLikelihoodTrees;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Random;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.duties.TreeSource;
import mesquite.zephyr.PAUPLikelihoodRunner.PAUPLikelihoodRunner;
import mesquite.zephyr.lib.*;

public class PAUPLikelihoodTrees extends PAUPTrees implements LikelihoodAnalysis {

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}

	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return " ML";
	}


	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

	
	public String getExplanation() {
		return "If PAUP is installed, will save a copy of a character matrix and script PAUP to conduct a likelihood search, and harvest the resulting trees.";
	}
	public String getName() {
		return "PAUP (Likelihood)";
	}
	public String getNameForMenuItem() {
		return "PAUP (Likelihood)...";
	}

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPLikelihoodRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPLikelihoodRunner.class;
	}


}
