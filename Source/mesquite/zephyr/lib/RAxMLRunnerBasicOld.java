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

public abstract class RAxMLRunnerBasicOld extends RAxMLRunnerBasic  implements KeyListener  {

	protected static final int THREADING_OTHER =0;
	protected static final int THREADING_PTHREADS = 1;
	protected static final int THREADING_MPI = 2;
	protected int threadingVersion = THREADING_OTHER;
	protected boolean RAxML814orLater = true;

	protected boolean showIntermediateTrees = true;

	protected RadioButtons threadingRadioButtons;
//	protected Checkbox RAxML814orLaterCheckbox;

	public String getExecutableName() {
		return "RAxML";
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
	//	if ("RAxML814orLater".equalsIgnoreCase(tag))
	//		RAxML814orLater = MesquiteBoolean.fromTrueFalseString(content);

		if ("raxmlThreadingVersion".equalsIgnoreCase(tag))
			threadingVersion = MesquiteInteger.fromString(content);

		if ("numProcessors".equalsIgnoreCase(tag))
			numProcessors = MesquiteInteger.fromString(content);

		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
//		StringUtil.appendXMLTag(buffer, 2, "RAxML814orLater", RAxML814orLater);  
		StringUtil.appendXMLTag(buffer, 2, "raxmlThreadingVersion", threadingVersion);  
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  

		buffer.append(super.preparePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "8.0.0–8.2.12";
	}

	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
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
		MesquiteString arguments = (MesquiteString)getProgramArguments(getDataFileName(), false);
		if (arguments!=null && !arguments.isBlank()){
			appendToSearchDetails("\n" + getProgramName() + " command options: " + arguments.toString());
		}
	}
	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return "RAxML Options & Locations";
	}

	//TEST IF WORKS ON SSH
	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+="Bootstrap analysis<br>";
				s+="Bootstrap replicates completed: <b>";
				if (numRunsCompleted>bootstrapreps)
					s+=numRuns +" of " + bootstrapreps;
				else
					s+=numRunsCompleted +" of " + bootstrapreps;
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


	/*.................................................................................................................*/
	public boolean requiresPThreads() {
		return false;
	}
	/*.................................................................................................................*/
	public void checkEnabling() {
		threadingRadioButtons.setEnabled(THREADING_OTHER, !usingBuiltinApp);
		Debugg.println("============\nCHECK ENABLING\n==========");
	}
	
	/*.................................................................................................................*/

	public void addRunnerOptions(ExtensibleDialog dialog) {
		dialog.addHorizontalLine(1);
		dialog.addLabel("RAxML parallelization style:");
		boolean requiresPThreads = requiresPThreads();
		if (requiresPThreads)
			threadingVersion = THREADING_PTHREADS;
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"non-PThreads", "PThreads"}, threadingVersion);	
		
/*		if (requiresPThreads) {
			threadingRadioButtons.setEnabled(THREADING_OTHER, false);
			threadingRadioButtons.setEnabled(THREADING_PTHREADS, false);
		}
		*/
		numProcessorsField = dialog.addIntegerField("Number of Processor Cores", numProcessors, 8, 1, MesquiteInteger.infinite);
		numProcessorsField.addKeyListener(this);
		dialog.addHorizontalLine(1);
		checkEnabling();

//		RAxML814orLaterCheckbox = dialog.addCheckBox("RAxML version 8.1.4 or later", RAxML814orLater);
//		dialog.addLabelSmallText("This version of Zephyr tested on the following RAxML version(s): " + getTestedProgramVersions());
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
//		RAxML814orLater = RAxML814orLaterCheckbox.getState();
		threadingVersion = threadingRadioButtons.getValue();
		numProcessors = numProcessorsField.getValue(); //
		dnaModel = dnaModelField.getText();
		proteinModel = proteinModelField.getText();
		proteinModelMatrix = proteinModelMatrixChoice.getSelectedItem();
}
	/*.................................................................................................................*/

	public void addModelOptions(ExtensibleDialog dialog) {
		dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);
		proteinModelMatrixChoice = dialog.addPopUpMenu("Protein Transition Matrix Model", getProteinModelMatrixOptions(), getProteinModelMatrixNumber(proteinModelMatrix));
	}

	/*.................................................................................................................*/
	public  String getComposedCommand(MesquiteString arguments) {
		isProtein = data instanceof ProteinData;

		String localModelFileName = null;
		if (charPartitionButtons!=null) {
			int localPartitionScheme = charPartitionButtons.getValue();
			prepareMultipleModelFile(localPartitionScheme);
			if (StringUtil.notEmpty(multipleModelFileContents)) 
				localModelFileName=MULTIPLEMODELFILENAME;
		}

		boolean nobfgsValue = false;
		if (nobfgsCheckBox!=null)
			nobfgsValue =nobfgsCheckBox.getState();
		String localProteinModel = proteinModelField.getText();
		if (StringUtil.blank(localProteinModel))
			localProteinModel = "PROTGAMMAJTT";
		else
			localProteinModel = localProteinModel+proteinModelMatrixChoice.getSelectedItem();
		getArguments(arguments, "[fileName]", localProteinModel, dnaModelField.getText(), otherOptionsField.getText(), doBootstrapCheckbox.getState(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, localModelFileName, nobfgsValue, false);
		return externalProcRunner.getExecutableCommand() + arguments.getValue() + getAdditionalArguments();
	}
/*.................................................................................................................*/
	public void getArguments(MesquiteString arguments, String fileName, String LOCproteinModel,
			String LOCdnaModel, String LOCotherOptions, 
			boolean LOCdoBootstrap, int LOCbootstrapreps, int LOCbootstrapSeed, 
			int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, boolean LOCnobfgs, boolean preflight){
		if (arguments == null)
			return;

		String localArguments = "";

		if (preflight)
			localArguments += " -n preflight.out "; 
		else
			localArguments += " -s " + fileName + " -n file.out "; 


		localArguments += " -m "; 
		if (isProtein) {
			if (StringUtil.blank(LOCproteinModel))
				localArguments += "PROTGAMMAJTT";
			else
				localArguments += LOCproteinModel;
		}
		else if (StringUtil.blank(LOCdnaModel))
			localArguments += "GTRGAMMA";
		else
			localArguments += LOCdnaModel;

		if (StringUtil.notEmpty(LOCMultipleModelFile))
			localArguments += " -q " + ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);

		localArguments += " -p " + randomIntSeed;

		if (!StringUtil.blank(LOCotherOptions)) 
			localArguments += " " + LOCotherOptions;
		if (useConstraintTree == SKELETAL)
			localArguments += " -r " + CONSTRAINTTREEFILENAME + " "; 
		else if (useConstraintTree == MONOPHYLY)
			localArguments += " -g " + CONSTRAINTTREEFILENAME + " "; 

		if (LOCdoBootstrap) {
			if (LOCbootstrapreps>0)
				localArguments += " -# " + LOCbootstrapreps + " -b " + LOCbootstrapSeed;
			else
				localArguments += " -# 1 -b " + LOCbootstrapSeed;   // just do one rep
			if (bootstrapBranchLengths)
				localArguments += " -k "; 
		}
		else {
			if (LOCnobfgs)
				localArguments += " --no-bfgs ";
			if (LOCnumRuns>1)
				localArguments += " -# " + LOCnumRuns;
			if (RAxML814orLater)
				localArguments += " --mesquite";
		}

		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				localArguments += " -o " + outgroupSet.getStringList(",", namer, false);
		}
		arguments.setValue(localArguments);
	}
	/*.................................................................................................................*/
	public String[] getLogFileNames(){
		String treeFileName;
		String workingTreeFileName;
		String logFileName;
		if (bootstrapOrJackknife())
			treeFileName = "RAxML_bootstrap.file.out";
		else 
			treeFileName = "RAxML_result.file.out";
		logFileName = "RAxML_log.file.out";
		workingTreeFileName= treeFileName;
		if (!bootstrapOrJackknife() && numRuns>1) {
			treeFileName+=".RUN.";
			workingTreeFileName= treeFileName + currentRun;
			logFileName+=".RUN.";
		}
		return new String[]{logFileName, treeFileName, "RAxML_info.file.out", workingTreeFileName, VERSIONFILENAME};
	}
	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (!bootstrapOrJackknife() && numRuns>1 ) {
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
		return "RAxML_log.file.out";	
	}


	//*************************


	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	//String arguments;
	/*.................................................................................................................*/
	public String getAdditionalArguments() {
		boolean thread = threadingVersion==THREADING_PTHREADS;
		if (threadingRadioButtons!=null)
			thread = threadingRadioButtons.getValue()==THREADING_PTHREADS;
		if (thread) {
			return " -T "+ MesquiteInteger.maximum(numProcessors, 2) + " ";   // have to ensure that there are at least two threads requested
		}
		return "";
	}
	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, boolean isPreflight) {
		MesquiteString arguments = new MesquiteString();
		String localProteinModel = proteinModel;
		if (StringUtil.blank(proteinModel))
			localProteinModel = "PROTGAMMAJTT";
		else
			localProteinModel = proteinModel+proteinModelMatrix;

		if (!isPreflight) {
			getArguments(arguments, dataFileName, localProteinModel, dnaModel, otherOptions, doBootstrap, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, false);
			if (isVerbose()) {
				logln("RAxML arguments: \n" + arguments.getValue() + getAdditionalArguments()+"\n");
			}
		} else {
			getArguments(arguments, dataFileName, localProteinModel, dnaModel, otherOptions, doBootstrap,bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, true);
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
		return "RAxML";
	}




}
