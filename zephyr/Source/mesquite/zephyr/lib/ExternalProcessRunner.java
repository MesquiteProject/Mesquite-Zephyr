/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

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


	public  boolean canCalculateTimeRemaining(int repsCompleted){
		return true;
	}

	public String getInputFilePath(int i){ 
		return null;
	}

	/*.................................................................................................................*/
	public String getDirectoryPath(){  
		return "";
	}

	public abstract boolean isWindows();
	public abstract boolean isLinux();


	// setting the requester, to whom this runner will communicate about the run
	public abstract void setProcessRequester(ExternalProcessRequester processRequester);

	// given the opportunity to fill in options for user
	public abstract void addItemsToDialogPanel(ExtensibleDialog dialog);
	public abstract boolean optionsChosen();

	// the actual data & scripts.  
	public abstract boolean setPreflightInputFiles(String script);
	public abstract boolean setProgramArgumentsAndInputFiles(String programCommand, Object arguments, String[] fileContents, String[] fileNames);  //assumes for now that all input files are in the same directory
	public abstract void setOutputFileNamesToWatch(String[] fileNames);
	public abstract void setOutputFileNameToWatch(int index, String fileName);
	public abstract String getOutputFilePath(String fileName);
	public abstract String[] getOutputFilePaths();
	public abstract String getLastLineOfOutputFile(String fileName);

	public abstract String getPreflightFile(String preflightLogFileName); 

	// starting the run
	public abstract boolean startExecution();  //do we assume these are disconnectable?
	public abstract boolean monitorExecution();
	public abstract String checkStatus();  
	public abstract boolean stopExecution();  

	public abstract String getStdErr();  
	public abstract String getStdOut();  

	/*.................................................................................................................*/
	public void resetLastModified(int i){
	}

	//results can be harvested by getOutputFile	
}
