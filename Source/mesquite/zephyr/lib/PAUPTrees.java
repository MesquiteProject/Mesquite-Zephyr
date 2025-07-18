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
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.taxa.TaxonNamer;
import mesquite.lib.tree.AdjustableTree;

public abstract class PAUPTrees extends ZephyrTreeSearcher  {

	/*.................................................................................................................*

	public String getExtraTreeWindowCommands (){

		String commands = "setSize 400 600; getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		commands += " setEdgeWidth 3; endTell; ";
		if (runner.bootstrapOrJackknife())
			commands += "labelBranchLengths on; setNumBrLenDecimals 0; showBrLenLabelsOnTerminals off; showBrLensUnspecified off; setBrLenLabelColor 0 0 0;";
		commands += " endTell; ladderize root; ";
		return commands;
	}
	

	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean showBranchLengthsProportional(boolean bootstrap, boolean finalTree){
		return !bootstrap && finalTree;
	}

	public String getExtraTreeWindowCommands (boolean finalTree, long treeBlockID){
		this.treeBlockID = treeBlockID;
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(), treeBlockID, showBranchLengthsProportional(runner.bootstrapOrJackknife(),finalTree))+ eachTreeCommands();
	}

	
	public String eachTreeCommands (){
		String commands="";
		if (runner.outgroupTaxSetString==null && rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";

		return commands;
	}


	/*.................................................................................................................*/
	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastDarkLine(path);
		TaxonNamer namer = runner.getTaxonNamer();
		
		latestTree = ZephyrUtil.readPhylipTree(s,taxa,false,namer);    

		if (latestTree instanceof AdjustableTree) {
			String name = "PAUP " + getMethodNameForTreeBlock() + " Tree";
			if (runner.showMultipleRuns())
				name+= ", Run " + (runner.getCurrentRun()+1);
			((AdjustableTree)latestTree).setName(name);
		}


		if (latestTree!=null && latestTree.isValid()) {
			rerootNode = latestTree.nodeOfTaxonNumber(0);
		}


		MesquiteThread.setCurrentCommandRecord(cr);
		if (latestTree!=null && latestTree.isValid()) {
			newResultsAvailable(outgroupTaxSet);
		}

	}
	/*.................................................................................................................*/
	public String getProgramName() {
		return "PAUP";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return PAUPRunner.PAUPURL;
	 }


}
