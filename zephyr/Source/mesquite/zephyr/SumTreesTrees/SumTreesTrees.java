package mesquite.zephyr.SumTreesTrees;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.SumTreesRunner.SumTreesRunner;
import mesquite.zephyr.lib.SimpleLogger;

public class SumTreesTrees extends TreeSource {
	SumTreesRunner sumTreesRunner;
	TreeSource treeRecoveryTask;
	int rerootNode = 0;
	Taxa taxa;
	SimpleLogger logger;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		logger = new SimpleLogger(this);
		loadPreferences();
		if (!this.hireSumTreesRunner()) 
			return false;
		return true;
	}

	private boolean hireSumTreesRunner() {
		if (this.sumTreesRunner != null)
			return true;
		this.sumTreesRunner = (SumTreesRunner)hireNamedEmployee(SumTreesRunner.class, "#mesquite.zephyr.SumTreesRunner.SumTreesRunner");
		return (this.sumTreesRunner != null);
	}
	
	public String getExtraTreeWindowCommands (){
		String commands = "setSize 400 600; getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		commands += " setEdgeWidth 3; endTell; ";
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

	public String getExplanation() {
		return "If SumTrees is installed, will use it to produce a majority-rule tree for a collection of trees.";
	}
	public String getName() {
		return "SumTrees MajRule Tree";
	}
	public String getNameForMenuItem() {
		return "SumTrees MajRule Tree...";
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
		return false;
	}
	
	
	/*.................................................................................................................*/
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || this.sumTreesRunner==null)
			return;
		this.taxa = treeList.getTaxa();
		initialize(this.taxa);

		Tree tree = getTree(this.taxa, 0);
		treeList.setName("Maj-Rule tree from SumTrees search");
		if (tree != null)
			treeList.addElement(tree, false);
	}

	public void initialize(Taxa taxaArg) {
		this.taxa = taxaArg;
		if (!this.hireSumTreesRunner())
			return;
		this.sumTreesRunner.initialize(this, this.taxa);
	}

	public int getNumberOfTrees(Taxa taxa) {
		return 1;
	}

	public Tree getTree(Taxa taxa, int itree) {
		if (itree > 0)
			return null;
		MesquiteTree initialTree = new MesquiteTree(taxa);
		initialTree.setToDefaultBush(2, false);
		CommandRecord.tick("Summarization in SumTrees in progress " );
		this.initialize(taxa);
		try {
			return this.sumTreesRunner.invokeSumTrees(null);
		} catch (Exception e) {
			logger.log(SimpleLogger.INFO, e);
			return null;
		}
	}
	

	public String getTreeNameString(Taxa taxa, int i) {
		return "Tree " + (i + 1) + " from SumTrees";
	}

	public void setPreferredTaxa(Taxa taxaArg) {
		this.initialize(taxaArg);
	}


}
