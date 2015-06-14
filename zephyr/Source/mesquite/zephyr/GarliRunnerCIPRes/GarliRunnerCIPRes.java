/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.GarliRunnerCIPRes;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.lib.*;

public class GarliRunnerCIPRes extends GarliRunner {

	String ofprefix = "output";

	String dataFileName = null;


	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return CIPResRESTRunner.class;
	}

	/*.................................................................................................................*/
	public void prepareRunnerObject(Object obj){
		if (obj instanceof MultipartEntityBuilder) {
			MultipartEntityBuilder builder = (MultipartEntityBuilder)obj;
			final File file = new File(externalProcRunner.getInputFilePath(DATAFILENUMBER));
			FileBody fb = new FileBody(file);
			builder.addPart("input.infile_", fb);  
			final File file2 = new File(externalProcRunner.getInputFilePath(CONFIGFILENUMBER));
			FileBody fb2 = new FileBody(file2);
			builder.addPart("input.upload_conffile_", fb2);  
		}
	}

	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, String configFilePath, boolean isPreflight) {
		MultipartEntityBuilder arguments = MultipartEntityBuilder.create();

		arguments.addTextBody("vparam.user_conffile_", "1");
		arguments.addTextBody("vparam.userconffilethere_", "1");
		arguments.addTextBody("vparam.userconffileconfirm_", "1");
		
		arguments.addTextBody("vparam.searchreps_value_", ""+numRuns);

		return arguments;
	}

	/*.................................................................................................................*/
	public void appendToConfigFileGeneral(StringBuffer config) {
		if (config!=null) {
			config.append("\ndatafname=infile");
			config.append("\nofprefix=" + ofprefix);

			if (useConstraintTree==0)
				config.append("\nconstraintfile = none");
			else
				config.append("\nconstraintfile = constraintTree"); // important to be user-editable

			config.append("\nstreefname = random");

			config.append("\navailablememory = 2000 \n");
			config.append(" \noutputmostlyuselessfiles = 0");

			config.append("\n\nrandseed = -1"); // important to be user-editable
			config.append("\nsearchreps = 1");
			config.append("\n");
		}
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0";
	}

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		externalProcRunner.optionsChosen();
	}


	/*.................................................................................................................*/
	public void setFileNames() {
		configFileName = "garli.conf";
	}
	/*.................................................................................................................*/
	public boolean mpiVersion() {
		return true;
	}

	/*.................................................................................................................*/
	public String[] getLogFileNames() {
		String treeFileName; 	
		String run = ".run" + MesquiteInteger.toStringDigitsSpecified(currentRun, 2);
		if (bootstrapOrJackknife())
			treeFileName = ofprefix + run + ".boot.tre";
		else
			treeFileName = ofprefix + run + ".best.tre";
		String currentTreeFilePath = ofprefix + run + ".best.current.tre";
		String allBestTreeFilePath = ofprefix + run + ".best.tre";
//		String allBestTreeFilePath = ofprefix + ".best.all.tre";
		String mainLogFileName = ofprefix + run + ".log00.log";
		return new String[] { mainLogFileName, currentTreeFilePath, "stdout.txt", treeFileName, allBestTreeFilePath };
	}

	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (!bootstrapOrJackknife() && numRuns>1 ) {
			if (currentRun!=previousCurrentRun) {
				String[] fileNames = getLogFileNames();
				externalProcRunner.setOutputFileNameToWatch(MAINLOGFILE, fileNames[MAINLOGFILE]);
				outputFilePaths[MAINLOGFILE] = externalProcRunner.getOutputFilePath(fileNames[MAINLOGFILE]);
				externalProcRunner.resetLastModified(MAINLOGFILE);
				externalProcRunner.setOutputFileNameToWatch(CURRENTTREEFILEPATH, fileNames[CURRENTTREEFILEPATH]);
				outputFilePaths[CURRENTTREEFILEPATH] = externalProcRunner.getOutputFilePath(fileNames[CURRENTTREEFILEPATH]);
				externalProcRunner.resetLastModified(CURRENTTREEFILEPATH);
				previousCurrentRun=currentRun;
				logln("\n----- Now displaying results from run " + currentRun);
			}
		}
		return outputFilePaths;
	}

	/*.................................................................................................................*/

	public Class getDutyClass() {
		return GarliRunner.class;
	}

	public String getName() {
		return "GARLI CIPRes Runner";
	}


	public String getProgramName() {
		return "GARLI";
	}

	public String getExecutableName() {
		return "GARLI2_TGB";
	}

	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

}
/*.................................................................................................................*/

