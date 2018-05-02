/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.PAUPSVDRunner;

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

public  class PAUPSVDRunner extends PAUPRunner implements ConstrainedSearcher {

	int bootStrapReps = 500;

	JLabel bootstrapPanelLabel;

	RadioButtons bootstrapBox;
	IntegerField bootStrapRepsField;
	TextArea paupPreSearchCommandsField,paupExtraSVDCommandsField;
	protected String paupPreSearchCommands = "";
	protected String paupExtraSVDCommands = "";

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootStrapReps = MesquiteInteger.fromString(content);
		if ("searchStyle".equalsIgnoreCase(tag))
			searchStyle = MesquiteInteger.fromString(content);
		if ("searchMethod".equalsIgnoreCase(tag))
			searchMethod = MesquiteInteger.fromString(content);
		if ("paupPreSearchCommands".equalsIgnoreCase(tag))
			paupPreSearchCommands = StringUtil.cleanXMLEscapeCharacters(content);
		if ("paupExtraSVDCommands".equalsIgnoreCase(tag))
			paupExtraSVDCommands = StringUtil.cleanXMLEscapeCharacters(content);
		
		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootStrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "searchStyle", searchStyle);  
		StringUtil.appendXMLTag(buffer, 2, "searchMethod", searchMethod);  
		StringUtil.appendXMLTag(buffer, 2, "paupPreSearchCommands", paupPreSearchCommands);  
		StringUtil.appendXMLTag(buffer, 2, "paupExtraSVDCommands", paupExtraSVDCommands);  
		
		preferencesSet = true;

	return buffer.toString();
	}
	
	/*.................................................................................................................*/
	public boolean allowRerooting() {
		return true;
	}
	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data, String constraintTree){
		StringBuffer sb = new StringBuffer();
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";\n");
		boolean enforceConstraint = false;
		if (isConstrainedSearch() && StringUtil.notEmpty(constraintTree)) {
			if (useConstraintTree == BACKBONE)
				sb.append("\tconstraints constraintTree (BACKBONE) =  " + constraintTree +";\n"); 
			else if (useConstraintTree == MONOPHYLY)
				sb.append("\tconstraints constraintTree (MONOPHYLY) =  " + constraintTree +";\n"); 
			enforceConstraint = true;
			sb.append("\tshowconstr constraintTree;\n"); 

		}
		
		
		if (bootstrapOrJackknife()) {  //bootstrap or jackknife
			
			String defaults = "";
			if (StringUtil.notEmpty(defaults)) {
				sb.append("\tdefaults ");
				sb.append(defaults);
				sb.append(";\n");
			}

			sb.append("\t"+paupPreSearchCommands+"\n");
			sb.append("\tsvdquartets ");
			if (enforceConstraint)
				sb.append(" constraints=constraintTree enforce ");
			sb.append(" bootstrap=standard nreps = " + bootStrapReps + " ");
			if (StringUtil.notEmpty(paupExtraSVDCommands))
				sb.append(paupExtraSVDCommands);
			sb.append(";\n");

			
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		else {  //regular search
			sb.append("\t"+paupPreSearchCommands+"\n");
			sb.append(getPAUPCommandExtras());
			sb.append("\tsvdquartets ");
			if (enforceConstraint)
				sb.append(" constraints=constraintTree enforce ");
			if (StringUtil.notEmpty(paupExtraSVDCommands))
				sb.append(paupExtraSVDCommands);
			sb.append(";\n");

				if (allowRerooting())
					sb.append("\n\troot rootmethod=outgroup outroot=paraphyl;");
				sb.append("\n\tsavetrees file=" + StringUtil.tokenize(outputTreeFileName));
				if (allowRerooting())
					sb.append(" root ");
				sb.append(" brLens;\n");
		}
		return sb.toString();
	}
	
	
	 /*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+=getResamplingKindName()+"<br>";
			}
			else {
				s+="Search for " + getOptimalTreeAdjectiveLowerCase() + " trees<br>";
			}
			s+="</b>";
		}
		return s;
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
			appendToSearchDetails("Search details: \n");
			if (bootstrapOrJackknife()){
				appendToSearchDetails("   "+getResamplingKindName() +"\n");
				appendToSearchDetails("   "+bootStrapReps + " replicates");
			} else {
				appendToSearchDetails("   Search for " + getOptimalTreeAdjectiveLowerCase() + " trees\n");
			}
	}


	/*.................................................................................................................*/
	public String getPAUPCommandExtras() {
		return "";
	}
	/*.................................................................................................................*/
	public void queryOptionsSetupExtra(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
	}
	/*.................................................................................................................*/
	public void queryOptionsProcessExtra(ExtensibleDialog dialog) {
	}

	/*.................................................................................................................*/
	public void queryOptionsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
//		String helpString = "\nIf \"bootstrap\" is on, the PAUP will do a bootstrap of the number of replicates specified; otherwise, it will do an heuristic search.";
//		helpString+= "\nAny PAUP commands entered in the Additional Commands field will be executed in PAUP immediately before the bootstrap or hs command.";
//		dialog.appendToHelpString(helpString);

		if (bootstrapAllowed) {
			dialog.addHorizontalLine(1);
			bootstrapBox = dialog.addRadioButtons(new String[] {"regular search", "bootstrap resampling", "jackknife resampling"}, searchStyle);
		}
		dialog.addHorizontalLine(1);
		
		dialog.addLabel("Additional commands before search command: ");
		paupPreSearchCommandsField =dialog.addTextAreaSmallFont(paupPreSearchCommands,4);
		//dialog.addCheckBox("bootstrap", doBootstrap);

		queryOptionsSetupExtra(dialog, tabbedPanel);
		
		dialog.addLabel("Additional options within the svdquartets command: ");
		paupExtraSVDCommandsField =dialog.addTextAreaSmallFont(paupExtraSVDCommands,4);
		
		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Resampled Searches", true);
			bootStrapRepsField = dialog.addIntegerField("Bootstrap/Jackknife Replicates", bootStrapReps, 8, 1, MesquiteInteger.infinite);

			dialog.addLabel("(To conduct resampling, Bootstrap or Jackknife must be selected in the General panel) ", Label.LEFT, true, true);
		}

	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		if (bootstrapAllowed) {
			bootStrapReps = bootStrapRepsField.getValue();
			searchStyle = bootstrapBox.getValue();
		}
		paupPreSearchCommands = paupPreSearchCommandsField.getText();
		paupExtraSVDCommands = paupExtraSVDCommandsField.getText();
		queryOptionsProcessExtra(dialog);
		
	}
	public boolean bootstrapOrJackknife() {
		if (!bootstrapAllowed)
			return false;
		return searchStyle==BOOTSTRAPSEARCH || searchStyle==JACKKNIFESEARCH;
	}

	public boolean doMajRuleConsensusOfResults() {
		return false;
	}

	public boolean singleTreeFromResampling() {
		return true;
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public String getOptimalTreeAdjectiveLowerCase() {
		return "optimal";
	}

	public String getOptimalTreeAdjectiveTitleCase() {
		return "Optimal";
	}
	

	public String getName() {
		return "PAUP SVD Quartets";
	}




}
