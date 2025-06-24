/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesLocalOrig;

import mesquite.zephyr.RAxMLRunnerLocalOrig.RAxMLRunnerLocalOrig;
import mesquite.zephyr.lib.RAxMLTreesOrig;


public class RAxMLTreesLocalOrig extends RAxMLTreesOrig {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerLocalOrig.RAxMLRunnerLocalOrig";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerLocalOrig.class;
	}

	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}


}
