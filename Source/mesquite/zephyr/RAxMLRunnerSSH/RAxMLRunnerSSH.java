/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.RAxMLRunnerSSH;


import java.awt.*;
import java.io.*;
import java.awt.event.*;


import mesquite.lib.*;
import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.*;


public class RAxMLRunnerSSH extends RAxMLRunnerBasic  {

	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.RAxML;
	}
	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.SSHRunner.SSHRunner";
	}

	public Class getDutyClass() {
		return RAxMLRunnerSSH.class;
	}

	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return SSHRunner.class;
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "8.2.10";
	}
	
	/*.................................................................................................................*/
	public void reportNewTreeAvailable(){
		log("[New tree acquired]");
 	}


	public String getLogText() {
		String log= externalProcRunner.getStdOut();
		if (StringUtil.blank(log))
			log="Waiting for log file from SSH...";
		return log;
	}
	/*.................................................................................................................*/
	public int getMaxCores(){
		if (externalProcRunner!=null)
			return externalProcRunner.getMaxCores();
		return MesquiteInteger.infinite ;
	}
	public void checkFields() {
		int max = getMaxCores();
		if (MesquiteInteger.isCombinable(max) && numProcessorsField.isValidInteger() && numProcessorsField.getValue()>max) {
			MesquiteMessage.notifyUser("Number of processors used cannot exceed "+max +", as that is the maximum specified in the SSH Server Profile");			
			numProcessorsField.setValue(max);
		}
	}

	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "RAxML Options on SSH Server";
	}

	/*.................................................................................................................*/
	int currentRunProcessed=0;
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (!bootstrapOrJackknife() && numRuns>1 ) {
			String[] fileNames = getLogFileNames();
			externalProcRunner.setOutputFileNameToWatch(WORKING_TREEFILE, fileNames[WORKING_TREEFILE]);
			outputFilePaths[WORKING_TREEFILE] = externalProcRunner.getOutputFilePath(fileNames[WORKING_TREEFILE]);
			for (int i=currentRunProcessed; i<numRuns; i++) {
				String candidate = outputFilePaths[WORKING_TREEFILE]+i;
				if (MesquiteFile.fileExists(candidate)) {
					outputFilePaths[WORKING_TREEFILE]= candidate;
					currentRunProcessed++;
				}
			}
			externalProcRunner.resetLastModified(WORKING_TREEFILE);
			previousCurrentRun=currentRun;
		}
		return outputFilePaths;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2500;  
	}

	public String getName() {
		return "RAxML Likelihood (SSH Server)";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}



}
