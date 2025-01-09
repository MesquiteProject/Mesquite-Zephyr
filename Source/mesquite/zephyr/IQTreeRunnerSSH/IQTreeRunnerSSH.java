/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.IQTreeRunnerSSH;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.io.ExportFusedPhylip.ExportFusedPhylip;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.RAxMLRunnerSSHOrig.RAxMLRunnerSSHOrig;
import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;


public class IQTreeRunnerSSH extends IQTreeRunnerBasic  {


	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.IQTREE;
	}
	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.SSHRunner.SSHRunner";
	}

	public Class getDutyClass() {
		return IQTreeRunnerSSH.class;
	}

	public String getLogText() {
		String log= externalProcRunner.getStdOut();
		if (StringUtil.blank(log))
			log="Waiting for log file from SSH...";
		return log;
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return SSHRunner.class;
	}

	/*.................................................................................................................*/
	public void reportNewTreeAvailable(){
		log("[New tree acquired]");
 	}

	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "IQ-TREE Options on SSH Server";
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -3000;  
	}

	public String getName() {
		return "IQ-TREE Likelihood (SSH Server)";
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
