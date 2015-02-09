/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

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
import mesquite.zephyr.GarliRunner.GarliRunner;
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

	int randomIntSeed = (int)System.currentTimeMillis();   // convert to int as RAxML doesn't like really big numbers

	boolean showIntermediateTrees = true;


	long  randseed = -1;

	SingleLineTextField MPISetupField;
	javax.swing.JLabel commandLabel;
	SingleLineTextArea commandField;
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
		return "8.0.0 and 8.1.4";
	}
	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"other", "PThreads version"}, threadingVersion);
		numProcessorsField = dialog.addIntegerField("Number of Processors", numProcessors, 8, 1, MesquiteInteger.infinite);
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
		if (e.getActionCommand().equalsIgnoreCase("composeRAxMLCommand")) {

			MesquiteString arguments = new MesquiteString();
			getArguments(arguments, "fileName", proteinModelField.getText(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null, false);
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
	void getArgumentsPise(MultipartEntityBuilder builder, String fileName, String LOCproteinModel, String LOCdnaModel, String LOCotherOptions, int LOCbootstrapreps, int LOCbootstrapSeed, int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, boolean preflight){
		if (builder==null)
			return;
		
		if (preflight)
			arguments += " -n preflight.out "; 
		else
			arguments += " -s " + fileName + " -n file.out "; 
		
		
		//arguments += " -m "; 
		if (isProtein) {
			if (StringUtil.blank(LOCproteinModel))
				builder.addTextBody("input.protein_opts_", "PROTGAMMAJTT");
			else
				builder.addTextBody("input.protein_opts_", LOCproteinModel);
		}
		else if (StringUtil.blank(LOCdnaModel))
			builder.addTextBody("input.dna_gtrcat_", "GTRGAMMA");
		else
			builder.addTextBody("input.dna_gtrcat_",LOCdnaModel);

		if (StringUtil.notEmpty(LOCMultipleModelFile))
			arguments += " -q " + ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);

		arguments += " -p " + randomIntSeed;


		if (!StringUtil.blank(LOCotherOptions)) 
			arguments += " " + LOCotherOptions;

		if (bootstrapOrJackknife() && LOCbootstrapreps>0) {
			arguments += " -# " + LOCbootstrapreps + " -b " + LOCbootstrapSeed;
		}
		else {
			if (LOCnumRuns>1)
				arguments += " -# " + LOCnumRuns;
			if (RAxML814orLater)
				arguments += " --mesquite";
		}

		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				arguments += " -o " + outgroupSet.getStringList(",", true);
		}

	}	
	
	/*.................................................................................................................*/
	void getArguments(MesquiteString arguments, String fileName, String LOCproteinModel, String LOCdnaModel, String LOCotherOptions, int LOCbootstrapreps, int LOCbootstrapSeed, int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, boolean preflight){
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

		if (bootstrapOrJackknife() && LOCbootstrapreps>0) {
			localArguments += " -# " + LOCbootstrapreps + " -b " + LOCbootstrapSeed;
		}
		else {
			if (LOCnumRuns>1)
				localArguments += " -# " + LOCnumRuns;
			if (RAxML814orLater)
				localArguments += " --mesquite";
		}

		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				localArguments += " -o " + outgroupSet.getStringList(",", true);
		}

		arguments.setValue(localArguments);
	}


	static final int OUT_LOGFILE=0;
	static final int OUT_TREEFILE=1;
	static final int OUT_SUMMARYFILE=2;
	/*.................................................................................................................*/
	public String[] getLogFileNames(){
		String treeFileName;
		String logFileName;
		if (bootstrapOrJackknife())
			treeFileName = "RAxML_bootstrap.file.out";
		else 
			treeFileName = "RAxML_result.file.out";
		logFileName = "RAxML_log.file.out";
		if (!bootstrapOrJackknife() && numRuns>1) {
			treeFileName+=".RUN.";
			logFileName+=".RUN.";
		}
		return new String[]{logFileName, treeFileName, "RAxML_info.file.out"};
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
			getArguments(arguments, dataFileName, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, false);
			logln("RAxML arguments: \n" + arguments.getValue() + "\n");
		} else {
			getArguments(arguments, dataFileName, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, true);
		}
		if (threadingVersion==THREADING_PTHREADS) {
			arguments.append(" -T "+ MesquiteInteger.maximum(numProcessors, 2) + " ");   // have to ensure that there are at least two threads requested
		}
		return arguments;

	}







	/*.................................................................................................................*
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore){
		logln("Preparing to receive RAxML trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		finalScore.setValue(finalValue);

		if (getProject()!=null)
			getProject().incrementProjectWindowSuppression();
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed. 
		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		String treeFilePath = outputFilePaths[OUT_TREEFILE];
		
		runFilesAvailable();

		// read in the tree files
		success = false;
		Tree t= null;
		int count =0;
		MesquiteBoolean readSuccess = new MesquiteBoolean(false);
		if (bootstrapOrJackknife()) {
			t =readRAxMLTreeFile(treeList, treeFilePath, "RAxML Bootstrap Tree", readSuccess, false);
			ZephyrUtil.adjustTree(t, outgroupSet);
		}
		else if (numRuns==1) {
			t =readRAxMLTreeFile(treeList, treeFilePath, "RAxMLTree", readSuccess, true);
			ZephyrUtil.adjustTree(t, outgroupSet);
		}
		else if (numRuns>1) {
			TreeVector tv = new TreeVector(taxa);
			for (int run=0; run<numRuns; run++)
				if (MesquiteFile.fileExists(treeFilePath+run)) {
					String path =treeFilePath+run;
					t = readRAxMLTreeFile(tv, path, "RAxMLTree Run " + (run+1), readSuccess, true);
					if (treeList!= null)
						treeList.addElement(t, false);
				}
			if (treeList !=null) {
				String summary = MesquiteFile.getFileContentsAsString(outputFilePaths[OUT_SUMMARYFILE]);
				Parser parser = new Parser(summary);
				parser.setAllowComments(false);
				parser.allowComments = false;

				String line = parser.getRawNextDarkLine();
				logln("\nSummary of RAxML Search");

				
				while (!StringUtil.blank(line) && count < finalValues.length) {
					if (line.startsWith("Inference[")) {
						Parser subParser = new Parser();
						subParser.setString(line);
						String token = subParser.getFirstToken();   // should be "Inference"
						while (!StringUtil.blank(token) && ! subParser.atEnd()){
							if (token.indexOf("likelihood")>=0) {
								token = subParser.getNextToken();
								finalValues[count] = -MesquiteDouble.fromString(token);
								//	finalScore[count].setValue(finalValues[count]);
								//logln("RAxML Run " + (count+1) + " ln L = -" + finalValues[count]);
							}
							token = subParser.getNextToken();
						}
						count++;
					}
					parser.setAllowComments(false);
					line = parser.getRawNextDarkLine();
				}

				count =0;

				while (!StringUtil.blank(line) && count < optimizedValues.length) {
					if (line.startsWith("Inference[")) {
						Parser subParser = new Parser();
						subParser.setString(line);
						String token = subParser.getFirstToken();   // should be "Inference"
						while (!StringUtil.blank(token) && ! subParser.atEnd()){
							if (token.indexOf("Likelihood")>=0) {
								token = subParser.getNextToken(); // :
								token = subParser.getNextToken(); // -
								optimizedValues[count] = -MesquiteDouble.fromString(token);
								//	finalScore[count].setValue(finalValues[count]);
								//logln("RAxML Run " + (count+1) + " ln L = -" + optimizedValues[count]);
							}
							token = subParser.getNextToken();
						}
						count++;
					}
					parser.setAllowComments(false);
					line = parser.getRawNextDarkLine();
				}
				
				boolean summaryWritten = false;
				for (int i=0; i<finalValues.length && i<optimizedValues.length; i++){
					if (MesquiteDouble.isCombinable(finalValues[i]) && MesquiteDouble.isCombinable(optimizedValues[i])) {
							logln("  RAxML Run " + (i+1) + " ln L = -" + finalValues[i] + ",  final gamma-based ln L = -" + optimizedValues[i]);
							summaryWritten = true;
					}
				}
				if (!summaryWritten)
					logln("  No ln L values for RAxML Runs available");


				double bestScore =MesquiteDouble.unassigned;
				int bestRun = MesquiteInteger.unassigned;
				for (int i=0; i<treeList.getNumberOfTrees() && i<finalValues.length; i++) {
					Tree newTree = treeList.getTree(i);
					ZephyrUtil.adjustTree(newTree, outgroupSet);
					if (MesquiteDouble.isCombinable(finalValues[i])){
						MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
						s.setName(IOUtil.RAXMLSCORENAME);
						((Attachable)newTree).attachIfUniqueName(s);
					}
					if (MesquiteDouble.isCombinable(optimizedValues[i])){
						MesquiteDouble s = new MesquiteDouble(-optimizedValues[i]);
						s.setName(IOUtil.RAXMLFINALSCORENAME);
						((Attachable)newTree).attachIfUniqueName(s);
					}

					if (MesquiteDouble.isCombinable(finalValues[i]))
						if (MesquiteDouble.isUnassigned(bestScore)) {
							bestScore = finalValues[i];
							bestRun = i;
						}
						else if (bestScore>finalValues[i]) {
							bestScore = finalValues[i];
							bestRun = i;
						}
				}

				if (MesquiteInteger.isCombinable(bestRun)) {
					t = treeList.getTree(bestRun);
					ZephyrUtil.adjustTree(t, outgroupSet);

					String newName = t.getName() + " BEST";
					if (t instanceof AdjustableTree )
						((AdjustableTree)t).setName(newName);
				}
				if (MesquiteDouble.isCombinable(bestScore)){
					logln("Best score: " + bestScore);
					finalScore.setValue(bestScore);
				}
				//Only retain the best tree in tree block.
				if(treeList.getTree(bestRun) != null && onlyBest){
					Tree bestTree = treeList.getTree(bestRun);
					treeList.removeAllElements(false);
					treeList.addElement(bestTree, false);
				}
			} 

		}
		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success)
			logln("Execution of RAxML unsuccessful [2]");

		if (getProject()!=null)
			getProject().decrementProjectWindowSuppression();
		if (data!=null)
			data.setEditorInhibition(false);
		//	manager.deleteElement(tv);  // get rid of temporary tree block
		if (success) {
			postBean("successful", false);
			return t;
		}
		postBean("failed, retrieveTreeBlock", false);
		return null;
	}	

	/*.................................................................................................................*
	String getProgramCommand(int threadingVersion, String LOCMPIsetupCommand, int LOCnumProcessors, String LOCraxmlPath, String arguments, boolean protect){
		String command = "";
		if (threadingVersion == threadingMPI) {
			if (!StringUtil.blank(LOCMPIsetupCommand)) {
				command += LOCMPIsetupCommand+ "\n";
			}
			command += "mpirun -np " + LOCnumProcessors + " ";
		}

		String fullArguments = arguments;
		if (threadingVersion==threadingPThreads) {
			fullArguments += " -T " + LOCnumProcessors + " ";
		}


		if (!protect)
			command += LOCraxmlPath + fullArguments;
		else if (MesquiteTrunk.isWindows())
			command += StringUtil.protectForWindows(LOCraxmlPath)+ fullArguments;
		else
			command += StringUtil.protectForUnix(LOCraxmlPath )+ fullArguments;
		return command;
	}

	/*.................................................................................................................*

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted()) || logFileNames==null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];

		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0]) && !bootstrapOrJackknife()) {   // screen log
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator!=null) {
						parser.setString(s);
						String gen = parser.getFirstToken(); 
						String lnL = parser.getNextToken();
						progIndicator.setText("ln L = " + lnL);
						logln("    ln L = " + lnL);
						progIndicator.spin();		

					}
//				count++;
			} 
		}

		if (fileNum==1 && outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1]) && !bootstrapOrJackknife() && showIntermediateTrees) {   // tree file
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

			}
			else ((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, null);
		}
		
		//David: if isDoomed() then module is closing down; abort somehow

		if (fileNum==2 && outputFilePaths.length>2 && !StringUtil.blank(outputFilePaths[2])) {   // info file
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath,2);
				if (!StringUtil.blank(s)) {
					Parser parser = new Parser();
					parser.allowComments=false;
					parser.setString(s);
					String searchString = "";
					if (bootstrapOrJackknife())
						searchString = "Bootstrap";
					else
						searchString = "Inference";

					if (s.startsWith(searchString+"[")) {
						Parser subParser = new Parser();
						subParser.setString(s);
						subParser.allowComments=false;

						String token = subParser.getFirstToken();
						boolean watchForNumber = false;
						boolean numberFound = false;
						runNumber=0;
						boolean foundRunInfo=false;
						while (!StringUtil.blank(token) && ! subParser.atEnd()){
							if (watchForNumber) {
								runNumber = MesquiteInteger.fromString(token);
								numberFound = true;
								watchForNumber = false;
							}
							if (token.equalsIgnoreCase(searchString) && !numberFound) {
								token = subParser.getNextToken();
								if (token.equals("["))
									watchForNumber = true;
							}
							if (token.indexOf("likelihood")>=0) {
								token = subParser.getNextToken();

								numRunsCompleted++;
								if (bootstrapOrJackknife()){
									logln("RAxML bootstrap replicate " + numRunsCompleted + " of " + bootstrapreps+" completed");
								}
								else
									logln("RAxML Run " + (runNumber+1) + ", final score ln L = " +token );
								//processOutputFile(outputFilePaths,1);
								foundRunInfo = true;
							}
							if (foundRunInfo)
								token="";
							else
								token = subParser.getNextToken();
						}
						if (externalProcRunner.canCalculateTimeRemaining(numRunsCompleted)) {
							double timePerRep = timer.timeSinceVeryStartInSeconds()/numRunsCompleted;   //this is time per rep
							int timeLeft = 0;
							if (bootstrapOrJackknife()) {
								timeLeft = (int)((bootstrapreps- numRunsCompleted) * timePerRep);
							}
							else {
								timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
							}
							double timeSoFar = timer.timeSinceVeryStartInSeconds();
							logln("   Run time " +  StringUtil.secondsToHHMMSS((int)timeSoFar)  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
							logln("    Average time per replicate:  " +  StringUtil.secondsToHHMMSS((int)timePerRep));
							logln("    Estimated total time:  " +  StringUtil.secondsToHHMMSS((int)(timeSoFar+timeLeft))+"\n");
						}

						if (!bootstrapOrJackknife() && runNumber+1<numRuns) {
							logln("");
							logln("Beginning Run " + (runNumber+2));
						}
					}
				}

			} 
		}

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
		return "RAxML Local Runner";
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

	public String getProgramName() {
		return "RAxML";
	}




}
