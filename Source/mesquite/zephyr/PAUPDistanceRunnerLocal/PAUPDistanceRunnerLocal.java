/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPDistanceRunnerLocal;

import mesquite.externalCommunication.AppHarvester.AppHarvester;
import mesquite.externalCommunication.lib.AppInformationFile;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.PAUPDistanceRunner;



public class PAUPDistanceRunnerLocal extends PAUPDistanceRunner {


	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}
	/*.................................................................................................................*/
	public void setUpRunner() { 
		super.setUpRunner();
		hasApp = AppHarvester.builtinAppExists(getAppOfficialName());
	}

	/*.................................................................................................................*/
	public AppInformationFile getAppInfoFile() {
		return AppHarvester.getAppInfoFileForProgram(this);
	}

	/*.................................................................................................................*/

	public String getName() {
		return "PAUP Trees (Distance) [Local] Runner";
	}
	/*.................................................................................................................*/
	public boolean mayHaveProblemsWithDeletingRunningOnReconnect() {
		return true;
	}



}
