/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.Checkbox;
import java.awt.TextArea;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public abstract class PAUPNJRunner extends PAUPRunner {
	int bootStrapReps = 500;
	protected String paupCommands = "";

	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("NJ Analysis details: \n");
		if (StringUtil.notEmpty(paupCommands))
			appendToSearchDetails("   Pre-NJ commands: " + paupCommands);
		if (bootstrapOrJackknife()){
			appendToSearchDetails("   "+getResamplingKindName() +"\n");
			appendToSearchDetails("   "+bootStrapReps + " replicates");
		} 
}

	
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootStrapReps = MesquiteInteger.fromString(content);
		if ("searchStyle".equalsIgnoreCase(tag))
			searchStyle = MesquiteInteger.fromString(content);
		if ("paupCommands".equalsIgnoreCase(tag))
			paupCommands = StringUtil.cleanXMLEscapeCharacters(content);
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootStrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "searchStyle", searchStyle);  
		StringUtil.appendXMLTag(buffer, 2, "paupCommands", paupCommands);  
		buffer.append(super.prepareMorePreferencesForXML());
		return buffer.toString();
	}

	RadioButtons searchStyleBox;
	IntegerField bootStrapRepsField;
	TextArea paupCommandsField;
	/*.................................................................................................................*/
	public void queryOptionsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		String helpString = "\nIf bootstrap or jackknife resampling is chosen, PAUP will do a neighbor-joining bootstrap/jackknife of the number of replicates specified; otherwise, it will do a simple neighbor-joining analysis.";
		helpString+= "\nAny PAUP commands entered in the Additional commands field will be executed in PAUP immediately before the nj or bootstrap command.";
		dialog.appendToHelpString(helpString);

		dialog.addHorizontalLine(1);
		searchStyleBox = dialog.addRadioButtons(new String[] {"simple neighbor-joining", "bootstrap resampling", "jackknife resampling"}, searchStyle);
		dialog.addHorizontalLine(1);

		dialog.addLabel("Additional commands before nj or bootstrap/jackknife command: ");
		paupCommandsField =dialog.addTextAreaSmallFont(paupCommands,4);

		tabbedPanel.addPanel("Resampling Options", true);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap/Jackknife Replicates", bootStrapReps, 8, 1, MesquiteInteger.infinite);

	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		bootStrapReps = bootStrapRepsField.getValue();
		searchStyle = searchStyleBox.getValue();
		paupCommands = paupCommandsField.getText();
	}

	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data, String constraintTree){
		StringBuffer sb = new StringBuffer();
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";\n");
		sb.append("\tdset negbrlen=prohibit;\n");
		sb.append(paupCommands+ "\n");
		if (isConstrainedSearch() && StringUtil.notEmpty(constraintTree)) {
			if (useConstraintTree == BACKBONE)
				sb.append("\tconstraints constraintTree (BACKBONE) =  " + constraintTree +";" + StringUtil.lineEnding()); 
			else if (useConstraintTree == MONOPHYLY)
				sb.append("\tconstraints constraintTree (MONOPHYLY) =  " + constraintTree +";" + StringUtil.lineEnding()); 
		}
		if (bootstrapOrJackknife() && bootStrapReps>0) {
			sb.append("\tset criterion=distance;\n");
		
			if (searchStyle==BOOTSTRAPSEARCH)
				sb.append("\tboot");
			else
				sb.append("\tjack");
			if ( isConstrainedSearch())
				sb.append(" constraint=constraintTree enforce"); 
			sb.append(" nreps = " + bootStrapReps + " search=nj;\n");
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		else {
			sb.append("\tnj");
			if ( isConstrainedSearch())
				sb.append(" constraint=constraintTree enforce"); 
			sb.append(";\n");
			TaxaSelectionSet outgroupSet =null;
			if (!StringUtil.blank(outgroupTaxSetString)) {
				outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				sb.append("\n\toutgroup " + outgroupSet.getStringList(" ", null, false)+";");
			}
			sb.append("\n\troot rootmethod=outgroup outroot=paraphyl;");

			sb.append("\tsavetrees brlens=yes file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		return sb.toString();
	}
	public boolean bootstrapOrJackknife() {
		return searchStyle==BOOTSTRAPSEARCH  || searchStyle==JACKKNIFESEARCH;
	}

	public boolean doMajRuleConsensusOfResults() {
		return bootstrapOrJackknife();
	}

	public  boolean singleTreeFromResampling(){
		return true ;
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public String getName() {
		return "PAUP Neighbor Joining Analysis";
	}


}
