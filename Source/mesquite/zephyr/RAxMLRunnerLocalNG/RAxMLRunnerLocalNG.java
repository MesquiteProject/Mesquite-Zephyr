/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.RAxMLRunnerLocalNG;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.externalCommunication.AppHarvester.AppHarvester;
import mesquite.externalCommunication.lib.AppInformationFile;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.io.ExportFusedPhylip.ExportFusedPhylip;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.RAxMLTreesLocalOld.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;


public class RAxMLRunnerLocalNG extends RAxMLRunnerBasicNG  {

	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}
	public Class getDutyClass() {
		return RAxMLRunnerLocalNG.class;
	}
	/*.................................................................................................................*/
	public boolean mayHaveProblemsWithDeletingRunningOnReconnect() {
		return true;
	}
	public boolean getDefaultExecutablePathAllowed() {
		return getHasApp();
	}

	/*.................................................................................................................*/
	public void setUpRunner() { 
		super.setUpRunner();
		AppHarvester.examineAppsFolder(this);
	}

	/*.................................................................................................................*/
	public AppInformationFile getAppInfoFile() {
		return AppHarvester.getAppInfoFileForProgram(this);
	}
	/*.................................................................................................................*/
	public String getAppOfficialName() {
		return "RAxML-NG";
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "RAxML-NG Likelihood (Local)";
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	public int getMaxCores(){
		return Runtime.getRuntime().availableProcessors();
	}

	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}



}
