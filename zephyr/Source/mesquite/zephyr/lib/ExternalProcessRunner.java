package mesquite.zephyr.lib;

import mesquite.lib.*;

public abstract class ExternalProcessRunner extends MesquiteModule {
	String executableName;

	public Class getDutyClass() {
		return ExternalProcessRunner.class;
	}
	public String getDutyName() {
		return "External Process Runner";
	}

	public String[] getDefaultModule() {
		return new String[] {""};
	}

	public String getExecutableName() {
		return executableName;
	}
	public void setExecutableName(String executableName) {
		this.executableName = executableName;
	}
	public abstract String getExecutableCommand();




	// setting the requester, to whom this runner will communicate about the run
	public abstract void setProcessRequester(ExternalProcessRequester processRequester);

	// given the opportunity to fill in options for user
	public abstract void addItemsToDialogPanel(ExtensibleDialog dialog);
	public abstract boolean optionsChosen();

	// the actual data & scripts.  
	public abstract boolean setInputFiles(String script, String[] fileContents, String[] fileNames);  //assumes for now that all input files are in the same directory
	public abstract void setOutputFileNamesToWatch(String[] fileNames);
	public abstract String getOutputFilePath(String fileName);
	public abstract String[] getOutputFilePaths();
	public abstract String getLastLineOfOutputFile(String fileName);

	// starting the run
	public abstract boolean startExecution();  //do we assume these are disconnectable?
	public abstract boolean monitorExecution();
	public abstract String checkStatus();  
	public abstract boolean stopExecution();  

	//results can be harvested by getOutputFile	
}
