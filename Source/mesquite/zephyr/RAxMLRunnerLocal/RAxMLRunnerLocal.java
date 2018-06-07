/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.RAxMLRunnerLocal;


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
import mesquite.zephyr.RAxMLTreesLocal.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public class RAxMLRunnerLocal extends RAxMLRunner  implements ActionListener, ItemListener, ExternalProcessRequester  {

	static final int THREADING_OTHER =0;
	static final int THREADING_PTHREADS = 1;
	static final int THREADING_MPI = 2;
	int threadingVersion = THREADING_OTHER;
	int numProcessors = 2;
	boolean RAxML814orLater = false;


	boolean showIntermediateTrees = true;



	SingleLineTextField MPISetupField;
	IntegerField numProcessorsField;
	RadioButtons threadingRadioButtons;
	Checkbox RAxML814orLaterCheckbox;


	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}

	public String getExecutableName() {
		return "RAxML";
	}

	public String getLogText() {
		return externalProcRunner.getStdOut();
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

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("RAxML814orLater".equalsIgnoreCase(tag))
			RAxML814orLater = MesquiteBoolean.fromTrueFalseString(content);

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
		StringUtil.appendXMLTag(buffer, 2, "RAxML814orLater", RAxML814orLater);  
		StringUtil.appendXMLTag(buffer, 2, "raxmlThreadingVersion", threadingVersion);  
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  

		buffer.append(super.preparePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "8.0.0–8.2.11";
	}

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

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		dialog.addHorizontalLine(1);
		dialog.addLabel("RAxML parallelization style:");
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"non-PThreads", "PThreads"}, threadingVersion);
		numProcessorsField = dialog.addIntegerField("Number of Processors", numProcessors, 8, 1, MesquiteInteger.infinite);
		dialog.addHorizontalLine(1);
		
		RAxML814orLaterCheckbox = dialog.addCheckBox("RAxML version 8.1.4 or later", RAxML814orLater);
		dialog.addLabelSmallText("This version of Zephyr tested on the following RAxML version(s): " + getTestedProgramVersions());
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		RAxML814orLater = RAxML814orLaterCheckbox.getState();
		threadingVersion = threadingRadioButtons.getValue();
		numProcessors = numProcessorsField.getValue(); //
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase(composeProgramCommand)) {

			MesquiteString arguments = new MesquiteString();
			getArguments(arguments, "[fileName]", proteinModelField.getText(), proteinModelMatrixChoice.getSelectedItem(), dnaModelField.getText(), otherOptionsField.getText(), doBootstrapCheckbox.getState(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null, nobfgsCheckBox.getState(), false);
			String command = externalProcRunner.getExecutableCommand() + arguments.getValue();
			commandLabel.setText("This command will be used to run RAxML:");
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




	/*.................................................................................................................*/
	void getArguments(MesquiteString arguments, String fileName, String LOCproteinModel, String LOCproteinModelMatrix,
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
				localArguments += LOCproteinModel+LOCproteinModelMatrix;
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
			localArguments += " -r constraintTree.tre "; 
		else if (useConstraintTree == MONOPHYLY)
			localArguments += " -g constraintTree.tre "; 
			
		if (LOCdoBootstrap) {
			if (LOCbootstrapreps>0)
				localArguments += " -# " + LOCbootstrapreps + " -b " + LOCbootstrapSeed;
			else
				localArguments += " -# 1 -b " + LOCbootstrapSeed;   // just do one rep
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
	protected void reportStdError() {
		reportStdOutput("RUN UNSUCCESFUL");   //apparently local RAxML does not output problems to stderr, so need to grab stdout
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
		return new String[]{logFileName, treeFileName, "RAxML_info.file.out", workingTreeFileName};
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



	TaxaSelectionSet outgroupSet;

	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	String arguments;
	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, boolean isPreflight) {
		MesquiteString arguments = new MesquiteString();

		if (!isPreflight) {
			getArguments(arguments, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, doBootstrap, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, false);
			if (isVerbose())
				logln("RAxML arguments: \n" + arguments.getValue() + "\n");
		} else {
			getArguments(arguments, dataFileName, proteinModel, proteinModelMatrix, dnaModel, otherOptions, doBootstrap,bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, nobfgs, true);
		}
		if (threadingVersion==THREADING_PTHREADS) {
			arguments.append(" -T "+ MesquiteInteger.maximum(numProcessors, 2) + " ");   // have to ensure that there are at least two threads requested
		}
		return arguments; // + " | tee log.txt"; // + "> log.txt";

	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	public Class getDutyClass() {
		return RAxMLRunnerLocal.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "RAxML Likelihood (Local)";
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
