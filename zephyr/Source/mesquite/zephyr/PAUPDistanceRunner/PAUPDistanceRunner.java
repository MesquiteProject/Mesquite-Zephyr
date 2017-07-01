/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPDistanceRunner;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public class PAUPDistanceRunner extends PAUPSearchRunner {


	public String getCriterionSetCommand() {
		return "set criterion=distance;";
	}

	public String getCriterionScoreCommand() {
		return "dscore";
	}


	public String getOptimalTreeAdjectiveLowerCase() {
		return "optimal distance";
	}

	public String getOptimalTreeAdjectiveTitleCase() {
		return "Optimal Distance";
	}


	public String getName() {
		return "PAUP Distance";
	}
	/*.................................................................................................................*/
	public boolean allowRerooting() {
		return false;
	}


}
