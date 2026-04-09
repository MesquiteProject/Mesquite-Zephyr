/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.IQTreeTreesLocal;

import mesquite.lib.CompatibilityTest;
import mesquite.lib.misc.KeywordsCompatibilityTest;
import mesquite.zephyr.IQTreeRunnerLocal.IQTreeRunnerLocal;
import mesquite.zephyr.lib.IQTreeTrees;


public class IQTreeTreesLocal extends IQTreeTrees {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.IQTreeRunner.IQTreeRunnerLocal";
	}
	
	/*.................................................................................................................*/
	public CompatibilityTest getCompatibilityTest(){
		return new KeywordsCompatibilityTest(new String[]{"acceptImposedMatrixSource", "parallelReady"});
	}

	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return IQTreeRunnerLocal.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}

		public boolean requestPrimaryChoice(){
			return true;
		}

}
