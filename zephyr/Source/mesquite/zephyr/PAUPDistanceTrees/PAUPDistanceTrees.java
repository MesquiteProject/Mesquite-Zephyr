/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPDistanceTrees;

import java.awt.*;

import mesquite.lib.*;
import mesquite.zephyr.PAUPDistanceRunner.PAUPDistanceRunner;
import mesquite.zephyr.PAUPParsimonyRunner.PAUPParsimonyRunner;
import mesquite.zephyr.lib.*;
import mesquite.categ.lib.*;

public class PAUPDistanceTrees extends ZephyrTreeSearcher {

	/*.................................................................................................................*
	public String getExtraTreeWindowCommands (){

		String commands = "setSize 400 600; ";
		commands += " getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		if (!runner.bootstrapOrJackknife())
			commands += " tell It; branchLengthsToggle on; endTell; ";
		commands += " setEdgeWidth 3; endTell; ";
		if (runner.bootstrapOrJackknife())
			commands += "labelBranchLengths on; setNumBrLenDecimals 0; showBrLenLabelsOnTerminals off; showBrLensUnspecified off; setBrLenLabelColor 0 0 0;";
		commands += " endTell; ladderize root; ";
		return commands;
	}


	
	/*.................................................................................................................*/
	public String getTreeBlockName(){
		if (runner.bootstrapOrJackknife()) {
			return "PAUP* NJ " + runner.getResamplingKindName() + " Tree (Matrix: " + observedStates.getName() + ")";
		} 
		else {
			return "PAUP* NJ Tree (Matrix: " + observedStates.getName() + ")";

		}
	}

	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}



	public String getExplanation() {
		return "If PAUP* is installed, will save a copy of a character matrix and script PAUP* to conduct a neighbor-joining or bootstrap neighbor-joining, and harvest the resulting trees.";
	}
	public String getName() {
		return "PAUP* (NJ)";
	}
	public String getNameForMenuItem() {
		return "PAUP* (NJ)...";
	}

	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return " NJ";
	}

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPDistanceRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPDistanceRunner.class;
	}

	/*.................................................................................................................*/
	public String getProgramName() {
		return "PAUP*";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://people.sc.fsu.edu/~dswofford/paup_test/";
	 }

}
