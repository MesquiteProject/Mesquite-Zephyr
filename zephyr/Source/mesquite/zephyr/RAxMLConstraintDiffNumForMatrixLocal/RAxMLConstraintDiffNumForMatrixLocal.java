/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLConstraintDiffNumForMatrixLocal;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.zephyr.RAxMLRunnerLocal.RAxMLRunnerLocal;
import mesquite.zephyr.lib.*;


public class RAxMLConstraintDiffNumForMatrixLocal extends RAxMLConstraintDiffNumForMatrix {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunner.RAxMLRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerLocal.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}



}
