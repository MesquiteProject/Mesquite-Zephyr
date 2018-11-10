/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.IQTreeTreesSSH;

import mesquite.zephyr.IQTreeRunnerSSH.IQTreeRunnerSSH;
import mesquite.zephyr.lib.*;


public class IQTreeTreesSSH extends IQTreeTrees {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.IQTreeRunner.IQTreeRunnerSSH";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return IQTreeRunnerSSH.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "SSH Server";
	}


}
