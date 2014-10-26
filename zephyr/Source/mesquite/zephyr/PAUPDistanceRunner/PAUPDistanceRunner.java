/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPDistanceRunner;

import java.awt.Checkbox;
import java.awt.TextArea;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public class PAUPDistanceRunner extends PAUPRunner {
	int bootStrapReps = 500;
	boolean doBootstrap = false;
	protected String paupCommands = "";

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootStrapReps = MesquiteInteger.fromString(content);
		if ("bootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("paupCommands".equalsIgnoreCase(tag))
			paupCommands = StringUtil.cleanXMLEscapeCharacters(content);
	}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootStrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "bootstrap", doBootstrap);  
		StringUtil.appendXMLTag(buffer, 2, "paupCommands", paupCommands);  
		return buffer.toString();
	}

	Checkbox bootstrapBox;
	IntegerField bootStrapRepsField;
	TextArea paupCommandsField;
	/*.................................................................................................................*/
	public void queryOptionsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		String helpString = "\nIf \"bootstrap\" is on, the PAUP will do a neighbor-joining bootstrap of the number of replicates specified; otherwise, it will do a simple neighbor-joining analysis.";
		helpString+= "\nAny PAUP commands entered in the Additional Commands field will be executed in PAUP immediately before the nj or bootstrap command.";
		dialog.appendToHelpString(helpString);

		dialog.addLabel("Additional commands before nj or bootstrap command: ");
		paupCommandsField =dialog.addTextAreaSmallFont(paupCommands,4);

		tabbedPanel.addPanel("Bootstrap", true);
		bootstrapBox = dialog.addCheckBox("bootstrap", doBootstrap);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Reps", bootStrapReps, 8, 1, MesquiteInteger.infinite);

	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		bootStrapReps = bootStrapRepsField.getValue();
		doBootstrap = bootstrapBox.getState();
		paupCommands = paupCommandsField.getText();
	}

	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data){
		StringBuffer sb = new StringBuffer();
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";\n");
		sb.append("\tset criterion=distance;\n");
		sb.append("\tdset negbrlen=prohibit;\n");
		if (data instanceof DNAData)
			sb.append("\tdset distance=hky85;\n");
		sb.append(paupCommands+ "\n");
		if (doBootstrap && bootStrapReps>0) {
			sb.append("\tboot nreps = " + bootStrapReps + " search=nj;\n");
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		else {
			sb.append("\tnj;\n");
			sb.append("\tsavetrees brlens=yes file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		return sb.toString();
	}
	public boolean bootstrapOrJackknife() {
		return doBootstrap;
	}

	public boolean doMajRuleConsensusOfResults() {
		return doBootstrap;
	}


	public String getName() {
		return "PAUP* Distance Analysis";
	}


}
