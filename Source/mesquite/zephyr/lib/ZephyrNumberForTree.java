/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.tree.MesquiteTree;
import mesquite.lib.tree.Tree;
import mesquite.lib.tree.TreeVector;
import mesquite.zephyr.lib.*;


public abstract class ZephyrNumberForTree extends NumberForTree implements Reconnectable {
	protected ZephyrRunner runner;
	protected TreeSource treeRecoveryTask;
	protected Tree latestTree = null;
	protected Taxa taxa;
	protected long treesInferred;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	int rerootNode = 0;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		runner = (ZephyrRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		if (runner ==null)
			return false;
//		runner.initialize(this);
		return true;
	}
	/*.................................................................................................................*/
	abstract public String getRunnerModuleName();
	/*.................................................................................................................*/
	abstract public Class getRunnerClass();
	/*.................................................................................................................*/
	abstract public String getProgramName();
	/*.................................................................................................................*/
	abstract public String getProgramURL();

	/*.................................................................................................................*/
	/** Notifies all employees that a file is about to be closed.*/
	public boolean fileCloseRequested () {
		if (!MesquiteThread.isScripting()){
			discreetAlert(runner.getFileCloseNotification(getProject().getHomeFile().isDirty()));
		}
		return super.fileCloseRequested();
	}
	MesquiteBoolean runSucceeded = new MesquiteBoolean(true);
	/** Called when Mesquite re-reads a file that had had unfinished tree filling, e.g. by an external process, to pass along the command that should be executed on the main thread when trees are ready.*/
	public void reconnectToRequester(MesquiteCommand command){
		if (runner ==null)
			return;
		runner.reconnectToRequester(command,runSucceeded);
	}
	/*.................................................................................................................*/
	public boolean successfulReconnect(){
		return runSucceeded.getValue();
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("getRunner ", runner);
		temp.addLine("getMatrixSource ", matrixSourceTask);
		temp.addLine("setTreeRecoveryTask ", treeRecoveryTask); //

		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the runner", "[module]", commandName, "getRunner")) {
			return runner;
		}
		else if (checker.compare(this.getClass(), "Sets the matrix source", "[module]", commandName, "getMatrixSource")) {
			return matrixSourceTask;
		}
		else if (checker.compare(this.getClass(), "Sets the tree recovery task", "[module]", commandName, "setTreeRecoveryTask")) {
			treeRecoveryTask = (TreeSource)hireNamedEmployee(TreeSource.class, "$ #ManyTreesFromFile xxx remain useStandardizedTaxonNames");  //xxx used because ManyTreesFromFiles needs exact argument sequence
			return treeRecoveryTask;
		}

		return null;
	}	



	/*.................................................................................................................*/
	public Class getCharacterClass() {
		return null;
	}

	private boolean initializeObservedStates(Taxa taxa) {
		if (matrixSourceTask!=null) {
			if (observedStates ==null) {
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
				if (observedStates==null)
					return false;
			}
		}
		else return false;
		return true;
	}
	
	public boolean initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
		} else
			return false;
		if (!initializeObservedStates(taxa))
			return false;
		if (runner ==null) {
			runner = (ZephyrRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		}
		if (runner !=null){
			runner.initializeTaxa(taxa);
		}
		else
			return false;
		return true;
	}

	public String getExplanation() {
		return "If "+ getProgramName() + " is installed, will save a copy of a character matrix and script "+ getProgramName() + " to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		return getProgramName() + " Trees";
	}
	public String getNameForMenuItem() {
		return getProgramName() + " Trees..."; 
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
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return true;
	}
	public Tree getLatestTree(Taxa taxa, MesquiteNumber score, MesquiteString titleForWindow){
		if (titleForWindow != null)
			titleForWindow.setValue("Tree from "+ getProgramName());
		if (score != null)
			score.setToUnassigned();
		return latestTree;
	}
	
	
	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
	}
	/*.................................................................................................................*/
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);
		MesquiteTree initialTree = new MesquiteTree(taxa);
		initialTree.setToDefaultBush(2, false);

		CommandRecord.tick(getProgramName() + " Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());

		Tree tree = null;

		double bestScore = MesquiteDouble.unassigned;
		MesquiteDouble finalScores = new MesquiteDouble();

		tree = runner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
		runner.setRunInProgress(false);
		if (trees!=null) {
			if (runner.bootstrapOrJackknife()) {
				//DISCONNECTABLE: here need to split this exit and outside here see if it's done
				trees.setName(getProgramName() +" "+runner.bootstrapOrJackknifeTreeListName()+ " (Matrix: " + observedStates.getName() + ")");
			} 
			else {
				//DISCONNECTABLE: here need to split this exit and outside here see if it's done
				if (tree==null)
					return null;
				bestScore = finalScores.getValue();

				//	logln("Best score: " + bestScore);
				trees.setName(getProgramName() + " Trees (Matrix: " + observedStates.getName() + ")");
			}
			treesInferred = trees.getID();
		}
		return trees;
	}
	/*.................................................................................................................*/

	//TEMPORARY Debugg.println  Should be only in disconnectable tree block fillers
	public void retrieveTreeBlock(TreeVector treeList){
		if (runner != null){
			MesquiteDouble finalScores = new MesquiteDouble();
			runner.retrieveTreeBlock(treeList, finalScores);
			taxa = treeList.getTaxa();
			initializeObservedStates(taxa);
//			boolean bootstrap = runner.bootstrap();
			if (runner.bootstrapOrJackknife()) {
				treeList.setName(getProgramName() + " " + runner.bootstrapOrJackknifeTreeListName() + " (Matrix: " + observedStates.getName() + ")");
			} 
			else {
				treeList.setName(getProgramName() + " Trees (Matrix: " + observedStates.getName() + ")");
				double bestScore = finalScores.getValue();
			}
		}


	}
	/*.................................................................................................................*/
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || runner==null)
			return;
		if (getProject()==null)
			return;
		if (getProject().getHomeFile()==null)
			return;
		getProject().getHomeFile().setDirtiedByCommand(true);
		taxa = treeList.getTaxa();
		if (!initialize(taxa))
			return;

		//DISCONNECTABLE
		TreeVector trees = getTrees(taxa);
		if (trees == null)
			return;
		treeList.setName(trees.getName());
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		trees.dispose();
		treesInferred = treeList.getID();
		
	}


}
