package mesquite.zephyr.lib;

import mesquite.lib.*;

public abstract class ExternalProcessRunner extends MesquiteModule {

	public Class getDutyClass() {
		return ExternalProcessRunner.class;
	}
	public String getDutyName() {
		return "External Process Runner";
	}

	public String[] getDefaultModule() {
		return new String[] {""};
	}



	// setting the requester, to whom this runner will communicate about the run
	public abstract void setProcessRequester(ExternalProcessRequester processRequester);

	// given the opportunity to fill in options for user
	public abstract void fillPanelInDialog(ExtensibleDialog dialog, MesquitePanel panel);
	public abstract boolean optionsChosen(ExtensibleDialog dialog, MesquitePanel panel, MesquiteBoolean okChosen);

	// the actual data & scripts.  
	public abstract void setInputFiles(String script, String[] files);  //assumes for now that all input files are in the same directory
	public abstract void setOutputFilePathsToWatch(String[] filePaths);
	public abstract String getOutputFile(String filePath);

	// starting the run
	public abstract boolean beginExecution();  //do we assume these are disconnectable?

	public abstract String checkStatus();  
	public abstract boolean stopExecution();  

	//results can be harvested by getOutputFile	
}
