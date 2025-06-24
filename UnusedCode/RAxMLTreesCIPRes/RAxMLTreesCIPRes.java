/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLTreesCIPRes;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.RAxMLRunnerCIPRes.RAxMLRunnerCIPRes;
import mesquite.zephyr.RAxMLRunnerLocalOrig.RAxMLRunnerLocalOrig;
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
	 public String getProgramURL() {
		 return "http://sco.h-its.org/exelixis/web/software/raxml/index.html";
	 }

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "CIPRes";
	}


}
