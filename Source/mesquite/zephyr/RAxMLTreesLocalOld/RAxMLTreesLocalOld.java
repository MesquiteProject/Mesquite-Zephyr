/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesLocalOld;

import mesquite.zephyr.RAxMLRunnerLocalOld.RAxMLRunnerLocalOld;
import mesquite.zephyr.lib.*;


public class RAxMLTreesLocalOld extends RAxMLTrees {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerLocalOld.RAxMLRunnerLocalOld";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerLocalOld.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}


}
