/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.IQTreeTreesCIPRes;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.IQTreeRunnerCIPRes.IQTreeRunnerCIPRes;
import mesquite.zephyr.lib.*;


public class IQTreeTreesCIPRes extends IQTreeTrees {

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.IQTreeRunnerCIPRes.IQTreeRunnerCIPRes";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return IQTreeRunnerCIPRes.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "CIPRes";
	}
	 
	/*.................................................................................................................*/
	 public boolean loadModule(){
		 return true;
	}


}
