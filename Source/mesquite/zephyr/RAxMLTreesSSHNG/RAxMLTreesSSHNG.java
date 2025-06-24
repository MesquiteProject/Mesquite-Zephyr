/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesSSHNG;

import mesquite.zephyr.RAxMLRunnerSSHNG.RAxMLRunnerSSHNG;
import mesquite.zephyr.lib.RAxMLTreesNG;


public class RAxMLTreesSSHNG extends RAxMLTreesNG {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerSSHNG.RAxMLRunnerSSHNG";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerSSHNG.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 if (runner!=null)
			 return runner.getProgramLocation();
		 return "SSH Server";
	}


}
