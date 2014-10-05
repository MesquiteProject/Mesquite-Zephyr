/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.TNTTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.TNTRunner.TNTRunner;
import mesquite.zephyr.lib.*;


public class TNTTrees extends ZephyrTreeSearcher {
	TNTRunner tntRunner;
	TreeSource treeRecoveryTask;
	Taxa taxa;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	int rerootNode = 0;

	/*.................................................................................................................*/
	public boolean  loadModule(){ 
		return true;
	}

	/*.................................................................................................................*
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		tntRunner = (TNTRunner)hireNamedEmployee(TNTRunner.class, "#mesquite.zephyr.TNTRunner.TNTRunner");
		if (tntRunner ==null)
			return false;
		tntRunner.initialize(this);
		return true;
	}


	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.TNTRunner.TNTRunner";
	}
	/*.................................................................................................................*/
	public String getProgramName() {
		return "TNT";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://www.lillo.org.ar/phylogeny/tnt/";
	 }

	 public Class getRunnerClass(){
		 return TNTRunner.class;
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
	/*.................................................................................................................*/


	public String eachTreeCommands (){
		String commands="";
		if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";
		return commands;
	}
	/*.................................................................................................................*
	public Class getCharacterClass() {
		return null;
	}

	public void initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
			if (observedStates ==null)
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
		}
		if (tntRunner ==null) {
			tntRunner = (TNTRunner)hireNamedEmployee(TNTRunner.class, "#mesquite.zephyr.TNTRunner.TNTRunner");
		}
		if (tntRunner !=null)
			tntRunner.initializeTaxa(taxa);
	}

	public String getExplanation() {
		return "If TNT is installed, will save a copy of a character matrix and script TNT to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		return "TNT Trees";
	}
	public String getNameForMenuItem() {
		return "TNT Trees...";
	}

	/*.................................................................................................................*
	public Tree getLatestTree(Taxa taxa, MesquiteNumber score, MesquiteString titleForWindow){
		if (titleForWindow != null)
			titleForWindow.setValue("Tree from TNT");
		if (score != null)
			score.setToUnassigned();
		return latestTree;
	}
	/*.................................................................................................................*/
	Tree latestTree = null;
	/*.................................................................................................................*/

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastContents(path);


		latestTree = ZephyrUtil.readTNTTrees(this, null,s,"TNTTree", 0, taxa,true, false);

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
	/*.................................................................................................................*
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);
		MesquiteTree initialTree = new MesquiteTree(taxa);
		initialTree.setToDefaultBush(2, false);

		CommandRecord.tick("TNT Tree Search in progress " );
		boolean bootstrap = tntRunner.getBootstrapreps()>0;

		Random rng = new Random(System.currentTimeMillis());

		Tree tree = null;



		if (bootstrap) {
			tntRunner.getTrees(trees, taxa, observedStates, rng.nextInt());
		} 
		else {
			tree = tntRunner.getTrees(trees, taxa, observedStates, rng.nextInt());
			if (tree==null)
				return null;
		}

		return trees;
	}

	/*.................................................................................................................*
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || tntRunner==null)
			return;
		taxa = treeList.getTaxa();
		initialize(taxa);

		TreeVector trees = getTrees(taxa);
		treeList.setName("Trees from TNT search");
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
	}
	/*.................................................................................................................*/


}
