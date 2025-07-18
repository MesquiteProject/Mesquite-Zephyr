/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;


import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.ProteinData;
import mesquite.io.lib.InterpretHennig86Base;
import mesquite.lib.Associable;
import mesquite.lib.CommandChecker;
import mesquite.lib.CommandRecord;
import mesquite.lib.EmployeeNeed;
import mesquite.lib.IntegerField;
import mesquite.lib.MesquiteBoolean;
import mesquite.lib.MesquiteCommand;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteFileUtil;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteLong;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteThread;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.NameReference;
import mesquite.lib.ResultCodes;
import mesquite.lib.Snapshot;
import mesquite.lib.StringUtil;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.characters.TaxaInfo;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.tree.Tree;
import mesquite.lib.tree.TreeUtil;
import mesquite.lib.tree.TreeVector;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.MesquiteDialog;
import mesquite.lib.ui.MesquiteTabbedPanel;
import mesquite.lib.ui.SingleLineTextField;

/* TODO:

- set random numbers seed ("RSEED xxx");

- deal with IUPAC and protein data correctly?

 */

public abstract class TNTRunner extends ZephyrRunner  implements ItemListener, ActionListener, ExternalProcessRequester, ZephyrFilePreparer  {
	public static final String SCORENAME = "TNTScore";


	int mxram = 1000;

	boolean preferencesSet = false;
	boolean convertGapsToMissing = true;
	boolean isProtein = false;
	final static int REGULARSEARCH=0;
	final static int BOOTSTRAPSEARCH=1;
	final static int JACKKNIFESEARCH=2;
	final static int SYMSEARCH=3;
	final static int POISSONSEARCH=4;
	int searchStyle = REGULARSEARCH;
	boolean resamplingAllConsensusTrees=false;  //if true, will pull in each of the consensus trees (one from each rep) from a resampling run


	int bootstrapreps = 100;
	long bootstrapSeed = System.currentTimeMillis();
	//	boolean doBootstrap= false;
	String otherOptions = "";
	String preLogReadOptions = "";

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
		setUpRunner();

		return true;
	}
	public String getLogFileName(){
		return logFileName;
	}

	/*.................................................................................................................*/
	public int getProgramNumber() {
		return -1;
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = super.getSnapshot(file);
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		temp.addLine("setSearchStyle "+ searchStyleName(searchStyle));  // this needs to be second so that search style isn't reset in starting the runner
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
		} else if (checker.compare(this.getClass(), "sets the searchStyle ", "[searchStyle]", commandName, "setSearchStyle")) {
			searchStyle = getSearchStyleFromName(parser.getFirstToken(arguments));
			return null;
			
		} else
			return super.doCommand(commandName, arguments, checker);
	}	
	

	/*.................................................................................................................*/
	public String searchStyleName(int searchStyle) {
		switch (searchStyle) {
		case REGULARSEARCH:
			return "regular";
		case BOOTSTRAPSEARCH:
			return "bootstrap";
		case JACKKNIFESEARCH:
			return "jackknife";
		case SYMSEARCH:
			return "symsearch";
		case POISSONSEARCH:
			return "poisson";
		default:
			return"";
		}
	}
	/*.................................................................................................................*/
	public int getSearchStyleFromName(String searchName) {
		if (StringUtil.blank(searchName))
				return REGULARSEARCH;
		if (searchName.equalsIgnoreCase("bootstrap"))
			return BOOTSTRAPSEARCH;
		if (searchName.equalsIgnoreCase("jackknife"))
			return JACKKNIFESEARCH;
		if (searchName.equalsIgnoreCase("symsearch"))
			return SYMSEARCH;
		if (searchName.equalsIgnoreCase("poisson"))
			return POISSONSEARCH;
		if (searchName.equalsIgnoreCase("regular"))
			return REGULARSEARCH;			
		return REGULARSEARCH;
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
		if ("preLogReadOptions".equalsIgnoreCase(tag))
			preLogReadOptions = StringUtil.cleanXMLEscapeCharacters(content);
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
		StringUtil.appendXMLTag(buffer, 2, "preLogReadOptions", preLogReadOptions);  

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
		//searchArguments +=   getTNTCommand("bbreak: tbr safe fillonly") ;   // actual search
		searchArguments +=   getTNTCommand("bbreak: tbr fillonly") ;   // actual search.  Removed "safe" Nov 2018
		searchArguments +=   getTNTCommand("xmult");   
		searchArguments +=   getTNTCommand("bbreak");  

		resamplingAllConsensusTrees=false;
		harvestOnlyStrictConsensus=false;
		bootstrapreps = 100;

	}
	public void setDefaultTNTCommandsOtherOptions(){
		otherOptions = "";   
		preLogReadOptions = "";   
		convertGapsToMissing = true;
	}
	public boolean localScriptRunsRequireTerminalWindow(){
		return false;
	}

	public boolean vversionAllowed(){
		return true;
	}

	/*.................................................................................................................*/
	void formCommandFile(String dataFileName, int firstOutgroup) {
		if (parallel) {
			commands = "";
		}
		commands += getTNTCommand("mxram " + mxram);

		commands += getTNTCommand("report+0/1/0");
		commands += preLogReadOptions;
		commands += getTNTCommand("log "+logFileName) ; 
		commands += getTNTCommand("p " + dataFileName);
		if (vversionAllowed())
			commands += getTNTCommand("vversion");
		if (MesquiteInteger.isCombinable(firstOutgroup) && firstOutgroup>=0)
			commands += getTNTCommand("outgroup " + firstOutgroup);
		if (bootstrapOrJackknife()) {
			if (parallel) {
				commands += indentTNTCommand("ptnt begin demo " + numSlaves + "/ram x 2 = ");
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
				commands +=  getTNTCommand("taxname =");			// added 20 May 2024 to ensure proper behavior on reconnect	
				commands +=  getTNTCommand("tsave *" + treeFileName);				
				if (bootstrapAllowed) {
					if (searchStyle==BOOTSTRAPSEARCH)
						commands += getTNTCommand("resample boot " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
					else if (searchStyle==JACKKNIFESEARCH)
						commands += getTNTCommand("resample jak cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
					else if (searchStyle==SYMSEARCH)
						commands += getTNTCommand("resample sym cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
					else if (searchStyle==POISSONSEARCH)
						commands += getTNTCommand("resample poisson cut 50 " + saveTreesString +" replications " + bootstrapreps + bootSearchString); // + getComDelim();   
				}
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

	public void reconnectToRequester(MesquiteCommand command, MesquiteBoolean runSucceeded){
		continueMonitoring(command,  runSucceeded);
	}

	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+=getResamplingKindName()+"<br>";
			}
			else {
				s+="Search for most-parsimonious trees<br>";
			}
			s+="</b>";
		}
		return s;
	}
	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {
		appendToSearchDetails("Search details: \n");
		if (bootstrapOrJackknife()){
			appendToSearchDetails("   "+getResamplingKindName() +"\n");
			appendToSearchDetails("   "+bootstrapreps + " replicates");
		} else {
			appendToSearchDetails("   Search for most-parsimonious trees\n");
			if (MesquiteInteger.isCombinable(numTreesFound))
				appendToSearchDetails("\n   Number of trees found: "+numTreesFound+"\n");
		}
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
	TextArea preLogReadOptionsField = null;
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
		String title = "TNT Options & Locations";
		String extra = getExtraQueryOptionsTitle();
		if (StringUtil.notEmpty(extra))
			title += " ("+extra+")";

		queryOptionsDialog = new ExtensibleDialog(containerOfModule(), title,buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//		queryOptionsDialog.addLabel("TNT - Options and Locations");
		String helpString = "This module will prepare a matrix for TNT, and ask TNT do to an analysis.  A command-line version of TNT must be installed. "
				+ "You can ask it to do a bootstrap analysis or not. "
				+ "Mesquite will read in the trees found by TNT. ";

		queryOptionsDialog.appendToHelpString(helpString);
		queryOptionsDialog.setHelpURL(getHelpURL(zephyrRunnerEmployer));

		MesquiteTabbedPanel tabbedPanel = queryOptionsDialog.addMesquiteTabbedPanel();
		String extraLabel = getLabelForQueryOptions();
		if (StringUtil.notEmpty(extraLabel))
			queryOptionsDialog.addLabel(extraLabel);

		tabbedPanel.addPanel("TNT Program Details", true);
		externalProcRunner.addItemsToDialogPanel(queryOptionsDialog);
		Checkbox parallelCheckBox = queryOptionsDialog.addCheckBox("use PVM for parallel processing", parallel);
		parallelCheckBox.addItemListener(this);
		IntegerField slavesField = queryOptionsDialog.addIntegerField("Number of Slaves", numSlaves, 4, 0, MesquiteInteger.infinite);
		IntegerField maxRamField = queryOptionsDialog.addIntegerField("mxram value", mxram, 4, 0, MesquiteInteger.infinite);


		tabbedPanel.addPanel("Search Options", true);
		Choice searchStyleChoice=null;
		if (bootstrapAllowed)
			searchStyleChoice = queryOptionsDialog.addPopUpMenu("Type of search/resampling:", new String[] {"Regular Search",  "Bootstrap", "Jackknife", "Symmetric Resampled",  "Poisson Bootstrap"}, searchStyle);
		queryOptionsDialog.addLabel("Regular Search Commands");
		searchField = queryOptionsDialog.addTextAreaSmallFont(searchArguments, 7,50);
		searchScriptPathField = queryOptionsDialog.addTextField("Path to TNT run file containing search commands", searchScriptPath, 40);
		Button browseSearchScriptPathButton = queryOptionsDialog.addAListenedButton("Browse...",null, this);
		browseSearchScriptPathButton.setActionCommand("browseSearchScript");
		harvestOnlyStrictConsensusBox = queryOptionsDialog.addCheckBox("only acquire strict consensus", harvestOnlyStrictConsensus);
		queryOptionsDialog.addHorizontalLine(1);
		//		Checkbox doBootstrapBox = queryOptionsDialog.addCheckBox("do bootstrapping", doBootstrap);
		if (bootstrapAllowed) {
			bootStrapRepsField = queryOptionsDialog.addIntegerField("Resampling Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
			queryOptionsDialog.addLabel("Resampling Search Commands");
			bootstrapSearchField = queryOptionsDialog.addTextAreaSmallFont(bootstrapSearchArguments, 7,50);
			bootSearchScriptPathField = queryOptionsDialog.addTextField("Path to TNT run file containing search commands for resampled", bootSearchScriptPath, 30);
			Button browseBootSearchScriptPathButton = queryOptionsDialog.addAListenedButton("Browse...",null, this);
			browseSearchScriptPathButton.setActionCommand("browseBootSearchScript");
			resamplingAllConsensusTreesBox = queryOptionsDialog.addCheckBox("allow TNT to calculate consensus tree", !resamplingAllConsensusTrees);
		}

		adjustDialogText();
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addNewDialogPanel();
		useDefaultsButton = queryOptionsDialog.addAListenedButton("Set to Defaults", null, this);
		useDefaultsButton.setActionCommand("setToDefaults");

		tabbedPanel.addPanel("Taxa & Outgroups", true);
		addTaxaOptions(queryOptionsDialog,taxa);

		tabbedPanel.addPanel("Other Options", true);
		convertGapsBox = queryOptionsDialog.addCheckBox("convert gaps to missing (to avoid gap=extra state)", convertGapsToMissing);
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addLabel("Pre-Log, Pre-Read TNT Commands:");
		preLogReadOptionsField = queryOptionsDialog.addTextAreaSmallFont(preLogReadOptions, 4, 80);
		queryOptionsDialog.addLabel("Post-Search TNT Commands:");
		otherOptionsField = queryOptionsDialog.addTextAreaSmallFont(otherOptions, 7, 80);
		queryOptionsDialog.addHorizontalLine(1);
		queryOptionsDialog.addNewDialogPanel();
		useDefaultsOtherOptionsButton = queryOptionsDialog.addAListenedButton("Set to Defaults", null, this);
		useDefaultsOtherOptionsButton.setActionCommand("setToDefaultsOtherOptions");


		tabbedPanel.cleanup();
		queryOptionsDialog.nullifyAddPanel();

		queryOptionsDialog.completeAndShowDialog("Search", "Cancel", null, null);
		boolean acceptableOptions = false;


		if (buttonPressed.getValue()==0)  {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (externalProcRunner.optionsChosen() && infererOK) {
				if (bootstrapAllowed) {
					bootstrapreps = bootStrapRepsField.getValue();
					bootstrapSearchArguments = bootstrapSearchField.getText();
					bootSearchScriptPath = bootSearchScriptPathField.getText();
					harvestOnlyStrictConsensus = harvestOnlyStrictConsensusBox.getState();
					resamplingAllConsensusTrees = !resamplingAllConsensusTreesBox.getState();
					searchStyle = searchStyleChoice.getSelectedIndex();
				}
				numSlaves = slavesField.getValue();
				otherOptions = otherOptionsField.getText();
				preLogReadOptions = preLogReadOptionsField.getText();
				convertGapsToMissing = convertGapsBox.getState();
				processTaxaOptions();
				parallel = parallelCheckBox.getState();
				//				doBootstrap = doBootstrapBox.getState();
				searchArguments = searchField.getText();
				searchScriptPath = searchScriptPathField.getText();
				mxram = maxRamField.getValue();

				storeRunnerPreferences();
				acceptableOptions=true;
			}
		}
		queryOptionsDialog.dispose();
		if (closeWizard) 
			MesquiteDialog.closeWizard();

		return (acceptableOptions);
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
			preLogReadOptionsField.setText(preLogReadOptions);
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
	
	public boolean getDirectProcessConnectionAllowed(){
		return false;
	}
	public boolean requiresLinuxTerminalCommands() {
		return false;
	}
	/*.................................................................................................................*/
	protected String getExecutableCommand() {
		String programCommand = externalProcRunner.getExecutableCommand();
		return programCommand;
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
	int[] taxonNumberTranslation = null;
	FileInterpreterI exporter;
	
	/*.................................................................................................................*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore, MesquiteInteger statusResult) {
		if (!initializeGetTrees(CategoricalData.class, taxa, matrix, statusResult))
			return null;
		setTNTSeed(seed);
		isProtein = data instanceof ProteinData;

		//David: if isDoomed() then module is closing down; abort somehow

// create local version of data file; this will then be copied over to the running location		
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "TNT","-Run.");  
		if (tempDir==null)
			return null;

		String dataFileName = "data.ss";   //replace this with actual file name?
		String dataFilePath = tempDir +  dataFileName;

		exporter = getFileInterpreter(this,"#InterpretTNT");
		if (exporter==null)
			return null;
		boolean fileSaved = false;
		String translationTable = namer.getTranslationTable(taxa);
		((InterpretHennig86Base)exporter).setTaxonNamer(namer);

		fileSaved = ZephyrUtil.saveExportFile(this,exporter,  dataFilePath,  data, selectedTaxaOnly);
		if (!fileSaved) return null;

		String translationFileName = TreeUtil.translationTableFileName;   
		setTaxonTranslation(taxa);
		taxonNumberTranslation = getTaxonNumberTranslation(taxa);
		namer.setNumberTranslationTable(taxonNumberTranslation);


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
		if (MesquiteTrunk.isMacOSX())
			arguments.setValue(" bground proc " + commandsFileName);  //19 May 2024 - added bground
		else
			arguments.setValue(" proc " + commandsFileName);  

		String programCommand = getExecutableCommand();

		if (externalProcRunner instanceof ScriptRunner){
			String path =((ScriptRunner)externalProcRunner).getExecutablePath();	//programCommand += StringUtil.lineEnding();  
			if (path != null)
				logln("Running TNT version at " + path);
		}
		if (StringUtil.blank(programCommand)) {
			MesquiteMessage.discreetNotifyUser("Path to TNT not specified!");
			((InterpretHennig86Base)exporter).setTaxonNamer(null);
			return null;
		}



		int numInputFiles = 4;
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
		fileContents[2] = translationTable;
		fileNames[2] = translationFileName;
		fileContents[3] = getRunInformation(arguments);
		fileNames[3] = runInformationFileName;
		int runInformationFileNumber = 3;

		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, null, fileContents, fileNames,  ownerModule.getName(),runInformationFileNumber);

		MesquiteFile.deleteDirectory(tempDir);  //delete directory in Support Files

		if (!isDoomed()){
			if (success){
				desuppressProjectPanelReset();
				return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
			} else {
				if (statusResult != null)
				statusResult.setValue(ResultCodes.ERROR);
				if (!beanWritten)
					postBean("unsuccessful [1] | "+externalProcRunner.getDefaultProgramLocation());
				beanWritten=true;
			}
		}
		desuppressProjectPanelReset();
		if (data == null)
			data.decrementEditInhibition();
		externalProcRunner.setLeaveAnalysisDirectoryIntact(true);  // we don't want to delete the directory here
		externalProcRunner.finalCleanup();
		((InterpretHennig86Base)exporter).setTaxonNamer(null);
		return null;
	}	

	int numTreesFound = MesquiteInteger.unassigned;
	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		logln("Preparing to receive TNT trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		//TODO		finalScore.setValue(finalValue);

		suppressProjectPanelReset();
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed. 
		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		String treeFilePath = outputFilePaths[OUT_TREEFILE];
		taxonNumberTranslation = getTaxonNumberTranslation(taxa);
		namer.setNumberTranslationTable(taxonNumberTranslation);

		runFilesAvailable();

		// read in the tree files
		success = false;
		Tree t= null;


		MesquiteBoolean readSuccess = new MesquiteBoolean(false);
		//TreeVector tv = new TreeVector(taxa);
		if (bootstrapOrJackknife()) {
			if (resamplingAllConsensusTrees)
				t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNT " + getResamplingKindName() + " Rep", 0, readSuccess, false, false, null, namer);  // set first tree number as 0 as will remove the first one later.
			else
				t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNT " + getResamplingKindName() + " Majority Rule Tree", 1, readSuccess, false, false, freqRef, namer);
		}
		else
			t =ZephyrUtil.readTNTTreeFile(this,treeList, taxa,treeFilePath, "TNTTree", 1, readSuccess, false, harvestOnlyStrictConsensus, null, namer);
		success = t!=null;
		if (success && bootstrapOrJackknife() && resamplingAllConsensusTrees) {
			t = t.cloneTree();
			treeList.removeElementAt(0, false);  // get rid of first one as this is the bootstrap tree
		}

		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success) {
			logln("Execution of TNT unsuccessful [2]");
			if (!beanWritten)
				postBean("unsuccessful [2] | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten=true;
		} else {
			numTreesFound = treeList.getNumberOfTrees();
			if (!beanWritten)
				postBean("successful | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten=true;
		}


		desuppressProjectPanelReset();
		if (data!=null)
			data.decrementEditInhibition();
		//	manager.deleteElement(tv);  // get rid of temporary tree block
		externalProcRunner.finalCleanup();
		if (exporter!=null)
			((InterpretHennig86Base)exporter).setTaxonNamer(null);
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
		if ((progIndicator!=null && progIndicator.isAborted())) {
			setUserAborted(true);
			return;
		}
		if (logFileNames==null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];



		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0]) && !bootstrapOrJackknife()) {   // tree file
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;
				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

				}
				else ((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
				reportNewTreeAvailable();

			}
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
			} else if (MesquiteTrunk.debugMode)
				logln("*** File does not exist (" + filePath + ") ***");
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
			setUserAborted(true);
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
		if (!bootstrapAllowed)
			return false;
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
		return "TNT Parsimony";
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
