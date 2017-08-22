/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.CIPResRESTRunner;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class CIPResRESTRunner extends ExternalProcessRunner implements OutputFileProcessor, ShellScriptWatcher, OutputFilePathModifier {
	String rootDir = null;
	MesquiteString jobURL = null;
	MesquiteString jobID = null;
	ExternalProcessRequester processRequester;
	MesquiteString xmlPrefs= new MesquiteString();
	String xmlPrefsString = null;

	boolean verbose = true;
	boolean forgetPassword=false;

	
	CIPResCommunicator communicator;

	/*.================================================================..*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		MesquiteModule mm = getEmployer();
		loadPreferences(xmlPrefs);
		xmlPrefsString = xmlPrefs.getValue();

		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}
	
	public  String getProgramLocation(){
		return "CIPRes";
	}

	
	public String getName() {
		return "CIPRes REST Runner";
	}
	public String getExplanation() {
		return "Runs jobs on CIPRes.";
	}
	public boolean isReconnectable(){
		return true;
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
		return false;
	}
	public  boolean isLinux() {
		return false;
	}



	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (communicator != null){
			temp.addLine("reviveCommunicator ");
			temp.addLine("tell It");
			temp.incorporate(communicator.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		if (jobURL != null)
			temp.addLine("setJobURL " +  ParseUtil.tokenize(jobURL.getValue()));
		if (rootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(rootDir));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveCommunicator")) {
			logln("Reviving CIPRes Communicator");
			communicator = new CIPResCommunicator(this, xmlPrefsString,outputFilePaths);
			communicator.setOutputProcessor(this);
			communicator.setWatcher(this);
			communicator.setRootDir(rootDir);
			if (forgetPassword)
				communicator.forgetPassword();
			forgetPassword = false;

			return communicator;
		}
		else if (checker.compare(this.getClass(), "Sets the job URL", null, commandName, "setJobURL")) {
			if (jobURL==null)
				jobURL = new MesquiteString();
			jobURL.setValue(parser.getFirstToken(arguments));
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
		setRootNameForDirectory(processRequester.getRootNameForDirectory());
		this.processRequester = processRequester;
		loadPreferences();
		processRequester.intializeAfterExternalProcessRunnerHired();
	}

	/*.................................................................................................................*/
	public void forgetCIPResPassword() {
		forgetPassword=true;
	}

	Checkbox ForgetPasswordCheckbox;

	// given the opportunity to fill in options for user
	public  void addItemsToDialogPanel(ExtensibleDialog dialog){
		dialog.addBoldLabel("CIPRes Options");
		ForgetPasswordCheckbox = dialog.addCheckBox("re-enter CIPRes password", false);
	}
	public boolean optionsChosen(){
		if (ForgetPasswordCheckbox.getState())
			forgetCIPResPassword();
		return true;
	}

	/*.................................................................................................................*/
	public String getExecutableCommand(){
		return processRequester.getExecutableName();
	}
	
	String executableCIPResName;
	MultipartEntityBuilder builder;
	String[] outputFilePaths;
	String[] outputFileNames;
	String[] inputFilePaths;
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
		executableCIPResName= programCommand;
		if (!(arguments instanceof MultipartEntityBuilder))
			return false;
		builder = (MultipartEntityBuilder)arguments;
		if (rootDir==null) 
			rootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getExecutableName(), "-Run.");
		if (rootDir==null)
			return false;
		
		inputFilePaths = new String[fileNames.length];
		for (int i=0; i<fileContents.length && i<fileNames.length; i++) {
			if (StringUtil.notEmpty(fileNames[i]) && fileContents[i]!=null) {
				MesquiteFile.putFileContents(rootDir+fileNames[i], fileContents[i], true);
				inputFilePaths[i]=rootDir+fileNames[i];
			}
		}
		processRequester.prepareRunnerObject(builder);

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
		communicator = new CIPResCommunicator(this,xmlPrefsString, outputFilePaths);
		communicator.setOutputProcessor(this);
		communicator.setWatcher(this);
		communicator.setRootDir(rootDir);
		if (forgetPassword)
			communicator.forgetPassword();
		forgetPassword = false;

		jobURL = new MesquiteString();
		return communicator.sendJobToCipres(builder, executableCIPResName, jobURL);
	}

	public boolean monitorExecution(ProgressIndicator progIndicator){
		 if (communicator!=null && jobURL!=null)
			 return communicator.monitorAndCleanUpShell(jobURL.getValue(), progIndicator);
		 return false;
	}

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		communicator = null;
		return false;
	}
	public String getPreflightFile(String preflightLogFileName){
		String filePath = rootDir + preflightLogFileName;
		String fileContents = MesquiteFile.getFileContentsAsString(filePath);
		return fileContents;
	}

	/*.................................................................................................................*/
	public String getStdErr() {

		return "";  //TODO: Implement!!!
	}
	/*.................................................................................................................*/
	public String getStdOut() {

		return ""; //TODO: Implement!!!
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
