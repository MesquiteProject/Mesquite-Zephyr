/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.IQTreeRunnerSSHqsub;


import mesquite.zephyr.IQTreeRunnerSSH.IQTreeRunnerSSH;
import mesquite.zephyr.SSHqsubRunner.SSHqsubRunner;
import mesquite.zephyr.lib.SSHServerProfile;


public class IQTreeRunnerSSHqsub extends IQTreeRunnerSSH  {


	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.IQTREE;
	}
	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.SSHqsubRunner.SSHqsubRunner";
	}

	public Class getDutyClass() {
		return IQTreeRunnerSSHqsub.class;
	}

	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return SSHqsubRunner.class;
	}


	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "IQ-TREE Options on SSH qsub Server";
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

	public String getName() {
		return "IQ-TREE Likelihood (SSH qsub Server)";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean loadModule(){
		return false;
	}








}
