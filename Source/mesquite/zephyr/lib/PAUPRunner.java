/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.*;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public abstract class PAUPRunner extends ZephyrRunner implements ItemListener, ExternalProcessRequester, PAUPCommander {
	public static final String SCORENAME = "PAUPScore";
	public static final String PAUPURL = "http://paup.phylosolutions.com";
	Random rng;
	String datafname = null;
	String ofprefix = "output";
	String PAUPCommandFileMiddle ="";
	protected String scoreFileName ="scoreFile.txt";
	protected String bestTreeFileName ="best_tree.tre";
	protected String currentTreeFileName ="current_tree.tre";
	protected String currentScoreFileName ="current_score.txt";
	long  randseed = -1;
	String dataFileName = "";
	String treeFileName = "";
	//	boolean writeOnlySelectedTaxa = false;
	PAUPCommander paupCommander = this;
	protected ExtensibleDialog dialog;
	protected static int REGULARSEARCH=0;
	protected static int BOOTSTRAPSEARCH=1;
	protected static int JACKKNIFESEARCH=2;
	protected int searchStyle = REGULARSEARCH;

	protected static int HEURISTICSEARCH=0;
	protected static int BRANCHANDBOUND=1;
	protected int searchMethod = HEURISTICSEARCH;

	protected static final int NOCONSTRAINT = 0;
	protected static final int MONOPHYLY = 1;
	protected static final int BACKBONE = 2;
	protected int useConstraintTree = NOCONSTRAINT;


	SingleLineTextField PAUPPathField =  null;
	protected boolean preferencesSet = false;


	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class, getName() + "  needs a module to run an external process.","");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		if (!hireExternalProcessRunner()){
			return sorry("Couldn't hire an external process runner");
		}
		externalProcRunner.setProcessRequester(this);
		rng = new Random(System.currentTimeMillis());
		loadPreferences();

		return true;
	}

	public String getLogText() {
		return externalProcRunner.getStdOut();
	}

	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
		MesquiteFile.suppressReadWriteLogging=!verbose;
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = super.getSnapshot(file);
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		return temp;
	}
	/*.................................................................................................................*/
	public  boolean showMultipleRuns() {
		return false;
	}
	public void setConstrainedSearch(boolean constrainedSearch) {
		if (useConstraintTree==NOCONSTRAINT && constrainedSearch)
			useConstraintTree=MONOPHYLY;
		else if (useConstraintTree!=NOCONSTRAINT && !constrainedSearch)
			useConstraintTree = NOCONSTRAINT;
		this.constrainedSearch = constrainedSearch;
	}
	public void setConstraintTreeType (int useConstraintTree) {
		this.useConstraintTree = useConstraintTree;
	}

	public int getConstraintTreeType() {
		return useConstraintTree;
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == constraintButtons && constraintButtons.getValue()>0){

			getConstraintTreeSource();

		}
	}
	protected OneTreeSource constraintTreeTask = null;
	protected OneTreeSource getConstraintTreeSource(){
		if (constraintTreeTask == null){
			constraintTreeTask = (OneTreeSource)hireEmployee(OneTreeSource.class, "Source of constraint tree");
		}
		return constraintTreeTask;
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Hires the ExternalProcessRunner", "[name of module]", commandName, "setExternalProcessRunner")) {
			ExternalProcessRunner temp = (ExternalProcessRunner)replaceEmployee(ExternalProcessRunner.class, arguments, "External Process Runner", externalProcRunner);
			if (temp != null) {
				externalProcRunner = temp;
				parametersChanged();
			}
			externalProcRunner.setProcessRequester(this);
			return externalProcRunner;
		} else
			return super.doCommand(commandName, arguments, checker);
	}	

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("useConstraintTree".equalsIgnoreCase(tag))
			useConstraintTree = MesquiteInteger.fromString(content);
		//		if ("writeOnlySelectedTaxa".equalsIgnoreCase(tag))
		//			writeOnlySelectedTaxa = MesquiteBoolean.fromTrueFalseString(content);
		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "useConstraintTree", useConstraintTree);  
		//		StringUtil.appendXMLTag(buffer, 2, "writeOnlySelectedTaxa", writeOnlySelectedTaxa);  
		buffer.append(prepareMorePreferencesForXML());
		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public String getDataFileName(){
		return dataFileName;
	}
	/*.................................................................................................................*/
	public String getOutputTreeFileName(){
		return treeFileName;
	}
	/*.................................................................................................................*/
	public String PAUPCommandFileStart(){
		String commandStart = "#NEXUS\n\n";
		commandStart+="begin paup;\n"
		        +"\tset torder=right tcompress outroot=monophyl taxlabels=full nowarnreset nowarnroot NotifyBeep=no nowarntree nowarntsave;\n"
				+ "\tlog file=logfile.txt replace=yes;\n";
		return commandStart;
	}

	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data, String constraintTree){
		return "";
	}
	/*.................................................................................................................*/
	public String PAUPCommandFileEnd(){
		return "\tquit;\nend;\n";
	}
	public PAUPCommander getPaupCommander() {
		return paupCommander;
	}

	public void setPaupCommander(PAUPCommander paupCommander) {
		this.paupCommander = paupCommander;
	}

	/*.................................................................................................................*/
	public void setPAUPCommandFileMiddle(String PAUPCommandFileMiddle){
		this.PAUPCommandFileMiddle = PAUPCommandFileMiddle;   		
	}
	/*.................................................................................................................*/
	public String getPAUPCommandFile(PAUPCommander paupCommander, String fileName, String treeFileName, CategoricalData data, String constraintTree){
		StringBuffer sb = new StringBuffer();
		sb.append(PAUPCommandFileStart());
		if (StringUtil.notEmpty(constraintTree)) {
			
		}
		sb.append(paupCommander.getPAUPCommandFileMiddle(fileName, treeFileName, data, constraintTree));
		sb.append(PAUPCommandFileEnd());
		return sb.toString();
	}
	/*.................................................................................................................*/
	public void setDataFName(String datafname){
		this.datafname = datafname;
	}
	/*.................................................................................................................*/
	public void setPAUPSeed(long seed){
		this.randseed = seed;
	}

	//	ProgressIndicator progIndicator;
	int count=0;

	double finalValue = MesquiteDouble.unassigned;

	String commandFileName = "";
	String logFileName = "";




	static final int OUT_TREEFILE=0;
	static final int OUT_LOGFILE = 1;
	static final int OUT_SCOREFILE = 2;
	static final int OUT_CURRENTTREEFILE = 3;
	static final int OUT_CURRENTSCOREFILE = 4;
	static final int OUT_BESTTREEFILE = 5;
	public String[] getLogFileNames(){
		return new String[]{treeFileName,  logFileName, scoreFileName, currentTreeFileName, currentScoreFileName, bestTreeFileName};
	}

	public void setFileNames () {
		commandFileName =  "PAUPCommands.txt";
		treeFileName = ofprefix+".tre";
		logFileName = ofprefix+".log00.log";
		scoreFileName = ofprefix+".score.txt";
		currentTreeFileName = "current_tree.tre";
		currentScoreFileName = "current_score.txt";
		bestTreeFileName = "best_tree.tre";
	}

	int firstOutgroup = 0;
	TaxaSelectionSet outgroupSet;

	/*.................................................................................................................*/

	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		if (!initializeGetTrees(CategoricalData.class, taxa, matrix))
			return null;
		setPAUPSeed(seed);
		//David: if isDoomed() then module is closing down; abort somehow

		//write data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "PAUP","-Run.");  
		if (tempDir==null)
			return null;
		dataFileName = "dataFile.nex";   //replace this with actual file name?
		String dataFilePath = tempDir +  dataFileName;
		boolean fileSaved = false;
		fileSaved = ZephyrUtil.writeNEXUSFile(taxa,  tempDir,  dataFileName,  dataFilePath,  data,false, true, selectedTaxaOnly, true, true, true);
		if (!fileSaved) return null;

		setFileNames();


		String constraintTree = "";

		if (useConstraintTree>NOCONSTRAINT || isConstrainedSearch()){
			if (isConstrainedSearch() && useConstraintTree==NOCONSTRAINT)  //TODO: change  Debugg.println
				useConstraintTree=MONOPHYLY;
			if (constraint==null) { // we don't have one
				getConstraintTreeSource();
				if (constraintTreeTask != null)
					constraint = constraintTreeTask.getTree(taxa, "This will be the constraint tree");
			}
			if (constraint == null){
				discreetAlert("Constraint tree is not available.");
				return null;
			}
			else if (useConstraintTree == BACKBONE){
				constraintTree = constraint.writeTreeSimpleByNames() + ";";
				appendToExtraSearchDetails("\nBACKBONEl constraint using tree \"" + constraint.getName() + "\"");
				appendToAddendumToTreeBlockName("Constrained by tree \"" + constraint.getName() + "\"");
			}
			else if (useConstraintTree == MONOPHYLY){
				if (constraint.hasPolytomies(constraint.getRoot())){
					constraintTree = constraint.writeTreeSimpleByNames() + ";";
					appendToExtraSearchDetails("\nPartial resolution constraint using tree \"" + constraint.getName() + "\"");
					appendToAddendumToTreeBlockName("Constrained by tree \"" + constraint.getName() + "\"");
				}
				else {
					discreetAlert("Constraint tree cannot be used as a constraint because it is strictly dichotomous");
					if (constraintTreeTask != null)
						constraintTreeTask.reset();
					return null;
				}
			}
		}

		setRootNameForDirectoryInProcRunner();

		String commands = getPAUPCommandFile(paupCommander, dataFileName, treeFileName, data, constraintTree);
		if (isVerbose()) {
			logln("\n\nCommands given to PAUP:");
			logln(commands);
			logln("");
		}

		String arguments = commandFileName;

		String programCommand = externalProcRunner.getExecutableCommand();
		//+ " " + arguments + StringUtil.lineEnding();  



		int numInputFiles = 2;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i=0; i<numInputFiles; i++){
			fileContents[i]="";
			fileNames[i]="";
		}
		fileContents[0] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[0] = dataFileName;
		fileContents[1] = commands;
		fileNames[1] = commandFileName;


		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, fileContents, fileNames,  ownerModule.getName());

		if (!isDoomed()){
			if (success){
				desuppressProjectPanelReset();
				return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
			} else
				reportStdError();
			if (!beanWritten)
				postBean("unsuccessful [1]", false);
			beanWritten=true;
		}
		desuppressProjectPanelReset();
		if (data != null)
			data.decrementEditInhibition();
		externalProcRunner.finalCleanup();
		return null;
	}	
	/*.................................................................................................................*/

	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		if (isVerbose()) 
			logln("Preparing to receive PAUP trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		//TODO		finalScore.setValue(finalValue);

		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		FileCoordinator coord = getFileCoordinator();
		String treeFilePath = outputFilePaths[OUT_TREEFILE];
		if (!MesquiteFile.fileExists(treeFilePath)) {
			logln("PAUP tree file not found");
			reportStdError();
			if (!beanWritten)
				postBean("failed - no tree file found", false);
			beanWritten=true;
			return null;
		}

		String scoreFilePath = outputFilePaths[OUT_SCOREFILE];
		if (MesquiteFile.fileExists(scoreFilePath) && finalScore!=null) {
			String contents = MesquiteFile.getFileContentsAsString(scoreFilePath);
			Parser parser = new Parser(contents);
			parser.setPunctuationString("");
			String s = parser.getRawNextDarkLine(); // title line
			s = parser.getNextToken();  // tree number
			s = parser.getNextToken();  // score of first tree
			double d = MesquiteDouble.fromString(s);
			if (MesquiteDouble.isCombinable(d))
				finalScore.setValue(d);
			/*
			while (!StringUtil.blank(s)) {

				s = parser.getRawNextDarkLine();
			}
			 */		}
		suppressProjectPanelReset();
		MesquiteFile tempDataFile = null;
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);
		tempDataFile = (MesquiteFile)coord.doCommand("includeTreeFile", StringUtil.tokenize(treeFilePath) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave taxa "+StringUtil.tokenize(taxa.getName()), CommandChecker.defaultChecker); //TODO: never scripting???
		MesquiteThread.setCurrentCommandRecord(oldCR);


		// define file paths and set tree files as needed. 

		runFilesAvailable();

		TreesManager manager = (TreesManager)findElementManager(TreeVector.class);
		Tree t =null;
		int numTB = manager.getNumberTreeBlocks(taxa);
		TreeVector tv = manager.getTreeBlock(taxa,numTB-1);
		if (tv!=null) {
			t = tv.getTree(0);
			if (t!=null)
				success=true;
			if (bootstrapOrJackknife() && !doMajRuleConsensusOfResults()) {
				if (t instanceof MesquiteTree) {
					((MesquiteTree)t).convertBranchLengthsToNodeValue("consensusFrequency");
				}
			}
			if (tv.getNumberOfTrees()>=1 && treeList !=null) {
				for (int i=0; i<tv.getNumberOfTrees(); i++)
					treeList.addElement(tv.getTree(i), false);
			} 
		}
		//int numTB = manager.getNumberTreeBlocks(taxa);

		//desuppressProjectPanelReset();
		if (tempDataFile!=null)
			tempDataFile.close();


		manager.deleteElement(tv);  // get rid of temporary tree block
		desuppressProjectPanelReset();
		if (data!=null)
			data.decrementEditInhibition();		
		externalProcRunner.finalCleanup();
		if (success) { 
			if (!beanWritten)
				postBean("successful", false);
			beanWritten=true;
			return t;
		} else {
			reportStdError();
			if (!beanWritten)
				postBean("failed", false);
			beanWritten=true;
			return null;
		}

	}

	/*.................................................................................................................*/
	public void initializeMonitoring(){
		outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		}
	}

	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted())) {
			setUserAborted(true);
			return;
		}
		if (logFileNames == null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath = outputFilePaths[fileNum];

		/* if (fileNum==1) filePath =
		 * getOutputFileToReadPath(outputFilePaths[fileNum]); else filePath =
		 * outputFilePaths[fileNum];
		 */

		if (fileNum == OUT_CURRENTTREEFILE && outputFilePaths.length > 1 && !StringUtil.blank(outputFilePaths[OUT_CURRENTTREEFILE]) && !bootstrapOrJackknife()) {
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;
				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);
				} else
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
			}
		}
/*		if (fileNum == OUT_BESTTREEFILE && outputFilePaths.length > 1 && !StringUtil.blank(outputFilePaths[OUT_BESTTREEFILE]) && !bootstrapOrJackknife()) {
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;
				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);
				} else
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
			}
		}
*/
		if (fileNum == OUT_CURRENTSCOREFILE && outputFilePaths.length > 1 && !StringUtil.blank(outputFilePaths[OUT_CURRENTSCOREFILE]) && !bootstrapOrJackknife()) {
			if (MesquiteFile.fileExists(filePath)) {
				String score = MesquiteFile.getFileLastDarkLine(filePath);
				if (!StringUtil.blank(score)){
					logln("current score: " + score);
					if (progIndicator != null) {
						parser.setString(score);
						// number
						progIndicator.setText("Score: " + score);
						progIndicator.spin();
					}
				}
				count++;
			} else if (MesquiteTrunk.debugMode)
				MesquiteMessage.warnProgrammer("*** File does not exist (" + filePath + ") ***");
		}

	}


	public boolean continueShellProcess(Process proc){
		return true;
	}


	/*.................................................................................................................*
	String getPAUPCommand(){
		if (MesquiteTrunk.isWindows())
			return StringUtil.protectForWindows(PAUPPath);
		else
			return StringUtil.protectForUnix(PAUPPath);
	}

	Parser parser = new Parser();

	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return outputFilePaths;
	}

	/*.................................................................................................................*/

	public void processOutputFile(String[] outputFilePaths, int fileNum) {
		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0])) {
			//	String s = MesquiteFile.getFileLastContents(outputFilePaths[0]);
			//	if (!StringUtil.blank(s))
		}

	}
	/*.................................................................................................................*/

	public void processCompletedOutputFiles(String[] outputFilePaths) {
		if ( outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0])) {
		}

	}

	public String getConstraintTreeName() {
		if (constraint==null)
			return null;
		return constraint.getName();
	}


	/*.................................................................................................................*
   	public void setPAUPPath(String PAUPPath){
   		this.PAUPPath = PAUPPath;
   	}
	/*.................................................................................................................*/

	public Class getDutyClass() {
		return PAUPRunner.class;
	}


	public void intializeAfterExternalProcessRunnerHired() {
		loadPreferences();
	}


	public void reconnectToRequester(MesquiteCommand command){
		continueMonitoring(command);
	}

	public String getProgramName() {
		return "PAUP";
	}

	public String getExecutableName() {
		return "PAUP";
	}

	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}
	/*.................................................................................................................*/
	public void processMorePreferences (String tag, String content) {
	}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		return "";
	}

	public abstract void queryOptionsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) ;
	/*.................................................................................................................*/
	public abstract void queryOptionsProcess(ExtensibleDialog dialog);

	RadioButtons constraintButtons;
	/*.................................................................................................................*
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("composeRAxMLCommand")) {

			MesquiteString arguments = new MesquiteString();
			getArguments(arguments, "fileName", proteinModelField.getText(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null, false);
			String command = externalProcRunner.getExecutableCommand() + arguments.getValue();
			commandLabel.setText("This command will be used to run RAxML:");
			commandField.setText(command);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		if (!okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying Options"))  //Debugg.println needs to check that options set well enough to proceed anyway
			return true;

		boolean closeWizard = false;

		if ((MesquiteTrunk.isMacOSXBeforeSnowLeopard()) && MesquiteDialog.currentWizard == null) {
			CommandRecord cRec = null;
			cRec = MesquiteThread.getCurrentCommandRecordDefIfNull(null);
			if (cRec != null){
				cRec.requestEstablishWizard(true);
				closeWizard = true;
			}
		}

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		String titleExtra = "";
		String extra = getExtraQueryOptionsTitle();
		if (StringUtil.notEmpty(extra))
			titleExtra += " ("+extra+")";
		dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options"+titleExtra,buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//		dialog.addLabel(getName() + " Options and Location");
		String helpString = "This module will prepare a matrix for PAUP, and ask PAUP do to an analysis.  A command-line version of PAUP must be installed. ";

		dialog.appendToHelpString(helpString);

		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();

		tabbedPanel.addPanel("PAUP Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);



		tabbedPanel.addPanel("General", true);

		queryOptionsSetup(dialog, tabbedPanel);
		
		if (getConstrainedSearchAllowed()) {
			tabbedPanel.addPanel("Constraints", true);

			//		Checkbox selectedOnlyBox = dialog.addCheckBox("consider only selected taxa", writeOnlySelectedTaxa);
			constraintButtons = dialog.addRadioButtons (new String[]{"No Constraint", "Monophyly", "Backbone"}, useConstraintTree);
			constraintButtons.addItemListener(this);
		}

		//TextArea PAUPOptionsField = queryFilesDialog.addTextArea(PAUPOptions, 20);
		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (getConstrainedSearchAllowed()) {
				useConstraintTree = constraintButtons.getValue();
				if (useConstraintTree!=NOCONSTRAINT)
					setConstrainedSearch(true);
				else
					setConstrainedSearch(false);
			}
			if (externalProcRunner.optionsChosen() && infererOK) {
				queryOptionsProcess(dialog);
				//				writeOnlySelectedTaxa = selectedOnlyBox.getState();
				storeRunnerPreferences();
			}
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	public String getResamplingKindName() {
		if (searchStyle==BOOTSTRAPSEARCH )
			return "Bootstrap";
		if (searchStyle==JACKKNIFESEARCH )
			return "Jackknife";
		return "Bootstrap";
	}

	public boolean errorsAreFatal(){
		return true;
	}

	public boolean allowStdErrRedirect() {
		return true;
	}

	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}


}
