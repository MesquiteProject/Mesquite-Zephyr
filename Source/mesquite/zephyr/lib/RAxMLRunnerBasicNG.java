/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.io.ExportFusedPhylip.ExportFusedPhylip;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.RAxMLTreesLocalOld.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public abstract class RAxMLRunnerBasicNG extends RAxMLRunnerBasic  implements KeyListener  {

	protected boolean autoNumProcessors=true;
	protected Checkbox autoNumProcessorsCheckBox;

	protected String outputFilePrefix="file";

	protected boolean showIntermediateTrees = true;



	protected SingleLineTextField MPISetupField;
	protected IntegerField numProcessorsField;
	protected RadioButtons threadingRadioButtons;



	public String getExecutableName() {
		return "RAxML-NG";
	}

	public boolean getConstrainedSearchAllowed() {
		return false;
	}

	/*.................................................................................................................*/
	public String[] getDNAModelMatrixOptions() {
		return new String[] {"JC", "K80", "F81", "HKY", "TN93ef", "TN93", "K81", "K81uf", "TPM2", "TPM2uf", "TPM3", "TPM3uf", "TIM1", "TIM1uf", "TIM2", "TIM2uf", "TIM3", "TIM3uf", "TVMef", "TVM", "SYM", "GTR"};
	}
	/*.................................................................................................................*/
	public String[] getProteinModelMatrixOptions() {
		return new String[] {"Blosum62", "cpREV", "Dayhoff", "DCMut", "DEN", "FLU", "HIVb", "HIVw", "JTT", "JTT-DCMut", "LG", "mtART","mtMAM", "mtREV", "mtZOA", "PMB", "rtREV","stmtREV", "VT", "WAG", "LG4M", "LG4X", "PROTGTR"};
	}
	/*.................................................................................................................*/
	public void setUpRunner() { 
		prepareModels();
	}
	/*.................................................................................................................*/
	public boolean isRAxMLNG() { 
		return true;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("autoNumProcessors".equalsIgnoreCase(tag))
			autoNumProcessors = MesquiteBoolean.fromTrueFalseString(content);
		if ("autoNumBootstrapReps".equalsIgnoreCase(tag))
			autoNumBootstrapReps = MesquiteBoolean.fromTrueFalseString(content);

		if ("numProcessors".equalsIgnoreCase(tag))
			numProcessors = MesquiteInteger.fromString(content);
		if ("propInvariant".equalsIgnoreCase(tag)){   
			propInvariant = StringUtil.cleanXMLEscapeCharacters(content);
		}
		if ("stationaryFreq".equalsIgnoreCase(tag)){   
			stationaryFreq = StringUtil.cleanXMLEscapeCharacters(content);
		}
		if ("amongSiteVariation".equalsIgnoreCase(tag)){   
			amongSiteVariation = StringUtil.cleanXMLEscapeCharacters(content);
		}
		if ("stationaryFreqSuffix".equalsIgnoreCase(tag)){   
			stationaryFreqSuffix = StringUtil.cleanXMLEscapeCharacters(content);
		}
		if ("proportionInvariantSuffix".equalsIgnoreCase(tag)){   
			proportionInvariantSuffix = StringUtil.cleanXMLEscapeCharacters(content);
		}
		if ("amongSiteVariationSuffix".equalsIgnoreCase(tag)){   
			amongSiteVariationSuffix = StringUtil.cleanXMLEscapeCharacters(content);
		}

		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
	}


	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "autoNumProcessors", autoNumProcessors);  
		StringUtil.appendXMLTag(buffer, 2, "autoNumBootstrapReps", autoNumBootstrapReps);  
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  
		StringUtil.appendXMLTag(buffer, 2, "propInvariant", propInvariant);  
		StringUtil.appendXMLTag(buffer, 2, "stationaryFreq", stationaryFreq);  
		StringUtil.appendXMLTag(buffer, 2, "amongSiteVariation", amongSiteVariation);  
		StringUtil.appendXMLTag(buffer, 2, "stationaryFreqSuffix", stationaryFreqSuffix);  
		StringUtil.appendXMLTag(buffer, 2, "proportionInvariantSuffix", proportionInvariantSuffix);  
		StringUtil.appendXMLTag(buffer, 2, "amongSiteVariationSuffix", amongSiteVariationSuffix);  

		buffer.append(super.preparePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "1.1.0";
	}

	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("Search details: \n");
		if (bootstrapOrJackknife()){
			appendToSearchDetails("   Bootstrap analysis\n");
			if (autoNumBootstrapReps)
				appendToSearchDetails("   autoMRE option used ");
			else
				appendToSearchDetails("   "+bootstrapreps + " bootstrap replicates");
		} else {
			appendToSearchDetails("   Search for maximum-likelihood tree\n");
			appendToSearchDetails("   "+numRuns + " search replicate");
			if (numRuns>1)
				appendToSearchDetails("s");
		}
		MesquiteString arguments = (MesquiteString)getProgramArguments(getDataFileName(), false);
		if (arguments!=null && !arguments.isBlank()){
			appendToSearchDetails("\n" + getProgramName() + " command options: " + arguments.toString());
		}
	}

	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "RAxML-NG Options & Locations";
	}

	//TEST IF WORKS ON SSH
	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+="Bootstrap analysis<br>";
				s+="Bootstrap replicates completed: <b>";
				if (autoNumBootstrapReps) {
					if (bootstrapreps>0)
						s+=numRunsCompleted +" of at most " + bootstrapreps;
					else
						s+=numRunsCompleted +" of " + bootstrapreps;
				} else {
					if (numRunsCompleted>bootstrapreps)
						s+=numRuns +" of " + bootstrapreps;
					else
						s+=numRunsCompleted +" of " + bootstrapreps;
				}
					
			}
			else {
				s+="Search for ML Tree<br>";
				s+="Search replicates completed: <b>";
				if (numRunsCompleted>numRuns)
					s+=numRuns +" of " + numRuns;
				else
					s+=numRunsCompleted +" of " + numRuns;
			}
			s+="</b>";
		}
		return s;
	}


	protected Choice dnaModelMatrixChoice;
	protected Choice stationaryFreqChoice;
	protected Choice propInvariantChoice;
	protected Choice amongSiteVariationChoice;
	protected  SingleLineTextField stationaryFreqSuffixField;
	protected  SingleLineTextField proportionInvariantSuffixField;
	protected  SingleLineTextField amongSiteVariationSuffixField;
	protected  String propInvariant = "none";
	protected  String stationaryFreq = "F";
	protected  String amongSiteVariation = "G";
	protected  String stationaryFreqSuffix = "";
	protected  String proportionInvariantSuffix = "";
	protected  String amongSiteVariationSuffix = "";

	/*.................................................................................................................*/
	public void prepareModels() { 
		String addendum = "";
		if (StringUtil.notEmpty(amongSiteVariationSuffix))
			addendum+="+"+ amongSiteVariation + amongSiteVariationSuffix;
		else
			addendum+="+"+ amongSiteVariation;
		
		if (propInvariant.equalsIgnoreCase("IU")) {
			if (StringUtil.notEmpty(proportionInvariantSuffix))
				addendum+="+"+ propInvariant + proportionInvariantSuffix;
		}
		else if (!propInvariant.equalsIgnoreCase("none"))
			addendum += "+"+ propInvariant;
		
		if (stationaryFreq.equalsIgnoreCase("FU")) {
			if (StringUtil.notEmpty(stationaryFreqSuffix))
				addendum+="+"+ stationaryFreq + stationaryFreqSuffix;
		}
		else
			addendum+="+"+ stationaryFreq;
		
		dnaModel = dnaModelMatrix+addendum;
		proteinModel = proteinModelMatrix+addendum;
		
	}


	/*.................................................................................................................*/

	public void addModelOptions(ExtensibleDialog dialog) {
		dnaModelMatrixChoice = dialog.addPopUpMenu("DNA Substitution Matrix Model", getDNAModelMatrixOptions(), getDNAModelMatrixNumber(dnaModelMatrix));
		proteinModelMatrixChoice = dialog.addPopUpMenu("Protein Transition Matrix Model", getProteinModelMatrixOptions(), getProteinModelMatrixNumber(proteinModelMatrix));
		amongSiteVariationChoice = dialog.addPopUpMenu("Among-site Variation Model", getAmongSiteVariationOptions(), getAmongSiteVariationNumber(amongSiteVariation));
		amongSiteVariationSuffixField = dialog.addTextField("Among-site variation suffix:", amongSiteVariationSuffix, 20);
		propInvariantChoice = dialog.addPopUpMenu("Proportion Invariant Site Model", getPropInvariantOptions(), getPropInvariantNumber(propInvariant));
		proportionInvariantSuffixField = dialog.addTextField("Proportion invariant suffix:", proportionInvariantSuffix, 20);
		stationaryFreqChoice = dialog.addPopUpMenu("Stationary Frequencies Model", getStationaryFreqOptions(), getStationaryFreqNumber(stationaryFreq));
		stationaryFreqSuffixField = dialog.addTextField("Stationary Frequencies suffix:", stationaryFreqSuffix, 20);
}
/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		dialog.addHorizontalLine(1);
		autoNumProcessorsCheckBox = dialog.addCheckBox("Let " + getProgramName() + " choose number of processor cores", autoNumProcessors);
		autoNumProcessorsCheckBox.addItemListener(this);
		numProcessorsField = dialog.addIntegerField("Specify number of processor cores", numProcessors, 8, 1, MesquiteInteger.infinite);
		dialog.addHorizontalLine(1);

		dialog.addLabelSmallText("This version of Zephyr tested on the following "+getExecutableName()+" version(s) of "+ getProgramName() + ": " + getTestedProgramVersions());
	}
	
	
	/*.................................................................................................................*/
	public String[] getAmongSiteVariationOptions() {
		return new String[] {"G", "GA", "R"};
	}
	/*.................................................................................................................*/
	public String[] getPropInvariantOptions() {
		return new String[] {"none", "I", "IC", "IU"};
	}
	/*.................................................................................................................*/
	public String[] getStationaryFreqOptions() {
		return new String[] {"F", "FO", "FE", "FU"};
	}
	/*.................................................................................................................*/
	public int getStationaryFreqNumber(String name) {
		return getPositionInArray(getStationaryFreqOptions(), name);
	}
	/*.................................................................................................................*/
	public int getAmongSiteVariationNumber(String name) {
		return getPositionInArray(getAmongSiteVariationOptions(), name);
	}
	/*.................................................................................................................*/
	public int getPropInvariantNumber(String name) {
		return getPositionInArray(getPropInvariantOptions(), name);
	}


	/*.................................................................................................................*/
	public void processRunnerOptions() {
		autoNumProcessors = autoNumProcessorsCheckBox.getState();
		numProcessors = numProcessorsField.getValue(); //
		String name = dnaModelMatrixChoice.getSelectedItem();
		if (StringUtil.notEmpty(name))
			dnaModelMatrix = name;
		name = stationaryFreqChoice.getSelectedItem();
		if (StringUtil.notEmpty(name))
			stationaryFreq = name;
		name = propInvariantChoice.getSelectedItem();
		if (StringUtil.notEmpty(name))
			propInvariant = name;
		stationaryFreqSuffix = stationaryFreqSuffixField.getText();
		proportionInvariantSuffix = proportionInvariantSuffixField.getText();
		amongSiteVariationSuffix = amongSiteVariationSuffixField.getText();
		prepareModels();
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == autoNumProcessorsCheckBox){
			numProcessorsField.getTextField().setEnabled(!autoNumProcessorsCheckBox.getState());
		} else if (e.getItemSelectable() == autoNumBootstrapRepsCheckBox){
			checkAdditionalFields();
		}
			super.itemStateChanged(e);
	}
	/*.................................................................................................................*/
	public void checkAdditionalFields() {
		if (autoNumBootstrapRepsCheckBox!=null && bootStrapRepsField!=null) {
			if (autoNumBootstrapRepsCheckBox.getState())
				bootStrapRepsField.setLabelText("Maximum bootstrap replicates");
			else
				bootStrapRepsField.setLabelText("              Bootstrap replicates");
		}
	}

	/*.................................................................................................................*/
	public void addExtraBootstrapOptions(ExtensibleDialog dialog) {
		autoNumBootstrapRepsCheckBox = dialog.addCheckBox("Let " + getProgramName() + " choose number of bootstrap replicates", autoNumBootstrapReps);
		autoNumBootstrapRepsCheckBox.addItemListener(this);
	}
	/*.................................................................................................................*/
	public void processExtraBootstrapOptions() {
		 autoNumBootstrapReps = autoNumBootstrapRepsCheckBox.getState();
	}
	/*.................................................................................................................*/
	protected String getHelpString() {
		return "This module will prepare a matrix for RAxML-NG, and ask RAxM-NG do to an analysis.  A command-line version of RAxML-NG must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by RAxML-NG, and, for non-bootstrap analyses.";
	}

	/*.................................................................................................................*/
	public void getArguments(MesquiteString arguments, String fileName, String LOCproteinModel, String LOCproteinModelMatrix,
			String LOCdnaModel, String LOCotherOptions, 
			boolean LOCdoBootstrap, int LOCbootstrapreps, int LOCbootstrapSeed, 
			int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, boolean LOCnobfgs, boolean preflight){
		if (arguments == null)
			return;

		String localArguments = "";


		if (preflight)
			localArguments += " -n preflight.out "; 
		else
			localArguments += " --msa " + fileName + " --prefix  " + outputFilePrefix; 

		localArguments += " --model "; 
		if (StringUtil.notEmpty(LOCMultipleModelFile))
			localArguments += ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);
		else if (isProtein) {
			if (StringUtil.blank(LOCproteinModel))
				localArguments += "JTT+G";
			else
				localArguments += LOCproteinModel;
		}
		else if (StringUtil.blank(LOCdnaModel))
			localArguments += "GTR+G";
		else
			localArguments += LOCdnaModel;



		if (!StringUtil.blank(LOCotherOptions)) 
			localArguments += " " + LOCotherOptions;
		
	/*	if (useConstraintTree == SKELETAL)
			localArguments += " -r " + CONSTRAINTTREEFILENAME + " "; 
		else if (useConstraintTree == MONOPHYLY)
			localArguments += " -g " + CONSTRAINTTREEFILENAME + " "; 
*/
		
		if (LOCdoBootstrap) {
			localArguments += " --bootstrap ";
			if (autoNumBootstrapReps) {
				 if (LOCbootstrapreps>0)
					 localArguments += " --bs-trees autoMRE{" + LOCbootstrapreps + "} --seed " + LOCbootstrapSeed;
				 else
					 localArguments += " --bs-trees autoMRE  --seed " + LOCbootstrapSeed;
			}
			else if (LOCbootstrapreps>0)
				localArguments += " --bs-trees " + LOCbootstrapreps + " --seed " + LOCbootstrapSeed;
			else
				localArguments += " --bs-trees 1 --seed " + LOCbootstrapSeed;   // just do one rep
			if (bootstrapBranchLengths)
				localArguments += " -k "; 
		}
		else {
			localArguments += " --search --seed " + randomIntSeed;
			localArguments += " -tree pars{" + LOCnumRuns + "}, rand{"+ LOCnumRuns + "}";
		}

		
		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				localArguments += " --outgroup " + outgroupSet.getStringList(",", namer, false);
		}
		
	
		arguments.setValue(localArguments);
	}
	/*.................................................................................................................*/
	public String[] getLogFileNames(){
		String treeFileName;
		String workingTreeFileName;
		String logFileName;
		String workingMLTrees ="";
		if (bootstrapOrJackknife()) {
			treeFileName = outputFilePrefix+".raxml.bootstraps";
			workingTreeFileName= outputFilePrefix+".raxml.bootstraps.TMP";
		} else {
			if (onlyBest)
				treeFileName = outputFilePrefix+".raxml.bestTree";
			else
				treeFileName = outputFilePrefix+".raxml.mlTrees";
			workingTreeFileName= outputFilePrefix+".raxml.lastTree.TMP";
			workingMLTrees= outputFilePrefix+".raxml.mlTrees.TMP";
		}
		logFileName = outputFilePrefix+".raxml.log";
		return new String[]{logFileName, treeFileName, logFileName, workingTreeFileName, workingMLTrees};
	}
	/*.................................................................................................................*
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (bootstrapOrJackknife()) {
			if (currentRun!=previousCurrentRun) {
				String[] fileNames = getLogFileNames();
				externalProcRunner.setOutputFileNameToWatch(WORKING_TREEFILE, fileNames[WORKING_TREEFILE]);
				outputFilePaths[WORKING_TREEFILE] = externalProcRunner.getOutputFilePath(fileNames[WORKING_TREEFILE]);
				externalProcRunner.resetLastModified(WORKING_TREEFILE);
				previousCurrentRun=currentRun;
				//				logln("\n----- Now displaying results from run " + currentRun);
			}
		} else
		if (bootstrapOrJackknife()) {
			if (currentRun!=previousCurrentRun) {
				String[] fileNames = getLogFileNames();
				externalProcRunner.setOutputFileNameToWatch(WORKING_TREEFILE, fileNames[WORKING_TREEFILE]);
				outputFilePaths[WORKING_TREEFILE] = externalProcRunner.getOutputFilePath(fileNames[WORKING_TREEFILE]);
				externalProcRunner.resetLastModified(WORKING_TREEFILE);
				previousCurrentRun=currentRun;
				//				logln("\n----- Now displaying results from run " + currentRun);
			}
		}
		return outputFilePaths;
	}
	/*.................................................................................................................*/
	public String getPreflightLogFileNames(){
		return outputFilePrefix+".raxml.log";
	}


	//*************************


	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	//String arguments;
	/*.................................................................................................................*/
	public String getAdditionalArguments() {
		boolean auto = autoNumProcessors;
		if (autoNumProcessorsCheckBox != null)
			auto = autoNumProcessorsCheckBox.getState();
		if (!auto)
			return " --threads "+ MesquiteInteger.maximum(numProcessors, 2) + " ";   // have to ensure that there are at least two threads requested
		return "";
	}
	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, boolean isPreflight) {
		MesquiteString arguments = new MesquiteString();

		if (!isPreflight) {
			getArguments(arguments, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, doBootstrap, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, false);
			if (isVerbose()) {
				logln(getProgramName() + " arguments: \n" + arguments.getValue() + getAdditionalArguments()+"\n");
			}
		} else {
			getArguments(arguments, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, doBootstrap,bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, true);
		}
		arguments.append(getAdditionalArguments());
	
		return arguments; // + " | tee log.txt"; // + "> log.txt";

	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

	public String getProgramName() {
		return "RAxML-NG";
	}




}
