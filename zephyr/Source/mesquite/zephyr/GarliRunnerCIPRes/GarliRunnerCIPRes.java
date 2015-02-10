/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.GarliRunnerCIPRes;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.lib.*;

public class GarliRunnerCIPRes extends GarliRunner {

	String ofprefix = "output";

	String dataFileName = null;


	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return CIPResRESTRunner.class;
	 }

	/*
	 * [model0] datatype = nucleotide ratematrix = 6rate statefrequencies =
	 * estimate ratehetmodel = gamma numratecats = 4 invariantsites = none
	 * 
	 * [model1] datatype = nucleotide ratematrix = 2rate statefrequencies =
	 * estimate ratehetmodel = none numratecats = 1 invariantsites = none
	 */


	// String rootDir;
	/*.................................................................................................................*

	public void getEmployeeNeeds() { // This gets called on startup to harvest
										// information; override this and
										// inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class,
				getName() + "  needs a module to run an external process.", "");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		externalProcRunner = (ExternalProcessRunner) hireEmployee(
				ExternalProcessRunner.class, "External Process Runner (for "
						+ getName() + ")");
		if (externalProcRunner == null) {
			return sorry("Couldn't find an external process runner");
		}
		externalProcRunner.setProcessRequester(this);

		return true;
	}

	public void intializeAfterExternalProcessRunnerHired() {
		loadPreferences();
	}

	/*.................................................................................................................*
	public Snapshot getSnapshot(MesquiteFile file) {
		Snapshot temp = super.getSnapshot(file);
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		return temp;
	}

	/*.................................................................................................................*
	public Object doCommand(String commandName, String arguments,
			CommandChecker checker) {
		if (checker.compare(this.getClass(), "Hires the ExternalProcessRunner",
				"[name of module]", commandName, "setExternalProcessRunner")) {
			ExternalProcessRunner temp = (ExternalProcessRunner) replaceEmployee(
					ExternalProcessRunner.class, arguments,
					"External Process Runner", externalProcRunner);
			if (temp != null) {
				externalProcRunner = temp;
				parametersChanged();
			}
			externalProcRunner.setProcessRequester(this);
			return externalProcRunner;
		} else
			return super.doCommand(commandName, arguments, checker);
	}

	public void reconnectToRequester(MesquiteCommand command) {
		continueMonitoring(command);
	}

	/*.................................................................................................................*/

	public void appendToConfigFileGeneral(StringBuffer config) {
		if (config!=null) {
			StringBuffer sb = new StringBuffer();
			sb.append("\ndatafname=" + dataFileName);
			sb.append("\nofprefix=" + ofprefix);

			if (StringUtil.blank(constraintfile))
				sb.append("\nconstraintfile = none");
			else
				sb.append("\nconstraintfile = constraint"); // important to be user-editable

			String garliGeneralOptions = "\nstreefname = random \n";

//			garliGeneralOptions += "availablememory = " + availMemory + " \n";
			garliGeneralOptions += " \noutputmostlyuselessfiles = 0";
			sb.append(garliGeneralOptions);

			sb.append("\n");
		}

	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*
	public void processSingleXMLPreference(String tag, String content) {
		if ("availMemory".equalsIgnoreCase(tag))
			availMemory = MesquiteInteger.fromString(content);
		if ("doBootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("showConfigDetails".equalsIgnoreCase(tag))
			showConfigDetails = MesquiteBoolean.fromTrueFalseString(content);

		preferencesSet = true;
	}

	/*.................................................................................................................*
	public String preparePreferencesForXML() {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);
		StringUtil.appendXMLTag(buffer, 2, "availMemory", availMemory);
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);
		StringUtil.appendXMLTag(buffer, 2, "doBootstrap", doBootstrap);
		StringUtil.appendXMLTag(buffer, 2, "showConfigDetails",
				showConfigDetails);

		preferencesSet = true;
		return buffer.toString();
	}



	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0";
	}

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
	}




	/*.................................................................................................................*
	public void initializeMonitoring() {
		if (finalValues == null) {
			if (bootstrapOrJackknife())
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numRuns];
			DoubleArray.deassignArray(finalValues);
		}
	}
	/*.................................................................................................................*/

	/*.................................................................................................................*/
	public void setFileNames() {
		configFileName = "garli.conf";
	}

	static final int MAINLOGFILE = 0;
	static final int CURRENTTREEFILEPATH = 1;
	static final int SCREENLOG = 2;
	static final int TREEFILE = 3;
	static final int BESTTREEFILE = 4;

	/*.................................................................................................................*/
	public String[] getLogFileNames() {
		String treeFileName;
		if (bootstrapOrJackknife())
			treeFileName = ofprefix + ".boot.tre";
		else
			treeFileName = ofprefix + ".best.tre";
		String currentTreeFilePath = ofprefix + ".best.current.tre";
		String allBestTreeFilePath = ofprefix + ".best.all.tre";
		String mainLogFileName = ofprefix + ".log00.log";

		return new String[] { mainLogFileName, currentTreeFilePath,ofprefix + ".screen.log", treeFileName, allBestTreeFilePath };
	}


	/* ================================================= *
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		if (!initializeGetTrees(MolecularData.class, taxa, matrix))
			return null;
		//David: if isDoomed() then module is closing down; abort somehow
		setGarliSeed(seed);

		// create the data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this,MesquiteFileUtil.IN_SUPPORT_DIR, "GARLI", "-Run.");
		if (tempDir == null)
			return null;
		dataFileName = "tempData"+ MesquiteFile.massageStringToFilePathSafe(unique) + ".nex"; // replace this with actual file name?

		String dataFilePath = tempDir + dataFileName;
		if (partitionScheme == noPartition)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, selectedTaxaOnly, false, false);
		else if (partitionScheme == partitionByCharacterGroups)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, selectedTaxaOnly, true, false);
		else if (partitionScheme == partitionByCodonPosition)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, selectedTaxaOnly, true, true);

		setFileNames();

		// setting up the GARLI config file
		String config = getGARLIConfigurationFile(data);
		if (!MesquiteThread.isScripting() && showConfigDetails) {
			config = MesquiteString.queryMultiLineString(getModuleWindow(),"GARLI Config File", "GARLI Config File", config, 30, false, true);
			if (StringUtil.blank(config))
				return null;
		}

		// setting up the arrays of input file names and contents
		int numInputFiles = 3;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i = 0; i < numInputFiles; i++) {
			fileContents[i] = "";
			fileNames[i] = "";
		}
		fileContents[0] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[0] = dataFileName;
		if (StringUtil.notEmpty(constraintfile)) {
			fileContents[1] = MesquiteFile.getFileContentsAsString(constraintfile);
			fileNames[1] = "constraint";
		}
		fileContents[2] = config;
		fileNames[2] = configFileName;

		String GARLIcommand = externalProcRunner.getExecutableCommand();
		MesquiteString arguments = new MesquiteString();
		if (externalProcRunner.isWindows())
			arguments.setValue(" --batch " + configFileName);
		else
			arguments.setValue(""); // GARLI command is very simple as all of the arguments are in the config file

		boolean success = runProgramOnExternalProcess(GARLIcommand, arguments, fileContents, fileNames, ownerModule.getName());

		if (!isDoomed()){
		if (success) {
			getProject().decrementProjectWindowSuppression();
			return retrieveTreeBlock(trees, finalScore); // here's where we actually process everything
		}
		}

		if (getProject() != null)
			getProject().decrementProjectWindowSuppression();
		if (data != null)
			data.setEditorInhibition(false);
		return null;
	}

	/*.................................................................................................................*
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		logln("Preparing to receive GARLI trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		finalScore.setValue(finalValue);

		getProject().incrementProjectWindowSuppression();
		FileCoordinator coord = getFileCoordinator();
		MesquiteFile tempDataFile = null;
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed.
		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		// read in the tree files
		if (onlyBest || numRuns == 1 || bootstrapOrJackknife())
			tempDataFile = (MesquiteFile) coord.doCommand("includeTreeFile", StringUtil.tokenize(outputFilePaths[TREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); // TODO: never scripting???
		else
			tempDataFile = (MesquiteFile) coord.doCommand("includeTreeFile",StringUtil.tokenize(outputFilePaths[BESTTREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); // TODO: never scripting???

		runFilesAvailable();

		MesquiteThread.setCurrentCommandRecord(oldCR);

		TreesManager manager = (TreesManager) findElementManager(TreeVector.class);
		Tree t = null;
		int numTB = manager.getNumberTreeBlocks(taxa);
		TreeVector tv = manager.getTreeBlock(taxa, numTB - 1);
		if (tv != null) {
			t = tv.getTree(0);
			ZephyrUtil.adjustTree(t, outgroupSet);

			if (t != null)
				success = true;

			if (treeList != null) {
				double bestScore = MesquiteDouble.unassigned;
				for (int i = 0; i < tv.getNumberOfTrees(); i++) {
					Tree newTree = tv.getTree(i);
					ZephyrUtil.adjustTree(newTree, outgroupSet);

					if (finalValues != null && i < finalValues.length
							&& MesquiteDouble.isCombinable(finalValues[i])) {
						MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
						s.setName(GarliRunner.SCORENAME);
						((Attachable) newTree).attachIfUniqueName(s);
					}

					treeList.addElement(newTree, false);

					if (finalValues != null && i < finalValues.length
							&& MesquiteDouble.isCombinable(finalValues[i]))
						if (MesquiteDouble.isUnassigned(bestScore))
							bestScore = finalValues[i]; // Debugg.println must refind final values, best score
						else if (bestScore < finalValues[i])
							bestScore = finalValues[i];
				}
				logln("Best score: " + bestScore);

			}
		}
		// int numTB = manager.getNumberTreeBlocks(taxa);

		getProject().decrementProjectWindowSuppression();
		if (tempDataFile != null)
			tempDataFile.close();
		// deleteSupportDirectory();
		if (data != null)
			data.setEditorInhibition(false);
		manager.deleteElement(tv); // get rid of temporary tree block
		if (success) {
			postBean("successful", false);
			return t;
		}
		postBean("failed, retrieveTreeBlock", false);
		return null;
	}

	/*.................................................................................................................*/


	/*.................................................................................................................*

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator != null && progIndicator.isAborted())
				|| logFileNames == null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner
				.getOutputFilePath(logFileNames[fileNum]);
		String filePath = outputFilePaths[fileNum];

		

		if (fileNum == MAINLOGFILE && outputFilePaths.length > 0
				&& !StringUtil.blank(outputFilePaths[MAINLOGFILE])
				&& !bootstrapOrJackknife()) { // screen log
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator != null) {
						parser.setString(s);
						String gen = parser.getFirstToken(); // generation
																// number
						progIndicator.setText("Generation: " + gen
								+ ", ln L = " + parser.getNextToken());
						progIndicator.spin();

					}
				count++;
			} else
				Debugg.println("*** File does not exist (" + filePath + ") ***");
		}

		if (fileNum == CURRENTTREEFILEPATH && outputFilePaths.length > 1 && !StringUtil.blank(outputFilePaths[CURRENTTREEFILEPATH]) && !bootstrapOrJackknife()) {
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);
			} else
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, null);
		}

		if (fileNum == SCREENLOG && outputFilePaths.length > 2
				&& !StringUtil.blank(outputFilePaths[SCREENLOG])) {
			if (screenFile == null) { // this is the output file
				if (MesquiteFile.fileExists(filePath))
					screenFile = MesquiteFile.open(true, filePath);
				else
					MesquiteMessage.warnProgrammer("*** File does not exist (" + filePath + ") ***");
			}
			if (screenFile != null) {
				screenFile.openReading();
				if (!MesquiteLong.isCombinable(screenFilePos))
					screenFilePos = 0;
				screenFile.goToFilePosition(screenFilePos);
				String s = screenFile.readLine();
				while (s != null) { // &&
									// screenFile.getFilePosition()<screenFile.existingLength()-2)
									// {
					if (s.startsWith("Final score")) {
						parser.setString(s);
						String s1 = parser.getFirstToken(); // Final
						s1 = parser.getNextToken(); // score
						s1 = parser.getNextToken(); // =
						s1 = parser.getNextToken(); // number
						if (finalValues != null && runNumber < finalValues.length)
							finalValues[runNumber] = MesquiteDouble.fromString(s1);
						runNumber++;
						if (bootstrapOrJackknife())
							logln("GARLI bootstrap replicate " + runNumber + " of " + getTotalReps() + ", ln L = " + s1);
						else
							logln("GARLI search replicate " + runNumber + " of " + getTotalReps() + ", ln L = " + s1);
						numRunsCompleted++;
						double timePerRep = timer.timeSinceVeryStartInSeconds()/ numRunsCompleted; // this is time per rep
						int timeLeft = 0;
						if (bootstrapOrJackknife()) {
							timeLeft = (int) ((bootstrapreps - numRunsCompleted) * timePerRep);
						} else {
							timeLeft = (int) ((numRuns - numRunsCompleted) * timePerRep);
						}
						logln("  Running time so far " + StringUtil.secondsToHHMMSS((int) timer.timeSinceVeryStartInSeconds()) + ", approximate time remaining "+ StringUtil.secondsToHHMMSS(timeLeft));

					} else if (s.startsWith("ERROR:"))
						MesquiteMessage.discreetNotifyUser("GARLI " + s);
					s = screenFile.readLine();
					count++;
				}

				screenFilePos = screenFile.getFilePosition();
				screenFile.closeReading();
			}
		}

	}

	/*.................................................................................................................*/

	public Class getDutyClass() {
		return GarliRunner.class;
	}

	public String getName() {
		return "GARLI CIPRes Runner";
	}




	public String getProgramName() {
		return "GARLI";
	}

	public String getExecutableName() {
		return "GARLI";
	}

	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

}
/*.................................................................................................................*/

