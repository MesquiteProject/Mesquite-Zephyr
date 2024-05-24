/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.GarliRunnerLocal;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.lib.*;

public class GarliRunnerLocal extends GarliRunner {


	String ofprefix = "output";

	String dataFileName = "dataMatrix.nex";
	int bootstrapreps = 100;
	int availMemory = 1024;
	int numProcessors = 0;
	

	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return LocalScriptRunner.class;
	 }

		/*.................................................................................................................*/
		public boolean mayHaveProblemsWithDeletingRunningOnReconnect() {
			return true;
		}
		/*.................................................................................................................*/
		public boolean canUseLocalApp() {
			return true;
		}

	 /*.................................................................................................................*/
	 public Object getProgramArguments(String dataFileName, String configFileName, boolean isPreflight) {

		 MesquiteString arguments = new MesquiteString();
		 String additionalArguments = "";
		 if (numProcessors>0)
			 externalProcRunner.setAdditionalShellScriptCommands("export OMP_NUM_THREADS="+numProcessors);
		 if (externalProcRunner.isWindows())
			 arguments.setValue(" --batch " + configFileName+additionalArguments);
		 else
			 arguments.setValue(additionalArguments); // GARLI command is very simple as all of the arguments are in the config file
		 return arguments;
	 }
		public String getLogText() {
			return externalProcRunner.getStdOut();
		}
	/*.................................................................................................................*/
	public void appendToConfigFileGeneral(StringBuffer config) {
		if (config!=null) {
			config.append("\ndatafname=" + dataFileName);
			config.append("\nofprefix=" + ofprefix);

			if (useConstraintTree==0)
				config.append("\nconstraintfile = none");
			else
				config.append("\nconstraintfile = constraintTree"); // important to be user-editable

			config.append("\nstreefname = random");

			config.append("\navailablememory = " + availMemory + " \n");
			config.append(" \noutputmostlyuselessfiles = 0");

			config.append("\n\nrandseed = " + randseed); // important to be user-editable

			config.append("\nsearchreps = " + numRuns);
			
			config.append("\n");
		}
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference(String tag, String content) {
		
		if ("availMemory".equalsIgnoreCase(tag))
			availMemory = MesquiteInteger.fromString(content);
		if ("numProcessors".equalsIgnoreCase(tag))
			numProcessors = MesquiteInteger.fromString(content);
		super.processSingleXMLPreference(tag, content);
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML() {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "availMemory", availMemory);
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  
		buffer.append(super.preparePreferencesForXML());
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0–2.01";
	}
	IntegerField availableMemoryField;
	IntegerField numProcessorsField;
	/*.................................................................................................................*/
	public String queryOptionsDialogTitle() {
		return "GARLI Options & Locations";
	}

	/*.................................................................................................................*/
	public boolean addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
		availableMemoryField = dialog.addIntegerField("Memory for GARLI (MB)", availMemory, 8, 256, MesquiteInteger.infinite);
		numProcessorsField = dialog.addIntegerField("Number of processor cores(to use all processors, enter 0)", numProcessors, 8, 0, MesquiteInteger.infinite);
		return true;
	//	dialog.addLabel("(To use all processors, enter 0)");
		
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		availMemory = availableMemoryField.getValue();
		numProcessors = numProcessorsField.getValue(); //
	}

	/*.................................................................................................................*/
	public void setFileNames() {
		configFileName = "garli.conf";
	}


	/*.................................................................................................................*/
	public String[] getLogFileNames() {
		String treeFileName;
		if (bootstrapOrJackknife())
			treeFileName = ofprefix + ".boot.tre";
		else
			treeFileName = ofprefix + ".best.tre";
		String currentTreeFilePath = ofprefix + ".best.current.tre";
		String allBestTreeFilePath = ofprefix + ".best.all.tre";
		String mainLogFileName = ofprefix + ".log00.log";

		return new String[] { mainLogFileName, currentTreeFilePath,ofprefix + ".screen.log", treeFileName, allBestTreeFilePath };
	}

	/*.................................................................................................................*/


	public Class getDutyClass() {
		return GarliRunner.class;
	}

	public String getName() {
		return "GARLI Likelihood (Local)";
	}

	public String getProgramName() {
		return "GARLI";
	}

	public String getExecutableName() {
		return "GARLI";
	}

	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}
}
/*.................................................................................................................*/
