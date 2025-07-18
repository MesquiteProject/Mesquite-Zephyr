/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPParsimonyRunnerSSH;

import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.PAUPParsimonyRunnerBasic;
import mesquite.zephyr.lib.SSHServerProfile;



public class PAUPParsimonyRunnerSSH extends PAUPParsimonyRunnerBasic {

	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.SSHRunner.SSHRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return SSHRunner.class;
	}

	/*.................................................................................................................*/

	public String getName() {
		return "PAUP Trees (Parsimony) [SSH Server] Runner";
	}

	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.PAUP;
	}

}
