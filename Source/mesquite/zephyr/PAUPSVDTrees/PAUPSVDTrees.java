/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPSVDTrees;

import java.util.*;

import mesquite.io.lib.IOUtil;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.PAUPSVDRunner.PAUPSVDRunner;
import mesquite.zephyr.lib.*;


public class PAUPSVDTrees extends ZephyrTreeSearcher implements InvariantsAnalysis {
	TreeSource treeRecoveryTask;
	//Taxa taxa;
	//private MatrixSourceCoord matrixSourceTask;
	//protected MCharactersDistribution observedStates;
	//int rerootNode = 0;


	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPSVDRunner.PAUPSVDRunner";
	}
	/*.................................................................................................................*/
	public String getProgramName() {
		return "PAUP";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return PAUPRunner.PAUPURL;
	 }
	 public Class getRunnerClass(){
		 return PAUPSVDRunner.class;
	 }

	 /*.................................................................................................................*/
	 public boolean isPrerelease(){
		return false;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean canGiveIntermediateResults(){
		return false;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2000;  
	}

	/*.................................................................................................................*/
	public String getTreeBlockName(boolean completedRun){
		if (runner.bootstrapOrJackknife()) {
			return "PAUP SVD Quartets " + runner.getResamplingKindName() + " Tree (Matrix: " + observedStates.getName() + ")";
		} 
		else {
			return "PAUP SVD Quartets Tree (Matrix: " + observedStates.getName() + ")";

		}
	}
	/*.................................................................................................................*/
	public String getName() {
		return "PAUP (SVD Quartets)";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "PAUP (SVD Quartets)...";
	}


}
