/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

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

public class PAUPParsimonyRunner extends PAUPRunner implements ItemListener {

	int bootStrapReps = 500;
	boolean getConsensus = false;
	String customSearchOptions = "";
	String customSearchOptionsBoot = "";
	int searchStyle = REGULARSEARCH;

	int nreps = 10;
	IntegerField nrepsField;
	int nchuck = 25;
	IntegerField nchuckField;
	int chuckScore = 1;
	IntegerField chuckScoreField;
	Checkbox channelSearchBox;
	boolean channelSearch=false;
	boolean standardSearch=true;
	Checkbox standardSearchBox;
	Checkbox customSearchBox;
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
	boolean standardSearchBoot=true;
	Checkbox standardSearchBootBox;
	Checkbox customSearchBootBox;


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
		if ("getConsensus".equalsIgnoreCase(tag))
			getConsensus = MesquiteBoolean.fromTrueFalseString(content);
		if ("customSearchOptionsBoot".equalsIgnoreCase(tag))
			customSearchOptionsBoot = StringUtil.cleanXMLEscapeCharacters(content);
		if ("customSearchOptions".equalsIgnoreCase(tag))
			customSearchOptions = StringUtil.cleanXMLEscapeCharacters(content);
		if ("paupCommands".equalsIgnoreCase(tag))
			paupCommands = StringUtil.cleanXMLEscapeCharacters(content);
		if ("standardSearch".equalsIgnoreCase(tag))
			standardSearch = MesquiteBoolean.fromTrueFalseString(content);
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
		if ("standardSearchBoot".equalsIgnoreCase(tag))
			standardSearchBoot = MesquiteBoolean.fromTrueFalseString(content);
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
		StringUtil.appendXMLTag(buffer, 2, "getConsensus", getConsensus);  
		StringUtil.appendXMLTag(buffer, 2, "customSearchOptions", customSearchOptions);  
		StringUtil.appendXMLTag(buffer, 2, "customSearchOptionsBoot", customSearchOptionsBoot);  
		StringUtil.appendXMLTag(buffer, 2, "paupCommands", paupCommands);  
		
		StringUtil.appendXMLTag(buffer, 2, "nreps", nreps);  
		StringUtil.appendXMLTag(buffer, 2, "nchuck", nchuck);  
		StringUtil.appendXMLTag(buffer, 2, "chuckScore", chuckScore);  
		StringUtil.appendXMLTag(buffer, 2, "channelSearch", channelSearch);  
		StringUtil.appendXMLTag(buffer, 2, "standardSearch", standardSearch);  
		StringUtil.appendXMLTag(buffer, 2, "secondarySearchNoChannel", secondarySearchNoChannel);  
		
		StringUtil.appendXMLTag(buffer, 2, "nrepsBoot", nrepsBoot);  
		StringUtil.appendXMLTag(buffer, 2, "nchuckBoot", nchuckBoot);  
		StringUtil.appendXMLTag(buffer, 2, "chuckScoreBoot", chuckScoreBoot);  
		StringUtil.appendXMLTag(buffer, 2, "channelSearchBoot", channelSearchBoot);  
		StringUtil.appendXMLTag(buffer, 2, "standardSearchBoot", standardSearchBoot);  
		
		
		
		
		StringUtil.appendXMLTag(buffer, 2, "maxTreesIncrease", maxTreesIncrease);  
		StringUtil.appendXMLTag(buffer, 2, "maxTrees", maxTrees);  
		
		
		
		preferencesSet = true;

	return buffer.toString();
	}
	/*.................................................................................................................*/
	public String getPAUPCommandFileMiddle(String dataFileName, String outputTreeFileName, CategoricalData data){
		StringBuffer sb = new StringBuffer();
		sb.append("\texec " + StringUtil.tokenize(dataFileName) + ";\n");
		sb.append("\tset criterion=parsimony ;\n");
		sb.append("\tset maxtrees=" + maxTrees + " increase=");
		if (maxTreesIncrease)
			sb.append("auto;\n");
		else
			sb.append("no;\n");
		
		
		if (bootstrapOrJackknife()) {  //bootstrap or jackknife
			sb.append("\tdefaults ");
			
			if (standardSearchBoot){
				sb.append("\ths addseq=random nreps=" + nrepsBoot);
				if (channelSearchBoot && chuckScoreBoot>0 && nchuckBoot>0)
					sb.append(" chuckscore=" + chuckScoreBoot + " nchuck="+nchuckBoot);
				sb.append(";\n");

			} else {
				sb.append(customSearchOptions + ";\n");
			}

			sb.append(paupCommands+"\n");
			if (searchStyle==BOOTSTRAPSEARCH) 
				sb.append("\tboot");
			else if (searchStyle==JACKKNIFESEARCH) 
				sb.append("\tjack");
			sb.append(" nreps = " + bootStrapReps + " search=heuristic;\n");
			sb.append("\tsavetrees from=1 to=1 SaveBootP=brlens file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		else {  //regular search
			sb.append(paupCommands+"\n");
			if (standardSearch){
				sb.append("\ths addseq=random nreps=" + nreps);
				if (channelSearch && chuckScore>0 && nchuck>0)
					sb.append(" chuckscore=" + chuckScore + " nchuck="+nchuck);
				sb.append(" rstatus;\n");
				if (secondarySearchNoChannel)
					sb.append("\ths start=current chuckscore=0 nchuck=0;");

			} else {
				sb.append("\t" + customSearchOptions + "\n");
			}
			if (getConsensus)
				sb.append("\tcontree all/strict=yes treefile=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
			else
				sb.append("\tsavetrees file=" + StringUtil.tokenize(outputTreeFileName) + ";\n");
		}
		return sb.toString();
	}
	
	/*.................................................................................................................*/
	void adjustDialogText(boolean standard) {
		if (channelSearchBox!=null){
			channelSearchBox.setEnabled(standard);
		}
		if (standardSearchBox!=null){
			standardSearchBox.setState(standard);
		}
		if (customSearchBox!=null){
			customSearchBox.setState(!standard);
		}
	}

	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (dialog!=null) {
			Debugg.println("arg0: " + arg0);
			boolean standard=standardSearch;
			if (arg0.getItemSelectable()==standardSearchBox && standardSearchBox!=null){
				standard = standardSearchBox.getState();
			}
			else if (arg0.getItemSelectable()==customSearchBox && customSearchBox!=null){
				standard = !customSearchBox.getState();
			}
			
			adjustDialogText(standard);	
		}
	}


	/*.................................................................................................................*/
	public void queryOptionsSetup(ExtensibleDialog dialog, MesquiteTabbedPanel tabbedPanel) {
		String helpString = "\nIf \"bootstrap\" is on, the PAUP will do a parsimony bootstrap of the number of replicates specified; otherwise, it will do a parsimony heuristic search.";
		helpString+= "\nAny PAUP commands entered in the Additional Commands field will be executed in PAUP immediately before the bootstrap or hs command.";
		dialog.appendToHelpString(helpString);

		dialog.addHorizontalLine(1);
		bootstrapBox = dialog.addRadioButtons(new String[] {"regular search", "bootstrap resampling", "jackknife resampling"}, searchStyle);
		dialog.addHorizontalLine(1);
		
		dialog.addLabel("Additional commands before search command: ");
		paupCommandsField =dialog.addTextAreaSmallFont(paupCommands,4);
		//dialog.addCheckBox("bootstrap", doBootstrap);
		maxTreesField = dialog.addIntegerField("Maximum number of trees (maxtrees)", maxTrees, 8, 1, MesquiteInteger.infinite);
		maxTreesIncreaseBox= dialog.addCheckBox("increase maxtrees if needed", maxTreesIncrease);


		
		tabbedPanel.addPanel("Regular Searches", true);
		
		
		CheckboxGroup searchGroup = new CheckboxGroup();
		dialog.addHorizontalLine(1);
		standardSearchBox = dialog.addCheckBox("pre-built search", standardSearch);
		standardSearchBox.addItemListener(this);
		standardSearchBox.setCheckboxGroup(searchGroup);
		
		nrepsField = dialog.addIntegerField("Number of search replicates", nreps, 8, 1, MesquiteInteger.infinite);
		
		channelSearchBox = dialog.addCheckBox("channeled search", channelSearch);
		nchuckField = dialog.addIntegerField("Save no more than", nchuck, 8, 1, MesquiteInteger.infinite);
		dialog.suppressNewPanel();
		chuckScoreField = dialog.addIntegerField("trees of length greater than or equal to", chuckScore, 8, 1, MesquiteInteger.infinite);
		secondarySearchNoChannelBox = dialog.addCheckBox("subsequent unchanneled search", secondarySearchNoChannel);
		
		dialog.addHorizontalLine(1);
		customSearchBox = dialog.addCheckBox("custom search", !standardSearch);
		customSearchBox.addItemListener(this);
		customSearchBox.setCheckboxGroup(searchGroup);

		dialog.addLabel("Custom Search Commands");
		customSearchOptionsField = dialog.addTextAreaSmallFont(customSearchOptions, 4,60);
		dialog.addHorizontalLine(1);
		
		getConsensusBox = dialog.addCheckBox("only read in consensus", getConsensus);

		
		
		tabbedPanel.addPanel("Resampled Searches", true);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap/Jackknife Replicates", bootStrapReps, 8, 1, MesquiteInteger.infinite);
		
		bootstrapPanelLabel = dialog.addLabel("Below are the search commands for each replicate: ", Label.LEFT, true, false);
		CheckboxGroup searchGroupBoot = new CheckboxGroup();
		dialog.addHorizontalLine(1);
		standardSearchBootBox = dialog.addCheckBox("pre-built search", standardSearchBoot);
		standardSearchBootBox.setCheckboxGroup(searchGroupBoot);
		
		nrepsBootField = dialog.addIntegerField("Number of search replicates", nrepsBoot, 8, 1, MesquiteInteger.infinite);
		
		channelSearchBootBox = dialog.addCheckBox("channeled search", channelSearchBoot);
		nchuckBootField = dialog.addIntegerField("Save no more than", nchuckBoot, 8, 1, MesquiteInteger.infinite);
		dialog.suppressNewPanel();
		chuckScoreBootField = dialog.addIntegerField("trees of length greater than or equal to", chuckScoreBoot, 8, 1, MesquiteInteger.infinite);
		
		dialog.addHorizontalLine(1);
		customSearchBootBox = dialog.addCheckBox("custom search", !standardSearchBoot);
		customSearchBootBox.addItemListener(this);
		customSearchBootBox.setCheckboxGroup(searchGroupBoot);

		dialog.addLabel("Custom Search Commands");
		customSearchOptionsBootField = dialog.addTextAreaSmallFont(customSearchOptionsBoot, 4,60);
		dialog.addHorizontalLine(1);
		dialog.addLabel("(To conduct resampling, Bootstrap or Jackknife must be selected in the General panel) ", Label.LEFT, true, true);

		
		adjustDialogText(standardSearch);	


	}

	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
		bootStrapReps = bootStrapRepsField.getValue();
		searchStyle = bootstrapBox.getValue();
		getConsensus = getConsensusBox.getState();
		customSearchOptions = customSearchOptionsField.getText();
		customSearchOptionsBoot = customSearchOptionsBootField.getText();
		paupCommands = paupCommandsField.getText();
		
		nreps=nrepsField.getValue();
		nchuck=nchuckField.getValue();
		chuckScore=chuckScoreField.getValue();
		channelSearch=channelSearchBox.getState();
		standardSearch=standardSearchBox.getState();
		secondarySearchNoChannel = secondarySearchNoChannelBox.getState();
		
		nrepsBoot=nrepsBootField.getValue();
		nchuckBoot=nchuckBootField.getValue();
		chuckScoreBoot=chuckScoreBootField.getValue();
		channelSearchBoot=channelSearchBootBox.getState();
		standardSearchBoot=standardSearchBootBox.getState();
		
		maxTrees = maxTreesField.getValue();
		maxTreesIncrease = maxTreesIncreaseBox.getState();

	}
	public boolean bootstrapOrJackknife() {
		return searchStyle==BOOTSTRAPSEARCH || searchStyle==JACKKNIFESEARCH;
	}

	public boolean doMajRuleConsensusOfResults() {
		return false;
	}


	public String getName() {
		return "PAUP* Parsimony Analysis";
	}


}
