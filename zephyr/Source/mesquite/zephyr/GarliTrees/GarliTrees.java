/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.zephyr.GarliTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.GarliRunner.GarliRunner;
import mesquite.zephyr.lib.*;


public class GarliTrees extends ZephyrTreeSearcher {
	int rerootNode = 0;

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.GarliRunner.GarliRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return GarliRunner.class;
	}

	/*.................................................................................................................*/
	public String getProgramName() {
		return "GARLI";
	}
	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "https://code.google.com/p/garli/";
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
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return true;
	}

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;
		if (treeRecoveryTask == null){
			treeRecoveryTask = (TreeSource)hireNamedEmployee(TreeSource.class, "$ #ManyTreesFromFile " + StringUtil.tokenize(path) + " remain useStandardizedTaxonNames");
			treeRecoveryTask.initialize(taxa);
			treeRecoveryTask.doCommand("quietOperation", null, CommandChecker.defaultChecker);
		}
		else {
			treeRecoveryTask.initialize(taxa);
			treeRecoveryTask.doCommand("quietOperation", null, CommandChecker.defaultChecker);
			treeRecoveryTask.doCommand("setFilePath", StringUtil.tokenize(path) + " remain useStandardizedTaxonNames", CommandChecker.defaultChecker);
		}

		if (treeRecoveryTask != null) {
			latestTree =  treeRecoveryTask.getTree(taxa, 0);
			if (latestTree!=null && latestTree.isValid()) {
				rerootNode = latestTree.nodeOfTaxonNumber(0);
			}

			MesquiteThread.setCurrentCommandRecord(cr);

			//Wayne: get tree here from file
			if (latestTree!=null && latestTree.isValid()) {
				newResultsAvailable(outgroupTaxSet);
			}
		}
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}


}
