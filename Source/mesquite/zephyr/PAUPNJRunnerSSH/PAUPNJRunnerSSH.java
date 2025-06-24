/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPNJRunnerSSH;

import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.PAUPNJRunner;
import mesquite.zephyr.lib.SSHServerProfile;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public class PAUPNJRunnerSSH extends PAUPNJRunner {
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
		return "PAUP Trees (NJ) [SSH Server] Runner";
	}

	/*.................................................................................................................*/
	public int getProgramNumber() {
		return SSHServerProfile.PAUP;
	}


}
