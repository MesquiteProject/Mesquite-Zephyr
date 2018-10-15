/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLRunnerSSH;


import java.awt.*;
import java.io.*;
import java.awt.event.*;


import mesquite.lib.*;
import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public class RAxMLRunnerSSH extends RAxMLRunner  implements ActionListener, ItemListener, ExternalProcessRequester  {

	//boolean RAxML814orLater = false;
	static final int THREADING_OTHER =0;
	static final int THREADING_PTHREADS = 1;
	static final int THREADING_MPI = 2;
	int threadingVersion = THREADING_OTHER;
	int numProcessors = 2;
	boolean RAxML814orLater = false;


	boolean showIntermediateTrees = true;


	//Checkbox RAxML814orLaterCheckbox;


	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.SSHRunner.SSHRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return SSHRunner.class;
	 }

		public String getLogText() {
			String log= externalProcRunner.getStdOut();
			if (StringUtil.blank(log))
				log="Waiting for log file from SSH...";
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
		return "?????";
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("SSH analysis completed "+StringUtil.getDateTime()+"\n");
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
		return "RAxML Options on SSH";
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
	/*.................................................................................................................*
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
	
	public int minimumNumSearchReplicates() {
		return 2;
	}
	/*.................................................................................................................*/

	static final int DATAFILENUMBER = 0;
	static final int MULTIMODELFILENUMBER = 1;
	static final int CONSTRAINTFILENUMBER = 3;
	
	String inputFilesInRunnerObject = "";

	public void prepareRunnerObject(Object obj){
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



	public String getExecutableName() {
		return "RAxML";
	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	public Class getDutyClass() {
		return RAxMLRunnerSSH.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "RAxML Likelihood (SSH)";
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
