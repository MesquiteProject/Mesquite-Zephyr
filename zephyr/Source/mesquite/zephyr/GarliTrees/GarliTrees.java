package mesquite.zephyr.GarliTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.GarliRunner.GarliRunner;


public class GarliTrees extends ExternalTreeSearcher implements Reconnectable {
	GarliRunner garliRunner;
	TreeSource treeRecoveryTask;
	Taxa taxa;
	long treesInferred;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	int rerootNode = 0;
	//String datafname = null;
	//String ofprefix = "output";
	//int bootStrapReps = 0;
	//int numRateCats = 4;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		garliRunner = (GarliRunner)hireNamedEmployee(GarliRunner.class, "#mesquite.zephyr.GarliRunner.GarliRunner");
		if (garliRunner ==null)
			return false;
		garliRunner.initialize(this);
		return true;
	}

	/** Called when Mesquite re-reads a file that had had unfinished tree filling, e.g. by an external process, to pass along the command that should be executed on the main thread when trees are ready.*/
	public void reconnectToRequester(MesquiteCommand command){
		if (garliRunner ==null)
			return;
		garliRunner.reconnectToRequester(command);
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("getGarliRunner ", garliRunner);
		temp.addLine("getMatrixSource ", matrixSourceTask);
		temp.addLine("setTreeRecoveryTask ", treeRecoveryTask); //

		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the runner", "[module]", commandName, "getGarliRunner")) {
			return garliRunner;
		}
		else if (checker.compare(this.getClass(), "Sets the runner", "[module]", commandName, "getMatrixSource")) {
			return matrixSourceTask;
		}
		else if (checker.compare(this.getClass(), "Sets the tree recovery task", "[module]", commandName, "setTreeRecoveryTask")) {
			treeRecoveryTask = (TreeSource)hireNamedEmployee(TreeSource.class, "$ #ManyTreesFromFile xxx remain useStandardizedTaxonNames");  //xxx used because ManyTreesFromFiles needs exact argument sequence
			return treeRecoveryTask;
		}

		return null;
	}	


	public String getExtraTreeWindowCommands (){
		String commands = "setSize 400 600; ";
		if (garliRunner.getBootstrapreps()>0){
			commands += "getOwnerModule; tell It; setTreeSource  #mesquite.consensus.ConsensusTree.ConsensusTree; tell It; setTreeSource  #mesquite.trees.StoredTrees.StoredTrees; tell It;  ";
			commands += " setTreeBlockByID " + treesInferred + ";";
			commands += " toggleUseWeights off; endTell; setConsenser  #mesquite.consensus.MajRuleTree.MajRuleTree; endTell; endTell;";
		}

		commands += "getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		if (garliRunner.getBootstrapreps()<=0)
			commands += " tell It; branchLengthsToggle on; endTell; ";
		commands += " setEdgeWidth 3; endTell; ";
		if (garliRunner.getBootstrapreps()>0){
			commands += "labelBranchLengths off;";
		}
		commands += " endTell; ";
		commands += "getOwnerModule; tell It; getEmployee #mesquite.ornamental.ColorTreeByPartition.ColorTreeByPartition; tell It; colorByPartition on; endTell; endTell; ";

		if (garliRunner.getBootstrapreps()>0){
			commands += "getOwnerModule; tell It; getEmployee #mesquite.ornamental.DrawTreeAssocDoubles.DrawTreeAssocDoubles; tell It; setOn on; toggleShow consensusFrequency; endTell; endTell; ";
		}		

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
	/*.................................................................................................................*
	public String getExtraIntermediateTreeWindowCommands (){
		String commands = "getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "getTreeWindowMaker;\ntell It; ";
		commands += " ladderize root; ";
		commands += " endTell; endTell; ";
		return commands;
	}


	/*.................................................................................................................*/
	public Class getCharacterClass() {
		return null;
	}

	private void initializeObservedStates(Taxa taxa) {
		if (matrixSourceTask!=null) {
			if (observedStates ==null)
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
		}
	}
	
	public void initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
		}
		initializeObservedStates(taxa);
		if (garliRunner ==null) {
			garliRunner = (GarliRunner)hireNamedEmployee(GarliRunner.class, "#mesquite.zephyr.GarliRunner.GarliRunner");
		}
		if (garliRunner !=null)
			garliRunner.initializeTaxa(taxa);
	}

	public String getExplanation() {
		return "If GARLI is installed, will save a copy of a character matrix and script GARLI to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		return "GARLI Trees";
	}
	public String getNameForMenuItem() {
		return "GARLI Trees...";
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
			titleForWindow.setValue("Tree from GARLI");
		if (score != null)
			score.setToUnassigned();
		return latestTree;
	}
	Tree latestTree = null;
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
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);
		MesquiteTree initialTree = new MesquiteTree(taxa);
		initialTree.setToDefaultBush(2, false);

		CommandRecord.tick("GARLI Tree Search in progress " );
		boolean bootstrap = garliRunner.getBootstrapreps()>0;

		Random rng = new Random(System.currentTimeMillis());
		//garliRunner.setGarliPath(garliPath);
		//garliRunner.setOfPrefix(ofprefix);
		//garliRunner.setBootstrapreps(bootStrapReps);

		Tree tree = null;

		double bestScore = MesquiteDouble.unassigned;
		MesquiteDouble finalScores = new MesquiteDouble();


		if (bootstrap) {
			//DISCONNECTABLE: here need to split this exit and outside here see if it's done
			garliRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
			trees.setName("GARLI Bootstrap Trees (Matrix: " + observedStates.getName() + ")");
		} 
		else {
			//DISCONNECTABLE: here need to split this exit and outside here see if it's done
			tree = garliRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
			if (tree==null)
				return null;
			bestScore = finalScores.getValue();

			//	logln("Best score: " + bestScore);
			trees.setName("GARLI Trees (Matrix: " + observedStates.getName() + ")");
		}
		treesInferred = trees.getID();
		return trees;
	}

	//TEMPORARY Debugg.println  Should be only in disconnectable tree block fillers
	public void retrieveTreeBlock(TreeVector treeList){
		if (garliRunner != null){
			MesquiteDouble finalScores = new MesquiteDouble();
			garliRunner.retrieveTreeBlock(treeList, finalScores);
			taxa = treeList.getTaxa();
			initializeObservedStates(taxa);
			boolean bootstrap = garliRunner.getBootstrapreps()>0;
			if (bootstrap) {
				treeList.setName("GARLI Bootstrap Trees (Matrix: " + observedStates.getName() + ")");
			} 
			else {
				treeList.setName("GARLI Trees (Matrix: " + observedStates.getName() + ")");
				double bestScore = finalScores.getValue();
			}
		}


	}
	/*.................................................................................................................*/
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || garliRunner==null)
			return;
		taxa = treeList.getTaxa();
		initialize(taxa);

		//DISCONNECTABLE
		TreeVector trees = getTrees(taxa);
		if (trees == null)
			return;
		treeList.setName(trees.getName());
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		treesInferred = treeList.getID();
	}


}
