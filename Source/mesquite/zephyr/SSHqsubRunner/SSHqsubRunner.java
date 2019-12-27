/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.SSHqsubRunner;

import mesquite.zephyr.SSHRunner.SSHRunner;

public class SSHqsubRunner extends SSHRunner  {



	/*.................................................................................................................*/
	public String getExecuteScriptPrefixCommand() {
		return "qsub -pe orte 4 -m e -M maddisod@science.oregonstate.edu -cwd -S /bin/sh -N testMesquite "; // Debugg
	}

	public String getName() {
		return "SSH qsub Runner";
	}
	public String getExplanation() {
		return "Runs jobs by SSH on a server using the qsub scheduler.";
	}


}
