/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPParsimonyRunner;

import java.awt.Checkbox;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public class PAUPParsimonyRunner extends PAUPRunner {

	int bootStrapReps = 500;
	boolean doBootstrap = false;
	boolean getConsensus = false;
	String hsOptions = "";

	Checkbox bootstrapBox;
	Checkbox getConsensusBox;
	IntegerField bootStrapRepsField;
	SingleLineTextField hsOptionsField;
	TextArea paupCommandsField;
	protected String paupCommands = "";

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootStrapReps = MesquiteInteger.fromString(content);
		if ("bootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("getConsensus".equalsIgnoreCase(tag))
			getConsensus = MesquiteBoolean.fromTrueFalseString(content);
		if ("hsOptions".equalsIgnoreCase(tag))
			hsOptions = StringUtil.cleanXMLEscapeCharacters(content);
		if ("paupCommands".equalsIgnoreCase(tag))
			paupCommands = StringUtil.cleanXMLEscapeCharacters(content);
}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootStrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "bootstrap", doBootstrap);  
		StringUtil.appendXMLTag(buffer, 2, "getConsensus", getConsensus);  
		StringUtil.appendXMLTag(buffer, 2, "hsOptions", hsOptions);  
		StringUtil.appendXMLTag(buffer, 2, "paupCommands", paupCommands);  
	return buffer.toString();
	}
	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data){
		StringBuffer sb = new StringBuffer();
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";\n");
		sb.append("\tset criterion=parsimony;\n");
		if (bootstrapOrJackknife()) {
			sb.append("\tdefaults hs " + hsOptions + ";\n");
			sb.append(paupCommands+"\n");
			sb.append("\tboot nreps = " + bootStrapReps + " search=heuristic;\n");
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		else {
			sb.append(paupCommands+"\n");
			sb.append("\ths " + hsOptions + ";\n");
			if (getConsensus)
				sb.append("\tcontree all/strict=yes treefile=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
			else
				sb.append("\tsavetrees file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		return sb.toString();
	}

	/*.................................................................................................................*/
	public void queryOptionsSetup(ExtensibleDialog dialog) {
		String helpString = "\nIf \"bootstrap\" is on, the PAUP will do a parsimony bootstrap of the number of replicates specified; otherwise, it will do a parsimony heuristic search.";
		helpString+= "\nAny PAUP commands entered in the Additional Commands field will be executed in PAUP immediately before the bootstrap or hs command.";
		dialog.appendToHelpString(helpString);

		hsOptionsField = dialog.addTextField("Heuristic search options:", hsOptions, 24);
		getConsensusBox = dialog.addCheckBox("only read in consensus", getConsensus);

		bootstrapBox = dialog.addCheckBox("bootstrap", doBootstrap);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Reps", bootStrapReps, 8, 1, MesquiteInteger.infinite);
		
		dialog.addLabel("Additional commands before hs or bootstrap command: ");
		paupCommandsField =dialog.addTextAreaSmallFont(paupCommands,4);

	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		bootStrapReps = bootStrapRepsField.getValue();
		doBootstrap = bootstrapBox.getState();
		getConsensus = getConsensusBox.getState();
		hsOptions = hsOptionsField.getText();
		paupCommands = paupCommandsField.getText();
	}
	public boolean bootstrapOrJackknife() {
		return doBootstrap;
	}

	public boolean doMajRuleConsensusOfResults() {
		return false;
	}


	public String getName() {
		return "PAUP* Parsimony Analysis";
	}


}
