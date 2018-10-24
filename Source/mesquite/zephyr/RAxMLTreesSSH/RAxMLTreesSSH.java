/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesSSH;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.RAxMLRunnerSSH.RAxMLRunnerSSH;
import mesquite.zephyr.RAxMLRunnerLocal.RAxMLRunnerLocal;
import mesquite.zephyr.lib.*;


public class RAxMLTreesSSH extends RAxMLTrees {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunnerSSH.RAxMLRunnerSSH";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunnerSSH.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "SSH";
	}


}
