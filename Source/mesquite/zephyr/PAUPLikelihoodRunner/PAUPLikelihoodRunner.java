/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPLikelihoodRunner;

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

public class PAUPLikelihoodRunner extends PAUPSearchRunner {
	int numThreads = 0;

	public String getCriterionSetCommand() {
		return "set criterion=likelihood;";
	}

	public String getCriterionScoreCommand() {
		return "lscore";
	}

	IntegerField numThreadsField;
	/*.................................................................................................................*/
	public void queryOptionsSetupExtra(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		numThreadsField = dialog.addIntegerField("number of threads (use 0 for auto option)", numThreads, 8, 0, MesquiteInteger.infinite);

	}
	/*.................................................................................................................*/
	public String getPAUPCommandExtras() {
		if (MesquiteInteger.isCombinable(numThreads))
			if (numThreads>1)
				return "\tlset nthreads=" + numThreads+";\n";
			else if (numThreads==0)
				return "\tlset nthreads=auto;\n";
		return "";
	}
	/*.................................................................................................................*/
	public void queryOptionsProcessExtra(ExtensibleDialog dialog) {
		if (numThreadsField!=null)
			numThreads = numThreadsField.getValue();
//lset nthreads=n
	}
	/*.................................................................................................................*/
	public int getNumberOfThreads() {
		return numThreads;
//lset nthreads=n
	}
	
	public String getOptimalTreeAdjectiveLowerCase() {
		return "maximum likelihood";
	}

	public String getOptimalTreeAdjectiveTitleCase() {
		return "Maximum Likelihood";
	}


	public String getName() {
		return "PAUP Likelihood";
	}


}
