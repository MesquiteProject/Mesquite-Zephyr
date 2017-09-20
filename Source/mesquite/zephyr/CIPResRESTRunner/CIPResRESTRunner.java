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
	double runLimit=0.5;
	
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
	
	
	public String getMessageIfUserAbortRequested () {
		return "Do you wish to stop the CIPRes analysis?  No intermediate trees will be saved if you do.";
	}
	public String getMessageIfCloseFileRequested () {  
		return "If Mesquite closes this file, it will not stop the run on CIPRes.  To stop the run on CIPRes, press the \"Stop\" link in the analysis window before closing.";  
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
		return false;
	}
	public  boolean isLinux() {
		return false;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "runLimit", runLimit);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("runLimit".equalsIgnoreCase(tag))
			runLimit = MesquiteDouble.fromString(content);
		super.processSingleXMLPreference(tag, content);
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
		temp.addLine("setRunLimit " +  runLimit);
		if (jobURL != null)
			temp.addLine("setJobURL " +  ParseUtil.tokenize(jobURL.getValue()));
		return temp;
	}
	boolean reportJobURL = false;
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the scriptRunner", "[file path]", commandName, "reviveCommunicator")) {
			logln("Reviving CIPRes Communicator");
			communicator = new CIPResCommunicator(this, xmlPrefsString,outputFilePaths);
			//setOutputFileNamesToWatch(fileNames[]);
			if (jobURL!=null)
				logln("\nJob URL: " + jobURL.getValue()+"\n");
			else
				reportJobURL=true;
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
		else if (checker.compare(this.getClass(), "Sets the job URL", null, commandName, "setJobURL")) {
			if (jobURL==null)
				jobURL = new MesquiteString();
			jobURL.setValue(parser.getFirstToken(arguments));
			if (reportJobURL) {
				logln("Job URL: " + jobURL.getValue());
				reportJobURL=false;
			}
		}
		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			rootDir = parser.getFirstToken(arguments);
			if (communicator!=null)
				communicator.setRootDir(rootDir);
		}
		else if (checker.compare(this.getClass(), "Sets runLimit", null, commandName, "setRunLimit")) {
			runLimit = MesquiteDouble.fromString(parser.getFirstToken(arguments));
			if (communicator!=null)
				communicator.setRunLimit(runLimit);
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
	DoubleField runLimitField;
	
	// given the opportunity to fill in options for user
	public  void addItemsToDialogPanel(ExtensibleDialog dialog){
		dialog.addBoldLabel("CIPRes Options");
		ForgetPasswordCheckbox = dialog.addCheckBox("Require new login to CIPRes", false);
		runLimitField = dialog.addDoubleField("maximum hours of CIPRes time for run", runLimit, 5, 0, 168);
	}
	
	public void addNoteToBottomOfDialog(ExtensibleDialog dialog){
		dialog.addHorizontalLine(1);
		dialog.addLabelSmallText("CIPRes features in Zephyr are preliminary, and may have some flaws.");
		dialog.addLabelSmallText("Please send feedback to info@mesquiteproject.org");
	}

	public boolean optionsChosen(){
		if (ForgetPasswordCheckbox.getState())
			forgetCIPResPassword();
		runLimit = runLimitField.getValue();
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
		communicator.setRunLimit(runLimit);
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
		communicator.deleteJob(jobURL.getValue());
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
