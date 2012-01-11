package mesquite.zephyr.GarliTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.GarliRunner.GarliRunner;


public class GarliTrees extends ExternalTreeSearcher {
	GarliRunner garliRunner;
	TreeSource treeRecoveryTask;
	Taxa taxa;
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


		garliRunner = (GarliRunner)hireNamedEmployee(GarliRunner.class, "#mesquite.bosque.GarliRunner.GarliRunner");
		if (garliRunner ==null)
			return false;
		garliRunner.initialize(this);
		return true;
	}


	
	public String getExtraTreeWindowCommands (){
		String commands = "setSize 400 600; getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		if (garliRunner.getBootstrapreps()<=0)
			commands += " tell It; branchLengthsToggle on; endTell; ";
		commands += " setEdgeWidth 3; endTell; ";
		if (garliRunner.getBootstrapreps()>0)
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

	public void initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
			if (observedStates ==null)
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
		}
		if (garliRunner ==null) {
			garliRunner = (GarliRunner)hireNamedEmployee(GarliRunner.class, "#mesquite.bosque.GarliRunner.GarliRunner");
		}
		if (garliRunner !=null)
			garliRunner.initializeTaxa(taxa);
	}

	public String getExplanation() {
		return "If Garli is installed, will save a copy of a character matrix and script Garli to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		return "Garli Trees";
	}
	public String getNameForMenuItem() {
		return "Garli Trees...";
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

		CommandRecord.tick("Garli Tree Search in progress " );
		boolean bootstrap = garliRunner.getBootstrapreps()>0;

		Random rng = new Random(System.currentTimeMillis());
		//garliRunner.setGarliPath(garliPath);
		//garliRunner.setOfPrefix(ofprefix);
		//garliRunner.setBootstrapreps(bootStrapReps);

		Tree tree = null;

		double bestScore = MesquiteDouble.unassigned;
		MesquiteDouble[] finalScores = null;
		if (garliRunner.isGarli96())
			finalScores = new MesquiteDouble[1];
		else
			finalScores = new MesquiteDouble[garliRunner.getNumRuns()];
		for (int i=0; i<finalScores.length; i++)
			finalScores[i] = new MesquiteDouble();
		
		int numRunsScriptedByMesquite;
		if (garliRunner.isGarli96())
			numRunsScriptedByMesquite = 1;
		else
			numRunsScriptedByMesquite = garliRunner.getNumRuns();

		if (bootstrap) {
			garliRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
		} 
		else {
			for (int run = 0; run<numRunsScriptedByMesquite; run++) {
				tree = garliRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
				if (tree==null)
					return null;
				if (!garliRunner.isGarli96()) {
					if (tree instanceof AdjustableTree )
						((AdjustableTree)tree).setName("Garli Run " + (run+1));

					MesquiteDouble s = new MesquiteDouble(-finalScores[0].getValue());
					s.setName(GarliRunner.SCORENAME);
					((Attachable)tree).attachIfUniqueName(s);

					if (MesquiteDouble.isUnassigned(bestScore)) {
						bestScore = finalScores[0].getValue();
						logln("\nGarli run " + (run+1) + ", ln L = " + finalScores[0].getValue() + " * \n");
						trees.addElement(tree, false);
					}
					else
						if (bestScore<finalScores[0].getValue()) {
							bestScore = finalScores[0].getValue();
							logln("\nGarli run " + (run+1) + ", ln L = " + finalScores[0].getValue() + " * \n");
							if (garliRunner.getOnlyBest())
								trees.removeElementAt(0, false);
							trees.addElement(tree, false);
						} else {
							logln("\nGarli run " + (run+1) + ", ln L = " + finalScores[0].getValue()+ "\n");
							if (!garliRunner.getOnlyBest())
								trees.addElement(tree, false);

						}
				} else {  // associate numbers
					if (trees !=null) {
						for (int i=0; i<trees.getNumberOfTrees() && i<finalScores.length; i++) {
							Tree newTree = trees.getTree(i);
							//MesquiteDouble s = new MesquiteDouble(-finalScores[i].getValue());
							//s.setName(GarliRunner.SCORENAME);
							//((Attachable)newTree).attachIfUniqueName(s);
							if (MesquiteDouble.isUnassigned(bestScore))
								bestScore = finalScores[i].getValue();
							else if (bestScore>finalScores[i].getValue()) 
								bestScore = finalScores[i].getValue();
						}
					} 
				}
			}
		//	logln("Best score: " + bestScore);
		}
		trees.setName("Trees from GARLI Search (Matrix: " + observedStates.getName() + ")");
		return trees;
	}

	/*.................................................................................................................*/
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || garliRunner==null)
			return;
		taxa = treeList.getTaxa();
		initialize(taxa);

		TreeVector trees = getTrees(taxa);
		if (trees == null)
			return;
		treeList.setName(trees.getName());
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
	}


}
