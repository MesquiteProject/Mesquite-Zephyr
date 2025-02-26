/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.*;


import mesquite.lib.*;
import mesquite.lib.analysis.DistanceAnalysis;
import mesquite.zephyr.PAUPNJRunnerSSH.PAUPNJRunnerSSH;
import mesquite.zephyr.PAUPParsimonyRunnerLocal.PAUPParsimonyRunnerLocal;
import mesquite.zephyr.lib.*;
import mesquite.categ.lib.*;

public abstract class PAUPNJTrees extends ZephyrTreeSearcher implements DistanceAnalysis {

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
	public String getTreeBlockName(boolean completedRun){
		if (runner.bootstrapOrJackknife()) {
			return "PAUP NJ " + runner.getResamplingKindName() + " Tree (Matrix: " + observedStates.getName() + ")";
		} 
		else {
			return "PAUP NJ Tree (Matrix: " + observedStates.getName() + ")";

		}
	}
	public String getExtraTreeWindowCommands (boolean finalTree, long treeBlockID){
		this.treeBlockID = treeBlockID;
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(),treeBlockID, finalTree)+ eachTreeCommands();
	}
	public String eachTreeCommands (){
		String commands="";
		if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";

		return commands;
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
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}



	public String getExplanation() {
		return "If PAUP is installed, will save a copy of a character matrix and script PAUP to conduct a neighbor-joining or bootstrap neighbor-joining, and harvest the resulting trees.";
	}
	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return "NJ";
	}

	/*.................................................................................................................*/
	public String getMethodNameForMenu() {
		return "NJ";
	}

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPNJRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPNJRunnerSSH.class;
	}

	/*.................................................................................................................*/
	public String getProgramName() {
		return "PAUP";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return PAUPRunner.PAUPURL;
	 }
		/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}

}
