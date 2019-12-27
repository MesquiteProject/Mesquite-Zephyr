/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.SSHRunner;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Random;

import mesquite.externalCommunication.lib.*;
import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class SSHRunner extends ScriptRunner implements OutputFileProcessor, ShellScriptWatcher, OutputFilePathModifier, ActionListener {
	MesquiteString xmlPrefs= new MesquiteString();
	String xmlPrefsString = null;
	StringBuffer extraPreferences;
	SSHServerProfileManager sshServerProfileManager;
	SSHServerProfile sshServerProfile = null;

	boolean verbose = true;
	boolean forgetPassword=false;

	SSHCommunicator communicator;

	/*.================================================================..*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		MesquiteModule mm = getEmployer();
		loadPreferences(xmlPrefs);
		if (sshServerProfileManager == null)
			sshServerProfileManager= (SSHServerProfileManager)MesquiteTrunk.mesquiteTrunk.hireEmployee(SSHServerProfileManager.class, "Supplier of SSH server specifications.");
		if (sshServerProfileManager == null) {
			return false;
		} 
		xmlPrefsString = xmlPrefs.getValue();
		scriptBased = true;
		setReadyForReconnectionSave(false);
		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}
	public int getMaxCores() {
		if (sshServerProfile!=null)
			return sshServerProfile.getMaxCores();
		return MesquiteInteger.infinite;
	}

	public static String getDefaultProgramLocation() {
		return "SSH Server";
	}

	public String getProgramLocation(){
		if (communicator!=null)
			return communicator.getSshServerProfileName() + " (" + communicator.getHost()+") via SSH";
		return getDefaultProgramLocation();
	}


	public String getName() {
		return "SSH Runner";
	}
	public String getExplanation() {
		return "Runs jobs by SSH on a server.";
	}
	public boolean isReconnectable(){
		return true;
	}
	public void setForgetPassword(boolean forgetPassword){
		this.forgetPassword=forgetPassword;
	}


	public String getMessageIfUserAbortRequested () {
		return "Do you wish to stop the analysis conducted via SSH?  No intermediate trees will be saved if you do.";
	}
	public String getMessageIfCloseFileRequested () {  
		return "If Mesquite closes this file, it will not stop the run on the server.  To stop the run on the server, press the \"Stop\" link in the analysis window before closing.";  
	}

	public  boolean canCalculateTimeRemaining(int repsCompleted){
		if (communicator!=null)
			return !communicator.hasBeenReconnected();
		return true;
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
		if (sshServerProfile==null)
			return false;
		return sshServerProfile.isWindows();
	}
	public  boolean isLinux() {
		if (sshServerProfile==null)
			return true;
		return sshServerProfile.isLinux();
	}
	public  boolean isMacOSX() {
		if (sshServerProfile==null)
			return false;
		return sshServerProfile.isMacOSX();
	}

	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		if (sshServerProfile!=null)
			StringUtil.appendXMLTag(buffer, 2, "sshServerProfileName", sshServerProfile.getName());  
		StringUtil.appendXMLTag(buffer, 2, "executablePath", getExecutableName(), remoteExecutablePath);  
		if (visibleTerminalOptionAllowed())
			StringUtil.appendXMLTag(buffer, 2, "visibleTerminal", visibleTerminal);  
		StringUtil.appendXMLTag(buffer, 2, "deleteAnalysisDirectory", deleteAnalysisDirectory);  
		StringUtil.appendXMLTag(buffer, 2, "scriptBased", scriptBased);  
		StringUtil.appendXMLTag(buffer, 2, "addExitCommand", addExitCommand);  

		buffer.append(extraPreferences);
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("sshServerProfileName".equalsIgnoreCase(tag) && sshServerProfileManager!=null) {
			sshServerProfile = sshServerProfileManager.getSSHServerProfile(content);
		}
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		if (sshServerProfile!=null)
			StringUtil.appendXMLTag(buffer, 2, "sshServerProfileName",sshServerProfile.getName());
		buffer.append(extraPreferences);
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (localRootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(localRootDir));
		if (sshServerProfile != null)
			temp.addLine("setServerProfileName " +  ParseUtil.tokenize(sshServerProfile.getName()));
		if (outputFilePaths != null){
			String files = " ";
			for (int i = 0; i< outputFilePaths.length; i++){
				files += " " + ParseUtil.tokenize(outputFilePaths[i]);
			}
			temp.addLine("setOutputFilePaths " + files);
		}
		if (communicator != null){
			temp.addLine("reviveCommunicator "+ ParseUtil.tokenize(communicator.getRemoteWorkingDirectoryPath())+ " " + ParseUtil.tokenize(communicator.getUserName()));
			temp.addLine("tell It");
			temp.incorporate(communicator.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		return temp;
	}
	boolean reportJobURL = false;
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the sshServerProfile", "[file path]", commandName, "setServerProfileName")) {
			String sshServerProfileName = parser.getFirstToken(arguments);
			sshServerProfile = sshServerProfileManager.getSSHServerProfile(sshServerProfileName);
			return sshServerProfile;
		} else if (checker.compare(this.getClass(), "Revives the communicator", "[file path]", commandName, "reviveCommunicator")) {
			logln("Reviving SSH Communicator");
			String path = parser.getFirstToken(arguments);
			String username = parser.getNextToken();
			if (!prepareCommunicator(true, username))
				return null;
			if (StringUtil.notEmpty(path)) {
				String separator = sshServerProfile.getDirectorySeparator();
				String name = Parser.getLastItem(path, separator, null, true);
				String directory = Parser.getAllButLastItem(path, separator, null, true);
				if (!StringUtil.endsWithIgnoreCase(directory, separator))
					directory += separator;
				communicator.setRemoteWorkingDirectoryName(name);
				communicator.setRemoteServerDirectoryPath(directory);
			}
			setReadyForReconnectionSave(true);
			return communicator;
		}
		else if (checker.compare(this.getClass(), "Sets the output file paths", "[file paths]", commandName, "setOutputFilePaths")) {
			int num = parser.getNumberOfTokens(arguments);
			outputFilePaths = new String[num];
			if (num >0)
				outputFilePaths[0] = parser.getFirstToken();
			for (int i=1; i<num; i++)
				outputFilePaths[i] = parser.getNextToken();
		}
		else if (checker.compare(this.getClass(), "Sets local directory for temporary files", null, commandName, "setRootDir")) {
			localRootDir = parser.getFirstToken(arguments);
			if (communicator!=null)
				communicator.setRootDir(localRootDir);
		}
		else if (checker.compare(this.getClass(), "Sets name of the directory for temporary files for this run on the remote computer", null, commandName, "setRemoteWorkingDirectoryName")) {
			String name = parser.getFirstToken(arguments);
			if (communicator!=null)
				communicator.setRemoteWorkingDirectoryName(name);
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

	/*.................................................................................................................*/
	public void forgetServerPassword() {
		forgetPassword=true;
	}

	Choice sshServerProfileChoice;
	Checkbox ForgetPasswordCheckbox;
	DoubleField runLimitField;
	ExtensibleDialog optionsDialog;
	/*.................................................................................................................*/
	// given the opportunity to fill in options for user
	int dialogCounter = 1;
	public  boolean addItemsToDialogPanel(ExtensibleDialog dialog){
		dialogCounter++;
		if (communicator!=null) {
			dialog.addBoldLabel(communicator.getServiceName()+" Server Options");
			ForgetPasswordCheckbox = dialog.addCheckBox("Require new login to "+communicator.getServiceName()+" Server", false);
		} else {
			dialog.addBoldLabel("SSH Server Options");
			ForgetPasswordCheckbox = dialog.addCheckBox("Require new login to SSH Server", false);
		}
		String[] specifications = sshServerProfileManager.getListOfProfiles();
		optionsDialog=dialog;
		if (specifications==null)
			if (!sshServerProfileManager.queryOptions())
				return false;

		int index = sshServerProfileManager.findProfileIndex(sshServerProfileManager.getSshServerProfileName());
		if (index<0) index=0;
		sshServerProfileChoice = dialog.addPopUpMenu("SSH Server Profile", sshServerProfileManager.getListOfProfiles(), index);
		final Button manageSpecificationsButton = dialog.addAListenedButton("Manage...",null, this);
		manageSpecificationsButton.setActionCommand("ManageSpecifications");
		sshServerProfileManager.setChoice(sshServerProfileChoice);

		return true;
	}

	public void addNoteToBottomOfDialog(ExtensibleDialog dialog){
		dialog.addHorizontalLine(1);
		dialog.addLabelSmallText("SSH features in Zephyr are preliminary, and may have some flaws.");
		dialog.addLabelSmallText("Please send feedback to info@mesquiteproject.org");
	}

	public boolean optionsChosen(){
		if (ForgetPasswordCheckbox.getState())
			forgetServerPassword();
		int sshServerProfileIndex = sshServerProfileChoice.getSelectedIndex();
		sshServerProfile = sshServerProfileManager.getSSHServerProfile(sshServerProfileIndex);
		//sshServerProfileName = sshServerProfile.getName();
		sshServerProfileManager.setSshServerProfileName(sshServerProfile.getName());
		return true;
	}
	public void storeRunnerPreferences() {
		if (sshServerProfileManager!=null)
			sshServerProfileManager.storePreferences();
		super.storePreferences();
	}

	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("ManageSpecifications")) {
			int currentSSHServerProfileIndex = sshServerProfileChoice.getSelectedIndex();
			SSHServerProfile currentSSHServerProfile = sshServerProfileManager.getSSHServerProfile(currentSSHServerProfileIndex);
			String currentSSHServerProfileName = currentSSHServerProfile.getName();
			if (sshServerProfileManager.manageSSHServerProfiles()) {
				int count2 = sshServerProfileChoice.getItemCount();
				while (sshServerProfileChoice.getItemCount()>0)
					sshServerProfileChoice.remove(0);
				String[] specList = sshServerProfileManager.getListOfProfiles();
				if (specList!=null && specList.length>0)
					for (int i=0; i<specList.length; i++)
						sshServerProfileChoice.add(specList[i]);
				if (MesquiteTrunk.isJavaGreaterThanOrEqualTo(1.8)) 
					sshServerProfileChoice.revalidate();
				int index = sshServerProfileManager.findProfileIndex(currentSSHServerProfileName);
				if (index<0) index=0;
				sshServerProfileChoice.select(index);
				sshServerProfileChoice.repaint();
				optionsDialog.prepareDialog();
				optionsDialog.repaint();
			} else
				logln("NOTE: manageSSHServerSpecifications=false");
		} 

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
	/*.................................................................................................................*/
	public String getProgramSSHName() {
		return "";
	}
	/*.................................................................................................................*/
	public String getExecutablePath(){
		int num = getExecutableNumber();
		if (num<0) {
			logln("WARNING: Executable number not specified!");
			return null;
		}
		if (!sshServerProfile.validProgramPath(num)) {
			logln("WARNING: Path to program on remote SSH Server not specified!");
			return null;
		}
		return sshServerProfile.getProgramPath(num);
	}


	String executableRemoteName;
	String[] commands;
	String[] outputFilePaths;
	String[] outputFileNames;
	String[] inputFilePaths;
	String[] inputFileNames;
	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return localRootDir;
	}

	/*.................................................................................................................*/
	public String getInputFilePath(int i){  //assumes for now that all input files are in the same directory
		if (i<inputFilePaths.length)
			return inputFilePaths[i];
		return null;
	}
	/*.................................................................................................................*/
	public String getAdditionalRunInformation(){  
		StringBuffer sb = new StringBuffer();
		sb.append("\n\n------------------------------------------\n");
		sb.append("Remote computer on which analysis was conducted: " + getProgramLocation() +" \n");
		if (communicator!=null) {
			sb.append("Username on remote computer: " + communicator.getUserName() +" \n");
			sb.append("Directory on remote computer holding working directory: " + communicator.getRemoteServerDirectoryPath() +" \n");
			sb.append("Name of working directory on remote computer : " + communicator.getRemoteWorkingDirectoryName() +" \n");
		}
		return sb.toString();
	}

	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String[] fileContents, String[] fileNames, int runInfoFileNumber){  //assumes for now that all input files are in the same directory
		executableRemoteName= programCommand;
		String args = null;
		if (arguments instanceof MesquiteString)
			args = ((MesquiteString)arguments).getValue();
		else if (arguments instanceof String)
			args = (String)arguments;
		else 
			return false;
		if (!setRootDir())
			return false;

		localScriptFilePath = localRootDir + scriptFileName;

		if (!prepareCommunicator(false, null))
			return false;
		if (scriptBased) {
			String shellScript = getShellScript(programCommand, communicator.getRemoteWorkingDirectoryPath(), args);
			MesquiteFile.putFileContents(localScriptFilePath, shellScript, false);
		}
		//communicator.setRemoteWorkingDirectoryPath("/Users/david/Desktop/runTest");
		commands = new String[] {"> "+ShellScriptUtil.runningFileName, programCommand+" "+args, "rm -f "+ShellScriptUtil.runningFileName};

		if (runInfoFileNumber<fileContents.length)
			fileContents[runInfoFileNumber]+= getAdditionalRunInformation();
		inputFilePaths = new String[fileNames.length];
		inputFileNames = new String[fileNames.length];
		for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
			if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
				MesquiteFile.putFileContents(localRootDir+fileNames[i], fileContents[i], true);
				inputFilePaths[i]=localRootDir+fileNames[i];
				inputFileNames[i]=fileNames[i];
			}
		}
		processRequester.prepareRunnerObject(commands);

		return true;
	}
	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setPreflightInputFiles(String script){  //assumes for now that all input files are in the same directory
		localRootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getExecutableName(), "-Run.");
		if (localRootDir==null)
			return false;

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
			if (communicator!=null)
				communicator.setOutputFilePaths(outputFilePaths);
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
	/*.................................................................................................................*/
	public void setRunningFilePath() {
		runningFilePath = communicator.getRemoteWorkingDirectoryPath() + getDirectorySeparator() + ShellScriptUtil.runningFileName;//+ MesquiteFile.massageStringToFilePathSafe(unique);

	}

	/*.................................................................................................................*/
	public String getDirectorySeparator() {
		if (isWindows())
			return "\\";
		return "/";
	}

	/*.................................................................................................................*/
	public String getRemoteScriptPath() {
		return communicator.getRemoteWorkingDirectoryPath() + getDirectorySeparator() + scriptFileName;
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	private boolean prepareCommunicator(boolean hasBeenReconnected, String userName) {
		communicator = new SSHCommunicator(this,xmlPrefsString, outputFilePaths);
		if (communicator==null) 
			return false;
		communicator.setHasBeenReconnected(hasBeenReconnected);
		if (sshServerProfile!=null) {
			communicator.setSSHServerProfile(sshServerProfile);
			if (!sshServerProfile.getName().equals(communicator.getSshServerProfileName())) // we've changed to a different 
					communicator.forgetPassword();
			//else
			//	sshServerProfile.setPassword(sshServerProfile.getPassword());
		}
		if (forgetPassword)
			communicator.forgetPassword();
		communicator.setOutputProcessor(this);
		communicator.setWatcher(this);
		communicator.setRootDir(localRootDir);
		communicator.setProgressIndicator(progressIndicator);
		communicator.setRunDetailsForHelp(processRequester.getRunDetailsForHelp());
		if (sshServerProfile!=null) {
			communicator.setSshServerProfileName(sshServerProfile.getName());
			communicator.setRemoteServerDirectoryPath(sshServerProfile.getTempFileDirectory());
			communicator.setHost(sshServerProfile.getHost());
			if (StringUtil.notEmpty(userName))
				communicator.setUserName(userName);
		}
		if (communicator.checkUsernamePassword(false))
			if (!hasBeenReconnected && !communicator.checkForUniqueRemoteWorkingDirectoryName(getExecutableName())) {
				logln("\n*********\nERROR: Could not identify remote working directory; there may be a problem communicating with the SSH Server\n*********");
				return false;
			}

		return true;
	}
	/*.................................................................................................................*/
	// starting the run
	public boolean startExecution(){  //do we assume these are disconnectable?
		if (sshServerProfile==null)
			return false;
		boolean successfulStart = false;
		if (communicator.checkUsernamePassword(false)) {
			if (communicator.createRemoteWorkingDirectory()) {
				communicator.transferFilesToServer(inputFilePaths, inputFileNames);
				if (MesquiteFile.fileExists(localScriptFilePath)) {
					communicator.transferFileToServer(localScriptFilePath, scriptFileName);
					communicator.setRemoteFileToExecutable(scriptFileName);
				}
				successfulStart = true;
			}
		}

		if (successfulStart) {
			setReadyForReconnectionSave(true);
			forgetPassword = false;
			if (scriptBased) {
				communicator.addEmptyFileToWorkingDirectory(ShellScriptUtil.runningFileName);
				return communicator.sendCommands(new String[] {getExecuteScriptCommand("./"+scriptFileName, visibleTerminal)},true, true, true);  // this works on Linux or Mac
				//	return communicator.sendCommands(new String[] {getExecuteScriptCommand(getRemoteScriptPath(), visibleTerminal)},true, true, true);  // this works on Mac
			}
			else
				return communicator.sendCommands(commands,true, true, true);
		}
		return false;
	}

	public boolean monitorExecution(ProgressIndicator progIndicator){
		if (communicator!=null) {
			return communicator.monitorAndCleanUpShell(null, progIndicator);
		}
		return false;
	}

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		if (communicator!=null) {
			communicator.deleteJob(null);
			communicator.setAborted(true);
		}
		return true;
	}
	public String getPreflightFile(String preflightLogFileName){
		String filePath = localRootDir + preflightLogFileName;
		String fileContents = MesquiteFile.getFileContentsAsString(filePath);
		return fileContents;
	}
	/*.................................................................................................................*/
	public String getExecuteScriptCommand(String scriptPath, boolean visibleTerminal){ 
		if (isMacOSX()){
			if (visibleTerminal) {
				return "open -a /Applications/Utilities/Terminal.app "+ scriptPath;
			}
			else {
				scriptPath = scriptPath.replaceAll("//", "/");
				return scriptPath;
			}
		}
		else if (isLinux()) {
			// remove double slashes or things won't execute properly
			scriptPath = scriptPath.replaceAll("//", "/");
			return scriptPath;
		} else {  
			scriptPath = "\"" + scriptPath + "\"";
			return "cmd /c start \"\" " + scriptPath;
		}
	}

	/*.................................................................................................................*/
	public String getStdErr() {
		//communicator.getRootDir();
		return "";  //TODO: Implement!!!
	}

	/*.................................................................................................................*/
	public String getStdOut() {
		if (communicator!=null) {
			String path = communicator.getRootDir()+processRequester.getLogFileName();
			String contents = MesquiteFile.getFileContentsAsStringNoWarn(path);
			return contents; 
		}
		return "";
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
	public boolean continueShellProcess(Process proc) {
		return true;
	}
	public boolean fatalErrorDetected() {
		String stdErr = getStdErr();
		if (StringUtil.notEmpty(stdErr))
			return true;
		return false;
	}


}
