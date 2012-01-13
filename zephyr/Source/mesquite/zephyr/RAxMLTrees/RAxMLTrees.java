package mesquite.zephyr.RAxMLTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.RAxMLRunner.RAxMLRunner;
import mesquite.zephyr.lib.*;


public class RAxMLTrees extends ExternalTreeSearcher {
	RAxMLRunner raxmlRunner;
	TreeSource treeRecoveryTask;
	Taxa taxa;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	int rerootNode = 0;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		raxmlRunner = (RAxMLRunner)hireNamedEmployee(RAxMLRunner.class, "#mesquite.bosque.RAxMLRunner.RAxMLRunner");
		if (raxmlRunner ==null)
			return false;
		raxmlRunner.initialize(this);
		return true;
	}



	public String getExtraTreeWindowCommands (){
		String commands = "setSize 400 600; getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		if (raxmlRunner.getBootstrapreps()<=0)
			commands += " tell It; branchLengthsToggle on; endTell; ";
		commands += " setEdgeWidth 3; endTell; ";
		if (raxmlRunner.getBootstrapreps()>0)
			commands += "labelBranchLengths on; setNumBrLenDecimals 0; showBrLenLabelsOnTerminals off; showBrLensUnspecified off; setBrLenLabelColor 0 0 0;";
		commands += " endTell; ";
		commands += "getOwnerModule; tell It; getEmployee #mesquite.ornamental.ColorTreeByPartition.ColorTreeByPartition; tell It; colorByPartition on; endTell; endTell; ";
		commands += eachTreeCommands();
		return commands;
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
		if (raxmlRunner ==null) {
			raxmlRunner = (RAxMLRunner)hireNamedEmployee(RAxMLRunner.class, "#mesquite.bosque.RAxMLRunner.RAxMLRunner");
		}
		if (raxmlRunner !=null)
			raxmlRunner.initializeTaxa(taxa);
	}

	public String getExplanation() {
		return "If RAxML is installed, will save a copy of a character matrix and script RAxML to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		return "RAxML Trees";
	}
	public String getNameForMenuItem() {
		return "RAxML Trees...";
	}

	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
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
	public Tree getLatestTree(Taxa taxa, MesquiteNumber score, MesquiteString titleForWindow){
		if (titleForWindow != null)
			titleForWindow.setValue("Tree from RAxML");
		if (score != null)
			score.setToUnassigned();
		return latestTree;
	}
	Tree latestTree = null;

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastContents(path);
		SimpleTaxonNamer namer = new SimpleTaxonNamer();
		latestTree = ZephyrUtil.readPhylipTree(s,taxa,false,namer);

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
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);
		MesquiteTree initialTree = new MesquiteTree(taxa);
		initialTree.setToDefaultBush(2, false);

		CommandRecord.tick("RAxML Tree Search in progress " );
		boolean bootstrap = raxmlRunner.getBootstrapreps()>0;

		Random rng = new Random(System.currentTimeMillis());

		Tree tree = null;

		MesquiteDouble[] finalScores = null;
		finalScores = new MesquiteDouble[raxmlRunner.getNumRuns()];
		for (int i=0; i<finalScores.length; i++)
			finalScores[i] = new MesquiteDouble();


		if (bootstrap) {
			raxmlRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
		} 
		else {
			tree = raxmlRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
			if (tree==null)
				return null;
		}

		trees.setName("Trees from RAxML Search (Matrix: " + observedStates.getName() + ")");
		return trees;
	}

	/*.................................................................................................................*/
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || raxmlRunner==null)
			return;
		taxa = treeList.getTaxa();
		initialize(taxa);

		TreeVector trees = getTrees(taxa);
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
	}


}
