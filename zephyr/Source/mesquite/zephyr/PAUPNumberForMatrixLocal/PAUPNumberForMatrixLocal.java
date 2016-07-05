/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPNumberForMatrixLocal;

import mesquite.zephyr.PAUPParsimonyRunner.PAUPParsimonyRunner;
import mesquite.zephyr.lib.*;


public class PAUPNumberForMatrixLocal extends PAUPNumberForMatrix {

	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.PAUPParsimonyRunner.PAUPParsimonyRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return PAUPParsimonyRunner.class;
	}

	/*.................................................................................................................*/
	 public String getProgramLocation(){
		 return "Local";
	}



}
