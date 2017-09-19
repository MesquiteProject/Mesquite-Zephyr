/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.LocalScriptRunner;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Random;

import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class LocalScriptRunner extends ExternalProcessRunner implements ActionListener, ItemListener, OutputFileProcessor, ShellScriptWatcher {
	ExternalProcessManager externalRunner;
	ShellScriptRunner scriptRunner;

	public boolean scriptBased = false;
	public boolean addExitCommand = true;
	Random rng;
	String rootDir = null;
	String executablePath;
	String arguments;

	String stdOutFileName;
	String runningFilePath = "";
	String scriptPath = "";
	String[] outputFilePaths;
	String[] outputFileNames;

	StringBuffer extraPreferences;
	ExternalProcessRequester processRequester;
	boolean visibleTerminal = false;
	boolean deleteAnalysisDirectory = false;
	boolean leaveAnalysisDirectoryIntact = false;

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

	public String getMessageIfUserAbortRequested () {
		if (scriptBased)
			return "Mesquite will stop its monitoring of the analysis, but it will not be able to directly stop the other program.  To stop the other program, you will need to "
					+ "use either the Task Manager (Windows) or the Activity Monitor (MacOS) or the equivalent to stop the other process.";
		return "";
	}
	public String getMessageIfCloseFileRequested () { 
		if (scriptBased)
			return "If Mesquite closes this file, it will not directly stop the other program.  To stop the other program, you will need to "
					+ "use either the Task Manager (Windows) or the Activity Monitor (MacOS) or the equivalent to stop the other process.";
		return "";
	}

	public String getName() {
		return "Local Script Runner";
	}
	public String getExplanation() {
		return "Runs local scripts.";
	}
	
	public  void setOutputTextListener(OutputTextListener textListener){
		if (scriptRunner!=null)
			scriptRunner.setOutputTextListener(textListener);
		if (externalRunner!=null)
			externalRunner.setOutputTextListener(textListener);
	}

	public  String getProgramLocation(){
		return "local computer";
	}
	public boolean isReconnectable(){
		return scriptBased;
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

	public  boolean isWindows() {
		return MesquiteTrunk.isWindows();
	}
	public  boolean isLinux() {
		return MesquiteTrunk.isLinux();
	}

	/*.................................................................................................................*/
	public String getStdErr() {
		if (scriptBased){
			if (scriptRunner!=null)
				return scriptRunner.getStdErr();
		}
		else if (externalRunner!=null)
			return externalRunner.getStdErr();
		return "";
	}
	/*.................................................................................................................*/
	public String getStdOut() {
		if (scriptBased){
			if (scriptRunner!=null)
				return scriptRunner.getStdOut();
		}
		else if (externalRunner!=null)
			return externalRunner.getStdOut();
		return "";
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
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("visibleTerminal".equalsIgnoreCase(tag) && visibleTerminalOptionAllowed())
			visibleTerminal = MesquiteBoolean.fromTrueFalseString(content);
		if ("scriptBased".equalsIgnoreCase(tag))
			scriptBased = MesquiteBoolean.fromTrueFalseString(content);
		if ("addExitCommand".equalsIgnoreCase(tag))
			addExitCommand = MesquiteBoolean.fromTrueFalseString(content);
		if ("deleteAnalysisDirectory".equalsIgnoreCase(tag))
			deleteAnalysisDirectory = MesquiteBoolean.fromTrueFalseString(content);
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "executablePath", getExecutableName(), executablePath);  
		if (visibleTerminalOptionAllowed())
			StringUtil.appendXMLTag(buffer, 2, "visibleTerminal", visibleTerminal);  
		StringUtil.appendXMLTag(buffer, 2, "deleteAnalysisDirectory", deleteAnalysisDirectory);  
		StringUtil.appendXMLTag(buffer, 2, "scriptBased", scriptBased);  
		StringUtil.appendXMLTag(buffer, 2, "addExitCommand", addExitCommand);  
		buffer.append(extraPreferences);
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public void resetLastModified(int i){
		if (scriptBased){
			if (scriptRunner!=null)
				scriptRunner.resetLastModified(i);
		}
		else if (externalRunner!=null)
			externalRunner.resetLastModified(i);
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (visibleTerminalOptionAllowed())
			temp.addLine("visibleTerminal "+MesquiteBoolean.toTrueFalseString(visibleTerminal));
		temp.addLine("deleteAnalysisDirectory "+MesquiteBoolean.toTrueFalseString(deleteAnalysisDirectory));
		temp.addLine("scriptBased "+MesquiteBoolean.toTrueFalseString(scriptBased));
		if (scriptBased) {
			if (scriptRunner != null){
				temp.addLine("reviveScriptRunner ");
				temp.addLine("tell It");
				temp.incorporate(scriptRunner.getSnapshot(file), true);
				temp.addLine("endTell");
				//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
			}
		} else if (externalRunner != null){
			temp.addLine("reviveExternalRunner ");
			temp.addLine("tell It");
			temp.incorporate(externalRunner.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		if (rootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(rootDir));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the externalRunner", "[file path]", commandName, "reviveExternalRunner")) {
			logln("Reviving ExternalProcessRunner");
			externalRunner = new ExternalProcessManager(this);
			externalRunner.setOutputProcessor(this);
			externalRunner.setWatcher(this);
			if (visibleTerminalOptionAllowed())
				externalRunner.setVisibleTerminal(visibleTerminal);
			return externalRunner;
		}
		else if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveScriptRunner")) {
			logln("Reviving ShellScriptRunner");
			scriptRunner = new ShellScriptRunner();
			scriptRunner.setOutputProcessor(this);
			scriptRunner.setWatcher(this);
			scriptRunner.pleaseReconnectToExternalProcess();
			if (visibleTerminalOptionAllowed())
				scriptRunner.setVisibleTerminal(visibleTerminal);
			return scriptRunner;
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the Terminal window should be visible on a Mac.", "[true; false]", commandName, "visibleTerminal")) {
			if (visibleTerminalOptionAllowed()){
				visibleTerminal = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
				if (scriptBased) {
					if (scriptRunner != null)
						scriptRunner.setVisibleTerminal(visibleTerminal);
				} else if (externalRunner != null)
					externalRunner.setVisibleTerminal(visibleTerminal);
			}
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the analysis is done via a shell script.", "[true; false]", commandName, "scriptBased")) {
			scriptBased = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the analysis folder should be deleted at the end of the run.", "[true; false]", commandName, "deleteAnalysisDirectory")) {
			deleteAnalysisDirectory = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
		}
		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			rootDir = parser.getFirstToken(arguments);
		}
		return null;
	}	

	// setting the requester, to whom this runner will communicate about the run
	public  void setProcessRequester(ExternalProcessRequester processRequester){
		setExecutableName(processRequester.getProgramName());
		setRootNameForDirectory(processRequester.getRootNameForDirectory());
		this.processRequester = processRequester;
		loadPreferences();
		processRequester.intializeAfterExternalProcessRunnerHired();
	}


	SingleLineTextField executablePathField =  null;
	Checkbox visibleTerminalCheckBox =  null;
	Checkbox deleteAnalysisDirectoryCheckBox =  null;
	Checkbox scriptBasedCheckBox =  null;
	Checkbox addExitCommandCheckBox = null;

	// given the opportunity to fill in options for user
	public  void addItemsToDialogPanel(ExtensibleDialog dialog){
		executablePathField = dialog.addTextField("Path to "+ getExecutableName()+":", executablePath, 40);
		Button browseButton = dialog.addAListenedButton("Browse...",null, this);
		browseButton.setActionCommand("browse");
		if (getDirectProcessConnectionAllowed()) {
			scriptBasedCheckBox = dialog.addCheckBox("Script-based analysis (allows reconnection, but can't be stopped easily)", scriptBased);
			scriptBasedCheckBox.addItemListener(this);
		} else
			scriptBased=true;
		if (visibleTerminalOptionAllowed()) {
			visibleTerminalCheckBox = dialog.addCheckBox("Terminal window visible (this will decrease error-reporting ability)", visibleTerminal);
			visibleTerminalCheckBox.setEnabled(scriptBased);	
		}
		if (ShellScriptUtil.exitCommandIsAvailable()) {
			addExitCommandCheckBox = dialog.addCheckBox("ask terminal window to exit after completion", addExitCommand);
			addExitCommandCheckBox.setEnabled(scriptBased);	
		} 
		deleteAnalysisDirectoryCheckBox = dialog.addCheckBox("Delete analysis directory after completion", deleteAnalysisDirectory);

	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getItemSelectable()==scriptBasedCheckBox  && scriptBasedCheckBox!=null){
			if (visibleTerminalCheckBox!=null)
				visibleTerminalCheckBox.setEnabled(scriptBasedCheckBox.getState());	
			if (addExitCommandCheckBox!=null)
				addExitCommandCheckBox.setEnabled(scriptBasedCheckBox.getState());	
		}
	}


	public boolean optionsChosen(){
		String tempPath = executablePathField.getText();
		if (StringUtil.blank(tempPath)){
			MesquiteMessage.discreetNotifyUser("The path to " +getExecutableName()+ " must be entered.");
			return false;
		}
		executablePath = tempPath;

		if (visibleTerminalCheckBox!=null)
			visibleTerminal = visibleTerminalCheckBox.getState();
		else if (processRequester.localMacRunsRequireTerminalWindow())
			visibleTerminal=true;
		if (deleteAnalysisDirectoryCheckBox!=null)
			deleteAnalysisDirectory = deleteAnalysisDirectoryCheckBox.getState();
		if (addExitCommandCheckBox!=null)
			addExitCommand = addExitCommandCheckBox.getState();
		if (!getDirectProcessConnectionAllowed())
			scriptBased=true;
		else if (scriptBasedCheckBox!=null)
			scriptBased = scriptBasedCheckBox.getState();
		return true;
	}
	public boolean visibleTerminalOptionAllowed(){
		return MesquiteTrunk.isMacOSX() && !processRequester.localMacRunsRequireTerminalWindow();
	}
	public boolean getDirectProcessConnectionAllowed(){
		return processRequester.getDirectProcessConnectionAllowed() && MesquiteTrunk.isJavaGreaterThanOrEqualTo(1.7);
	}

	public boolean requiresLinuxTerminalCommands(){
		return processRequester.requiresLinuxTerminalCommands();
	}
	
	/** Following section on how to invoke a linux terminal and have it not be asynchronous comes from
	 * https://askubuntu.com/questions/627019/blocking-start-of-terminal, courtesy of users Byte Commander and terdon.
	 * */
	
	String linuxTerminalCommand = "gnome-terminal -x bash -c \"echo \\$$>$pidfile; ";

	public String getLinuxTerminalCommand() {
		return linuxTerminalCommand;
	}
	public void setLinuxTerminalCommand(String linuxTerminalCommand) {
		this.linuxTerminalCommand = linuxTerminalCommand;
	}
	
	public String getLinuxBashScriptPreCommand () {
		  return "delay=0.1\n" + 
		  		"pidfile=$(mktemp)\n";
		}
	public String getLinuxBashScriptPostCommand () {
		  return "until [ -s $pidfile ] \n" + 
		  		"    do sleep $delay\n" + 
		  		"done\n" + 
		  		"terminalpid=$(cat \"$pidfile\")\n" + 
		  		"rm $pidfile\n" + 
		  		"while ps -p $terminalpid > /dev/null 2>&1\n" + 
		  		"    do sleep $delay\n" + 
		  		"done\n";
		}
	/*.................................................................................................................*/
	public String getExecutableCommand(){
		if (MesquiteTrunk.isWindows())
			return "call " + StringUtil.protectFilePathForWindows(executablePath);
		else if (MesquiteTrunk.isLinux()) {
			if (requiresLinuxTerminalCommands())
				return getLinuxTerminalCommand() + " " + StringUtil.protectFilePathForUnix(executablePath);
			else 
				return " \"" + executablePath+"\"";
		}
		else
			return StringUtil.protectFilePathForUnix(executablePath);
	}

	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return rootDir;
	}

	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String[] fileContents, String[] fileNames){  //assumes for now that all input files are in the same directory
		//String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		if (rootDir==null) 
			rootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getRootNameForDirectory(), "-Run.");
		if (rootDir==null)
			return false;

		for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
			if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
				MesquiteFile.putFileContents(rootDir+fileNames[i], fileContents[i], true);
			}
		}
		String args = null;
		if (arguments instanceof MesquiteString)
			args = ((MesquiteString)arguments).getValue();
		else if (arguments instanceof String)
			args = (String)arguments;
		this.arguments = args;

		if (scriptBased) {
			runningFilePath = rootDir + "running";//+ MesquiteFile.massageStringToFilePathSafe(unique);
			StringBuffer shellScript = new StringBuffer(1000);
			shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir)+ StringUtil.lineEnding());
			if (StringUtil.notEmpty(additionalShellScriptCommands))
				shellScript.append(additionalShellScriptCommands + StringUtil.lineEnding());
			// 30 June 2017: added redirect of stderr
			//		shellScript.append(programCommand + " " + args+ " 2> " + ShellScriptRunner.stErrorFileName +  StringUtil.lineEnding());
			String suffix = "";
			if (MesquiteTrunk.isLinux()&&requiresLinuxTerminalCommands()) {
				shellScript.append(getLinuxBashScriptPreCommand());
				suffix="\"";
			}
			if (!processRequester.allowStdErrRedirect())
				shellScript.append(programCommand + " " + args + suffix+StringUtil.lineEnding());
			else {
				if (visibleTerminal) {
					shellScript.append(programCommand + " " + args+ " >/dev/tty   2> " + ShellScriptRunner.stErrorFileName +  suffix+StringUtil.lineEnding());
				}
				else
					shellScript.append(programCommand + " " + args+ " > " + ShellScriptRunner.stOutFileName+ " 2> " + ShellScriptRunner.stErrorFileName + suffix+ StringUtil.lineEnding());
			}
			if (MesquiteTrunk.isLinux()&&requiresLinuxTerminalCommands())
				shellScript.append(getLinuxBashScriptPostCommand());
			shellScript.append(ShellScriptUtil.getRemoveCommand(runningFilePath));
			if (scriptBased&&addExitCommand && ShellScriptUtil.exitCommandIsAvailable())
				shellScript.append("\n" + ShellScriptUtil.getExitCommand() + "\n");

			scriptPath = rootDir + "Script.bat";// + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
			MesquiteFile.putFileContents(scriptPath, shellScript.toString(), false);
		}
		return true;
	}
	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setPreflightInputFiles(String script){  //assumes for now that all input files are in the same directory
		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		rootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getRootNameForDirectory(), "-Run.");
		if (rootDir==null)
			return false;

		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir)+ StringUtil.lineEnding());
		shellScript.append(script);

		scriptPath = rootDir + "preflight.bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), false);
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
	public void setOutputFileNameToWatch(int index, String fileName){
		if (outputFileNames!=null && index>=0 && index < outputFileNames.length) {
			outputFilePaths[index]=rootDir+fileName;
			outputFileNames[index]=fileName;
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

	public String getStdOutFileName() {
		return stdOutFileName;
	}
	public void setStdOutFileName(String stdOutFileName) {
		this.stdOutFileName = stdOutFileName;
	}


	/*.................................................................................................................*/
	// starting the run
	public boolean startExecution(){  //do we assume these are disconnectable?
		if (scriptBased) {
			scriptRunner = new ShellScriptRunner(scriptPath, runningFilePath, null, false, getExecutableName(), outputFilePaths, this, this, visibleTerminal);  //scriptPath, runningFilePath, null, true, name, outputFilePaths, outputFileProcessor, watcher, true
			return scriptRunner.executeInShell();
		} else {
			externalRunner = new ExternalProcessManager(this, rootDir, executablePath, arguments, getExecutableName(), outputFilePaths, this, this, false);
			return externalRunner.executeInShell();
		}
	}



	public boolean monitorExecution(ProgressIndicator progIndicator){
		if (scriptBased) {
			if (scriptRunner!=null) {
				boolean success = scriptRunner.monitorAndCleanUpShell(progIndicator);
				if (progIndicator!=null && progIndicator.isAborted())
					processRequester.setUserAborted(true);
				return success;
			}
		} else {
			if (externalRunner!=null) {
				boolean success = externalRunner.monitorAndCleanUpShell(progIndicator);
				if (externalRunner.exitCodeIsBad())  // if bad exit code, then don't autodelete the directory
					leaveAnalysisDirectoryIntact=true;
				if (progIndicator!=null && progIndicator.isAborted())
					processRequester.setUserAborted(true);
				return success;
			}
		}
		return false;
	}

	/*.................................................................................................................*/

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		if (scriptBased) {
			if (scriptRunner!=null)
				scriptRunner.stopExecution();
		} else if (externalRunner!=null)
			externalRunner.stopExecution();
		//scriptRunner = null;
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
	public String getPreflightFile(String preflightLogFileName){
		String filePath = rootDir + preflightLogFileName;
		String fileContents = MesquiteFile.getFileContentsAsString(filePath);
		return fileContents;
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
		if (outputFilePaths==null)
			return;
		boolean filesAvailable[] = new boolean[outputFilePaths.length];
		for (int i=0; i<outputFilePaths.length; i++)
			filesAvailable[i]=true;
		processRequester.runFilesAvailable(filesAvailable);
	}
	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return processRequester.modifyOutputPaths(outputFilePaths);
	}
	/*.................................................................................................................*/
	public void finalCleanup() {
		if (deleteAnalysisDirectory && !leaveAnalysisDirectoryIntact)
			MesquiteFile.deleteDirectory(rootDir);
		rootDir=null;
	}

	public boolean continueShellProcess(Process proc) {
		if (processRequester.errorsAreFatal()) { 
			String stdErr = getStdErr();
			if (StringUtil.notEmpty(stdErr))
				return false;
		}
		return true;
	}
	public boolean fatalErrorDetected() {
		if (processRequester.errorsAreFatal()) { 
			String stdErr = getStdErr();
			if (StringUtil.notEmpty(stdErr))
				return true;
		}
		return false;
	}


}
