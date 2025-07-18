/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.LocalScriptRunner;

import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;

import mesquite.externalCommunication.lib.AppChooser;
import mesquite.externalCommunication.lib.AppInformationFile;
import mesquite.lib.CommandChecker;
import mesquite.lib.ExternalProcessManager;
import mesquite.lib.MesquiteBoolean;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteFileUtil;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.OutputFileProcessor;
import mesquite.lib.OutputTextListener;
import mesquite.lib.ParseUtil;
import mesquite.lib.ProcessWatcher;
import mesquite.lib.ShellScriptRunner;
import mesquite.lib.ShellScriptUtil;
import mesquite.lib.Snapshot;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.ProgressIndicator;
import mesquite.lib.ui.RadioButtons;
import mesquite.lib.ui.SingleLineTextField;
import mesquite.zephyr.lib.ExternalProcessRequester;
import mesquite.zephyr.lib.ScriptRunner;

public class LocalScriptRunner extends ScriptRunner implements ActionListener, ItemListener, OutputFileProcessor, ProcessWatcher {

	ExternalProcessManager externalProcessManager;
	ShellScriptRunner scriptRunner;
	AppInformationFile appInfoFile;

	Random rng;
	private String executablePath;
	String arguments;
	boolean useDefaultExecutablePath=true;

	String stdOutFileName;
	String scriptPath = "";
	String[] outputFilePaths;
	String[] outputFileNames;

	StringBuffer extraPreferences;
	boolean deleteAnalysisDirectory = false;

	/*.================================================================..*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		MesquiteModule mm = getEmployer();
		rng = new Random(System.currentTimeMillis());
		extraPreferences = new StringBuffer();
		setReadyForReconnectionSave(false);
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
	
	public  void setOutputTextListener(OutputTextListener textListener){
		if (scriptRunner!=null)
			scriptRunner.setOutputTextListener(textListener);
		if (externalProcessManager!=null)
			externalProcessManager.setOutputTextListener(textListener);
	}

	public static String getDefaultProgramLocation() {
		return "Local Computer";
	}

	public  String getProgramLocation(){
		return "local computer";
	}
	public boolean isReconnectable(){
		return scriptBased;
	}
	/*.................................................................................................................*/
	public String getDefaultExecutablePath(){
		if (appInfoFile==null) {
			appInfoFile = getExternalProcessRequester().getAppInfoFile();
		}
		if (appInfoFile!=null) {
			String fullPath = appInfoFile.getFullPath();
			return fullPath;
		}
		return null;
	}
	/*.................................................................................................................*/
	public String getVersionFromAppInfo(){
		if (!useDefaultExecutablePath || !getBuiltInExecutableAllowed()) 
			return null;
		if (appInfoFile==null) {
			appInfoFile = getExternalProcessRequester().getAppInfoFile();
		}
		if (appInfoFile!=null) {
			return appInfoFile.getVersion();
		}
		return null;
	}
	/*.................................................................................................................*/
	public String getOtherPropertiesFromAppInfo(){
		if (!useDefaultExecutablePath || !getBuiltInExecutableAllowed()) 
			return null;
		if (appInfoFile==null) {
			appInfoFile = getExternalProcessRequester().getAppInfoFile();
		}
		if (appInfoFile!=null) {
			return appInfoFile.getOtherProperties();
		}
		return null;
	}
	/*.................................................................................................................*/
	public String getAppOtherProperties() {
		return getOtherPropertiesFromAppInfo();
	}

	/*.................................................................................................................*/
	public String getAppInfoForLog(){
		if (appInfoFile==null) {
			appInfoFile = getExternalProcessRequester().getAppInfoFile();
		}
		if (appInfoFile!=null) {
			StringBuffer sb = new StringBuffer(0);
			sb.append("\nVersion " + appInfoFile.getVersion());
			return sb.toString();
		}
		return null;
	}
	/*.................................................................................................................*/
	public String getExecutablePath(){
		if (useDefaultExecutablePath && getBuiltInExecutableAllowed()) 
			return getDefaultExecutablePath();
		else
			return executablePath;
	}

	public boolean useAppInAppFolder() {
		return useDefaultExecutablePath && getBuiltInExecutableAllowed();
	}
	
	public void appChooserDialogBoxEntryChanged() {
		if (processRequester!=null)
			processRequester.appChooserDialogBoxEntryChanged();
	}

	public AppChooser getAppChooser() {
		return appChooser;
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
	public  boolean isWindows() {
		return MesquiteTrunk.isWindows();
	}
	public  boolean isLinux() {
		return MesquiteTrunk.isLinux();
	}
	public  boolean isMacOSX() {
		return MesquiteTrunk.isMacOSX();
	}

	/*.................................................................................................................*/
	public String getStdErr() {
		if (scriptBased){
			if (scriptRunner!=null)
				return scriptRunner.getStdErr();
		}
		else if (externalProcessManager!=null)
			return externalProcessManager.getStdErr();
		return "";
	}
	/*.................................................................................................................*/
	public String getStdOut() {
		if (scriptBased){
			if (scriptRunner!=null)
				return scriptRunner.getStdOut();
		}
		else if (externalProcessManager!=null)
			return externalProcessManager.getStdOut();
		return "";
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String flavor, String content) {    // for preferences that are program-specific
		if (StringUtil.notEmpty(flavor)) {// it is one with the flavor attribute; in this case this is the executable
			if ("executablePath".equalsIgnoreCase(tag)){   
				if (flavor.equalsIgnoreCase(getExecutableName()))   /// check to see if flavor is correct!!!
					executablePath = StringUtil.cleanXMLEscapeCharacters(content);
				else {
					String path = StringUtil.cleanXMLEscapeCharacters(content);
					StringUtil.appendXMLTag(extraPreferences, 2, "executablePath", flavor, path);  		// store for next time
				}
			}
			if ("useDefaultExecutablePath".equalsIgnoreCase(tag)){   
				String s = getExecutableName();
				if (flavor.equalsIgnoreCase(getExecutableName())) {   /// check to see if flavor is correct!!!
					boolean temp = MesquiteBoolean.fromTrueFalseString(content);
					//if (getDefaultExecutablePathAllowed())
						useDefaultExecutablePath = temp;
				} else {
					boolean use = MesquiteBoolean.fromTrueFalseString(content);
					StringUtil.appendXMLTag(extraPreferences, 2, "useDefaultExecutablePath", flavor, use);  		// store for next time
				}
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
		//if (getDefaultExecutablePathAllowed())
		StringUtil.appendXMLTag(buffer, 2, "useDefaultExecutablePath", getExecutableName(), useDefaultExecutablePath);  
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
		else if (externalProcessManager!=null)
			externalProcessManager.resetLastModified(i);
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
		} else if (externalProcessManager != null){
			temp.addLine("reviveExternalRunner ");
			temp.addLine("tell It");
			temp.incorporate(externalProcessManager.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		if (localRootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(localRootDir));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the externalRunner", "[file path]", commandName, "reviveExternalRunner")) {
			logln("Reviving ExternalProcessRunner");
			externalProcessManager = new ExternalProcessManager(this);
			externalProcessManager.setOutputProcessor(this);
			externalProcessManager.setWatcher(this);
			if (visibleTerminalOptionAllowed())
				externalProcessManager.setVisibleTerminal(visibleTerminal);
			return externalProcessManager;
		}
		else if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveScriptRunner")) {
			logln("Reviving ShellScriptRunner");
			scriptRunner = new ShellScriptRunner();
			scriptRunner.setOutputProcessor(this);
			scriptRunner.setWatcher(this);
			scriptRunner.pleaseReconnectToExternalProcess();
			reconnectionDetailsSaved = true;
			if (visibleTerminalOptionAllowed())
				scriptRunner.setVisibleTerminal(visibleTerminal);
			return scriptRunner;
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the Terminal window should be visible.", "[true; false]", commandName, "visibleTerminal")) {
			if (visibleTerminalOptionAllowed()){
				visibleTerminal = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
				if (scriptBased) {
					if (scriptRunner != null)
						scriptRunner.setVisibleTerminal(visibleTerminal);
				} else if (externalProcessManager != null)
					externalProcessManager.setVisibleTerminal(visibleTerminal);
			}
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the analysis is done via a shell script.", "[true; false]", commandName, "scriptBased")) {
			scriptBased = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
		}
		else  if (checker.compare(this.getClass(), "Sets whether or not the analysis folder should be deleted at the end of the run.", "[true; false]", commandName, "deleteAnalysisDirectory")) {
			deleteAnalysisDirectory = MesquiteBoolean.fromTrueFalseString(parser.getFirstToken(arguments));
		}

		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			localRootDir = parser.getFirstToken(arguments);
		}
		return null;
	}	

	// setting the requester, to whom this runner will communicate about the run
	public  void setProcessRequester(ExternalProcessRequester processRequester){
		setExecutableName(processRequester.getProgramName());
		setExecutableNumber(processRequester.getProgramNumber());
		setRootNameForDirectory(processRequester.getRootNameForDirectory());
		this.processRequester = processRequester;
		loadPreferences();
		processRequester.intializeAfterExternalProcessRunnerHired();
	}


	SingleLineTextField executablePathField =  null;
	Checkbox defaultExecutablePathCheckBox =  null;
	Checkbox visibleTerminalCheckBox =  null;
	Checkbox deleteAnalysisDirectoryCheckBox =  null;
//	Checkbox scriptBasedCheckBox =  null;
	RadioButtons scriptBasedRadioButtons =  null;
	Checkbox addExitCommandCheckBox = null;
	
	AppChooser appChooser;

	public String getHelpString() {
		String helpString = "<h3>Direct versus indirect communication</h3>With indirect communication, reconnection is enabled. The analysis can continue even after you close the file or quit Mesquite."
				+ " As long as you save the file before closing/quitting, you can later reopen it in Mesquite, and it will reconnect with the ongoing analysis."
				+ " However, if something goes wrong (an error by the user, or something crashes) then Mesquite will not be able to reconnect, "
				+ "and the analysis program won’t stop until it is finished OR Mesquite will not know that the program has finished. If it continues, and if you want to stop it, you can use your computer’s "
				+ " Task Manager (Windows) or Activity Manager (macOS) to force quit the analysis. "
				+ "(Note: With indirect communication, Mesquite communicates via scripts. Otherwise, it connects more directly via Processes.)";

		return helpString;
	}
	/*.................................................................................................................*/
	

	// given the opportunity to fill in options for user
	public  boolean addItemsToDialogPanel(ExtensibleDialog dialog){
		
		dialog.addHorizontalLine(1);
		appChooser = new AppChooser(processRequester.getModule(), processRequester.getAppUser(), useAppInAppFolder(), executablePath);
		appChooser.addToDialog(dialog);
		dialog.addAttachment(appChooser);
		dialog.addHorizontalLine(1);
	
		/*
		if (getBuiltInExecutableAllowed()) {
			defaultExecutablePathCheckBox = dialog.addCheckBox("Use built-in app path for "+ getExecutableName(), useDefaultExecutablePath);
			executablePathField = dialog.addTextField("Path to alternative version:", executablePath, 40);
		} else
			executablePathField = dialog.addTextField("Path to "+ getExecutableName()+":", executablePath, 40);
		Button browseButton = dialog.addAListenedButton("Browse...",null, this);
		browseButton.setActionCommand("browse");
		*/
		
		
		
		if (getDirectProcessConnectionAllowed()) {
			
			//ZQ
			String[] rbStrings = new String[] {"Direct communication. Safe, but analysis stops if file is closed; can't later reopen and reconnect.",
					"Indirect. Can reconnect, but if there's an error, Mesquite might not stop " + processRequester.getProgramName() + " or recover trees."};
			if (getMultipleMatrixMode())
				scriptBased=false;
			int current = 0;
			if (scriptBased)
				current = 1;
			scriptBasedRadioButtons = dialog.addRadioButtons(rbStrings, current);
			scriptBasedRadioButtons.addItemListener(this);
			scriptBasedRadioButtons.setEnabled(1, !getMultipleMatrixMode());
			dialog.addLabel("[See help button (?) below for details about these choices.]");
			dialog.addHorizontalLine(1);
			
	//		scriptBasedCheckBox = dialog.addCheckBox("Enable reconnection (see ? button below for details)", scriptBased);
	//		scriptBasedCheckBox.addItemListener(this);
		} else
			scriptBased=true;
		if (visibleTerminalOptionAllowed()) {
			visibleTerminalCheckBox = dialog.addCheckBox("Terminal window visible (this will decrease error-reporting ability)", visibleTerminal);
			visibleTerminalCheckBox.setEnabled(scriptBased);	
		}
		if (ShellScriptUtil.exitCommandIsAvailableAndUseful(isWindows())) {
			addExitCommandCheckBox = dialog.addCheckBox("ask terminal window to exit after completion", addExitCommand);
			addExitCommandCheckBox.setEnabled(scriptBased);	
		} 
		deleteAnalysisDirectoryCheckBox = dialog.addCheckBox("Delete analysis folder after completion", deleteAnalysisDirectory);
		deleteAnalysisDirectoryCheckBox.setEnabled(!getMultipleMatrixMode());

		return true;

	}
	
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getItemSelectable()==scriptBasedRadioButtons  && scriptBasedRadioButtons!=null){
			if (visibleTerminalCheckBox!=null)
				visibleTerminalCheckBox.setEnabled(scriptBasedRadioButtons.getValue()==1);	
			if (addExitCommandCheckBox!=null)
				addExitCommandCheckBox.setEnabled(scriptBasedRadioButtons.getValue()==1);	
		} else {
		}
/*
 * 		if (arg0.getItemSelectable()==scriptBasedCheckBox  && scriptBasedCheckBox!=null){
			if (visibleTerminalCheckBox!=null)
				visibleTerminalCheckBox.setEnabled(scriptBasedCheckBox.getState());	
			if (addExitCommandCheckBox!=null)
				addExitCommandCheckBox.setEnabled(scriptBasedCheckBox.getState());	
		} else {
		}
		*/
	}

	/*.................................................................................................................*/

	public boolean optionsChosen(){
		executablePath = appChooser.getManualPath(); //for preference writing
		useDefaultExecutablePath = appChooser.useBuiltInExecutable(); //for preference writing
		if (StringUtil.blank(executablePath) && !useDefaultExecutablePath) {
			MesquiteMessage.discreetNotifyUser("You must specify the path of " + processRequester.getProgramName() + " in order for Mesquite to be able to use it." );
			return false;
		}
		if (useDefaultExecutablePath && !appChooser.builtInAppAvailableForUse()) {
			MesquiteMessage.discreetNotifyUser("There is no built in version of " + processRequester.getProgramName() + " available for use."
					+ "  In the previous dialog box, press the \"App...\" button and specify the path to a copy of the program." );
			return false;
		}
				
	//	builtInVersion = appChooser.getVersion(); //for informing user; only if built-in

		/*		if (defaultExecutablePathCheckBox!=null)
			useDefaultExecutablePath = defaultExecutablePathCheckBox.getState();
		String tempPath = executablePathField.getText();
		if (StringUtil.blank(tempPath) && !useDefaultExecutablePath){
			MesquiteMessage.discreetNotifyUser("The path to " +getExecutableName()+ " must be entered.");
			return false;
		}
		executablePath = tempPath;
*/
		
		if (processRequester.localScriptRunsRequireTerminalWindow())
			visibleTerminal=true;
		else if (visibleTerminalCheckBox!=null)
			visibleTerminal = visibleTerminalCheckBox.getState();
		if (deleteAnalysisDirectoryCheckBox!=null)
			deleteAnalysisDirectory = deleteAnalysisDirectoryCheckBox.getState();
		if (addExitCommandCheckBox!=null)
			addExitCommand = addExitCommandCheckBox.getState();
		if (!getDirectProcessConnectionAllowed())
			scriptBased=true;
		else if (scriptBasedRadioButtons != null)
			scriptBased = scriptBasedRadioButtons.getValue()==1;
		//else if (scriptBasedCheckBox!=null)
		//	scriptBased = scriptBasedCheckBox.getState();
		
		
		

		
/*		if (useDefaultExecutablePath) {
			appInfoFile = new AppInformationFile(getExternalProcessRequester().getAppNameWithinAppsDirectory());
			appInfoFile.processAppInfoFile();
		}
*/		return true;
	}

	public boolean visibleTerminalOptionAllowed(){
		return ShellScriptRunner.localScriptRunsCanDisplayTerminalWindow() && !processRequester.localScriptRunsRequireTerminalWindow() && !MesquiteTrunk.isMacOSXCatalinaOrLater();
	}
	public boolean getDirectProcessConnectionAllowed(){
		return processRequester.getDirectProcessConnectionAllowed() && MesquiteTrunk.isJavaGreaterThanOrEqualTo(1.7);
	}

	public boolean getBuiltInExecutableAllowed() {
		return processRequester.getBuiltInExecutableAllowed();
	}



	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return localRootDir;
	}

	/*.................................................................................................................*/
	public void deleteRunningFile(){  
		if (scriptRunner!=null)
			scriptRunner.deleteRunningFile();
	}


	/*.................................................................................................................*
	public String getShellScript(String programCommand, String args) {
		runningFilePath = rootDir + "running";//+ MesquiteFile.massageStringToFilePathSafe(unique);
		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(isWindows(), rootDir)+ StringUtil.lineEnding());
		if (StringUtil.notEmpty(additionalShellScriptCommands))
			shellScript.append(additionalShellScriptCommands + StringUtil.lineEnding());
		// 30 June 2017: added redirect of stderr
		//		shellScript.append(programCommand + " " + args+ " 2> " + ShellScriptRunner.stErrorFileName +  StringUtil.lineEnding());
		String suffix = "";
		if (isLinux()&&requiresLinuxTerminalCommands()) {
			shellScript.append(getLinuxBashScriptPreCommand());
			suffix="\"";
		}
		if (!processRequester.allowStdErrRedirect())
			shellScript.append(programCommand + " " + args + suffix+StringUtil.lineEnding());
		else {
			if (visibleTerminal && isMacOSX()) {
				shellScript.append(programCommand + " " + args+ " >/dev/tty   2> " + ShellScriptRunner.stErrorFileName +  suffix+StringUtil.lineEnding());
			}
			else
				shellScript.append(programCommand + " " + args+ " > " + ShellScriptRunner.stOutFileName+ " 2> " + ShellScriptRunner.stErrorFileName + suffix+ StringUtil.lineEnding());
		}
		if (isLinux()&&requiresLinuxTerminalCommands())
			shellScript.append(getLinuxBashScriptPostCommand());
		shellScript.append(ShellScriptUtil.getRemoveCommand(isWindows(), runningFilePath));
		if (scriptBased&&addExitCommand && ShellScriptUtil.exitCommandIsAvailableAndUseful())
			shellScript.append("\n" + ShellScriptUtil.getExitCommand() + "\n");
		return shellScript.toString();
	}
	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String presetDirectory, String[] fileContents, String[] fileNames, int runInfoFileNumber){  //assumes for now that all input files are in the same directory
		//String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		if (!setRootDirectory(presetDirectory))
			return false;

		if (fileContents !=null && fileNames !=null)  // if there are input files to write, then write them.
			for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
				if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
					MesquiteFile.putFileContents(localRootDir+fileNames[i], fileContents[i], true);
				}
			}
		String args = null;
		if (arguments instanceof MesquiteString)
			args = ((MesquiteString)arguments).getValue();
		else if (arguments instanceof String)
			args = (String)arguments;
		this.arguments = args;

		if (scriptBased) {
			String shellScript = getShellScript(programCommand, localRootDir, args);
			scriptPath = localRootDir + scriptFileName;// + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
			MesquiteFile.putFileContents(scriptPath, shellScript, false);
		}
		/* alternative, not well tested
		if (scriptBased) {
			runningFilePath = rootDir + "running";//+ MesquiteFile.massageStringToFilePathSafe(unique);
			StringBuffer shellScript = new StringBuffer(1000);
			String suffix = "";
			if (MesquiteTrunk.isLinux()&&requiresLinuxTerminalCommands())
				suffix="\"";
			shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir)+ StringUtil.lineEnding());
			if (processRequester.allowStdErrRedirect()) {  //using "exec" redirects all script commands, not just program's. 
				if (visibleTerminal && MesquiteTrunk.isMacOSX())
					shellScript.append("exec >/dev/tty" + StringUtil.lineEnding());
				else
					shellScript.append("exec > " + ShellScriptRunner.stOutFileName +StringUtil.lineEnding());
				shellScript.append("exec 2> " + ShellScriptRunner.stErrorFileName +StringUtil.lineEnding());
			}
			if (StringUtil.notEmpty(additionalShellScriptCommands))
				shellScript.append(additionalShellScriptCommands + StringUtil.lineEnding());
			// 30 June 2017: added redirect of stderr
			//		shellScript.append(programCommand + " " + args+ " 2> " + ShellScriptRunner.stErrorFileName +  StringUtil.lineEnding());
			if (MesquiteTrunk.isLinux()&&requiresLinuxTerminalCommands()) 
				shellScript.append(getLinuxBashScriptPreCommand());			
			shellScript.append(programCommand + " " + args +  suffix+StringUtil.lineEnding()); 
			if (MesquiteTrunk.isLinux()&&requiresLinuxTerminalCommands())
				shellScript.append(getLinuxBashScriptPostCommand());
			shellScript.append(ShellScriptUtil.getRemoveCommand(runningFilePath));
			//shellScript.append("badCommand2 "+StringUtil.lineEnding()); 
			if (scriptBased&&addExitCommand && ShellScriptUtil.exitCommandIsAvailableAndUseful())
				shellScript.append("\n" + ShellScriptUtil.getExitCommand() + "\n");
			scriptPath = rootDir + scriptFileName;// + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
			MesquiteFile.putFileContents(scriptPath, shellScript.toString(), false);
		}
		*/
		return true;
	}
	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setPreflightInputFiles(String script){  //assumes for now that all input files are in the same directory
		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		localRootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getRootNameForDirectory(), "-Run.");
		if (localRootDir==null)
			return false;

		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(isWindows(), localRootDir)+ StringUtil.lineEnding());
		shellScript.append(script);

		scriptPath = localRootDir + "preflight.bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), false);
		return true;
	}

	/*.................................................................................................................*/
	public void setOutputFileNamesToWatch(String[] fileNames){
		if (fileNames!=null) {
			outputFileNames = new String[fileNames.length];
			outputFilePaths = new String[fileNames.length];
			for (int i=0; i<fileNames.length; i++){
				outputFilePaths[i]=localRootDir+fileNames[i];
				outputFileNames[i]=fileNames[i];
			}
		}
	}
	/*.................................................................................................................*/
	public void setOutputFileNameToWatch(int index, String fileName){
		if (outputFileNames!=null && index>=0 && index < outputFileNames.length) {
			outputFilePaths[index]=localRootDir+fileName;
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
			setReadyForReconnectionSave(true);
			return scriptRunner.executeInShell();
		} else {
			externalProcessManager = new ExternalProcessManager(this, localRootDir, getExecutablePath(), arguments, getExecutableName(), outputFilePaths, this, this, false);
			setReadyForReconnectionSave(true);
			return externalProcessManager.executeInShell();
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
			if (externalProcessManager!=null) {
				boolean success = externalProcessManager.monitorAndCleanUpShell(progIndicator);
				if (externalProcessManager.exitCodeIsBad())  // if bad exit code, then don't autodelete the directory
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
		} else if (externalProcessManager!=null)
			externalProcessManager.stopExecution();
		//scriptRunner = null;
		return false;
	}
	
	
	/*.................................................................................................................*/
	/** If file close has been requested, here say whether or not to ask about killing the. After deciding, the caller should call setDontKill below!!!!.*/
	public boolean askAboutKillingRun() {
		if (scriptBased) {
			if (scriptRunner!=null) {
				return true;
			}
			
		} 
		return false;
	}
	/*.................................................................................................................*/
	/** To be called after askAboutKillingRun.*/
	public void setDontKill (boolean letRun) {
		if (scriptBased) {
			if (scriptRunner!=null) {
				if (letRun)
					scriptRunner.setDontKill(true);
			}
		} 
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
		String filePath = localRootDir + preflightLogFileName;
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
			MesquiteFile.deleteDirectory(localRootDir);
		localRootDir=null;
	}

	public boolean continueProcess(Process proc) {
		if (proc!=null && scriptBased) {
			if (!scriptRunner.processRunning()) {
				return false;
			}
		}
		if (processRequester.errorsAreFatal()) { 
			String stdErr = getStdErr();
			return !processRequester.stdErrIsTrueError(stdErr);
		}
		return true;
	}
	public boolean fatalErrorDetected() {
		if (processRequester.errorsAreFatal()) { 
			String stdErr = getStdErr();
			return processRequester.stdErrIsTrueError(stdErr);
		}
		return false;
	}

	public boolean warnIfError() {
		return true;
	}

}

