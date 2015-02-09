/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesCIPRes;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.GarliRunner.GarliRunner;
import mesquite.zephyr.RAxMLRunnerCIPRes.RAxMLRunnerCIPRes;
import mesquite.zephyr.RAxMLRunnerLocal.RAxMLRunnerLocal;
import mesquite.zephyr.lib.*;


public class RAxMLTreesCIPRes extends RAxMLTrees {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerCIPRes.RAxMLRunnerCIPRes";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerCIPRes.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "CIPRes";
	}


}
