/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.TNTTreesLocal;

import mesquite.zephyr.TNTRunnerLocal.TNTRunnerLocal;
import mesquite.zephyr.lib.TNTTrees;


public class TNTTreesLocal extends TNTTrees {

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public String getExplanation() {
		return "If TNT is installed, will save a copy of a character matrix and script TNT to conduct a parsimony search, and harvest the resulting trees.";
	}
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.TNTRunnerLocal.TNTRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return TNTRunnerLocal.class;
	}
	/*.................................................................................................................*/
	public String getProgramLocation(){
		return "Local";
	}
	/*.................................................................................................................*/

	public String getName() {
		return "TNT Trees [Local]";
	}


}
