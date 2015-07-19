/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.TNTRunner;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.TNTTrees.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;

/* TODO:

- set random numbers seed ("RSEED xxx");

- deal with IUPAC and protein data correctly?

 */

public class TNTRunner extends ZephyrRunner  implements ItemListener, ActionListener, ExternalProcessRequester, ZephyrFilePreparer  {
	public static final String SCORENAME = "TNTScore";


	int mxram = 1000;

	boolean preferencesSet = false;
	boolean convertGapsToMissing = true;
	boolean isProtein = false;
	static int REGULARSEARCH=0;
	static int BOOTSTRAPSEARCH=1;
	static int JACKKNIFESEARCH=2;
	static int SYMSEARCH=3;
	static int POISSONSEARCH=4;
	int searchStyle = REGULARSEARCH;
	boolean resamplingAllConsensusTrees=false;  //if true, will pull in each of the consensus trees (one from each rep) from a resampling run


	int bootstrapreps = 100;
	long bootstrapSeed = System.currentTimeMillis();
	//	boolean doBootstrap= false;
	String otherOptions = "";

	boolean parallel = false;
	int numSlaves = 6;
	String bootstrapSearchArguments = "";
	String searchArguments = "";
	boolean harvestOnlyStrictConsensus = false;
	String searchScriptPath = "";
	String bootSearchScriptPath = "";


	long  randseed = -1;
	String constraintfile = "none";
	int totalNumHits = 250;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class, getName() + "  needs a module to run an external process.","");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		if (!hireExternalProcessRunner()){
			return sorry("Couldn't hire an external process runner");
		}
		externalProcRunner.setProcessRequester(this);

		return true;
	}
	/*.................................................................................................................*/
	 public String getExternalProcessRunnerModuleName(){
			return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	 }
	/*.................................................................................................................*/
	 public Class getExternalProcessRunnerClass(){
			return LocalScriptRunner.class;
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

	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootstrapreps = MesquiteInteger.fromString(content);
		if ("mxram".equalsIgnoreCase(tag))
			mxram = MesquiteInteger.fromString(content);
		if ("convertGapsToMissing".equalsIgnoreCase(tag))
			convertGapsToMissing = MesquiteBoolean.fromTrueFalseString(content);
		if ("resamplingAllConsensusTrees".equalsIgnoreCase(tag))
			resamplingAllConsensusTrees = MesquiteBoolean.fromTrueFalseString(content);
		if ("harvestOnlyStrictConsensus".equalsIgnoreCase(tag))
			harvestOnlyStrictConsensus = MesquiteBoolean.fromTrueFalseString(content);
		if ("searchStyle".equalsIgnoreCase(tag))
			searchStyle = MesquiteInteger.fromString(content);
		if ("searchArguments".equalsIgnoreCase(tag))
			searchArguments = StringUtil.cleanXMLEscapeCharacters(content);
		if ("bootstrapSearchArguments".equalsIgnoreCase(tag))
			bootstrapSearchArguments = StringUtil.cleanXMLEscapeCharacters(content);
		if ("otherOptions".equalsIgnoreCase(tag))
			otherOptions = StringUtil.cleanXMLEscapeCharacters(content);

		//parallel, etc.
		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "mxram", mxram);  
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "convertGapsToMissing", convertGapsToMissing);  
		StringUtil.appendXMLTag(buffer, 2, "resamplingAllConsensusTrees", resamplingAllConsensusTrees);  
		StringUtil.appendXMLTag(buffer, 2, "harvestOnlyStrictConsensus", harvestOnlyStrictConsensus);  
		StringUtil.appendXMLTag(buffer, 2, "searchArguments", searchArguments);  
		StringUtil.appendXMLTag(buffer, 2, "bootstrapSearchArguments", bootstrapSearchArguments);  
		StringUtil.appendXMLTag(buffer, 2, "searchStyle", searchStyle);  
		StringUtil.appendXMLTag(buffer, 2, "otherOptions", otherOptions);  

		preferencesSet = true;
		return buffer.toString();
	}

	public void setDefaultTNTCommandsSearchOptions(){
		bootstrapSearchArguments="";
		bootstrapSearchArguments +=   getTNTCommand("rseed[");   // if showing intermediate trees
		bootstrapSearchArguments +=   getTNTCommand("hold 3000");   
		//	bootstrapSearchArguments +=   " sect: slack 5"+getComDelim();   
		//	bootstrapSearchArguments +=   " xmult: replications 2 hits 2 ratchet 15 verbose drift 10"+getComDelim();  
		bootstrapSearchArguments +=   getTNTCommand("sect: slack 30");   
		bootstrapSearchArguments +=   getTNTCommand("sec: xss 4-2+3-1 gocomb 60 fuse 4 drift 5 combstart 5");   
		bootstrapSearchArguments +=   getTNTCommand("xmult: replications 1 hits 1 ratchet 15 verbose rss xss drift 10 dumpfuse") ;   // actual search

		searchArguments="";
		searchArguments +=   getTNTCommand("rseed[");   
		searchArguments +=   getTNTCommand("hold 10000");   
		searchArguments +=   getTNTCommand("xinact");   
		searchArguments +=   getTNTCommand("xmult:  rss css fuse 6 drift 6 ratchet 20 replic 100");   
		searchArguments +=   getTNTCommand("sec: slack 20");   
		searchArguments +=   getTNTCommand("bbreak: tbr safe fillonly") ;   // actual search
		searchArguments +=   getTNTCommand("xmult");   
		searchArguments +=   getTNTCommand("bbreak");  

		resamplingAllConsensusTrees=false;
		harvestOnlyStrictConsensus=false;
		bootstrapreps = 100;
		
	}
	public void setDefaultTNTCommandsOtherOptions(){
		otherOptions = "";   
		convertGapsToMissing = true;
	}
	/*.................................................................................................................*/
	void formCommandFile(String dataFileName, int firstOutgroup) {
		if (parallel) {
			commands = "";
		}
		commands += getTNTCommand("mxram " + mxram);

		commands += getTNTCommand("report+0/1/0");
		commands += getTNTCommand("log "+logFileName) ; 
		commands += getTNTCommand("p " + dataFileName);
		commands += getTNTCommand("vversion");
		if (MesquiteInteger.isCombinable(firstOutgroup) && firstOutgroup>=0)
			commands += getTNTCommand("outgroup " + firstOutgroup);
		if (bootstrapOrJackknife()) {
			if (parallel) {
				commands += indentTNTCommand("ptnt begin parallelRun " + numSlaves + "/ram x 2 = ");
			}
			if (StringUtil.notEmpty(bootSearchScriptPath)) {
				String script = MesquiteFile.getFileContentsAsString(bootSearchScriptPath);
				if (StringUtil.notEmpty(script))
					commands += script;
			}
			else commands += StringUtil.lineEnding() + bootstrapSearchArguments + StringUtil.lineEnding();
			String saveTreesString="";
			if (resamplingAllConsensusTrees)
				saveTreesString= " savetrees "; 
			String bootSearchString = " [xmult; bb]";
			bootSearchString="";
			
			if (parallel) {
				int numRepsPerSlave = bootstrapreps/numSlaves;
				if (numRepsPerSlave*numSlaves<bootstrapreps) numRepsPerSlave++;
				if (searchStyle==BOOTSTRAPSEARCH)
					commands += getTNTCommand("resample boot cut 50 " + saveTreesString +" replications " + numRepsPerSlave + " [xmult; bb] savetrees"); // + getComDelim();   
				else if (searchStyle==JACKKNIFESEARCH)
					commands += getTNTCommand("resample jak cut 50 " + saveTreesString +" replications " + numRepsPerSlave + " [xmult; bb] savetrees"); // + getComDelim();   
				else if (searchStyle==SYMSEARCH)
					commands += getTNTCommand("resample sym cut 50 " + saveTreesString +" replications " + numRepsPerSlave + " [xmult; bb] savetrees"); // + getComDelim();   
				else if (searchStyle==POISSONSEARCH)
					commands += getTNTCommand("resample poisson cut 50 " + saveTreesString +" replications " + numRepsPerSlave + " [xmult; bb] savetrees"); // + getComDelim();   
				commands += getTNTCommand("return");
				commands += getTNTCommand("ptnt wait parallelRun");
				commands += getTNTCommand("ptnt get parallelRun");
			} else {
				if (!resamplingAllConsensusTrees) {
					commands +=  getTNTCommand("macro=");				
					commands +=  getTNTCommand("ttags =");		
				}
				commands +=  getTNTCommand("tsave *" + treeFileName);				
				if (searchStyle==BOOTSTRAPSEARCH)
					commands += getTNTCommand("resample boot " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
				else if (searchStyle==JACKKNIFESEARCH)
					commands += getTNTCommand("resample jak cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
				else if (searchStyle==SYMSEARCH)
					commands += getTNTCommand("resample sym cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
				else if (searchStyle==POISSONSEARCH)
					commands += getTNTCommand("resample poisson cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
				if (!resamplingAllConsensusTrees)
					commands += getTNTCommand("save *") ; 
				else 
					commands += getTNTCommand("save") ; 
				commands += getTNTCommand("tsave/") ; 
				if (!resamplingAllConsensusTrees) {
					commands +=  getTNTCommand("ttags -/");		
					commands +=  getTNTCommand("macro-");				
				}
			}

			//commands += getTNTCommand("proc/") ; 

			commands += getTNTCommand("log/") ; 


			//	if (!parallel)
			commands += getTNTCommand("quit") ; 
		}
		else {
			//commands += getTNTCommand("tsave !5 " + treeFileName) ;   // if showing intermediate trees
			commands +=  getTNTCommand("tsave *" + treeFileName);
			if (StringUtil.notEmpty(searchScriptPath)) {
				String script = MesquiteFile.getFileContentsAsString(searchScriptPath);
				if (StringUtil.notEmpty(script))
					commands += script;
			}
			else commands += searchArguments;
			commands += otherOptions;
			if (harvestOnlyStrictConsensus) 
				commands += getTNTCommand("nelsen *") ; 
			commands +=   getTNTCommand("save");   
			commands += getTNTCommand("log/") ; 

			commands += getTNTCommand("tsave/") ; 
			commands += getTNTCommand("quit") ; 
		}
	}


	public void intializeAfterExternalProcessRunnerHired() {
		setDefaultTNTCommandsSearchOptions();
		setDefaultTNTCommandsOtherOptions();
		loadPreferences();
	}

	public void reconnectToRequester(MesquiteCommand command){
		continueMonitoring(command);
	}

	/*.................................................................................................................*/
	void adjustDialogText() {
		if (bootStrapRepsField!=null)
			if (parallel)
				bootStrapRepsField.setLabelText("Resampling Replicates Per Slave");
			else
				bootStrapRepsField.setLabelText("Resampling Replicates");
	}
	ExtensibleDialog queryOptionsDialog=null;
	IntegerField bootStrapRepsField = null;
	Button useDefaultsButton = null;
	Button useDefaultsOtherOptionsButton = null;
	TextArea searchField = null;
	TextArea bootstrapSearchField = null;
	TextArea otherOptionsField = null;
	SingleLineTextField searchScriptPathField=null;
	SingleLineTextField bootSearchScriptPathField=null;
	Checkbox convertGapsBox=null;
	Checkbox harvestOnlyStrictConsensusBox=null;
	Checkbox resamplingAllConsensusTreesBox=null;

	/*.................................................................................................................*/
	static int QUERYDOSEARCH = 0;
	static int QUERYSTORE = 1;
	static int QUERYCANCEL = 2;
	public boolean queryOptions() {
		if (!okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying Options"))  //Debugg.println needs to check that options set well enough to proceed anyway
			return true;
		boolean closeWizard = false;

		if ((MesquiteTrunk.isMacOSXBeforeSnowLeopard()) && MesquiteDialog.currentWizard == null) {
			CommandRecord cRec = null;
			cRec = MesquiteThread.getCurrentCommandRecordDefIfNull(null);
			if (cRec != null){
				cRec.requestEstablishWizard(true);
				closeWizard = true;
			}
		}

		MesquiteInteger buttonPressed = new MesquiteInteger(1);

		queryOptionsDialog = new ExtensibleDialog(containerOfModule(), "TNT Options & Locations",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//		queryOptionsDialog.addLabel("TNT - Options and Locations");
		String helpString = "This module will prepare a matrix for TNT, and ask TNT do to an analysis.  A command-line version of TNT must be installed. "
				+ "You can ask it to do a bootstrap analysis or not. "
				+ "Mesquite will read in the trees found by TNT. ";

		queryOptionsDialog.appendToHelpString(helpString);
		queryOptionsDialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());

		MesquiteTabbedPanel tabbedPanel = queryOptionsDialog.addMesquiteTabbedPanel();

		tabbedPanel.addPanel("TNT Program Details", true);
		externalProcRunner.addItemsToDialogPanel(queryOptionsDialog);
		Checkbox parallelCheckBox = queryOptionsDialog.addCheckBox("use PVM for parallel processing", parallel);
		parallelCheckBox.addItemListener(this);
		IntegerField slavesField = queryOptionsDialog.addIntegerField("Number of Slaves", numSlaves, 4, 0, MesquiteInteger.infinite);
		IntegerField maxRamField = queryOptionsDialog.addIntegerField("mxram value", mxram, 4, 0, MesquiteInteger.infinite);


		tabbedPanel.addPanel("Search Options", true);
		Choice searchStyleChoice = queryOptionsDialog.addPopUpMenu("Type of search/resampling:", new String[] {"Regular Search",  "Bootstrap", "Jackknife", "Symmetric Resampled",  "Poisson Bootstrap"}, searchStyle);
		queryOptionsDialog.addLabel("Regular Search Commands");
		searchField = queryOptionsDialog.addTextAreaSmallFont(searchArguments, 7,80);
		searchScriptPathField = queryOptionsDialog.addTextField("Path to TNT run file containing search commands", searchScriptPath, 40);
		Button browseSearchScriptPathButton = queryOptionsDialog.addAListenedButton("Browse...",null, this);
		browseSearchScriptPathButton.setActionCommand("browseSearchScript");
		harvestOnlyStrictConsensusBox = queryOptionsDialog.addCheckBox("only acquire strict consensus", harvestOnlyStrictConsensus);
		queryOptionsDialog.addHorizontalLine(1);
		//		Checkbox doBootstrapBox = queryOptionsDialog.addCheckBox("do bootstrapping", doBootstrap);
		bootStrapRepsField = queryOptionsDialog.addIntegerField("Resampling Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		queryOptionsDialog.addLabel("Resampling Search Commands");
		bootstrapSearchField = queryOptionsDialog.addTextAreaSmallFont(bootstrapSearchArguments, 7,80);
		bootSearchScriptPathField = queryOptionsDialog.addTextField("Path to TNT run file containing search commands for resampled", bootSearchScriptPath, 30);
		Button browseBootSearchScriptPathButton = queryOptionsDialog.addAListenedButton("Browse...",null, this);
		browseSearchScriptPathButton.setActionCommand("browseBootSearchScript");
		 resamplingAllConsensusTreesBox = queryOptionsDialog.addCheckBox("allow TNT to calculate consensus tree", !resamplingAllConsensusTrees);

		adjustDialogText();
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addNewDialogPanel();
		useDefaultsButton = queryOptionsDialog.addAListenedButton("Set to Defaults", null, this);
		useDefaultsButton.setActionCommand("setToDefaults");

		tabbedPanel.addPanel("Other Options", true);
		convertGapsBox = queryOptionsDialog.addCheckBox("convert gaps to missing (to avoid gap=extra state)", convertGapsToMissing);
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addLabel("Post-Search TNT Commands");
		otherOptionsField = queryOptionsDialog.addTextAreaSmallFont(otherOptions, 7, 80);
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addNewDialogPanel();
		useDefaultsOtherOptionsButton = queryOptionsDialog.addAListenedButton("Set to Defaults", null, this);
		useDefaultsOtherOptionsButton.setActionCommand("setToDefaultsOtherOptions");


		tabbedPanel.cleanup();
		queryOptionsDialog.nullifyAddPanel();

		queryOptionsDialog.completeAndShowDialog("Search", "Cancel", null, null);

		if (buttonPressed.getValue()==0)  {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (externalProcRunner.optionsChosen() && infererOK) {
				bootstrapreps = bootStrapRepsField.getValue();
				numSlaves = slavesField.getValue();
				otherOptions = otherOptionsField.getText();
				convertGapsToMissing = convertGapsBox.getState();
				parallel = parallelCheckBox.getState();
				//				doBootstrap = doBootstrapBox.getState();
				searchArguments = searchField.getText();
				searchScriptPath = searchScriptPathField.getText();
				bootstrapSearchArguments = bootstrapSearchField.getText();
				bootSearchScriptPath = bootSearchScriptPathField.getText();
				harvestOnlyStrictConsensus = harvestOnlyStrictConsensusBox.getState();
				resamplingAllConsensusTrees = !resamplingAllConsensusTreesBox.getState();
				searchStyle = searchStyleChoice.getSelectedIndex();
				mxram = maxRamField.getValue();

				storeRunnerPreferences();
			}
		}
		queryOptionsDialog.dispose();
		if (closeWizard) 
			MesquiteDialog.closeWizard();

		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*
	public boolean queryTaxaOptions(Taxa taxa) {
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (ssv==null || ssv.size()<=0)
			return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "TNT Outgroup Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("TNT Outgroup Options");


		Choice taxonSetChoice = null;
		taxonSetChoice = dialog.addPopUpMenu ("Outgroups: ", ssv, 0);


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {

			outgroupTaxSetString = taxonSetChoice.getSelectedItem();

		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("setToDefaults")) {
			setDefaultTNTCommandsSearchOptions();
			searchField.setText(searchArguments);	
			bootstrapSearchField.setText(bootstrapSearchArguments);
			harvestOnlyStrictConsensusBox.setState(harvestOnlyStrictConsensus);
			resamplingAllConsensusTreesBox.setState(!resamplingAllConsensusTrees);
			bootStrapRepsField.setValue(bootstrapreps);

		} else if (e.getActionCommand().equalsIgnoreCase("setToDefaultsOtherOptions")) {
			setDefaultTNTCommandsOtherOptions();
			otherOptionsField.setText(otherOptions);
			convertGapsBox.setState(convertGapsToMissing);
		} else if (e.getActionCommand().equalsIgnoreCase("browseSearchScript") && searchScriptPathField!=null) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String path = MesquiteFile.openFileDialog("Choose Search Script File", directoryName, fileName);
			if (StringUtil.notEmpty(path))
				searchScriptPathField.setText(path);
		} else if (e.getActionCommand().equalsIgnoreCase("browseBootSearchScript") && bootSearchScriptPathField!=null) {

			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String path = MesquiteFile.openFileDialog("Choose Resampling Search Script File", directoryName, fileName);
			if (StringUtil.notEmpty(path))
				bootSearchScriptPathField.setText(path);
		}
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent arg0) {
		if (queryOptionsDialog!=null) {
			adjustDialogText();	
		}
	}

	/*.................................................................................................................*/
	public void setTNTSeed(long seed){
		this.randseed = seed;
	}

	int count=0;

	public void prepareExportFile(FileInterpreterI exporter) {
		((InterpretHennig86Base)exporter).setConvertGapsToMissing(convertGapsToMissing);
		((InterpretHennig86Base)exporter).setIncludeQuotes(false);
	}

	/*.................................................................................................................*/
	String getComDelim(){
		if (externalProcRunner.isWindows())
			return ";"+StringUtil.lineEnding();
		else
			return ";"+StringUtil.lineEnding();
	}

	String commands = "";

	/*.................................................................................................................*
	String prettyCommand(String command) {
		return "   "+command.replaceAll(";", ";" + StringUtil.lineEnding()+"  ");
	}
	/*.................................................................................................................*/
	String getTNTCommand(String command) {
		return "   " + command + getComDelim();
	}
	/*.................................................................................................................*/
	String indentTNTCommand(String command) {
		return "   " + command;
	}



	String treeFileName="TNT_Trees.txt";
	//	String currentTreeFileName;
	String logFileName;
	String commandsFileName;


	/*.................................................................................................................*/
	public void setFileNames(){
		/*if (bootstrap())
			treeFileName = "TNT_bootstrapTrees.txt";
		else 
			treeFileName = "TNT_Trees.txt";  */
		//		currentTreeFileName = treeFileName+"-1";
		logFileName = "log.out";
		commandsFileName = "TNTCommands.txt";
	}
	/*.................................................................................................................*/

	static final int OUT_TREEFILE=0;
	static final int OUT_LOGFILE = 1;
	public String[] getLogFileNames(){
		return new String[]{treeFileName,  logFileName};
	}

	/*.................................................................................................................*/
	public static final NameReference TNTtoAnalyze = NameReference.getNameReference("TNTtoAnalyze"); 

	//this stores matrix-specific information on the taxa to be included in the run
	Associable taxaInfo; 
	public Associable getTaxaInfo(CharacterData data, boolean makeIfNotPresent){
		if (makeIfNotPresent && taxaInfo == null){
			taxaInfo = new TaxaInfo(data.getNumTaxa(), data);
		}
		return taxaInfo;
	}

	/* .......................................... DNAData .................................................. */
	public long getTaxonNumberInTree(Taxa taxa, int it) {
		Associable tInfo = getTaxaInfo(data, true);
		if (tInfo!=null) {
			return (int)tInfo.getAssociatedLong(TNTtoAnalyze,it);
		}
		return -1;
	}
	/* .......................................... MolecularData .................................................. */
	public void setTaxonNumberInTree(Taxa taxa, int it, int value) {
		Associable tInfo = getTaxaInfo(data, true);
		if (tInfo!=null) {
			tInfo.setAssociatedLong(TNTtoAnalyze, it, value);
		}
	}

	/*.................................................................................................................*/
	public int[] getTaxonNumberTranslation(Taxa taxa) {
		int max = -1;
		for (int it = 0; it<taxa.getNumTaxa(); it++){
			long translateNumber = getTaxonNumberInTree(taxa,it);
			if (MesquiteLong.isCombinable(translateNumber) && translateNumber>max) {
				max=(int)translateNumber;
			}
		}
		int[] translate = new int[max+1];
		for (int it = 0; it<data.getNumTaxa(); it++) {
			long translateNumber = getTaxonNumberInTree(taxa,it);
			if (MesquiteLong.isCombinable(translateNumber) && translateNumber>=0) {
				translate[(int)translateNumber]=it;
			}
		}
		return translate;
	}
	/*.................................................................................................................*/
	public void setTaxonTranslation(Taxa taxa) {
		int countTaxa = 0;
		for (int it = 0; it<data.getNumTaxa(); it++)
			if ((!selectedTaxaOnly || taxa.getSelected(it)) && (data.hasDataForTaxon(it, false))) {
				setTaxonNumberInTree(taxa,it,countTaxa);
				countTaxa++;
			} else
				setTaxonNumberInTree(taxa,it,-1);

	}
	/*.................................................................................................................*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		if (!initializeGetTrees(CategoricalData.class, taxa, matrix))
			return null;
		setTNTSeed(seed);
		isProtein = data instanceof ProteinData;

		//David: if isDoomed() then module is closing down; abort somehow

		//write data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "TNT","-Run.");  
		if (tempDir==null)
			return null;
		String dataFileName = "data.ss";   //replace this with actual file name?
		String dataFilePath = tempDir +  dataFileName;
		FileInterpreterI exporter = ZephyrUtil.getFileInterpreter(this,"#InterpretTNT");
		if (exporter==null)
			return null;
		boolean fileSaved = false;
		
		fileSaved = ZephyrUtil.saveExportFile(this,exporter,  dataFilePath,  data, selectedTaxaOnly);
		if (!fileSaved) return null;
		
		setTaxonTranslation(taxa);


		setFileNames();

		TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		int firstOutgroup = MesquiteInteger.unassigned;
		if (outgroupSet!=null)
			firstOutgroup = outgroupSet.firstBitOn();
		formCommandFile(dataFileName, firstOutgroup);
		logln("\n\nCommands given to TNT:");
		logln(commands);
		logln("");

		MesquiteString arguments = new MesquiteString();
		arguments.setValue(" proc " + commandsFileName);

		String programCommand = externalProcRunner.getExecutableCommand();


		int numInputFiles = 2;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i=0; i<numInputFiles; i++){
			fileContents[i]="";
			fileNames[i]="";
		}
		fileContents[0] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[0] = dataFileName;
		fileContents[1] = commands;
		fileNames[1] = commandsFileName;


		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, fileContents, fileNames,  ownerModule.getName());

		if (!isDoomed()){
			if (success){
			getProject().decrementProjectWindowSuppression();
			return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
		} else
			postBean("unsuccessful [1]", false);
		}
		if (getProject() != null)
			getProject().decrementProjectWindowSuppression();
		if (data == null)
			data.setEditorInhibition(false);
		return null;
	}	


	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		logln("Preparing to receive TNT trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		//TODO		finalScore.setValue(finalValue);

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

		int[] taxonNumberTranslation = getTaxonNumberTranslation(taxa);
		
		MesquiteBoolean readSuccess = new MesquiteBoolean(false);
		//TreeVector tv = new TreeVector(taxa);
		if (bootstrapOrJackknife()) {
			if (resamplingAllConsensusTrees)
				t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNT " + getResamplingKindName() + " Rep", 0, readSuccess, false, false, null, taxonNumberTranslation);  // set first tree number as 0 as will remove the first one later.
			else
				t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNT " + getResamplingKindName() + " Majority Rule Tree", 1, readSuccess, false, false, freqRef, taxonNumberTranslation);
		}
		else
			t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNTTree", 1, readSuccess, false, harvestOnlyStrictConsensus, null, taxonNumberTranslation);
		success = t!=null;
		if (success && bootstrapOrJackknife() && resamplingAllConsensusTrees) {
			t = t.cloneTree();
			treeList.removeElementAt(0, false);  // get rid of first one as this is the bootstrap tree
		}

		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success) {
			logln("Execution of TNT unsuccessful [2]");
			postBean("unsuccessful [2]", false);
		} else
			postBean("successful", false);


		getProject().decrementProjectWindowSuppression();
		if (data!=null)
			data.setEditorInhibition(false);
		//	manager.deleteElement(tv);  // get rid of temporary tree block
		if (success) 
			return t;
		return null;
	}



	public String getProgramName() {
		return "TNT";
	}

	public String getExecutableName() {
		return "TNT";
	}

	/*.................................................................................................................*
	String getProgramCommand(String arguments){
		String command = "";
		if (MesquiteTrunk.isWindows())
			command += StringUtil.protectForWindows(TNTPath)+ arguments;
		else
			command += StringUtil.protectForUnix(TNTPath )+ arguments;
		return command;
	}
	/*.................................................................................................................*/

	//	Parser parser = new Parser();
	//	long screenFilePos = 0;
	//	MesquiteFile screenFile = null;

	/*.................................................................................................................*
	public String getOutputFileToReadPath(String originalPath) {
		//File file = new File(originalPath);
		//File fileCopy = new File(originalPath + "2");
		String newPath = originalPath + "2";
		MesquiteFile.copyFileFromPaths(originalPath, newPath, false);
		//		if (file.renameTo(fileCopy))
		if (MesquiteFile.fileExists(newPath)) {
			return newPath;
		}
		return originalPath;
	}
	
		/*.................................................................................................................*/
	public  boolean showMultipleRuns() {
		return false;
	}

	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted()) || logFileNames==null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];



		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0]) && !bootstrapOrJackknife()) {   // tree file
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

			}
			else ((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, null);
		} 	
		else	if (fileNum==1 && outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1]) && !bootstrapOrJackknife()) {   // log file
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator!=null) {
						parser.setString(s);
						String rep = parser.getFirstToken(); // generation number
						logln("");
						if (MesquiteInteger.isNumber(rep)) {
							int numReps = MesquiteInteger.fromString(rep)+1;
							progIndicator.setText("Replicate: " + numReps);// + ", ln L = " + parser.getNextToken());
							if (bootstrapOrJackknife()) {
								logln("Replicate " + numReps + " of " + bootstrapreps);
							}
							logln("Replicate " + numReps + " of " + totalNumHits);

							progIndicator.spin();		
							double timePerRep=0;
							if (MesquiteInteger.isCombinable(numReps) && numReps>0){
								timePerRep = timer.timeSinceVeryStartInSeconds()/numReps;   //this is time per rep
							}
							int timeLeft = 0;
							if (bootstrapOrJackknife()) {
								timeLeft = (int)((bootstrapreps- numReps) * timePerRep);
							}
							else {
								String token = parser.getNextToken();  //algorithm
								token = parser.getNextToken();  //Tree
								token = parser.getNextToken();  //Score
								String best = parser.getNextToken();  //Best
								logln("  Score " +  token + "; best found so far " + best);
								timeLeft = (int)((totalNumHits- numReps) * timePerRep);
							}

							logln("  Running time so far " +  StringUtil.secondsToHHMMSS((int)timer.timeSinceVeryStartInSeconds())  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
						}
					}
				count++;
			} else
				Debugg.println("*** File does not exist (" + filePath + ") ***");
		}




	}
	/*.................................................................................................................*/

	public void processCompletedOutputFiles(String[] outputFilePaths) {
		if (outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1])) {
			ZephyrUtil.copyLogFile(this, "TNT", outputFilePaths[1]);

		}
		if (outputFilePaths.length>2 && !StringUtil.blank(outputFilePaths[2])) {
			ZephyrUtil.copyOutputText(this,outputFilePaths[2], commands);
		}
	}

	public boolean continueShellProcess(Process proc){
		if (progIndicator.isAborted()) {
			try {
				Writer stream;
				stream = new OutputStreamWriter((BufferedOutputStream)proc.getOutputStream());
				stream.write((char)3);
				stream.flush();
				stream.close();
			}
			catch (IOException e) {
			}
			return false;
		}
		return true;
	}

	public String getResamplingKindName() {
		if (searchStyle==BOOTSTRAPSEARCH )
			return "Bootstrap";
		if (searchStyle==JACKKNIFESEARCH )
			return "Jackknife";
		if (searchStyle==SYMSEARCH)
			return "Symmetric Resampling";
		if (searchStyle==POISSONSEARCH)
			return "Poisson Bootstrap";
		return "Bootstrap";
	}

	public boolean bootstrapOrJackknife() {
		return searchStyle==BOOTSTRAPSEARCH || searchStyle==JACKKNIFESEARCH || searchStyle==SYMSEARCH || searchStyle==POISSONSEARCH;
	}
	public  boolean doMajRuleConsensusOfResults(){
		return bootstrapOrJackknife() && resamplingAllConsensusTrees ;
	}

	public  boolean singleTreeFromResampling(){
		return bootstrapOrJackknife() && resamplingAllConsensusTrees ;
	}

	public int getBootstrapreps() {
		return bootstrapreps;
	}

	public void setBootstrapreps(int bootstrapreps) {
		this.bootstrapreps = bootstrapreps;
	}

	/*.................................................................................................................*
	public void setTNTPath(String TNTPath){
		this.TNTPath = TNTPath;
	}
	/*.................................................................................................................*/

	public Class getDutyClass() {
		return TNTRunner.class;
	}

	public String getParameters() {
		return "TNT commands: " + commands;
	}

	public String getName() {
		return "TNT Runner";
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




}
