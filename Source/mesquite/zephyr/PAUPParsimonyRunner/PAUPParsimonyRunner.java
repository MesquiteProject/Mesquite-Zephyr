/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPParsimonyRunner;

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

public class PAUPParsimonyRunner extends PAUPSearchRunner {


	public String getCriterionSetCommand() {
		return "set criterion=parsimony;";
	}

	public String getCriterionScoreCommand() {
		return "pscore";
	}


	public String getOptimalTreeAdjectiveLowerCase() {
		return "most-parsimonious";
	}

	public String getOptimalTreeAdjectiveTitleCase() {
		return "Most-Parsimonious";
	}


	public String getName() {
		return "PAUP Parsimony";
	}


}
