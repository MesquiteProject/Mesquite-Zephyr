/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import mesquite.lib.CommandRecord;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteThread;
import mesquite.lib.analysis.LikelihoodAnalysis;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.taxa.TaxonNamer;
import mesquite.lib.tree.AdjustableTree;

public abstract class IQTreeTrees extends ZephyrTreeSearcher implements LikelihoodAnalysis {
	int rerootNode = 0;


	public String eachTreeCommands (){
		String commands="";
		if (runner.outgroupTaxSetString==null && rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";
		return commands;
	}

	public String getExtraTreeWindowCommands (boolean finalTree, long treeBlockID){
		this.treeBlockID = treeBlockID;
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(), runner.showAssocStrings(),runner.getNameRefForAssocStrings(), treeBlockID, !runner.bootstrapOrJackknife())+ eachTreeCommands();
	}

	/*.................................................................................................................*/
	public abstract String getRunnerModuleName();
	/*.................................................................................................................*/
	public abstract Class getRunnerClass() ;
	/*.................................................................................................................*/
	public String getProgramName() {
		return "IQ-TREE";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://iqtree.org";
	 }

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return true;
	}
	



	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;
		
		String s = MesquiteFile.getFileLastDarkLine(path);
		TaxonNamer namer = runner.getTaxonNamer();
		latestTree = ZephyrUtil.readPhylipTree(s,taxa,false,namer);    
		if (latestTree instanceof AdjustableTree) {
			String name = "IQ-TREE Tree";
			if (runner.showMultipleRuns())
				name+= ", Run " + (runner.getCurrentRun()+1);
			((AdjustableTree)latestTree).setName(name);
		}

		if (latestTree!=null && latestTree.isValid()) {
			rerootNode = latestTree.nodeOfTaxonNumber(0);
		}


		MesquiteThread.setCurrentCommandRecord(cr);
		//Wayne: get tree here from file
		if (latestTree!=null && latestTree.isValid()) {
			newResultsAvailable(outgroupTaxSet);
		}

	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2100;  
	}


}
