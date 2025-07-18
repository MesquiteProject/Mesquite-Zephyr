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
import mesquite.lib.analysis.ParsimonyAnalysis;
import mesquite.lib.duties.TreeSource;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.taxa.TaxonNamer;
import mesquite.lib.tree.Tree;
import mesquite.zephyr.TNTRunnerSSH.TNTRunnerSSH;


public abstract class TNTTrees extends ZephyrTreeSearcher implements ParsimonyAnalysis {
	TreeSource treeRecoveryTask;
	//Taxa taxa;
	//private MatrixSourceCoord matrixSourceTask;
	//protected MCharactersDistribution observedStates;
	//int rerootNode = 0;


	/*.................................................................................................................*/
	public String getProgramName() {
		return "TNT";
	}
	public String getExtraTreeWindowCommands (boolean finalTree, long treeBlockID){
		this.treeBlockID = treeBlockID;
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(), treeBlockID, false)+ eachTreeCommands();
	}


	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://www.lillo.org.ar/phylogeny/tnt/";
	 }

	 public Class getRunnerClass(){
		 return TNTRunnerSSH.class;
	 }
	 /*.................................................................................................................*/
	 public String getCitation() {
		 return "Maddison DR and Will KW. 2014.  TNT Tree Searcher, in " + getPackageIntroModule().getPackageCitation();
	 }

	 /*.................................................................................................................*/
	 public boolean isPrerelease(){
		return false;
	}
	
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return true;
	}
	/*.................................................................................................................*/

	public String eachTreeCommands (){
		String commands="";
		if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";

		return commands;
	}

	/*.................................................................................................................*/
	Tree latestTree = null;
	/*.................................................................................................................*/

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastContents(path);
		
		TaxonNamer namer = runner.getTaxonNamer();


//		int[] taxonNumberTranslation = ((TNTRunner)runner).getTaxonNumberTranslation(taxa);


		latestTree = ZephyrUtil.readTNTTrees(this, null,path, s,"TNTTree", 0, taxa,true, false, null, namer);

		if (latestTree!=null && latestTree.isValid()) {
			rerootNode = latestTree.nodeOfTaxonNumber(1);
			if (outgroupTaxSet!=null) {
				int firstOutgroup = outgroupTaxSet.firstBitOn();
				if (MesquiteInteger.isCombinable(firstOutgroup) && firstOutgroup>=0)
					rerootNode = latestTree.nodeOfTaxonNumber(firstOutgroup+1);
			}
			//logln(latestTree.getName());
		}

		MesquiteThread.setCurrentCommandRecord(cr);
		//Wayne: get tree here from file
		if (latestTree!=null && latestTree.isValid())
			newResultsAvailable(outgroupTaxSet);

	}
	/*.................................................................................................................*/


}
