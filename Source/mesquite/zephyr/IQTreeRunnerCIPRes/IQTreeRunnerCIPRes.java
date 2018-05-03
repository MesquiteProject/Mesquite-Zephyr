/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.IQTreeRunnerCIPRes;


import java.awt.*;
import java.io.*;
import java.awt.event.*;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import mesquite.lib.*;
import mesquite.lib.system.SystemUtil;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.lib.*;


public class IQTreeRunnerCIPRes extends IQTreeRunner  implements ActionListener, ItemListener, ExternalProcessRequester  {

	boolean showIntermediateTrees = true;


	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return CIPResRESTRunner.class;
	 }

		public String getLogText() {
			String log= externalProcRunner.getStdOut();
			if (StringUtil.blank(log))
				log="Waiting for log file from CIPRes...";
			return log;
		}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = super.getSnapshot(file);
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Hires the ExternalProcessRunner", "[name of module]", commandName, "setExternalProcessRunner")) {
			ExternalProcessRunner temp = (ExternalProcessRunner)replaceEmployee(ExternalProcessRunner.class, arguments, "External Process Runner", externalProcRunner);
			if (temp != null) {
				externalProcRunner = temp;
				parametersChanged();
			}
			externalProcRunner.setProcessRequester(this);
			return externalProcRunner;
		} else
			return super.doCommand(commandName, arguments, checker);
	}	

	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String content) {
		if ("RAxML814orLater".equalsIgnoreCase(tag))
		RAxML814orLater = MesquiteBoolean.fromTrueFalseString(content);

		
		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
	}
	
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "RAxML814orLater", RAxML814orLater);  
		
		buffer.append(super.preparePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "1.6.4";
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("CIPRes analysis completed "+StringUtil.getDateTime()+"\n");
		super.appendAdditionalSearchDetails();
	}

	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "IQ-TREE Options on CIPRes";
	}

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
//		RAxML814orLaterCheckbox = dialog.addCheckBox("RAxML version 8.1.4 or later", RAxML814orLater);
		//dialog.addLabelSmallText("This version of Zephyr tested on the following RAxML version(s): " + getTestedProgramVersions());
		//externalProcRunner.addItemsToDialogPanel(dialog);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
//		RAxML814orLater = RAxML814orLaterCheckbox.getState();
//		externalProcRunner.optionsChosen();
	}
	int currentRunProcessed=0;
	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (!bootstrapOrJackknife() && numSearchRuns>1 ) {
			String[] fileNames = getLogFileNames();
			externalProcRunner.setOutputFileNameToWatch(WORKING_TREEFILE, fileNames[WORKING_TREEFILE]);
			outputFilePaths[WORKING_TREEFILE] = externalProcRunner.getOutputFilePath(fileNames[WORKING_TREEFILE]);
			for (int i=currentRunProcessed; i<numSearchRuns; i++) {
				String candidate = outputFilePaths[WORKING_TREEFILE]+i;
				if (MesquiteFile.fileExists(candidate)) {
					outputFilePaths[WORKING_TREEFILE]= candidate;
					currentRunProcessed++;
				}
			}
			externalProcRunner.resetLastModified(WORKING_TREEFILE);
			previousCurrentRun=currentRun;
		}
		return outputFilePaths;
	}

	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase(composeProgramCommand)) {

			MultipartEntityBuilder arguments = MultipartEntityBuilder.create();
			StringBuffer sb = new StringBuffer();
			getArguments(arguments, sb, "[fileName]", "[setsBlockFileName]", substitutionModelField.getText(), otherOptionsField.getText(), searchStyleButtons.getValue(), bootStrapRepsField.getValue(), bootstrapSeed, numSearchRunsField.getValue(), numUFBootRunsField.getValue(), charPartitionButtons.getValue(), partitionLinkageChoice.getSelectedIndex(), outgroupTaxSetString, null,  false);
			String command = externalProcRunner.getExecutableCommand() + arguments.toString();
			commandLabel.setText("This command will be used by CIPRes to run IQ-TREE:");
			commandField.setText(command);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}
	
	public int minimumNumSearchReplicates() {
		return 1;
	}


	/*.................................................................................................................*/
	void addArgument(MultipartEntityBuilder builder, StringBuffer sb, String param, String value) {
		if (builder!=null)
			builder.addTextBody(param, value);
		if (sb!=null)
			sb.append("\n  " + param + " = " + value);
	}
	/*.................................................................................................................*/
	void getArguments(MultipartEntityBuilder builder, StringBuffer sb, String fileName, 
			String setsFileName,
			String LOCSubstitutionModel, String LOCotherOptions, 
			int LOCsearchStyle, int LOCbootstrapreps, int LOCbootstrapSeed, 
			int LOCnumSearchRuns, 
			int LOCnumUFBootRuns, 
			int LOCPartitionScheme,
			int LOCPartitionLinkage,
			String LOCoutgroupTaxSetString, String LOCMultipleModelFile, 
			boolean preflight){

		if (builder==null)
			return;
	/*	
		if (preflight)
			arguments += " -n preflight.out "; 
		else
			arguments += " -s " + fileName + " -n file.out "; 
		*/

		addArgument(builder, sb, "input.file_", fileName); 

		if (LOCsearchStyle==STANDARDSEARCH) {
			// number of reps
		} else {
			if (LOCsearchStyle==ULTRAFASTBOOTSTRAP) {
				addArgument(builder, sb, "vparam.bootstrap_type_",  "b");
				addArgument(builder, sb, "vparam.write_boottrees1_ ",  "1");
				if (LOCbootstrapreps>0)
					addArgument(builder, sb, "vparam.vparam.num_bootreps_",  ""+LOCbootstrapreps);
				else
					addArgument(builder, sb, "vparam.vparam.num_bootreps_",  "1000");
			} else {
				addArgument(builder, sb, "vparam.bootstrap_type_",  "bc");
				if (LOCbootstrapreps>0)
					addArgument(builder, sb, "vparam.vparam.num_bootreps_",  ""+LOCbootstrapreps);
				else
					addArgument(builder, sb, "vparam.vparam.num_bootreps_",  "100");
			}
		}

		 if (StringUtil.notEmpty(LOCSubstitutionModel))
				addArgument(builder, sb, "vparam.specify_model_",  LOCSubstitutionModel);  // TODO: WILL NOT WORK if specific model specified

		 if (useConstraintTree)
				addArgument(builder, sb, "input.constraint_file_",  "constraintTree.tre");
		 
			addArgument(builder, sb, "vparam.specify_seed_",  ""+LOCbootstrapSeed);
			addArgument(builder, sb, "vparam.partition_type_",  "-q");
		 
		 if (LOCPartitionScheme!=noPartition) {
			 if (LOCPartitionLinkage==qPartitionLinkage)
					addArgument(builder, sb, "vparam.partition_type_",  "-q");
			 else if (LOCPartitionLinkage==sppPartitionLinkage)
					addArgument(builder, sb, "vparam.partition_type_",  "-spp");
			 else if (LOCPartitionLinkage==spPartitionLinkage)
					addArgument(builder, sb, "vparam.partition_type_",  "-sp");
				addArgument(builder, sb, "input.partition_file_",  setsFileName);
		 }

		addArgument(builder, sb, "vparam.specify_prefix_",  getOutputFilePrefix());

/*
		if (!StringUtil.blank(LOCotherOptions)) 
			localArguments += " " + LOCotherOptions;



		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				addArgument(builder, sb, "vparam.outgroup_",outgroupSet.getStringList(",", namer, false));
				arguments += " -o " + outgroupSet.getStringList(",", namer, false);
		}
		*/

	}	
	


	/*.................................................................................................................*
	public String[] getLogFileNames(){
		String treeFileName;
		String workingTreeFileName;
		String logFileName;
		if (bootstrapOrJackknife())
			treeFileName = "RAxML_bootstrap.result";
		else 
			treeFileName = "RAxML_result.result";
		logFileName = "RAxML_info.result";
		workingTreeFileName= treeFileName;
		if (!bootstrapOrJackknife() && numRuns>1) {
			treeFileName+=".RUN.";
			workingTreeFileName= treeFileName;
			logFileName+=".RUN.";
		}
		return new String[]{logFileName, treeFileName, "RAxML_info.result", workingTreeFileName};
	}


	
	
	TaxaSelectionSet outgroupSet;
	
	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	String arguments;

	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, String setsFileName, boolean isPreflight) {
		MultipartEntityBuilder arguments = MultipartEntityBuilder.create();
		StringBuffer sb = new StringBuffer();

		if (!isPreflight) {
			getArguments(arguments, sb, dataFileName, setsFileName, substitutionModel, otherOptions, searchStyle, bootstrapreps, bootstrapSeed, numSearchRuns, numUFBootRuns, partitionScheme, partitionLinkage, outgroupTaxSetString, multipleModelFileName, false);
		} else {
			getArguments(arguments, sb, dataFileName, setsFileName, substitutionModel, otherOptions, searchStyle, bootstrapreps, bootstrapSeed, numSearchRuns, numUFBootRuns, partitionScheme, partitionLinkage, outgroupTaxSetString, multipleModelFileName, true);
		}
	
		if (!isPreflight && isVerbose())
				logln(getExecutableName() + " arguments: \n" + sb.toString() + "\n");

		return arguments; 

	}


	public String getExecutableName() {
		return "IQTREE_XSEDE";
	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	public Class getDutyClass() {
		return IQTreeRunnerCIPRes.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "IQ-TREE Likelihood (CIPRes)";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

	/*.................................................................................................................*/
	 public boolean loadModule(){
		 return false;
	}




}
