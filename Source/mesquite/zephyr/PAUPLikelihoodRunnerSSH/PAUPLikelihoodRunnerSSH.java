/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPLikelihoodRunnerSSH;

import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.PAUPLikelihoodRunner;
import mesquite.zephyr.lib.SSHServerProfile;



public class PAUPLikelihoodRunnerSSH extends PAUPLikelihoodRunner {
	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.SSHRunner.SSHRunner";
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

	public String getName() {
		return "PAUP Trees (Likelihood) [SSH Server] Runner";
	}

	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.PAUP;
	}


}
