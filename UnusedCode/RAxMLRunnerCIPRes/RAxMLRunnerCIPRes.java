/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLRunnerCIPRes;


import java.awt.*;
import java.io.*;
import java.awt.event.*;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import mesquite.externalCommunication.lib.RemoteProcessCommunicator;
import mesquite.lib.*;
import mesquite.lib.system.SystemUtil;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.zephyr.CIPResRESTRunner.CIPResRESTRunner;
import mesquite.zephyr.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public class RAxMLRunnerCIPRes extends RAxMLRunner  implements ActionListener, ItemListener, ExternalProcessRequester, RemoteProcessCommunicator  {

	//boolean RAxML814orLater = false;


	boolean showIntermediateTrees = true;


	//Checkbox RAxML814orLaterCheckbox;

	/*.................................................................................................................*/
	public boolean loadModule(){
		return false;
	}
	/*.................................................................................................................*/

	public void addModelOptions(ExtensibleDialog dialog) {
	}

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
		return "8.0.0 through 8.2.10";
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("CIPRes analysis completed "+StringUtil.getDateTime()+"\n");
		appendToSearchDetails("Search details: \n");
			if (bootstrapOrJackknife()){
				appendToSearchDetails("   Bootstrap analysis\n");
				appendToSearchDetails("   "+bootstrapreps + " bootstrap replicates");
			} else {
				appendToSearchDetails("   Search for maximum-likelihood tree\n");
				appendToSearchDetails("   "+numRuns + " search replicate");
				if (numRuns>1)
					appendToSearchDetails("s");
			}
	}

	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "RAxML Options on CIPRes";
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
		if (!bootstrapOrJackknife() && numRuns>1 ) {
			String[] fileNames = getLogFileNames();
			externalProcRunner.setOutputFileNameToWatch(WORKING_TREEFILE, fileNames[WORKING_TREEFILE]);
			outputFilePaths[WORKING_TREEFILE] = externalProcRunner.getOutputFilePath(fileNames[WORKING_TREEFILE]);
			for (int i=currentRunProcessed; i<numRuns; i++) {
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
			getArguments(arguments, sb, "fileName", proteinModelField.getText(), proteinModelMatrixChoice.getSelectedItem(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null, nobfgsCheckBox.getState(), false);
			String command = externalProcRunner.getExecutableCommand() + arguments.toString();
			commandLabel.setText("This command will be used by CIPRes to run RAxML:");
			commandField.setText(command);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}
	/*.................................................................................................................*/
	public void setRAxMLSeed(long seed){
		this.randseed = seed;
	}
	
	public int minimumNumSearchReplicates() {
		return 2;
	}
	/*.................................................................................................................*/

	String inputFilesInRunnerObject = "";

	public void prepareRunnerObject(Object obj){
		if (obj instanceof MultipartEntityBuilder) {
			MultipartEntityBuilder builder = (MultipartEntityBuilder)obj;
			final File file = new File(externalProcRunner.getInputFilePath(DATAFILENUMBER));
			FileBody fb = new FileBody(file);
			builder.addPart("input.infile_", fb);  
			inputFilesInRunnerObject+= " input.infile_ transmitted\n";
			if (useConstraintTree==SKELETAL || useConstraintTree==MONOPHYLY) {
				final File constraintFile = new File(externalProcRunner.getInputFilePath(CONSTRAINTFILENUMBER));
				if (constraintFile!=null && constraintFile.exists()) {
					FileBody fb2 = new FileBody(constraintFile);
					if (useConstraintTree == SKELETAL) {
						builder.addPart("input.binary_backbone_", fb2);  
						inputFilesInRunnerObject+= " input.binary_backbone_ constraint tree transmitted\n";
					}
					else if (useConstraintTree == MONOPHYLY) {
						builder.addPart("input.constraint_", fb2);  
						inputFilesInRunnerObject+= " input.constraint_ constraint tree transmitted\n";
					}
				}
			}
			String modelFilePath = externalProcRunner.getInputFilePath(MULTIMODELFILENUMBER);
			if (StringUtil.notEmpty(modelFilePath)) {
				final File modelFile = new File(modelFilePath);
				if (modelFile!=null && modelFile.exists()) {
					FileBody fb2 = new FileBody(modelFile);
					builder.addPart("input.partition_", fb2);  
					inputFilesInRunnerObject+= " input.partition_ model file transmitted\n";
				}
			}
			if (isVerbose() && StringUtil.notEmpty(inputFilesInRunnerObject))
				logln(inputFilesInRunnerObject);		
		}
	}

	/*.................................................................................................................*/
	void addArgument(MultipartEntityBuilder builder, StringBuffer sb, String param, String value) {
		if (builder!=null)
			builder.addTextBody(param, value);
		if (sb!=null)
			sb.append("\n  " + param + " = " + value);
	}
	/*.................................................................................................................*/
	void getArguments(MultipartEntityBuilder builder, StringBuffer sb, 
			String fileName, 
			String LOCproteinModel, 
			String LOCproteinModelMatrix, 
			String LOCdnaModel, 
			String LOCotherOptions, 
			int LOCbootstrapreps, 
			int LOCbootstrapSeed, 
			int LOCnumRuns, 
			String LOCoutgroupTaxSetString, 
			String LOCMultipleModelFile, 
			boolean LOCnobfgs, boolean preflight){
		if (builder==null)
			return;
	/*	
		if (preflight)
			arguments += " -n preflight.out "; 
		else
			arguments += " -s " + fileName + " -n file.out "; 
		*/
		

		
		if (isProtein) {
			addArgument(builder, sb, "vparam.datatype_", "protein");
			if (StringUtil.blank(LOCproteinModel))
				addArgument(builder, sb, "vparam.prot_sub_model_", "PROTGAMMA");
			else
				addArgument(builder, sb, "vparam.prot_sub_model_", LOCproteinModel);
			if (StringUtil.blank(LOCproteinModelMatrix))
				addArgument(builder, sb, "vparam.prot_matrix_spec_", "JTT");
			else
				addArgument(builder, sb, "vparam.prot_matrix_spec_", LOCproteinModelMatrix);
		} else {
			addArgument(builder, sb, "vparam.datatype_", "dna");
			if (StringUtil.blank(LOCdnaModel))
				addArgument(builder, sb, "vparam.dna_gtrcat_", "GTRGAMMA");
			else
				addArgument(builder, sb, "vparam.dna_gtrcat_","GTRGAMMA");
		}
	//	builder.addTextBody("vparam.dna_gtrcat_",LOCdnaModel);

		
		if (StringUtil.notEmpty(LOCMultipleModelFile)){
//			addArgument(builder, sb, "input.partition_",multipleModelFileName);
		}
			
			//arguments += " -q " + ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);
/*
		if (!StringUtil.blank(LOCotherOptions)) 
			arguments += " " + LOCotherOptions;

*/
		
		addArgument(builder, sb, "vparam.provide_parsimony_seed_","1");
		addArgument(builder, sb, "vparam.parsimony_seed_val_",""+randomIntSeed);

/*		if (useConstraintTree == SKELETAL) 
			addArgument(builder, sb, "input.binary_backbone_",constraintTreeFileName);
		//localArguments += " -r constraintTree.tre "; 
		else if (useConstraintTree == MONOPHYLY)
			addArgument(builder, sb, "input.constraint_",constraintTreeFileName);
		//localArguments += " -g constraintTree.tre "; 
*/


		if (bootstrapOrJackknife()) {
			if (LOCbootstrapreps>0) {
				addArgument(builder, sb, "vparam.choose_bootstrap_","b");
				addArgument(builder, sb, "vparam.bootstrap_value_",""+LOCbootstrapreps);
				addArgument(builder, sb, "vparam.seed_value_",""+LOCbootstrapSeed);
			//	addArgument(builder, sb, "vparam.bootstrap_",""+LOCbootstrapreps);
			//	addArgument(builder, sb, "vparam.mulparambootstrap_seed_",""+LOCbootstrapSeed);
				if (bootstrapBranchLengths)
					addArgument(builder, sb, "vparam.printbrlength_","1");
			} else
				logln("TOO FEW BOOTSTRAP REPS.  CIPRes requires multiple bootstrap replicates.");
			
		}
		else {
			if (LOCnobfgs)   
				addArgument(builder, sb, "vparam.no_bfgs_","1");
			addArgument(builder, sb, "vparam.specify_runs_","1");
			addArgument(builder, sb, "vparam.altrun_number_",""+LOCnumRuns);
		//	if (RAxML814orLater)
			//addArgument(builder, sb, "vparam.mesquite_output_","1");
		}
		
		

		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				addArgument(builder, sb, "vparam.outgroup_",outgroupSet.getStringList(",", namer, false));
				arguments += " -o " + outgroupSet.getStringList(",", namer, false);
		}
		

	}	
	


	/*.................................................................................................................*/
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

	/*.................................................................................................................*/
	public String getPreflightLogFileNames(){
		return "RAxML_log.file.out";	
	}

	
	
	TaxaSelectionSet outgroupSet;
	/*.................................................................................................................*/
	public boolean multipleModelFileAllowed() {
		return true;
	}

	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	String arguments;
	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, boolean isPreflight) {
		MultipartEntityBuilder arguments = MultipartEntityBuilder.create();
		StringBuffer sb = new StringBuffer();

		if (!isPreflight) {
			getArguments(arguments, sb, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, false);
			if (isVerbose()) {
				logln("RAxML arguments: \n" + sb.toString() + "\n");
			}
		} else {
			getArguments(arguments, sb, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, true);
		}
		return arguments;

	}



	public String getExecutableName() {
		return "RAXMLHPC8_REST_XSEDE";
	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	public Class getDutyClass() {
		return RAxMLRunnerCIPRes.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "RAxML Likelihood (CIPRes)";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

	public String getProgramName() {
		return "RAxML";
	}




}
