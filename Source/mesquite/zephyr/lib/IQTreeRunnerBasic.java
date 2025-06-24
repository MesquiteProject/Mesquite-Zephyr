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

//import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.RadioButtons;
import mesquite.lib.ui.SingleLineTextField;
import mesquite.io.ExportFusedPhylip.ExportFusedPhylip;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;



public abstract class IQTreeRunnerBasic extends IQTreeRunner  implements ActionListener, ItemListener, ExternalProcessRequester  {

	protected int numProcessors = 2;
	protected boolean autoNumProcessors = true;


	protected boolean showIntermediateTrees = true;



	protected SingleLineTextField MPISetupField;
	protected IntegerField numProcessorsField;
	RadioButtons numProcessorsRadioButtons;


	public String getExecutableName() {
		return "IQ-TREE";
	}

	public String getLogText() {
		return externalProcRunner.getStdOut();
	}
	/*.................................................................................................................*/
	public int getProgramNumber() {
		return -1;
	}

	/*.................................................................................................................*
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = super.getSnapshot(file);
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
		if ("autoNumProcessors".equalsIgnoreCase(tag))
			autoNumProcessors = MesquiteBoolean.fromTrueFalseString(content);

		if ("numProcessors".equalsIgnoreCase(tag))
			numProcessors = MesquiteInteger.fromString(content);

		super.processSingleXMLPreference(tag, content);

		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "autoNumProcessors", autoNumProcessors);  
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  

		buffer.append(super.preparePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "1.6.4-1.6.12, 2.2.0–2.3.6";
	}

	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				if (searchStyle==ULTRAFASTBOOTSTRAP) {
					s+="UF Bootstrap analysis<br>";
					if (numUFBootRuns>1) {   //TODO: detect number of current runs.
						s+="UF Bootstrap runs completed: <b>";
						s+=numRunsCompleted +" of " + numUFBootRuns;
					}
					
				} else {
					s+="Bootstrap analysis<br>";
					s+="Bootstrap replicates completed: <b>";
					if (numRunsCompleted>bootstrapreps)
						s+=numSearchRuns +" of " + bootstrapreps;
					else
						s+=numRunsCompleted +" of " + bootstrapreps;
				}
			}
			else {
				s+="Search for ML Tree<br>";
			}
			s+="</b>";
		}
		return s;
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		super.appendAdditionalSearchDetails();
		MesquiteString arguments = (MesquiteString)getProgramArguments(getDataFileName(),getSetsFileName(), false, numPartsInStartingPartition);
		if (arguments!=null && !arguments.isBlank()){
			appendToSearchDetails("\n" + getProgramName() + " command options: " + arguments.toString());
		}
	}
	/*.................................................................................................................*/
	public  String queryOptionsDialogTitle() {
		return getExecutableName() + " Options & Locations";
	}

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		dialog.addHorizontalLine(1);
		int defNumProc = 0;
		if (!autoNumProcessors)
			defNumProc = 1;
		numProcessorsRadioButtons = dialog.addRadioButtons(new String[] {"Let IQ-TREE choose number of processor cores", "Specify number of processor cores"}, defNumProc);
		numProcessorsRadioButtons.addItemListener(this);
		numProcessorsField = dialog.addIntegerField("Number of cores", numProcessors, 8, 1, MesquiteInteger.infinite);
		numProcessorsField.getTextField().setEnabled(numProcessorsRadioButtons.getValue() == 1);
		dialog.addHorizontalLine(1);

	//	dialog.addLabelSmallText("This version of Zephyr tested on the following "+getExecutableName()+" version(s): " + getTestedProgramVersions());
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent e) {
			numProcessorsField.getTextField().setEnabled(numProcessorsRadioButtons.getValue() == 1);
			super.itemStateChanged(e);
	}

	/*.................................................................................................................*/
	public void processRunnerOptions() {
		autoNumProcessors = numProcessorsRadioButtons.getValue() == 0;
		numProcessors = numProcessorsField.getValue(); //
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase(composeProgramCommand)) {

			MesquiteString arguments = new MesquiteString();
			getArguments(arguments, "[fileName]", "[setsBlockFileName]", substitutionModelField.getText(), otherOptionsField.getText(), 
					searchStyleButtons.getValue(), bootStrapRepsField.getValue(), bootstrapSeed, numSearchRunsField.getValue(),
					numUFBootRunsField.getValue(), charPartitionButtons.getValue(), partitionLinkageChoice.getSelectedIndex(), 
					outgroupTaxSetString, null,  alrtBox.getState(), alrtRepsField.getValue(),false);
			String command = externalProcRunner.getExecutableCommand() + arguments.getValue();
			commandLabel.setText("This command will be used to run IQ-TREE:");
			commandField.setText(command);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		} 
	}
	/*.................................................................................................................*/
	public void setProgramSeed(long seed){
		this.randseed = seed;
	}




	/*.................................................................................................................*/
	void getArguments(MesquiteString arguments, String fileName, 
			String setsFileName,
			String LOCSubstitutionModel, String LOCotherOptions, 
			int LOCsearchStyle, int LOCbootstrapreps, int LOCbootstrapSeed, 
			int LOCnumSearchRuns, 
			int LOCnumUFBootRuns, 
			int LOCPartitionScheme,
			int LOCPartitionLinkage,
			String LOCoutgroupTaxSetString, String LOCMultipleModelFile, 
			boolean LOCdoALRT,
			int LOCalrtReps,
			boolean preflight){
		if (arguments == null)
			return;

		String localArguments = "";



		if (preflight)
			localArguments += " -n preflight.out "; 
		else
			localArguments += " -s " + fileName ; 

		if (LOCsearchStyle==STANDARDSEARCH) {
			if (LOCnumSearchRuns>1)
				localArguments += " --runs " + LOCnumSearchRuns; 
		} else {
			if (LOCsearchStyle==ULTRAFASTBOOTSTRAP) {
				if (LOCbootstrapreps>0)
					localArguments += " -bb " + LOCbootstrapreps+" -wbt "; 
				else
					localArguments += " -bb 1000 -wbt";
				if (LOCnumUFBootRuns>1)
					localArguments += " --runs " + LOCnumUFBootRuns; 
				if (doALRT) {
					if (LOCalrtReps>1)
						localArguments += " -alrt " +LOCalrtReps;
					else 
						localArguments += " -alrt 1000";
				}
			} else {  // normal bootstrap
				if (LOCbootstrapreps>0)
					localArguments += " -bo " + LOCbootstrapreps; 
				else
					localArguments += " -bo 100 ";
			}
			localArguments += " -seed " + LOCbootstrapSeed;

		}


		if (StringUtil.notEmpty(LOCSubstitutionModel))
			localArguments += " -m " + LOCSubstitutionModel;

		if (useConstraintTree)
			localArguments += " -g constraintTree.tre "; 

		if (LOCPartitionScheme!=noPartition) {
			if (LOCPartitionLinkage==qPartitionLinkage)
				localArguments += " -q " + setsFileName;
			else if (LOCPartitionLinkage==sppPartitionLinkage)
				localArguments += " -spp " + setsFileName;
			else if (LOCPartitionLinkage==spPartitionLinkage)
				localArguments += " -sp " + setsFileName;

		}

		localArguments += " -pre " + getOutputFilePrefix();

		if (!StringUtil.blank(LOCotherOptions)) 
			localArguments += " " + LOCotherOptions;

		/*
		if (StringUtil.notEmpty(LOCMultipleModelFile))
			localArguments += " -q " + ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);

		localArguments += " -p " + randomIntSeed;


		 */
		arguments.setValue(localArguments);
	}


	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (!bootstrapOrJackknife() && numSearchRuns>1 ) {
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



	TaxaSelectionSet outgroupSet;

	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	String arguments;
	/*.................................................................................................................*/
	public Object getProgramArguments(String dataFileName, String setsFileName, boolean isPreflight, int numParts) {
		MesquiteString arguments = new MesquiteString();

		if (!isPreflight) {
			getArguments(arguments, dataFileName, setsFileName, substitutionModel, otherOptions, searchStyle, bootstrapreps, bootstrapSeed, numSearchRuns, numUFBootRuns, partitionScheme, partitionLinkage, outgroupTaxSetString, null, doALRT, alrtReps, false);
		} else {
			getArguments(arguments, dataFileName, setsFileName, substitutionModel, otherOptions, searchStyle, bootstrapreps, bootstrapSeed, numSearchRuns, numUFBootRuns, partitionScheme, partitionLinkage, outgroupTaxSetString, null, doALRT, alrtReps, true);
		}
		if (autoNumProcessors)
			arguments.append(" -nt AUTO ");   
		else
			arguments.append(" -nt "+ MesquiteInteger.maximum(numProcessors, 1) + " ");   // have to ensure that there are at least two threads requested

		if (!isPreflight && isVerbose())
			logln(getExecutableName() + " arguments: \n" + arguments.getValue() + "\n");

		return arguments; // + " | tee log.txt"; // + "> log.txt";

	}


	/*.................................................................................................................*/


	public boolean singleTreeFromResampling() {
		return false;
	}


	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -2100;  
	}



	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}





}
