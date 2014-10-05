/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.TNTRunner;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.TNTTrees.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;

/* TODO:

- displays final bootstrap tree

- set random numbers seed ("RSEED xxx");
- use semicolons on windows?

- control over search, mxram

- deal with ccode properly, etc.
- deal with IUPAC and protein data correctly

 */

public class TNTRunner extends ZephyrRunner  implements ItemListener, ActionListener, ExternalProcessRequester, ZephyrFilePreparer  {
	public static final String SCORENAME = "TNTScore";


	int mxram = 1000;

	boolean preferencesSet = false;
	boolean convertGapsToMissing = true;
	boolean isProtein = false;

	int bootstrapreps = 0;
	long bootstrapSeed = System.currentTimeMillis();
	boolean doBootstrap= false;
	String otherOptions = "";

	boolean parallel = false;
	int numSlaves = 6;
	String bootstrapSearchArguments = "";
	String searchArguments = "";


	long  randseed = -1;
	String constraintfile = "none";
	int totalNumHits = 250;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class, getName() + "  needs a module to run an external process.","");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		externalProcRunner = (ExternalProcessRunner)hireEmployee(ExternalProcessRunner.class, "External Process Runner (for " + getName() + ")"); 
		if (externalProcRunner==null){
			return sorry("Couldn't find an external process runner");
		}
		externalProcRunner.setProcessRequester(this);

		return true;
	}
	/*.................................................................................................................*/
	public boolean  loadModule(){ 
		return true;
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		return temp;
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
		}
		return null;
	}	

	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootstrapreps = MesquiteInteger.fromString(content);
		if ("mxram".equalsIgnoreCase(tag))
			mxram = MesquiteInteger.fromString(content);
		if ("convertGapsToMissing".equalsIgnoreCase(tag))
			convertGapsToMissing = MesquiteBoolean.fromTrueFalseString(content);
		if ("doBootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("searchArguments".equalsIgnoreCase(tag))
			searchArguments = StringUtil.cleanXMLEscapeCharacters(content);
		if ("bootstrapSearchArguments".equalsIgnoreCase(tag))
			bootstrapSearchArguments = StringUtil.cleanXMLEscapeCharacters(content);

		//parallel, etc.
		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "mxram", mxram);  
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "convertGapsToMissing", convertGapsToMissing);  
		StringUtil.appendXMLTag(buffer, 2, "searchArguments", searchArguments);  
		StringUtil.appendXMLTag(buffer, 2, "bootstrapSearchArguments", bootstrapSearchArguments);  
		StringUtil.appendXMLTag(buffer, 2, "doBootstrap", doBootstrap);  

		preferencesSet = true;
		return buffer.toString();
	}
	
	public void setDefaultSearchArguments(){
		bootstrapSearchArguments="";
		bootstrapSearchArguments +=   getTNTCommand("rseed[");   // if showing intermediate trees
		bootstrapSearchArguments +=   getTNTCommand("hold 3000");   
		//	bootstrapSearchArguments +=   " sect: slack 5"+getComDelim();   
		//	bootstrapSearchArguments +=   " xmult: replications 2 hits 2 ratchet 15 verbose drift 10"+getComDelim();  
		bootstrapSearchArguments +=   getTNTCommand("sect: slack 30");   
		bootstrapSearchArguments +=   getTNTCommand("sec: xss 4-2+3-1 gocomb 60 fuse 4 drift 5 combstart 5");   
		bootstrapSearchArguments +=   getTNTCommand("xmult: replications 1 hits 1 ratchet 15 verbose rss xss drift 10 dumpfuse") ;   // actual search

		searchArguments="";
		searchArguments +=   getTNTCommand("rseed[");   
		searchArguments +=   getTNTCommand("hold 10000");   
		searchArguments +=   getTNTCommand("xinact");   
		searchArguments +=   getTNTCommand("xmult:  rss css fuse 6 drift 6 ratchet 20 replic 100");   
		searchArguments +=   getTNTCommand("sec: slack 20");   
		searchArguments +=   getTNTCommand("bbreak: tbr safe fillonly") ;   // actual search
		searchArguments +=   getTNTCommand("xmult");   
		searchArguments +=   getTNTCommand("bbreak");   
	}
	
	public void intializeAfterExternalProcessRunnerHired() {
		setDefaultSearchArguments();
		loadPreferences();
	}

	public void reconnectToRequester(MesquiteCommand command){
		continueMonitoring(command);
	}

	/*.................................................................................................................*/
	void adjustDialogText() {
		if (bootStrapRepsField!=null)
			if (parallel)
				bootStrapRepsField.setLabelText("Bootstrap Replicates Per Slave");
			else
				bootStrapRepsField.setLabelText("Bootstrap Replicates");
	}
	ExtensibleDialog queryOptionsDialog=null;
	IntegerField bootStrapRepsField = null;
	Button useDefaultsButton = null;
	TextArea searchField = null;
	TextArea bootstrapSearchField = null;
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

		queryOptionsDialog = new ExtensibleDialog(containerOfModule(), "TNT Options & Locations",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//		queryOptionsDialog.addLabel("TNT - Options and Locations");
		String helpString = "This module will prepare a matrix for TNT, and ask TNT do to an analysis.  A command-line version of TNT must be installed. "
				+ "You can ask it to do a bootstrap analysis or not. "
				+ "Mesquite will read in the trees found by TNT. ";

		queryOptionsDialog.appendToHelpString(helpString);
		queryOptionsDialog.setHelpURL(ownerModule.getProgramURL());

		MesquiteTabbedPanel tabbedPanel = queryOptionsDialog.addMesquiteTabbedPanel();

		tabbedPanel.addPanel("TNT Program Details", true);
		externalProcRunner.addItemsToDialogPanel(queryOptionsDialog);
		Checkbox parallelCheckBox = queryOptionsDialog.addCheckBox("use PVM for parallel processing", parallel);
		parallelCheckBox.addItemListener(this);
		IntegerField slavesField = queryOptionsDialog.addIntegerField("Number of Slaves", numSlaves, 4, 0, MesquiteInteger.infinite);
		IntegerField maxRamField = queryOptionsDialog.addIntegerField("mxram value", mxram, 4, 0, MesquiteInteger.infinite);


		tabbedPanel.addPanel("Search Options", true);

		queryOptionsDialog.addLabel("Regular Search Options");
		searchField = queryOptionsDialog.addTextAreaSmallFont(searchArguments, 7,80);

		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addLabel("Bootstrap Search Options");
		Checkbox doBootstrapBox = queryOptionsDialog.addCheckBox("do bootstrapping", doBootstrap);
		bootStrapRepsField = queryOptionsDialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		bootstrapSearchField = queryOptionsDialog.addTextAreaSmallFont(bootstrapSearchArguments, 7,80);

		tabbedPanel.addPanel("Other Options", true);
		Checkbox convertGapsBox = queryOptionsDialog.addCheckBox("convert gaps to missing (to avoid gap=extra state)", convertGapsToMissing);
		SingleLineTextField otherOptionsField = queryOptionsDialog.addTextField("Other TNT options:", otherOptions, 40);

		adjustDialogText();
		queryOptionsDialog.addNewDialogPanel();
		useDefaultsButton = queryOptionsDialog.addAListenedButton("Set to Defaults", null, this);
		useDefaultsButton.setActionCommand("setToDefaults");

		tabbedPanel.cleanup();
		queryOptionsDialog.nullifyAddPanel();

		queryOptionsDialog.completeAndShowDialog("Search", "Cancel", null, null);

		if (buttonPressed.getValue()==0)  {
			if (externalProcRunner.optionsChosen()) {
				bootstrapreps = bootStrapRepsField.getValue();
				numSlaves = slavesField.getValue();
				otherOptions = otherOptionsField.getText();
				convertGapsToMissing = convertGapsBox.getState();
				parallel = parallelCheckBox.getState();
				doBootstrap = doBootstrapBox.getState();
				searchArguments = searchField.getText();
				bootstrapSearchArguments = bootstrapSearchField.getText();
				mxram = maxRamField.getValue();

				externalProcRunner.storePreferences();
				storePreferences();
			}
		}
		queryOptionsDialog.dispose();
		if (closeWizard) 
			MesquiteDialog.closeWizard();

		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*
	public boolean queryTaxaOptions(Taxa taxa) {
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (ssv==null || ssv.size()<=0)
			return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "TNT Outgroup Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("TNT Outgroup Options");


		Choice taxonSetChoice = null;
		taxonSetChoice = dialog.addPopUpMenu ("Outgroups: ", ssv, 0);


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {

			outgroupTaxSetString = taxonSetChoice.getSelectedItem();

		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("setToDefaults")) {
			setDefaultSearchArguments();
			searchField.setText(searchArguments);	
			bootstrapSearchField.setText(bootstrapSearchArguments);
		}
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (queryOptionsDialog!=null) {
			adjustDialogText();	
		}
	}

	/*.................................................................................................................*/
	public void setTNTSeed(long seed){
		this.randseed = seed;
	}

	int count=0;

	public void prepareExportFile(FileInterpreterI exporter) {
		((InterpretHennig86Base)exporter).setConvertGapsToMissing(convertGapsToMissing);
		((InterpretHennig86Base)exporter).setIncludeQuotes(false);
	}

	/*.................................................................................................................*/
	String getComDelim(){
		if (externalProcRunner.isWindows())
			return ";"+StringUtil.lineEnding();
		else
			return ";"+StringUtil.lineEnding();
	}

	String commands = "";

	/*.................................................................................................................*
	String prettyCommand(String command) {
		return "   "+command.replaceAll(";", ";" + StringUtil.lineEnding()+"  ");
	}
	/*.................................................................................................................*/
	String getTNTCommand(String command) {
		return "   " + command + getComDelim();
	}
	/*.................................................................................................................*/
	String indentTNTCommand(String command) {
		return "   " + command;
	}


	/*.................................................................................................................*/
	void formCommandFile(String dataFileName, int firstOutgroup) {
		if (parallel) {
			commands = "";
		}
		commands += getTNTCommand("mxram " + mxram);
		
		commands += getTNTCommand("report+0/1/0");
		commands += getTNTCommand("p " + dataFileName);
		commands += getTNTCommand("log "+logFileName) ; 
		commands += getTNTCommand("vversion");
		if (MesquiteInteger.isCombinable(firstOutgroup) && firstOutgroup>=0)
			commands += getTNTCommand("outgroup " + firstOutgroup);
		if (bootstrap()) {
			if (parallel) {
				commands += indentTNTCommand("ptnt begin parallelRun " + numSlaves + "/ram x 2 = ");
			}
			commands += StringUtil.lineEnding() + bootstrapSearchArguments + StringUtil.lineEnding();
			if (parallel) {
				int numRepsPerSlave = bootstrapreps/numSlaves;
				if (numRepsPerSlave*numSlaves<bootstrapreps) numRepsPerSlave++;
				commands += getTNTCommand("resample boot cut 50 savetrees replications " + numRepsPerSlave + " [xmult; bb] savetrees"); // + getComDelim();   
				commands += getTNTCommand("return");
				commands += getTNTCommand("ptnt wait parallelRun");
				commands += getTNTCommand("ptnt get parallelRun");
			} else
				commands += getTNTCommand("resample boot cut 50 savetrees replications " + bootstrapreps + " [xmult; bb]"); // + getComDelim();   

			commands += getTNTCommand("save") ; 
			//commands += getTNTCommand("proc/") ; 


			//	if (!parallel)
			commands += getTNTCommand("quit") ; 
		}
		else {
			//commands += getTNTCommand("tsave !5 " + treeFileName) ;   // if showing intermediate trees
			commands +=  getTNTCommand("tsave *" + treeFileName);
			commands += searchArguments;
			commands +=   getTNTCommand("save");   
			commands += getTNTCommand("log/") ; 
			commands += getTNTCommand("tsave/") ; 
			commands += getTNTCommand("quit") ; 
		}
	}

	String treeFileName="TNT_Trees.txt";
//	String currentTreeFileName;
	String logFileName;
	String commandsFileName;


	/*.................................................................................................................*/
	public void setFileNames(){
		/*if (bootstrap())
			treeFileName = "TNT_bootstrapTrees.txt";
		else 
			treeFileName = "TNT_Trees.txt";  */
//		currentTreeFileName = treeFileName+"-1";
		logFileName = "log.out";
		commandsFileName = "TNTCommands.txt";
	}
	/*.................................................................................................................*/
	
	static final int OUT_TREEFILE=0;
	static final int OUT_LOGFILE = 1;
	public String[] getLogFileNames(){
		return new String[]{treeFileName,  logFileName};
	}

	/*.................................................................................................................*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		if (!initializeGetTrees(MolecularData.class, matrix))
			return null;
		setTNTSeed(seed);
		isProtein = data instanceof ProteinData;


		//write data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "TNT","-Run.");  
		if (tempDir==null)
			return null;
		String dataFileName = "tempData" + MesquiteFile.massageStringToFilePathSafe(unique) + ".ss";   //replace this with actual file name?
		String dataFilePath = tempDir +  dataFileName;
		FileInterpreterI exporter = ZephyrUtil.getFileInterpreter(this,"#InterpretTNT");
		if (exporter==null)
			return null;
		boolean fileSaved = false;
		fileSaved = ZephyrUtil.saveExportFile(this,exporter,  dataFilePath,  data, selectedTaxaOnly);
		if (!fileSaved) return null;

		setFileNames();

		TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		int firstOutgroup = MesquiteInteger.unassigned;
		if (outgroupSet!=null)
			firstOutgroup = outgroupSet.firstBitOn();
		formCommandFile(dataFileName, firstOutgroup);
		logln("\n\nCommands given to TNT:");
		logln(commands);
		logln("");

		String arguments = "";
		if (MesquiteTrunk.isWindows())
			arguments += " proc " + commandsFileName;
		else
			arguments += " proc " + commandsFileName;
		
		String programCommand = externalProcRunner.getExecutableCommand()+arguments;
		programCommand += StringUtil.lineEnding();  


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
		fileNames[1] = commandsFileName;


		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, fileContents, fileNames,  ownerModule.getName());

		if (success){
			getProject().decrementProjectWindowSuppression();
			return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
		}
		getProject().decrementProjectWindowSuppression();
		data.setEditorInhibition(false);
		return null;
	}	


	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		logln("Preparing to receive RAxML trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		//TODO		finalScore.setValue(finalValue);

		getProject().incrementProjectWindowSuppression();
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed. 
		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		String treeFilePath = outputFilePaths[OUT_TREEFILE];

		runFilesAvailable();

		// read in the tree files
		success = false;
		Tree t= null;

		MesquiteBoolean readSuccess = new MesquiteBoolean(false);
		//TreeVector tv = new TreeVector(taxa);
		if (bootstrap())
			t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNTBootstrap Rep", 0, readSuccess, false);  // set first tree number as 0 as will remove the first one later.
		else
			t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNTTree", 1, readSuccess, false);
		success = t!=null;
		if (success && bootstrap()) {
			t = t.cloneTree();
			treeList.removeElementAt(0, false);  // get rid of first one as this is the bootstrap tree
		}

		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success)
			logln("Execution of TNT unsuccessful [2]");

		getProject().decrementProjectWindowSuppression();
		if (data!=null)
			data.setEditorInhibition(false);
		//	manager.deleteElement(tv);  // get rid of temporary tree block
		if (success) 
			return t;
		return null;
	}



	public String getProgramName() {
		return "TNT";
	}

	/*.................................................................................................................*
	String getProgramCommand(String arguments){
		String command = "";
		if (MesquiteTrunk.isWindows())
			command += StringUtil.protectForWindows(TNTPath)+ arguments;
		else
			command += StringUtil.protectForUnix(TNTPath )+ arguments;
		return command;
	}
	/*.................................................................................................................*/

	//	Parser parser = new Parser();
	//	long screenFilePos = 0;
	//	MesquiteFile screenFile = null;

	/*.................................................................................................................*
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return outputFilePaths;
	}
	/*.................................................................................................................*
	public String getOutputFileToReadPath(String originalPath) {
		//File file = new File(originalPath);
		//File fileCopy = new File(originalPath + "2");
		String newPath = originalPath + "2";
		MesquiteFile.copyFileFromPaths(originalPath, newPath, false);
		//		if (file.renameTo(fileCopy))
		if (MesquiteFile.fileExists(newPath)) {
			return newPath;
		}
		return originalPath;
	}
	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted()) || logFileNames==null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];



		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0]) && !bootstrap()) {   // tree file
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				ownerModule.newTreeAvailable(treeFilePath, outgroupSet);

			}
			else ownerModule.newTreeAvailable(treeFilePath, null);
		} 	
		else	if (fileNum==1 && outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1]) && !bootstrap()) {   // log file
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator!=null) {
						parser.setString(s);
						String rep = parser.getFirstToken(); // generation number
						logln("");
						if (MesquiteInteger.isNumber(rep)) {
							int numReps = MesquiteInteger.fromString(rep)+1;
							progIndicator.setText("Replicate: " + numReps);// + ", ln L = " + parser.getNextToken());
							if (bootstrap()) {
								logln("Replicate " + numReps + " of " + bootstrapreps);
							}
							logln("Replicate " + numReps + " of " + totalNumHits);

							progIndicator.spin();		
							double timePerRep=0;
							if (MesquiteInteger.isCombinable(numReps) && numReps>0){
								timePerRep = timer.timeSinceVeryStartInSeconds()/numReps;   //this is time per rep
							}
							int timeLeft = 0;
							if (bootstrap()) {
								timeLeft = (int)((bootstrapreps- numReps) * timePerRep);
							}
							else {
								String token = parser.getNextToken();  //algorithm
								token = parser.getNextToken();  //Tree
								token = parser.getNextToken();  //Score
								String best = parser.getNextToken();  //Best
								logln("  Score " +  token + "; best found so far " + best);
								timeLeft = (int)((totalNumHits- numReps) * timePerRep);
							}

							logln("  Running time so far " +  StringUtil.secondsToHHMMSS((int)timer.timeSinceVeryStartInSeconds())  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
						}
					}
				count++;
			} else
				Debugg.println("*** File does not exist (" + filePath + ") ***");
		}




	}
	/*.................................................................................................................*/

	public void processCompletedOutputFiles(String[] outputFilePaths) {
		if (outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1])) {
			ZephyrUtil.copyLogFile(this, "TNT", outputFilePaths[1]);

		}
		if (outputFilePaths.length>2 && !StringUtil.blank(outputFilePaths[2])) {
			ZephyrUtil.copyOutputText(this,outputFilePaths[2], commands);
		}
	}

	public boolean continueShellProcess(Process proc){
		if (progIndicator.isAborted()) {
			try {
				Writer stream;
				stream = new OutputStreamWriter((BufferedOutputStream)proc.getOutputStream());
				stream.write((char)3);
				stream.flush();
				stream.close();
			}
			catch (IOException e) {
			}
			return false;
		}
		return true;
	}


	public boolean bootstrap() {
		return bootstrapreps>0;
	}
	public int getBootstrapreps() {
		return bootstrapreps;
	}

	public void setBootstrapreps(int bootstrapreps) {
		this.bootstrapreps = bootstrapreps;
	}

	/*.................................................................................................................*
	public void setTNTPath(String TNTPath){
		this.TNTPath = TNTPath;
	}
	/*.................................................................................................................*/

	public Class getDutyClass() {
		return TNTRunner.class;
	}

	public String getParameters() {
		return "TNT commands: " + commands;
	}

	public String getName() {
		return "TNT Runner";
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}




}
