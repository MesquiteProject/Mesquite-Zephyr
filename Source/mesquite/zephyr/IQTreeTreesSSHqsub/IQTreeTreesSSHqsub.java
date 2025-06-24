/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.IQTreeTreesSSHqsub;

import mesquite.zephyr.IQTreeRunnerSSHqsub.IQTreeRunnerSSHqsub;
import mesquite.zephyr.lib.IQTreeTrees;


public class IQTreeTreesSSHqsub extends IQTreeTrees {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.IQTreeRunnerSSHqsub.IQTreeRunnerSSHqsub";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return IQTreeRunnerSSHqsub.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 if (runner!=null)
			 return runner.getProgramLocation();
		 return "SSH Server QSub";
	}
		/*.................................................................................................................*/
		public boolean loadModule(){
			return false;
		}


}
