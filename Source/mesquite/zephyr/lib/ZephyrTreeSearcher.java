/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;

import java.util.*;


import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.CategoricalState;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.misc.CanRetrieveTreeBlock;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.tree.MesquiteTree;
import mesquite.lib.tree.Tree;
import mesquite.lib.tree.TreeVector;
import mesquite.lib.ui.MesquiteMenuSpec;
import mesquite.zephyr.lib.*;


public abstract class ZephyrTreeSearcher extends ExternalTreeSearcher implements Reconnectable, CanRetrieveTreeBlock, ZephyrRunnerEmployer, NewTreeProcessor {
	protected boolean userAborted = false;
	protected ZephyrRunner runner;
	protected TreeSource treeRecoveryTask;
	protected Tree latestTree = null;
	protected Taxa taxa;
	protected long treeBlockID;
	protected MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	protected int rerootNode = 0;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		if (!(condition instanceof String && ((String)condition).equals("acceptImposedMatrixSource"))) {
			matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
			matrixSourceTask.setMenuToUse(new MesquiteMenuSpec(null, null, null));
			if (matrixSourceTask == null)
				return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");
		}
		runner = (ZephyrRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		if (runner ==null)
			return false;
		runner.initialize(this);
		runner.setUpdateWindow(true);
		return true;
	}
	/*.................................................................................................................*/
	 public void setMatrixSource(MatrixSourceCoord msource) {
		 super.setMatrixSource(msource);
		 this.matrixSourceTask = getMatrixSource();
	 }
	public  void setOutputTextListener(OutputTextListener textListener){
		if (runner != null)
			runner.setOutputTextListener(textListener);
	}
	/*.................................................................................................................*/
	public  void setUserAborted(){
		userAborted = true;
		if (runner!=null)
			runner.setUserAborted(true);
	}
	public String getMessageIfUserAbortRequested () {
		if (runner!=null)
			return runner.getMessageIfUserAbortRequested();
		return null;
	}
	public String getInferenceDetails() {
		if (runner==null)
			return "";
		return runner.getSearchDetails();
	}

	/*.................................................................................................................*/
	public String getTitleOfTextCommandLink() {
		if (runner!=null)
			return runner.getTitleOfTextCommandLink();
		return "";
	}
	/*.................................................................................................................*/
	public String getCommandOfTextCommandLink() {
		if (runner!=null)
			return runner.getCommandOfTextCommandLink();
		return "";
	}
	/*.................................................................................................................*/
	public void processUserClickingOnTextCommandLink(String command) {
		if (runner!=null)
			runner.processUserClickingOnTextCommandLink(command);
	}


	/*.................................................................................................................*/
	public  CategoricalData getData(){
		if (runner!=null)
			return runner.getData();
		return null;
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
	public boolean stopInference(){
		if (observedStates != null){
			CharacterData data = observedStates.getParentData();
			data.decrementEditInhibition();
		}
		return runner.stopExecution();

	}

	public String getLogText() {
		if (runner==null)
			return "";
		return runner.getLogText();
	}

	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		StringBuffer sb = new StringBuffer();
		if (observedStates != null){
			CharacterData data = observedStates.getParentData();
			if (data != null)
				sb.append( "<b>" + getNameForHTML() + "</b> using the matrix " + data.getName() +"<br>");
		}
		String s = runner.getHTMLDescriptionOfStatus();
		if (StringUtil.notEmpty(s))
			sb.append(s+"<br>");
		s = sb.toString();
		if (StringUtil.notEmpty(s))
			return s;
		return getName();
	}
	public String getInferenceName(){
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
		runner.appendMatrixInformation();
		runner.appendAdditionalSearchDetails();
	}
	/*.................................................................................................................*/
	public boolean isReconnectable(){
		return runner.isReconnectable();
	}
	/*.................................................................................................................*/
	/** Notifies all employees that a file is about to be closed.*/
	public boolean fileCloseRequested () {
		if (!MesquiteThread.isScripting()){
			if (!isReconnectable() || !runner.getReadyForReconnectionSave()){
				if (taxa != null)
					taxa.setDirty(true);
			}
			return runner.queryWhetherToCloseFile(getProject().getHomeFile().isDirty());

		}
		return true;
	}
	/*.................................................................................................................*/
	public boolean successfulReconnect(){
		return runSucceeded.getValue();
	}
	MesquiteBoolean runSucceeded = new MesquiteBoolean(true);
	/** Called when Mesquite re-reads a file that had had unfinished tree filling, e.g. by an external process, to pass along the command that should be executed on the main thread when trees are ready.*/
	public void reconnectToRequester(MesquiteCommand command){
		if (runner ==null)
			return;
		String callBackArguments = command.getDefaultArguments();
		String taxaID = parser.getFirstToken(callBackArguments);
		appendSearchDetails();
		if (taxaID !=null)
			taxa = getProject().getTaxa(taxaID);
		else if (getProject().getNumberTaxas() == 1)
			taxa = getProject().getTaxa(0);
		runner.reconnectToRequester(command,runSucceeded);
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("getRunner ", runner);
		temp.addLine("getMatrixSource ", matrixSourceTask);
		if (isReconnectable())
			temp.addLine("setTreeRecoveryTask ", treeRecoveryTask); 

		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the runner", "[module]", commandName, "getRunner")) {
			return runner;
		}
		else if (checker.compare(this.getClass(), "Gets the matrix source", "[module]", commandName, "getMatrixSource")) {
			if (matrixSourceTask!=null && observedStates ==null) {
				if (getData()!=null)
					observedStates = getData().getMCharactersDistribution();   //WAYNECHECK: if we have just reconnected, we can't use the matrixSource to get the current matrix - we need to get the matrix that was recorded on save (but is that necessarily stored???)
				else
					observedStates = matrixSourceTask.getCurrentMatrix(taxa);
			}
			return matrixSourceTask;
		}
		else if (checker.compare(this.getClass(), "Sets the tree recovery task", "[module]", commandName, "setTreeRecoveryTask")) {
			treeRecoveryTask = (TreeSource)hireNamedEmployee(TreeSource.class, "$ #ManyTreesFromFile xxx remain useStandardizedTaxonNames");  //xxx used because ManyTreesFromFiles needs exact argument sequence
			return treeRecoveryTask;
		}
		return null;
	}	



	public String getExtraTreeWindowCommands (boolean finalTree, long treeBlockID){
		this.treeBlockID = treeBlockID;
		return ZephyrUtil.getStandardExtraTreeWindowCommands(runner.doMajRuleConsensusOfResults(), runner.bootstrapOrJackknife(),treeBlockID, true)+ eachTreeCommands();
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
	//This should be the subclass of CharacterState, not CharacterData
	// subclasses can override this if they want something more specific or a different character type
	public Class getCharacterClass() {
		return CategoricalState.class;
		//return null; 
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
		observedStates = null; 
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
			return runner.initializeTaxa(taxa);
		}
		else
			return false;
	}

	public String getExplanation() {
		return "If "+ getProgramName() + " is installed, will save a copy of a character matrix and script "+ getProgramName() + " to conduct one or more searches, and harvest the resulting trees, including their scores.";
	}
	public String getName() {
		String name =   getProgramName() + " Trees";
		if (StringUtil.notEmpty(getMethodNameForTreeBlock()))
			name += " ("+ getMethodNameForMenu() +")";
		if (StringUtil.notEmpty(getProgramLocation()))
			name += " ["+ getProgramLocation() +"]";
		return name;
	}

	public String getColorForProgramLocationHTMLText() {
		return "#3d7040";
	}

	public String getNameForHTML() {
		String name =  getProgramName() + " Trees";
		if (StringUtil.notEmpty(getMethodNameForTreeBlock()))
			name += " ("+ getMethodNameForMenu() +")";
		if (StringUtil.notEmpty(getProgramLocation())) {
			String color = getColorForProgramLocationHTMLText();
			if (StringUtil.notEmpty(color))
				name += " <font color="+color+">["+ getProgramLocation() +"]<font color=black>";
			else
				name += " ["+ getProgramLocation() +"]";
		}
		return name;
	}

	public String getNameForMenuItem() {
		return getName()+ "..."; 
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

   	public TreeVector getCurrentMultipleTrees(Taxa taxa, MesquiteString titleForWindow){
		if (titleForWindow != null)
			titleForWindow.setValue("Tree from "+ getProgramName());
		TreeVector trees = null;
		if (taxa==null)
			trees = runner.retrieveCurrentMultipleTrees(this.taxa);
		else
			trees =  runner.retrieveCurrentMultipleTrees(taxa);
		if (trees !=null) {
			trees.setName(getTreeBlockName(false));
			trees.setAnnotation (runner.getSearchDetails(), false);
		}
		return trees;
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
			trees.setName(getTreeBlockName(true));
			if (!runner.bootstrapOrJackknife()) {
				//DISCONNECTABLE: here need to split this exit and outside here see if it's done
				if (tree==null)
					return null;
				bestScore = finalScores.getValue();
			}
			treeBlockID = trees.getID();
		}
		return trees;
	}

	/*.................................................................................................................*/
	public String getMethodNameForMenu() {
		return "";
	}
	/*.................................................................................................................*/
	public String getMethodNameForTreeBlock() {
		return "";
	}
	/*.................................................................................................................*/
	public String getTreeBlockName(boolean completedRun){
		String s = getProgramName() + " " + getMethodNameForTreeBlock() ;
		if (runner != null){
			if (runner.bootstrapOrJackknife()) {
				if (runner.singleTreeFromResampling()) //this means we have read in all of the bootstrap trees
					s += " " + runner.getResamplingKindName() +  " Consensus Tree";
				else
					s += " " + runner.getResamplingKindName() +  " Trees";
			} 
			else {
				s +=  " Trees";
			}
			if (observedStates!=null) {
				s +=  " (Matrix: " + observedStates.getName();
				String add =  runner.getAddendumToTreeBlockName();
				if (!StringUtil.blank(add))
					s += "; " + add;
				s += ")";
			} else {
				String add =  runner.getAddendumToTreeBlockName();
				if (!StringUtil.blank(add))
					s += "; " + add;
			}

			if (!completedRun)
				if (runner.bootstrapOrJackknife()) 
					s+= " PARTIAL RUN";
				else
					s+= " INCOMPLETE SEARCH";
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
			treeList.setName(getTreeBlockName(true));
			treeList.setAnnotation (runner.getSearchDetails(), false);
			if (!runner.bootstrapOrJackknife()){
				double bestScore = finalScores.getValue();
			}
		}


	}
	/*.................................................................................................................*/
	public int fillTreeBlock(TreeVector treeList){
		if (treeList==null || runner==null || getProject()==null)
			return NULLVALUE;
		if (getProject().getHomeFile()==null)
			return NULLVALUE;
		getProject().getHomeFile().setDirtiedByCommand(true);
		taxa = treeList.getTaxa();
		if (!initialize(taxa))
			return USERABORTONINITIALIZE;

		//DISCONNECTABLE
		TreeVector trees = getTrees(taxa);
		if (trees == null)
			return NULLVALUE;
		treeList.setName(trees.getName());
		treeList.setAnnotation (runner.getSearchDetails(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		trees.dispose();
		treeBlockID = treeList.getID();
		return NOERROR;

	}


}
