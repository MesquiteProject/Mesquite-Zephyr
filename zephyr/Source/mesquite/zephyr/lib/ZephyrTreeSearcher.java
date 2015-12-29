/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;


public abstract class ZephyrTreeSearcher extends ExternalTreeSearcher implements Reconnectable, CanRetrieveTreeBlock, ZephyrRunnerEmployer {
	protected ZephyrRunner runner;
	protected TreeSource treeRecoveryTask;
	protected Tree latestTree = null;
	protected Taxa taxa;
	protected long treesInferred;
	protected MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	protected int rerootNode = 0;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");

		runner = (ZephyrRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		if (runner ==null)
			return false;
		runner.initialize(this);
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
	 public String getProgramLocation(){
		 return "";
	}
	 /*.................................................................................................................*/
		public String getHTMLDescriptionOfStatus(){
			StringBuffer sb = new StringBuffer();
			if (observedStates != null){
				CharacterData data = observedStates.getParentData();
				if (data != null)
					sb.append( "<b>" + getName() + "</b> using the matrix " + data.getName() +"<br>");
			}
			String s = runner.getHTMLDescriptionOfStatus();
			if (StringUtil.notEmpty(s))
				sb.append(s+"<br>");
			s = sb.toString();
			if (StringUtil.notEmpty(s))
				return s;
			return getName();
		}

	/*.................................................................................................................*/
	/** Generated by an employee who quit.  The MesquiteModule should act accordingly. */
	public void employeeQuit(MesquiteModule employee) {
		if (employee == runner)  // runner quit and none rehired automatically
			iQuit();
	}
	/*.................................................................................................................*/
	public void appendSearchDetails() {
//		runner.appendToSearchDetails("Method: " + getMethodNameForTreeBlock() + " " + runner.getResamplingKindName() + "\nMatrix: " + observedStates.getName() + "\n");
		runner.appendToSearchDetails("\nMatrix: " + observedStates.getName() + "\n");
		runner.appendAdditionalSearchDetails();
	}

	/*.................................................................................................................*/
	/** Notifies all employees that a file is about to be closed.*/
	public void fileCloseRequested () {
		if (!MesquiteThread.isScripting() && getProject().getHomeFile().isDirty())
			alert("There is a run of "+ getProgramName() + " underway.  If you save the file now, you will be able to reconnect to it by reopening this file, as long as you haven't moved the file or those files involved in the "+ getProgramName() + " search");
		super.fileCloseRequested();
	}
	/** Called when Mesquite re-reads a file that had had unfinished tree filling, e.g. by an external process, to pass along the command that should be executed on the main thread when trees are ready.*/
	public void reconnectToRequester(MesquiteCommand command){
		if (runner ==null)
			return;
		String callBackArguments = command.getDefaultArguments();
		String taxaID = parser.getFirstToken(callBackArguments);
		if (taxaID !=null)
			taxa = getProject().getTaxa(taxaID);
		runner.reconnectToRequester(command);
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
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(), treesInferred)+ eachTreeCommands();
	}


	public String eachTreeCommands (){
		return "";
	}
	/*.................................................................................................................*/
	public String getExtraIntermediateTreeWindowCommands (){
		String commands = " setTitle " +StringUtil.tokenize(runner.getWindowTitle())+"; ";
		return commands;
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
		if (StringUtil.notEmpty(getProgramLocation()))
				return getProgramName() + " Trees ["+ getProgramLocation() +"]";
		else
			return getProgramName() + " Trees";
	}
	public String getNameForMenuItem() {
		if (StringUtil.notEmpty(getProgramLocation()))
			return getProgramName() + " Trees ["+ getProgramLocation() +"]...";
	else
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

		runner.setTreeInferer(getTreeInferer());
		tree = runner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScores);
		runner.setRunInProgress(false);
		appendSearchDetails();
		if (trees!=null) {
			trees.setName(getTreeBlockName());
			if (!runner.bootstrapOrJackknife()) {
				//DISCONNECTABLE: here need to split this exit and outside here see if it's done
				if (tree==null)
					return null;
				bestScore = finalScores.getValue();
			}
			treesInferred = trees.getID();
		}
		return trees;
	}
	
	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return "";
	}
	/*.................................................................................................................*/
	public String getTreeBlockName(){
		String s = getProgramName() + getMethodNameForTreeBlock() ;
		if (runner != null){
			if (runner.bootstrapOrJackknife()) {
				if (runner.singleTreeFromResampling()) //this means we have read in all of the bootstrap trees
					s += " " + runner.getResamplingKindName() +  " Consensus Tree (Matrix: " + observedStates.getName();
				else
					s += " " + runner.getResamplingKindName() +  " Trees (Matrix: " + observedStates.getName();
			} 
			else {
				s +=  " Trees (Matrix: " + observedStates.getName();
			}
			String add =  runner.getAddendumToTreeBlockName();
			if (!StringUtil.blank(add))
				s += "; " + add;
			s += ")";
		}
		else s +=  " Trees";
		return s;
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
			treeList.setName(getTreeBlockName());
			treeList.setAnnotation (runner.getSearchDetails(), false);
			if (!runner.bootstrapOrJackknife()){
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
		treeList.setAnnotation (runner.getSearchDetails(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		trees.dispose();
		treesInferred = treeList.getID();
		
	}


}
