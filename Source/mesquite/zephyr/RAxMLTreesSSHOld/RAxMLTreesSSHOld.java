/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesSSHOld;

import java.util.*;

import mesquite.zephyr.RAxMLRunnerSSHOld.RAxMLRunnerSSHOld;
import mesquite.zephyr.lib.*;


public class RAxMLTreesSSHOld extends RAxMLTrees {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerSSHOld.RAxMLRunnerSSHOld";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerSSHOld.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 if (runner!=null)
			 return runner.getProgramLocation();
		 return "SSH Server";
	}


}
