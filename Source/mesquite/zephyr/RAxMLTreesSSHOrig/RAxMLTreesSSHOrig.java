/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesSSHOrig;

import mesquite.zephyr.RAxMLRunnerSSHOrig.RAxMLRunnerSSHOrig;
import mesquite.zephyr.lib.RAxMLTreesOrig;


public class RAxMLTreesSSHOrig extends RAxMLTreesOrig {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerSSHOrig.RAxMLRunnerSSHOrig";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerSSHOrig.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 if (runner!=null)
			 return runner.getProgramLocation();
		 return "SSH Server";
	}


}
