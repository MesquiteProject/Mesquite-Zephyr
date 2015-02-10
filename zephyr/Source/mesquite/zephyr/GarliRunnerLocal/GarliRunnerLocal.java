/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.GarliRunnerLocal;

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
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.lib.*;

public class GarliRunnerLocal extends GarliRunner {

	SingleLineTextField garliPathField = null;
	SingleLineTextField constraintFileField = null;

	String ofprefix = "output";

	String dataFileName = "dataMatrix.nex";
	int bootstrapreps = 100;
	int availMemory = 1024;

	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return LocalScriptRunner.class;
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

	public boolean startJob(String arguments, Object condition,
			boolean hiredByName) {
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
			config.append("\ndatafname=" + dataFileName);
			config.append("\nofprefix=" + ofprefix);

			if (StringUtil.blank(constraintfile))
				config.append("\nconstraintfile = none");
			else
				config.append("\nconstraintfile = constraint"); // important to be user-editable

			config.append("\nstreefname = random");

			config.append("\navailablememory = " + availMemory + " \n");
			config.append(" \noutputmostlyuselessfiles = 0");

			config.append("\n");
		}

	}


	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference(String tag, String content) {
		
		if ("availMemory".equalsIgnoreCase(tag))
			availMemory = MesquiteInteger.fromString(content);
		super.processSingleXMLPreference(tag, content);
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML() {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "availMemory", availMemory);
		buffer.append(super.preparePreferencesForXML());
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0";
	}
	IntegerField availableMemoryField;

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
		availableMemoryField = dialog.addIntegerField("Memory for GARLI (MB)", availMemory, 8, 256, MesquiteInteger.infinite);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		availMemory = availableMemoryField.getValue();
	}

	/*.................................................................................................................*
	public boolean queryOptions() {
		if (!okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying Options")) // Debugg.println needs to check that options set well enough to proceed anyway

			return true;

		boolean closeWizard = false;

		if ((MesquiteTrunk.isMacOSXBeforeSnowLeopard())
				&& MesquiteDialog.currentWizard == null) {
			CommandRecord cRec = null;
			cRec = MesquiteThread.getCurrentCommandRecordDefIfNull(null);
			if (cRec != null) {
				cRec.requestEstablishWizard(true);
				closeWizard = true;
			}
		}

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "GARLI Options & Locations", buttonPressed);

		// dialog.addLabel("GARLI - Options and Locations");

		String helpString = "This module will prepare a matrix for GARLI, and ask GARLI do to an analysis.  A command-line version of GARLI must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by GARLI, and, for non-bootstrap analyses, also read in the value of the GARLI score (-ln L) of the tree. "
				+ "You can see the GARLI score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose GARLI Score.";

		dialog.appendToHelpString(helpString);
		dialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());

		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();

		tabbedPanel.addPanel("GARLI Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);
		IntegerField availableMemoryField = dialog.addIntegerField("Memory for GARLI (MB)", availMemory, 8, 256, MesquiteInteger.infinite);
		dialog.addLabelSmallText("This version of Zephyr tested on the following GARLI version(s): " + getTestedProgramVersions());

		tabbedPanel.addPanel("Search Replicates & Bootstrap", true);
		doBootstrapCheckbox = dialog.addCheckBox("do bootstrap analysis", doBootstrap);
		dialog.addHorizontalLine(1);
		dialog.addLabel("Bootstrap Options", Label.LEFT, false, true);
		doBootstrapCheckbox.addItemListener(this);
		IntegerField bootStrapRepsField = dialog.addIntegerField("Bootstrap Reps", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		dialog.addHorizontalLine(1);
		dialog.addLabel("Maximum Likelihood Tree Search Options", Label.LEFT, false, true);
		IntegerField numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);

		tabbedPanel.addPanel("Character Models", true);
		if (!data.hasCharacterGroups()) {
			if (partitionScheme == partitionByCharacterGroups)
				partitionScheme = noPartition;
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			if (partitionScheme == partitionByCodonPosition)
				partitionScheme = noPartition;
		}
		charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups","use codon positions" }, partitionScheme);
		charPartitionButtons.addItemListener(this);
		if (!data.hasCharacterGroups()) {
			charPartitionButtons.setEnabled(1, false);
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			charPartitionButtons.setEnabled(2, false);
		}

		Checkbox linkModelsBox = dialog.addCheckBox("use same set of model parameters for all partition subsets",linkModels);
		Checkbox subsetSpecificRatesBox = dialog.addCheckBox("infer overall rate multipliers for each partition subset",subsetSpecificRates);

		dialog.addHorizontalLine(1);
		partitionChoice = dialog.addPopUpMenu("Edit model for this partition subset:",new String[] { "All Characters" }, 0);
		preparePartitionChoice(partitionChoice, partitionScheme);
		partitionChoice.addItemListener(this);

		rateMatrixChoice = dialog.addPopUpMenu("Rate Matrix", new String[] {"Equal Rates", "2-Parameter", "GTR       ", "Custom" }, 2);
		rateMatrixChoice.addItemListener(this);
		customMatrix = dialog.addTextField("6rate", 20); // since 2 is selected
															// as default in
															// previous
		customMatrix.setEditable(false);
		customMatrix.setBackground(ColorDistribution.veryLightGray);

		invarSitesChoice = dialog.addPopUpMenu("Invariant Sites", new String[] {"none", "Estimate Proportion" }, 1);
		rateHetChoice = dialog.addPopUpMenu("Gamma Site-to-Site Rate Model",new String[] { "none", "Estimate Shape Parameter" }, 1);
		numRateCatField = dialog.addIntegerField("Number of Rate Categories for Gamma", numratecats, 4, 1, 20);

		tabbedPanel.addPanel("Constraint File", true);
		constraintFileField = dialog.addTextField("Path to Constraint File:",constraintfile, 40);
		Button constraintFileBrowseButton = dialog.addAListenedButton("Browse...", null, this);
		constraintFileBrowseButton.setActionCommand("constraintBrowse");

		tabbedPanel.addPanel("Other options", true);
		Checkbox showConfigDetailsBox = dialog.addCheckBox("show config file",
				showConfigDetails);

		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.completeAndShowDialog(true);

		if (buttonPressed.getValue() == 0) {
			if (externalProcRunner.optionsChosen()) {
				constraintfile = constraintFileField.getText();
				numRuns = numRunsField.getValue();
				bootstrapreps = bootStrapRepsField.getValue();
				onlyBest = onlyBestBox.getState();
				doBootstrap = doBootstrapCheckbox.getState();
				showConfigDetails = showConfigDetailsBox.getState();
				partitionScheme = charPartitionButtons.getValue();
				linkModels = linkModelsBox.getState();
				subsetSpecificRates = subsetSpecificRatesBox.getState();
				availMemory = availableMemoryField.getValue();

				// garliOptions = garliOptionsField.getText();

				processCharacterModels();

				storePreferences();
				externalProcRunner.storePreferences();
			}
		}
		dialog.dispose();
		if (closeWizard)
			MesquiteDialog.closeWizard();

		return (buttonPressed.getValue() == 0);
	}

	/*.................................................................................................................*
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("constraintBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			constraintfile = MesquiteFile.openFileDialog("Choose Constraint File", directoryName, fileName);
			if (StringUtil.notEmpty(constraintfile))
				constraintFileField.setText(constraintfile);
		}
	}


	int count = 0;
	double finalValue = MesquiteDouble.unassigned;
	double[] finalValues = null;
	int runNumber = 0;

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
		dataFileName = "dataMatrix.nex"; // replace this with actual file name?

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

	/*.................................................................................................................*

	int numRunsCompleted = 0;
	long screenFilePos = 0;
	MesquiteFile screenFile = null;

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
		return "GARLI Local Runner";
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
