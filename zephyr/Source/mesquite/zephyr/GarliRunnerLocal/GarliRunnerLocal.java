/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

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

	SingleLineTextField garliPathField = null;
	SingleLineTextField constraintFileField = null;

	String ofprefix = "output";

	String dataFileName = "dataMatrix.nex";
	int bootstrapreps = 100;
	int availMemory = 1024;

	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return LocalScriptRunner.class;
	 }

	 /*.................................................................................................................*/
	 public Object getProgramArguments(String dataFileName, String configFileName, boolean isPreflight) {

		 MesquiteString arguments = new MesquiteString();
		 if (externalProcRunner.isWindows())
			 arguments.setValue(" --batch " + configFileName);
		 else
			 arguments.setValue(""); // GARLI command is very simple as all of the arguments are in the config file
		 return arguments;
	 }

	/*.................................................................................................................*/
	public void appendToConfigFileGeneral(StringBuffer config) {
		if (config!=null) {
			config.append("\ndatafname=" + dataFileName);
			config.append("\nofprefix=" + ofprefix);

			if (StringUtil.blank(constraintfile))
				config.append("\nconstraintfile = none");
			else
				config.append("\nconstraintfile = constraint"); // important to be user-editable

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
		super.processSingleXMLPreference(tag, content);
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML() {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "availMemory", availMemory);
		buffer.append(super.preparePreferencesForXML());
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0";
	}
	IntegerField availableMemoryField;

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
		availableMemoryField = dialog.addIntegerField("Memory for GARLI (MB)", availMemory, 8, 256, MesquiteInteger.infinite);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		availMemory = availableMemoryField.getValue();
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
		return "GARLI Local Runner";
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
