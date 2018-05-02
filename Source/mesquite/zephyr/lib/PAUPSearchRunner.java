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
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public abstract class PAUPSearchRunner extends PAUPRunner implements ItemListener, ConstrainedSearcherTreeScoreProvider {

	int bootStrapReps = 500;
	boolean getConsensus = false;
	String customSearchOptions = "";
	String customSearchOptionsBoot = "";

	int nreps = 10;
	IntegerField nrepsField;
	int nchuck = 25;
	IntegerField nchuckField;
	int chuckScore = 1;
	IntegerField chuckScoreField;
	Checkbox channelSearchBox;
	boolean channelSearch=false;
	
	final static int STANDARDHEURISTIC =0;
	final static int CUSTOMHEURISTIC =1;
	final static int BANDB =2;
	int searchCategory = STANDARDHEURISTIC;
	
	int bootSearchCategory=STANDARDHEURISTIC;

	
	Checkbox standardSearchBox;
	Checkbox customSearchBox;
	Checkbox branchAndBoundSearchBox;
	boolean secondarySearchNoChannel=false;
	Checkbox secondarySearchNoChannelBox;
	
	int nrepsBoot = 10;
	IntegerField nrepsBootField;
	int nchuckBoot = 25;
	IntegerField nchuckBootField;
	int chuckScoreBoot = 1;
	IntegerField chuckScoreBootField;
	Checkbox channelSearchBootBox;
	boolean channelSearchBoot=false;
	Checkbox standardSearchBootBox;
	Checkbox customSearchBootBox;
	Checkbox branchAndBoundSearchBootBox;


	JLabel bootstrapPanelLabel;
	

	int maxTrees = 100;
	IntegerField maxTreesField;
	boolean maxTreesIncrease = false;
	Checkbox maxTreesIncreaseBox;


	RadioButtons bootstrapBox;
	Checkbox getConsensusBox;
	IntegerField bootStrapRepsField;
	TextArea customSearchOptionsField, customSearchOptionsBootField;
	TextArea paupCommandsField;
	protected String paupCommands = "";

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("nreps".equalsIgnoreCase(tag))
			nreps = MesquiteInteger.fromString(content);
		if ("nchuck".equalsIgnoreCase(tag))
			nchuck = MesquiteInteger.fromString(content);
		if ("chuckScore".equalsIgnoreCase(tag))
			chuckScore = MesquiteInteger.fromString(content);
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootStrapReps = MesquiteInteger.fromString(content);
		if ("searchStyle".equalsIgnoreCase(tag))
			searchStyle = MesquiteInteger.fromString(content);
		if ("searchMethod".equalsIgnoreCase(tag))
			searchMethod = MesquiteInteger.fromString(content);
		if ("getConsensus".equalsIgnoreCase(tag))
			getConsensus = MesquiteBoolean.fromTrueFalseString(content);
		if ("customSearchOptionsBoot".equalsIgnoreCase(tag))
			customSearchOptionsBoot = StringUtil.cleanXMLEscapeCharacters(content);
		if ("customSearchOptions".equalsIgnoreCase(tag))
			customSearchOptions = StringUtil.cleanXMLEscapeCharacters(content);
		if ("paupCommands".equalsIgnoreCase(tag))
			paupCommands = StringUtil.cleanXMLEscapeCharacters(content);
		if ("searchCategory".equalsIgnoreCase(tag))
			searchCategory = MesquiteInteger.fromString(content);
		if ("secondarySearchNoChannel".equalsIgnoreCase(tag))
			secondarySearchNoChannel = MesquiteBoolean.fromTrueFalseString(content);
		if ("channelSearch".equalsIgnoreCase(tag))
			channelSearch = MesquiteBoolean.fromTrueFalseString(content);
		if ("maxTreesIncrease".equalsIgnoreCase(tag))
			maxTreesIncrease = MesquiteBoolean.fromTrueFalseString(content);
		if ("maxTrees".equalsIgnoreCase(tag))
			maxTrees = MesquiteInteger.fromString(content);

		
		if ("nrepsBoot".equalsIgnoreCase(tag))
			nrepsBoot = MesquiteInteger.fromString(content);
		if ("nchuckBoot".equalsIgnoreCase(tag))
			nchuckBoot = MesquiteInteger.fromString(content);
		if ("chuckScoreBoot".equalsIgnoreCase(tag))
			chuckScoreBoot = MesquiteInteger.fromString(content);
		if ("bootSearchCategory".equalsIgnoreCase(tag))
			bootSearchCategory = MesquiteInteger.fromString(content);
		if ("channelSearchBoot".equalsIgnoreCase(tag))
			channelSearchBoot = MesquiteBoolean.fromTrueFalseString(content);

		
		
		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootStrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "searchStyle", searchStyle);  
		StringUtil.appendXMLTag(buffer, 2, "searchMethod", searchMethod);  
		StringUtil.appendXMLTag(buffer, 2, "getConsensus", getConsensus);  
		StringUtil.appendXMLTag(buffer, 2, "customSearchOptions", customSearchOptions);  
		StringUtil.appendXMLTag(buffer, 2, "customSearchOptionsBoot", customSearchOptionsBoot);  
		StringUtil.appendXMLTag(buffer, 2, "paupCommands", paupCommands);  
		
		StringUtil.appendXMLTag(buffer, 2, "nreps", nreps);  
		StringUtil.appendXMLTag(buffer, 2, "nchuck", nchuck);  
		StringUtil.appendXMLTag(buffer, 2, "chuckScore", chuckScore);  
		StringUtil.appendXMLTag(buffer, 2, "channelSearch", channelSearch);  
		StringUtil.appendXMLTag(buffer, 2, "searchCategory", searchCategory);  
		StringUtil.appendXMLTag(buffer, 2, "secondarySearchNoChannel", secondarySearchNoChannel);  
		
		StringUtil.appendXMLTag(buffer, 2, "nrepsBoot", nrepsBoot);  
		StringUtil.appendXMLTag(buffer, 2, "nchuckBoot", nchuckBoot);  
		StringUtil.appendXMLTag(buffer, 2, "chuckScoreBoot", chuckScoreBoot);  
		StringUtil.appendXMLTag(buffer, 2, "channelSearchBoot", channelSearchBoot);  
		StringUtil.appendXMLTag(buffer, 2, "bootSearchCategory", bootSearchCategory);  
		
		
		
		
		StringUtil.appendXMLTag(buffer, 2, "maxTreesIncrease", maxTreesIncrease);  
		StringUtil.appendXMLTag(buffer, 2, "maxTrees", maxTrees);  
		
		
		
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
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";" + StringUtil.lineEnding());
		sb.append("\t"+ getCriterionSetCommand() +  StringUtil.lineEnding());
		sb.append("\tset maxtrees=" + maxTrees + " increase=");
		if (maxTreesIncrease)
			sb.append("auto;" + StringUtil.lineEnding());
		else
			sb.append("no;" + StringUtil.lineEnding());
		if (isConstrainedSearch() && StringUtil.notEmpty(constraintTree)) {
			if (useConstraintTree == BACKBONE)
				sb.append("\tconstraints constraintTree (BACKBONE) =  " + constraintTree +";" + StringUtil.lineEnding()); 
			else if (useConstraintTree == MONOPHYLY)
				sb.append("\tconstraints constraintTree (MONOPHYLY) =  " + constraintTree +";" + StringUtil.lineEnding()); 
		}
		
		
		if (bootstrapOrJackknife()) {  //bootstrap or jackknife
			
			String defaults = "";
			if (bootSearchCategory==STANDARDHEURISTIC){
				defaults+="\ths addseq=random nreps=" + nrepsBoot;
				if ( isConstrainedSearch())
					defaults+=" constraint=constraintTree enforce"; 
				if (channelSearchBoot && chuckScoreBoot>0 && nchuckBoot>0)
					defaults+=" chuckscore=" + chuckScoreBoot + " nchuck="+nchuckBoot;

			} else if (bootSearchCategory==CUSTOMHEURISTIC) {
				defaults+=customSearchOptions;
				if ( isConstrainedSearch())
					defaults+=" constraint=constraintTree enforce"; 
			} else if (bootSearchCategory==BANDB) {
				if ( isConstrainedSearch())
					defaults+=" bandb constraint=constraintTree enforce"; 
			}
			if (StringUtil.notEmpty(defaults)) {
				sb.append("\tdefaults ");
				sb.append(defaults);
				sb.append(";" + StringUtil.lineEnding());
			}

			sb.append("\t"+paupCommands + StringUtil.lineEnding());
			sb.append(getPAUPCommandExtras());
			if (searchStyle==BOOTSTRAPSEARCH) 
				sb.append("\tboot");
			else if (searchStyle==JACKKNIFESEARCH) 
				sb.append("\tjack");
			sb.append(" nreps = " + bootStrapReps);
			if (bootSearchCategory==BANDB) 
				sb.append(" search=bandb;" + StringUtil.lineEnding());
			else 
				sb.append(" search=heuristic;" + StringUtil.lineEnding());
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";" + StringUtil.lineEnding());
		}
		else {  //regular search
			sb.append("\t"+paupCommands+ StringUtil.lineEnding());
			sb.append(getPAUPCommandExtras());
			if (searchCategory==STANDARDHEURISTIC){
				sb.append("\ths addseq=random writecurtree nreps=" + nreps);
				if (isConstrainedSearch())
					sb.append(" constraint=constraintTree enforce"); 
				if (channelSearch && chuckScore>0 && nchuck>0)
					sb.append(" chuckscore=" + chuckScore + " nchuck="+nchuck);
				sb.append(" rstatus;" + StringUtil.lineEnding());
				if (secondarySearchNoChannel)
					sb.append("\ths start=current chuckscore=0 nchuck=0;");

			} else if (searchCategory==CUSTOMHEURISTIC){
				sb.append("\t" + customSearchOptions  + StringUtil.lineEnding());
			} else if (searchCategory==BANDB) {
				sb.append("\tbandb;" + StringUtil.lineEnding());
			}
			sb.append("\t"+ getCriterionScoreCommand() + " 1 / scorefile=" + StringUtil.tokenize(scoreFileName) + " replace;" + StringUtil.lineEnding());
			if (getConsensus)
				sb.append("\n\tcontree all/strict=yes treefile=" + StringUtil.tokenize(outputTreeFileName) + ";" + StringUtil.lineEnding());
			else {
				if (allowRerooting()) {
					TaxaSelectionSet outgroupSet =null;
					if (!StringUtil.blank(outgroupTaxSetString)) {
						outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
						sb.append("\n\toutgroup " + outgroupSet.getStringList(" ", null, false)+";");
					}
					sb.append("\n\troot rootmethod=outgroup outroot=paraphyl;");
				}
				sb.append("\n\tsavetrees file=" + StringUtil.tokenize(outputTreeFileName));
				if (allowRerooting())
					sb.append(" root ");
				sb.append(" brLens;" + StringUtil.lineEnding());
			}
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
	int numTreesFound=0;
	/*.................................................................................................................*/
		public void appendAdditionalSearchDetails() {
			appendToSearchDetails("Search details: \n");
			if (bootstrapOrJackknife()){
				appendToSearchDetails("   "+getResamplingKindName() +"\n");
				appendToSearchDetails("   "+bootStrapReps + " replicates");
			} else {
				appendToSearchDetails("   Search for " + getOptimalTreeAdjectiveLowerCase() + " trees\n");
				if (MesquiteInteger.isCombinable(numTreesFound))
					appendToSearchDetails("\n   Number of trees found: "+numTreesFound+"\n");
			}
	}

	/*.................................................................................................................*/
	void adjustDialogText(int searchCategory, boolean bootstrap) {
		if (!bootstrap) {
			if (channelSearchBox!=null){
				channelSearchBox.setEnabled(searchCategory==STANDARDHEURISTIC);
			}
		} else {

			if (channelSearchBootBox!=null){
				channelSearchBootBox.setEnabled(bootSearchCategory==STANDARDHEURISTIC);
			}
		}
	}

	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (dialog!=null) {
			if (arg0.getItemSelectable()==standardSearchBox || arg0.getItemSelectable()==customSearchBox || arg0.getItemSelectable()==branchAndBoundSearchBox){
				if (arg0.getItemSelectable()==standardSearchBox && standardSearchBox!=null && standardSearchBox.getState())
					adjustDialogText(STANDARDHEURISTIC, false);
				 if (arg0.getItemSelectable()==customSearchBox && customSearchBox!=null && customSearchBox.getState())
					adjustDialogText(CUSTOMHEURISTIC, false);
				 if (arg0.getItemSelectable()==branchAndBoundSearchBox && branchAndBoundSearchBox!=null && branchAndBoundSearchBox.getState())
					adjustDialogText(BANDB, false);
			}
			else if (arg0.getItemSelectable()==standardSearchBootBox || arg0.getItemSelectable()==customSearchBootBox || arg0.getItemSelectable()==branchAndBoundSearchBootBox){
				if (arg0.getItemSelectable()==standardSearchBootBox && standardSearchBootBox!=null && standardSearchBootBox.getState())
					adjustDialogText(STANDARDHEURISTIC, true);
				 if (arg0.getItemSelectable()==customSearchBootBox && customSearchBootBox!=null && customSearchBootBox.getState())
					adjustDialogText(CUSTOMHEURISTIC, true);
				 if (arg0.getItemSelectable()==branchAndBoundSearchBootBox && branchAndBoundSearchBootBox!=null && branchAndBoundSearchBootBox.getState())
					adjustDialogText(BANDB, true);
			}
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
		paupCommandsField =dialog.addTextAreaSmallFont(paupCommands,4);
		//dialog.addCheckBox("bootstrap", doBootstrap);
		maxTreesField = dialog.addIntegerField("Maximum number of trees (maxtrees)", maxTrees, 8, 1, MesquiteInteger.infinite);
		maxTreesIncreaseBox= dialog.addCheckBox("increase maxtrees if needed", maxTreesIncrease);

		queryOptionsSetupExtra(dialog, tabbedPanel);
		
		tabbedPanel.addPanel("Regular Searches", true);
		
		
		CheckboxGroup searchGroup = new CheckboxGroup();
		dialog.addHorizontalLine(1);
		standardSearchBox = dialog.addCheckBox("pre-built heuristic search", searchCategory==STANDARDHEURISTIC);
		standardSearchBox.addItemListener(this);
		standardSearchBox.setCheckboxGroup(searchGroup);
		
		nrepsField = dialog.addIntegerField("Number of search replicates", nreps, 8, 1, MesquiteInteger.infinite);
		
		channelSearchBox = dialog.addCheckBox("channeled search", channelSearch);
		nchuckField = dialog.addIntegerField("Save no more than", nchuck, 8, 1, MesquiteInteger.infinite);
		dialog.suppressNewPanel();
		chuckScoreField = dialog.addIntegerField("trees of length greater than or equal to", chuckScore, 8, 1, MesquiteInteger.infinite);
		secondarySearchNoChannelBox = dialog.addCheckBox("subsequent unchanneled search", secondarySearchNoChannel);
		
		dialog.addHorizontalLine(1);
		customSearchBox = dialog.addCheckBox("custom heuristic search", searchCategory==CUSTOMHEURISTIC);
		customSearchBox.addItemListener(this);
		customSearchBox.setCheckboxGroup(searchGroup);

		dialog.addLabel("Custom Search Commands");
		customSearchOptionsField = dialog.addTextAreaSmallFont(customSearchOptions, 4,60);
		
		dialog.addHorizontalLine(1);
		branchAndBoundSearchBox = dialog.addCheckBox("branch and bound search", searchCategory==BANDB);
		branchAndBoundSearchBox.addItemListener(this);
		branchAndBoundSearchBox.setCheckboxGroup(searchGroup);

		dialog.addHorizontalLine(1);
		
		getConsensusBox = dialog.addCheckBox("only read in strict consensus", getConsensus);

		
		
		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Resampled Searches", true);
			bootStrapRepsField = dialog.addIntegerField("Bootstrap/Jackknife Replicates", bootStrapReps, 8, 1, MesquiteInteger.infinite);

			bootstrapPanelLabel = dialog.addLabel("Below are the search commands for each replicate: ", Label.LEFT, true, false);
			CheckboxGroup searchGroupBoot = new CheckboxGroup();
			dialog.addHorizontalLine(1);
			standardSearchBootBox = dialog.addCheckBox("pre-built heuristic search", bootSearchCategory==STANDARDHEURISTIC);
			standardSearchBootBox.setCheckboxGroup(searchGroupBoot);

			nrepsBootField = dialog.addIntegerField("Number of search replicates", nrepsBoot, 8, 1, MesquiteInteger.infinite);

			channelSearchBootBox = dialog.addCheckBox("channeled search", channelSearchBoot);
			nchuckBootField = dialog.addIntegerField("Save no more than", nchuckBoot, 8, 1, MesquiteInteger.infinite);
			dialog.suppressNewPanel();
			chuckScoreBootField = dialog.addIntegerField("trees of length greater than or equal to", chuckScoreBoot, 8, 1, MesquiteInteger.infinite);

			dialog.addHorizontalLine(1);
			customSearchBootBox = dialog.addCheckBox("custom heuristic search", bootSearchCategory==CUSTOMHEURISTIC);
			customSearchBootBox.addItemListener(this);
			customSearchBootBox.setCheckboxGroup(searchGroupBoot);

			
			dialog.addLabel("Custom Search Commands");
			customSearchOptionsBootField = dialog.addTextAreaSmallFont(customSearchOptionsBoot, 4,60);

			dialog.addHorizontalLine(1);
			branchAndBoundSearchBootBox = dialog.addCheckBox("branch and bound search", bootSearchCategory==BANDB);
			branchAndBoundSearchBootBox.addItemListener(this);
			branchAndBoundSearchBootBox.setCheckboxGroup(searchGroupBoot);

			dialog.addHorizontalLine(1);
			dialog.addLabel("(To conduct resampling, Bootstrap or Jackknife must be selected in the General panel) ", Label.LEFT, true, true);
		}

		
		adjustDialogText(searchCategory, false);	
		adjustDialogText(bootSearchCategory, true);	


	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		if (bootstrapAllowed) {
			bootStrapReps = bootStrapRepsField.getValue();
			searchStyle = bootstrapBox.getValue();
			customSearchOptionsBoot = customSearchOptionsBootField.getText();
		}
		getConsensus = getConsensusBox.getState();
		customSearchOptions = customSearchOptionsField.getText();
		paupCommands = paupCommandsField.getText();
		
		queryOptionsProcessExtra(dialog);
		
		nreps=nrepsField.getValue();
		nchuck=nchuckField.getValue();
		chuckScore=chuckScoreField.getValue();
		channelSearch=channelSearchBox.getState();
		if (standardSearchBox.getState())
			searchCategory=STANDARDHEURISTIC;
		else if (customSearchBox.getState())
			searchCategory=CUSTOMHEURISTIC;
		else if (branchAndBoundSearchBox.getState())
			searchCategory=BANDB;
		secondarySearchNoChannel = secondarySearchNoChannelBox.getState();
		
		if (bootstrapAllowed) {
			nrepsBoot=nrepsBootField.getValue();
			nchuckBoot=nchuckBootField.getValue();
			chuckScoreBoot=chuckScoreBootField.getValue();
			channelSearchBoot=channelSearchBootBox.getState();
			if (standardSearchBootBox.getState())
				bootSearchCategory=STANDARDHEURISTIC;
			else if (customSearchBootBox.getState())
				bootSearchCategory=CUSTOMHEURISTIC;
			else if (branchAndBoundSearchBootBox.getState())
				bootSearchCategory=BANDB;
		}
		
		maxTrees = maxTreesField.getValue();
		maxTreesIncrease = maxTreesIncreaseBox.getState();

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

	public abstract String getCriterionSetCommand() ;
	public abstract String getCriterionScoreCommand() ;


	public String getOptimalTreeAdjectiveLowerCase() {
		return "optimal";
	}

	public String getOptimalTreeAdjectiveTitleCase() {
		return "Optimal";
	}




}
