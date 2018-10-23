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
import java.util.Random;

import mesquite.externalCommunication.lib.*;
import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class SSHRunner extends ExternalProcessRunner implements OutputFileProcessor, ShellScriptWatcher, OutputFilePathModifier, ActionListener {
	String rootDir = null;  // local directory for storing files on local machine
//	MesquiteString jobURL = null;
//	MesquiteString jobID = null;
	ExternalProcessRequester processRequester;
	MesquiteString xmlPrefs= new MesquiteString();
	String xmlPrefsString = null;
	StringBuffer extraPreferences;
	SSHServerProfileManager sshServerProfileManager;
	SSHServerProfile sshServerProfile = null;
//	String sshServerProfileName = "";

//	String remoteExecutablePath = "./usr/local/bin/raxmlHPC8211-PTHREADS-AVX2";

	boolean verbose = true;
	boolean forgetPassword=false;
	
	SimpleSSHCommunicator communicator;

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

		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}
	
	public  String getProgramLocation(){
		if (communicator!=null)
			return "SSH "+communicator.getHost();
		return "SSH";
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
	
	
	public String getMessageIfUserAbortRequested () {
		return "Do you wish to stop the analysis conducted via SSH?  No intermediate trees will be saved if you do.";
	}
	public String getMessageIfCloseFileRequested () {  
		return "If Mesquite closes this file, it will not stop the run on the server.  To stop the run on the server, press the \"Stop\" link in the analysis window before closing.";  
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
	public void processSingleXMLPreference (String tag, String flavor, String content) {
		if (StringUtil.notEmpty(flavor) && "remoteExecutablePath".equalsIgnoreCase(tag)){   // it is one with the flavor attribute
			if (flavor.equalsIgnoreCase(getExecutableName()))   /// check to see if flavor is correct!!!
				remoteExecutablePath = StringUtil.cleanXMLEscapeCharacters(content);
			else {
				String path = StringUtil.cleanXMLEscapeCharacters(content);
				StringUtil.appendXMLTag(extraPreferences, 2, "remoteExecutablePath", flavor, path);  		// store for next time
			}
		}
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "executablePath", getExecutableName(), remoteExecutablePath);  
		if (visibleTerminalOptionAllowed())
			StringUtil.appendXMLTag(buffer, 2, "visibleTerminal", visibleTerminal);  
		StringUtil.appendXMLTag(buffer, 2, "deleteAnalysisDirectory", deleteAnalysisDirectory);  
		StringUtil.appendXMLTag(buffer, 2, "scriptBased", scriptBased);  
		StringUtil.appendXMLTag(buffer, 2, "addExitCommand", addExitCommand);  
		
		buffer.append(extraPreferences);
		return buffer.toString();
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "executablePath", getExecutableName(), executablePath);  
		if (StringUtil.notEmpty(sshServerProfileName))
			StringUtil.appendXMLTag(buffer, 2, "serverProfileName",sshServerProfileName);
		buffer.append(extraPreferences);
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (rootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(rootDir));
		if (outputFilePaths != null){
			String files = " ";
			for (int i = 0; i< outputFilePaths.length; i++){
				files += " " + ParseUtil.tokenize(outputFilePaths[i]);
			}
			temp.addLine("setOutputFilePaths " + files);
		}
		if (communicator != null){
			temp.addLine("reviveCommunicator ");
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
		if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveCommunicator")) {
			logln("Reviving SSH Communicator");
			communicator = new SimpleSSHCommunicator(this, xmlPrefsString,outputFilePaths);
			communicator.setOutputProcessor(this);
			communicator.setWatcher(this);
			communicator.setRootDir(rootDir);
			if (forgetPassword)
				communicator.forgetPassword();
			forgetPassword = false;

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
		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			rootDir = parser.getFirstToken(arguments);
			if (communicator!=null)
				communicator.setRootDir(rootDir);
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
		Debugg.println("add items to dialog panel "+dialogCounter);
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
				Debugg.println("manageSSHServerSpecifications=false");
		} 

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
	/*.................................................................................................................*/
	public String getProgramSSHName() {
		return "";
	}
	/*.................................................................................................................*/
	public String getExecutableCommand(){
		String remoteExecutablePath = sshServerProfile.getProgramPath(getExecutableNumber());
		if (MesquiteTrunk.isWindows())
			return "call " + StringUtil.protectFilePathForWindows(remoteExecutablePath);
		else if (MesquiteTrunk.isLinux()) {
			if (requiresLinuxTerminalCommands())
				return getLinuxTerminalCommand() + " " + StringUtil.protectFilePathForUnix(remoteExecutablePath);
			else 
				return " \"" + remoteExecutablePath+"\"";
		}
		else
			return StringUtil.protectFilePathForUnix(remoteExecutablePath);
	}
	
	String executableRemoteName;
	String[] commands;
	String[] outputFilePaths;
	String[] outputFileNames;
	String[] inputFilePaths;
	String[] inputFileNames;
	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return rootDir;
	}

	/*.................................................................................................................*/
	public String getInputFilePath(int i){  //assumes for now that all input files are in the same directory
		if (i<inputFilePaths.length)
			return inputFilePaths[i];
		return null;
	}

	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String[] fileContents, String[] fileNames){  //assumes for now that all input files are in the same directory
		executableRemoteName= programCommand;
		if (!(arguments instanceof MesquiteString))
			return false;
		//communicator.setRemoteWorkingDirectoryPath("/Users/david/Desktop/runTest");
		commands = new String[] {"> "+communicator.runningFileName, programCommand+" "+((MesquiteString)arguments).getValue(), "rm -f "+communicator.runningFileName};
		if (rootDir==null) 
			rootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getExecutableName(), "-Run.");
		if (rootDir==null)
			return false;
		
		inputFilePaths = new String[fileNames.length];
		inputFileNames = new String[fileNames.length];
		for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
			if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
				MesquiteFile.putFileContents(rootDir+fileNames[i], fileContents[i], true);
				inputFilePaths[i]=rootDir+fileNames[i];
				inputFileNames[i]=fileNames[i];
			}
		}
		processRequester.prepareRunnerObject(commands);

		return true;
	}
	/*.................................................................................................................*/
	// the actual data & scripts.  
	public boolean setPreflightInputFiles(String script){  //assumes for now that all input files are in the same directory
		rootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getExecutableName(), "-Run.");
		if (rootDir==null)
			return false;
		
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

	/*.................................................................................................................*/
	// starting the run
	public boolean startExecution(){  //do we assume these are disconnectable?
		if (sshServerProfile==null)
			return false;
		communicator = new SimpleSSHCommunicator(this,xmlPrefsString, outputFilePaths);
		if (forgetPassword)
			communicator.forgetPassword();
		communicator.setOutputProcessor(this);
		communicator.setWatcher(this);
		communicator.setRootDir(rootDir);
		communicator.setProgressIndicator(progressIndicator);
		communicator.setRemoteServerDirectoryPath(sshServerProfile.getTempFileDirectory());
		communicator.setRemoteWorkingDirectoryName(MesquiteFile.getFileNameFromFilePath(rootDir));
		communicator.setHost(sshServerProfile.getHost());
		communicator.setUsername(sshServerProfile.getUsername());
		if (communicator.checkUsernamePassword(false))
			communicator.transferFilesToServer(inputFilePaths, inputFileNames);
		
		forgetPassword = false;

		return communicator.sendCommands(commands,true, true, true);
	}

	public boolean monitorExecution(ProgressIndicator progIndicator){
		 if (communicator!=null)
			 return communicator.monitorAndCleanUpShell(null, progIndicator);
		 return false;
	}

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		communicator.deleteJob(null);
		communicator.setAborted(true);
		return true;
	}
	public String getPreflightFile(String preflightLogFileName){
		String filePath = rootDir + preflightLogFileName;
		String fileContents = MesquiteFile.getFileContentsAsString(filePath);
		return fileContents;
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
