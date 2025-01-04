/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;



import mesquite.externalCommunication.lib.AppChooser;
import mesquite.lib.*;
import mesquite.lib.duties.*;

public abstract class ExternalProcessRunner extends MesquiteModule {
	String executableName;
	int executableNumber;
	String rootNameForDirectory;
	TreeInferer treeInferrer;
	boolean userAborted = false;
	protected ProgressIndicator progressIndicator;
	protected String localRootDir = null;  // local directory for storing files on local machine
	protected boolean readyForReconnectionSave = false;
	protected boolean onlySetUpRun = false;
	protected boolean leaveAnalysisDirectoryIntact = false;
	protected boolean scriptBased = false;
	protected boolean visibleTerminal = false;


	public Class getDutyClass() {
		return ExternalProcessRunner.class;
	}
	public String getDutyName() {
		return "External Process Runner";
	}
	public static String getDefaultProgramLocation() {
		return "";
	}
	public boolean getLeaveAnalysisDirectoryIntact() {
		return leaveAnalysisDirectoryIntact;
	}
	public void setLeaveAnalysisDirectoryIntact(boolean leaveAnalysisDirectoryIntact) {
		this.leaveAnalysisDirectoryIntact = leaveAnalysisDirectoryIntact;
	}
	public boolean isVisibleTerminal() {
		return visibleTerminal;
	}
	public boolean isScriptBased() {
		return scriptBased;
	}

	public void setScriptBased(boolean scriptBased) {
		this.scriptBased = scriptBased;
	}
	
	public boolean useAppInAppFolder() {
		return false;
	}

	public String getAppOtherProperties() {
		return "";
	}

	public void appChooserDialogBoxEntryChanged() {
		
	}

	public AppChooser getAppChooser() {
		return null;
	}

	public String[] getDefaultModule() {
		return new String[] {""};
	}
	public boolean isOnlySetUpRun() {
		return onlySetUpRun;
	}
	public void setOnlySetUpRun(boolean onlySetUpRun) {
		this.onlySetUpRun = onlySetUpRun;
	}

	public int getMaxCores() {
		return MesquiteInteger.infinite;
	}
	
	public String getMessageIfUserAbortRequested () {
		return "";
	}

	public void storeRunnerPreferences() {
		super.storePreferences();
	}
	public String getMessageIfCloseFileRequested () {
		return "";
	}
	public boolean userAborted(){
		return userAborted;
	}
	public void setUserAborted(boolean aborted){
		this.userAborted = aborted;
	}

	public void setReadyForReconnectionSave(boolean readyForReconnectionSave){
		this.readyForReconnectionSave = readyForReconnectionSave;
		if (readyForReconnectionSave && isReconnectable())
			logln("\n[Run sufficiently established so that file can be saved, closed, reopened, and then reconnected.]");
	}
	public boolean getReadyForReconnectionSave(){
		return readyForReconnectionSave;
	}

	public String getExecutableName() {
		return executableName;
	}
	
	public void setExecutableName(String executableName) {
		this.executableName = executableName;
	}
	
	public int getExecutableNumber() {
		return executableNumber;
	}
	
	public void setExecutableNumber(int executableNumber) {
		this.executableNumber = executableNumber;
	}
	
	public String getRootNameForDirectory() {
		return rootNameForDirectory;
	}
	
	public void setRootNameForDirectory(String rootNameForDirectory) {
		this.rootNameForDirectory = rootNameForDirectory;
	}
	public abstract boolean isReconnectable();
	
	public abstract String getExecutableCommand();

	public  void setOutputTextListener(OutputTextListener textListener){
	}

	public abstract String getProgramLocation();

	public  boolean canCalculateTimeRemaining(int repsCompleted){
		return true;
	}
	public void setProgressIndicator(ProgressIndicator progressIndicator) {
		this.progressIndicator= progressIndicator;
	}

	public String getInputFilePath(int i){ 
		return null;
	}
	protected String additionalShellScriptCommands = "";
	/*.................................................................................................................*/
	public void setAdditionalShellScriptCommands(String additionalShellScriptCommands) {
		this.additionalShellScriptCommands = additionalShellScriptCommands;
	}
	/*.................................................................................................................*/
	public boolean setRootDir() {
		if (StringUtil.blank(localRootDir)) 
			localRootDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.BESIDE_HOME_FILE, getExecutableName(), "-Run.");
		return localRootDir!=null; 
	}
	/*.................................................................................................................*/
	public void setRootDir(String localRootDir) {
		this.localRootDir = localRootDir;
	}

	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return "";
	}

	public abstract boolean isWindows();
	public abstract boolean isLinux();
	public abstract boolean isMacOSX();


	// setting the requester, to whom this runner will communicate about the run
	public abstract void setProcessRequester(ExternalProcessRequester processRequester);

	// given the opportunity to fill in options for user
	public abstract boolean addItemsToDialogPanel(ExtensibleDialog dialog);
	public  void addNoteToBottomOfDialog(ExtensibleDialog dialog){
	}
	public abstract boolean optionsChosen();

	// the actual data & scripts.  
	public abstract boolean setPreflightInputFiles(String script);
	public abstract boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String[] fileContents, String[] fileNames, int runInfoFileNumber);  //assumes for now that all input files are in the same directory
	public abstract void setOutputFileNamesToWatch(String[] fileNames);
	public abstract void setOutputFileNameToWatch(int index, String fileName);
	public abstract String getOutputFilePath(String fileName);
	public abstract String[] getOutputFilePaths();
	public abstract String getLastLineOfOutputFile(String fileName);

	public abstract String getPreflightFile(String preflightLogFileName); 

	// starting the run
	public abstract boolean startExecution();  //do we assume these are disconnectable?
	public abstract boolean monitorExecution(ProgressIndicator progIndicator);
	public abstract String checkStatus();  
	public abstract boolean stopExecution();  
	/*.................................................................................................................*/
	public void finalCleanup() {
	}

	public abstract String getStdErr();  
	public abstract String getStdOut();  

	/*.................................................................................................................*/
	public void resetLastModified(int i){
	}

	
	//results can be harvested by getOutputFile	
}
