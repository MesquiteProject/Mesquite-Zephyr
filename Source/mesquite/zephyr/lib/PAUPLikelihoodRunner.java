/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public abstract class PAUPLikelihoodRunner extends PAUPSearchRunner implements ItemListener {
	int numThreads = 0;
	protected RadioButtons charPartitionButtons = null;
	protected boolean autoModels = false;
	protected Checkbox autoModelsCheckBox;

	public String getCriterionSetCommand() {
		return "set criterion=likelihood;";
	}

	public String getCriterionScoreCommand() {
		return "lscore";
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("autoModels".equalsIgnoreCase(tag))
			autoModels = MesquiteBoolean.fromTrueFalseString(content);
		if ("partitionScheme".equalsIgnoreCase(tag))
			partitionScheme = MesquiteInteger.fromString(content);
	//	if ("paupExtraSVDCommands".equalsIgnoreCase(tag))
	//		paupExtraSVDCommands = StringUtil.cleanXMLEscapeCharacters(content);
		
		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "autoModels", autoModels);  
		StringUtil.appendXMLTag(buffer, 2, "partitionScheme", partitionScheme);  
		
		preferencesSet = true;

	return buffer.toString();
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == charPartitionButtons){
			adjustAutoModelsCheckBox();
		} else
			super.itemStateChanged(e);
	}

	/*.................................................................................................................*/
	private void adjustAutoModelsCheckBox() {
		if (partitionScheme  == noPartition)
			autoModelsCheckBox.setLabel("Infer best character evolution models");
		else 
			autoModelsCheckBox.setLabel("Infer best character evolution partition and models");
	}
	/*.................................................................................................................*/
	public void queryExtraPanelsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		tabbedPanel.addPanel("Character Models", true);
		if (!data.hasCharacterGroups()) {
			if (partitionScheme == partitionByCharacterGroups)
				partitionScheme = noPartition;
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			if (partitionScheme == partitionByCodonPosition)
				partitionScheme = noPartition;
		}
		if (data instanceof ProteinData)
			charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups" }, partitionScheme);
		else
			charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups","use codon positions" }, partitionScheme);

		charPartitionButtons.addItemListener(this);
		if (!data.hasCharacterGroups()) {
			charPartitionButtons.setEnabled(1, false);
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			charPartitionButtons.setEnabled(2, false);
		}
		if (partitionScheme  == noPartition)
			autoModelsCheckBox = dialog.addCheckBox("Infer best character evolution models", autoModels);
		else 
			autoModelsCheckBox = dialog.addCheckBox("Infer best character evolution partition and models", autoModels);
		autoModelsCheckBox.addItemListener(this);
	}
	/*.................................................................................................................*/
	public void queryExtraPanelsProcess(ExtensibleDialog dialog) {
		partitionScheme = charPartitionButtons.getValue();
		autoModels = autoModelsCheckBox.getState();
	}

	IntegerField numThreadsField;
	/*.................................................................................................................*/
	public void queryOptionsSetupExtra(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		numThreadsField = dialog.addIntegerField("number of threads (use 0 for auto option)", numThreads, 8, 0, MesquiteInteger.infinite);

	}

	/*.................................................................................................................*/
	public String getPAUPAutoPartitionCommands() {
		
		StringBuffer sb = new StringBuffer();
		sb.append("\tdset distance=logdet; \n\tnj;\n");
		if (partitionScheme  == noPartition)
			sb.append("\tautoModel;\n");
		else 
			sb.append("\tautoPartition partition=currentPartition;\n\tlset mpartition=autopart;\n");
		sb.append("\tset criterion=likelihood;\n");
		sb.append("\tlscore 1; \n\tlset model=estModel fixAllParams;\n");
		sb.append("\ths start=current swap=nni;\n");
		sb.append("\tlset  estAllParams; \n\tlscore 1; \n\tlset fixAllParams;\n");
		
		return sb.toString();
	}
	/*.................................................................................................................*/
	public String getPAUPCommandExtras() {
		String s = "";
		if (MesquiteInteger.isCombinable(numThreads))
			if (numThreads>1)
				s= "\tlset nthreads=" + numThreads+";\n"+s;
			else if (numThreads==0)
				s= "\tlset nthreads=auto;\n"+s;
		if (autoModels)
			s+=getPAUPAutoPartitionCommands();
	return s;
	}
	/*.................................................................................................................*/
	public void queryOptionsProcessExtra(ExtensibleDialog dialog) {
		if (numThreadsField!=null)
			numThreads = numThreadsField.getValue();
	}
	/*.................................................................................................................*/
	public int getNumberOfThreads() {
		return numThreads;
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
