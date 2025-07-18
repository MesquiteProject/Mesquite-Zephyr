/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.zephyr.GarliTreesLocal;

import mesquite.zephyr.GarliRunnerLocal.GarliRunnerLocal;
import mesquite.zephyr.lib.GarliTrees;


public class GarliTreesLocal extends GarliTrees {
	int rerootNode = 0;

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.GarliRunnerLocal.GarliRunnerLocal";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return GarliRunnerLocal.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	 }

	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return true;
	}



}
