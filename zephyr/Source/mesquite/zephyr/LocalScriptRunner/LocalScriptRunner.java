package mesquite.zephyr.LocalScriptRunner;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class LocalScriptRunner extends ExternalProcessRunner implements ActionListener, OutputFileProcessor, ShellScriptWatcher {
	ShellScriptRunner scriptRunner;
	Random rng;
	String rootDir;
	String executablePath;
	StringBuffer extraPreferences;
	ExternalProcessRequester processRequester;

	/*.================================================================..*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		MesquiteModule mm = getEmployer();
		rng = new Random(System.currentTimeMillis());
		extraPreferences = new StringBuffer();
		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}
	
	public String getName() {
		return "Local Script Runner";
	}
	public String getExplanation() {
		return "Runs local scripts.";
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

	public  boolean isWindows() {
		return MesquiteTrunk.isWindows();
	}
	public  boolean isLinux() {
		return MesquiteTrunk.isLinux();
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String flavor, String content) {
		if (StringUtil.notEmpty(flavor) && "executablePath".equalsIgnoreCase(tag)){   // it is one with the flavor attribute
			if (flavor.equalsIgnoreCase(getExecutableName()))   /// check to see if flavor is correct!!!
				executablePath = StringUtil.cleanXMLEscapeCharacters(content);
			else {
				String path = StringUtil.cleanXMLEscapeCharacters(content);
				StringUtil.appendXMLTag(extraPreferences, 2, "executablePath", flavor, path);  		// store for next time
			}
		}
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "executablePath", getExecutableName(), executablePath);  
		buffer.append(extraPreferences);
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (scriptRunner != null){
			temp.addLine("reviveScriptRunner ");
			temp.addLine("tell It");
			temp.incorporate(scriptRunner.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		if (rootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(rootDir));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveScriptRunner")) {
			Debugg.println("ReviveScriptRunner");
			scriptRunner = new ShellScriptRunner();
			scriptRunner.setOutputProcessor(this);
			scriptRunner.setWatcher(this);
			return scriptRunner;
		}
		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			rootDir = parser.getFirstToken(arguments);
		}
		return null;
	}	

	// setting the requester, to whom this runner will communicate about the run
	public  void setProcessRequester(ExternalProcessRequester processRequester){
		setExecutableName(processRequester.getExecutableName());
		this.processRequester = processRequester;
		loadPreferences();
		processRequester.intializeAfterExternalProcessRunnerHired();
	}

	SingleLineTextField executablePathField =  null;

	// given the opportunity to fill in options for user
	public  void addItemsToDialogPanel(ExtensibleDialog dialog){
		executablePathField = dialog.addTextField("Path to "+ getExecutableName()+":", executablePath, 40);
		Button browseButton = dialog.addAListenedButton("Browse...",null, this);
		browseButton.setActionCommand("browse");

	}
	public boolean optionsChosen(){
		executablePath = executablePathField.getText();
		return true;
	}

	/*.................................................................................................................*/
	public String getExecutableCommand(){
		if (MesquiteTrunk.isWindows())
			return StringUtil.protectForWindows(executablePath);
		else
			return StringUtil.protectForUnix(executablePath);
	}
	
	String runningFilePath = "";
	String scriptPath = "";
	String[] outputFilePaths;
	String[] outputFileNames;

	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setInputFiles(String script, String[] fileContents, String[] fileNames){  //assumes for now that all input files are in the same directory
		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		rootDir = ZephyrUtil.createDirectoryForFiles(this, ZephyrUtil.BESIDE_HOME_FILE, getExecutableName());
		if (rootDir==null)
			return false;
		
		for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
			if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
				MesquiteFile.putFileContents(rootDir+fileNames[i], fileContents[i], true);
			}
		}

		runningFilePath = rootDir + "running" + MesquiteFile.massageStringToFilePathSafe(unique);
		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir)+ StringUtil.lineEnding());
		shellScript.append(script);
		shellScript.append(ShellScriptUtil.getRemoveCommand(runningFilePath));

		scriptPath = rootDir + "Script" + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), true);
		return true;
	}

	/*.................................................................................................................*/
	public void setOutputFileNamesToWatch(String[] fileNames){
		if (fileNames!=null) {
			outputFileNames = new String[fileNames.length];
			outputFilePaths = new String[fileNames.length];
			for (int i=0; i<fileNames.length; i++){
				outputFilePaths[i]=rootDir+fileNames[i];
				outputFileNames[i]=fileNames[i];
			}
		}
	}

	/*.................................................................................................................*/
	public String getOutputFilePath(String fileName){
		int fileNumber = StringArray.indexOf(outputFileNames, fileName);
		if (fileNumber>=0 && fileNumber<outputFileNames.length)
			return outputFilePaths[fileNumber];
		return null;
	}
	/*.................................................................................................................*/
	public String[] getOutputFilePaths(){
		return outputFilePaths;
	}
	
	/*.................................................................................................................*/
	public String getLastLineOfOutputFile(String fileName){
		String contents = null;
		int fileNumber = StringArray.indexOf(outputFileNames, fileName);
		if (fileNumber>=0 && fileNumber<outputFileNames.length)
			contents = MesquiteFile.getFileLastContents(outputFileNames[fileNumber]);
		return contents;
	}

	/*.................................................................................................................*/
	// starting the run
	public boolean startExecution(){  //do we assume these are disconnectable?
		scriptRunner = new ShellScriptRunner(scriptPath, runningFilePath, null, false, getExecutableName(), outputFilePaths, this, this, true);  //scriptPath, runningFilePath, null, true, name, outputFilePaths, outputFileProcessor, watcher, true
		return scriptRunner.executeInShell();
	}

	public boolean monitorExecution(){
		 if (scriptRunner!=null)
			 return scriptRunner.monitorAndCleanUpShell();
		 return false;
	}

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		scriptRunner = null;
		return false;
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("browse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String path = MesquiteFile.openFileDialog("Choose "+getExecutableName(), directoryName, fileName);
			if (StringUtil.notEmpty(path))
				executablePathField.setText(path);
		}
	}
	/*.................................................................................................................*/
	public void processOutputFile(String[] outputFilePaths, int fileNum) {
		boolean filesAvailable[] = new boolean[outputFilePaths.length];
		for (int i=0; i<outputFilePaths.length; i++)
			filesAvailable[i]=false;
		filesAvailable[fileNum]=true;
		processRequester.runFilesAvailable(filesAvailable);
	}
	/*.................................................................................................................*/
	public void processCompletedOutputFiles(String[] outputFilePaths) {
		boolean filesAvailable[] = new boolean[outputFilePaths.length];
		for (int i=0; i<outputFilePaths.length; i++)
			filesAvailable[i]=true;
		processRequester.runFilesAvailable(filesAvailable);
	}
	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return outputFilePaths;
	}
	public boolean continueShellProcess(Process proc) {
		return true;
	}
	
	
}
