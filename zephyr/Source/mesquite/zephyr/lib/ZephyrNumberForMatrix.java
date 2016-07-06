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
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;


public abstract class ZephyrNumberForMatrix extends NumberForMatrix implements Reconnectable, ZephyrRunnerEmployer {
	protected ZephyrRunner runner;
	protected TreeSource treeRecoveryTask;
	protected Taxa taxa;
//	protected StringBuffer outputBuffer= new StringBuffer(0);
//	protected String outputFilePath;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		runner = (ZephyrRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		if (runner ==null)
			return false;
		runner.initialize(this);
		runner.setBootstrapAllowed(false);
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

	public void initialize(MCharactersDistribution data) {
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
		runner.reconnectToRequester(command);
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("getRunner ", runner);
		//		temp.addLine("getMatrixSource ", matrixSourceTask);
		temp.addLine("setTreeRecoveryTask ", treeRecoveryTask); //

		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the runner", "[module]", commandName, "getRunner")) {
			return runner;
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

	/*.................................................................................................................*/

	public boolean initialize(Taxa taxa) {
		this.taxa = taxa;
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
		return "If "+ getProgramName() + " is installed, will save a copy of a character matrix and script "+ getProgramName() + " to conduct one or more searches, and harvest the resulting scores.";
	}
	public String getName() {
		return getProgramName() + " Optimal Tree Score";
	}
	public String getNameForMenuItem() {
		return getProgramName() + " Optimal Tree Score...";
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

	public void calculateNumber(MCharactersDistribution data, MesquiteNumber result, MesquiteString resultString) {
		if (taxa==null) 
			taxa=data.getTaxa();
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick(getProgramName() + " Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());

		MesquiteDouble finalScores = new MesquiteDouble();

		runner.getTrees(trees, taxa, data, rng.nextInt(), finalScores);
		runner.setRunInProgress(false);

		if (result!=null)
			result.setValue(finalScores.getValue());

		if (resultString!=null)
			resultString.setValue(""+finalScores.getValue());
		trees.dispose();
		trees=null;
	}

	/*.................................................................................................................*/

	//TEMPORARY Debugg.println  Should be only in disconnectable tree block fillers
	public void retrieveTreeBlock(TreeVector treeList){
		if (runner != null){
			MesquiteDouble finalScores = new MesquiteDouble();
			runner.retrieveTreeBlock(treeList, finalScores);
			taxa = treeList.getTaxa();
			//			initializeObservedStates(taxa);
			//			boolean bootstrap = runner.bootstrap();
			double bestScore = finalScores.getValue();

		}


	}

}
