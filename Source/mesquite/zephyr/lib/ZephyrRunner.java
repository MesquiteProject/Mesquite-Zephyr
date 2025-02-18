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
import java.awt.ItemSelectable;
import java.util.Random;

import mesquite.categ.lib.CategoricalData;
import mesquite.consensus.lib.BasicTreeConsenser;
import mesquite.externalCommunication.lib.*;
import mesquite.externalCommunication.AppHarvester.*;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.FileCoordinator;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.duties.TWindowMaker;
import mesquite.lib.duties.TreeInferer;
import mesquite.lib.duties.TreeSearcher;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.taxa.TaxonNamer;
import mesquite.lib.tree.MesquiteTree;
import mesquite.lib.tree.Tree;
import mesquite.lib.tree.TreeVector;
import mesquite.lib.ui.AlertDialog;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.MesquiteWindow;
import mesquite.lib.ui.ProgressIndicator;
import mesquite.trees.lib.SimpleTreeWindow;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;

public abstract class ZephyrRunner extends MesquiteModule implements ExternalProcessRequester, OutputFilePathModifier, AppUser{

	protected TreeInferer treeInferer = null;
	public static String runInformationFileName = "runInformation.txt";

	String[] logFileNames;
	protected static String VERSIONFILENAME = "version.txt";
	public ExternalProcessRunner externalProcRunner;
	protected ProgressIndicator progIndicator;
	protected CategoricalData data;
	protected boolean createdNewDataObject;
	protected MesquiteTimer timer = new MesquiteTimer();
	protected double timeOfEarlyRep = 0.0;
	protected int numEarlyReps = 0;
	protected boolean earlyRepsSet = false;
	protected Taxa taxa;
	protected String unique;
	protected Random rng;
	protected MesquiteModule ownerModule;
	protected ZephyrRunnerEmployer zephyrRunnerEmployer;
	protected boolean selectedTaxaOnly = false;
	protected boolean optionsHaveBeenSet = false;
	protected boolean constrainedSearch = false;
	protected boolean constrainSearchAllowed = true;
	protected String extraQueryOptionsTitle = "";
	private boolean userAborted = false;
	private String programVersion = "";
	protected static String composeProgramCommand = "composeProgramCommand";
	protected boolean hasApp = false;

	protected NameReference freqRef = NameReference.getNameReference("consensusFrequency");

	protected static final int noPartition = 0;
	protected static final int partitionByCharacterGroups = 1;
	protected static final int partitionByCodonPosition = 2;
	protected int partitionScheme = partitionByCharacterGroups;

	protected int currentRun=0;
	protected boolean[] completedRuns=null;
	protected int previousCurrentRun=0;
	protected boolean runInProgress = false;
	protected boolean updateWindow = false;
	protected boolean bootstrapAllowed = true;
	protected boolean beanWritten = false;
	protected boolean onlySetUpRun = false;
	boolean verbose=true;

	protected Tree constraint = null;

	boolean hasBeenReconnected = false;
	protected boolean scriptBasedNoTerminal = false;

	boolean usingBuiltinApp = false;


	protected String outgroupTaxSetString = "";
	protected int outgroupTaxSetNumber = 0;

	public abstract Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore, MesquiteInteger statusResult);
	public  String getVersionAsReportedByProgram(String programCommand) {
		return "";
	}
	public abstract Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore);
	public TreeVector retrieveCurrentMultipleTrees(Taxa taxa) {
		return null;
	}


	public abstract boolean bootstrapOrJackknife();
	public abstract boolean showMultipleRuns();

	/* =================================*/
	//temporarily here? until it can be merged with TreeInferer's version?
	TWindowMaker tWindowMaker;//Debugg.println this should be fired after the run
	BasicTreeConsenser majRulesConsenser; //Debugg.println this should be fired after the run
	int repsInConsensus = 0;

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */
	void prepareConsensusWindow() {
		MesquiteWindow w;
		if (tWindowMaker == null) {
			tWindowMaker = (TWindowMaker)hireNamedEmployee(TWindowMaker.class, "#ObedientTreeWindow");
			if (tWindowMaker == null)
				return;
			w = tWindowMaker.getModuleWindow();
			String commands = "getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
			commands += "setTreeDrawer  #mesquite.trees.SquareLineTree.SquareLineTree; tell It; showEdgeLines off; ";
			commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;  tell It; orientRight; inhibitStretchToggle off; branchLengthsDisplay 0; ";
			commands += " endTell; setEdgeWidth 3; endTell; endTell;";  
			commands += "getEmployee #DrawQuickAssociateOnTree;\ntell It; showAssociate consensusFrequency 2; endTell;";
			CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
			CommandRecord scr = new CommandRecord(true); //this sets as scriptiong just to tell consenser not to ask about options
			MesquiteThread.setCurrentCommandRecord(scr);
			Puppeteer p = new Puppeteer(this);
			p.execute(tWindowMaker, commands, new MesquiteInteger(0), "end;", false);
			MesquiteThread.setCurrentCommandRecord(oldCR);
		}
		else
			w = tWindowMaker.getModuleWindow();


		if (w != null && w instanceof SimpleTreeWindow) 
			((SimpleTreeWindow)w).setWindowTitle("Consensus tree from Inference in Progress");
		tWindowMaker.setWindowVisible(true);

		if (majRulesConsenser == null) {
			CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
			CommandRecord scr = new CommandRecord(true); //this sets as scriptiong just to tell consenser not to ask about options
			MesquiteThread.setCurrentCommandRecord(scr);
			majRulesConsenser = (BasicTreeConsenser)hireNamedEmployee(BasicTreeConsenser.class, "#MajRuleTree");
			Puppeteer p = new Puppeteer(this);
			p.execute(majRulesConsenser, "useWeights off; dumpTable off; frequencyLimit 0.5;", new MesquiteInteger(0), "end;", false);
			MesquiteThread.setCurrentCommandRecord(oldCR);
			majRulesConsenser.initialize();
			majRulesConsenser.reset(taxa);
			repsInConsensus = 0;
		}
		tWindowMaker.setWindowVisible(true);
	}
	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */
	protected void showIntermediateConsensusFromFile(String path) {  //assumes reset to zero && that it's a phylip tree file
		repsInConsensus = 0;
		if (!MesquiteFile.fileExists(path) || taxa == null)
			return;
		prepareConsensusWindow();
		if (majRulesConsenser == null)
			return;
		String[] ds = MesquiteFile.getFileContentsAsStrings(path);
		if (ds == null)
			return;
		for (int i = 0; i<ds.length; i++) {
			Tree rep = ZephyrUtil.readPhylipTree(ds[i],taxa,false,getTaxonNamer());    
			majRulesConsenser.addTree(rep);
			repsInConsensus++;
		}
		MesquiteTree consensus = (MesquiteTree)majRulesConsenser.getConsensus();
		consensus.setName("Majority Rules Consensus of " + repsInConsensus + " trees");
		tWindowMaker.setTree(consensus, false);

	}
	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */
	protected void showIntermediateConsensusAddingTree(String treeDescription) {  //possibly pass boolean to reset?
		if (taxa == null)
			return;
		prepareConsensusWindow();
		if (majRulesConsenser == null)
			return;
		Tree rep = ZephyrUtil.readPhylipTree(treeDescription,taxa,false,getTaxonNamer());    
		majRulesConsenser.addTree(rep);
		repsInConsensus++;

		MesquiteTree consensus = (MesquiteTree)majRulesConsenser.getConsensus();
		consensus.setName("Majority Rules Consensus of " + repsInConsensus + " trees");
		tWindowMaker.setTree(consensus, false);

	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */
	protected int repsInConsensusWindow() {
		return repsInConsensus;
	}

	/* =================================*/

	public  void setOutputTextListener(OutputTextListener textListener){
		if (externalProcRunner != null)
			externalProcRunner.setOutputTextListener(textListener);
	}

	public TreeInferer getTreeInferer() {
		return treeInferer;
	}
	public void setTreeInferer(TreeInferer treeInferer) {
		this.treeInferer = treeInferer;
	}

	public boolean constraintTreeIsNull() {
		return constraint == null;
	}

	public boolean stopExecution(){
		return externalProcRunner.stopExecution();
	}

	public AppUser getAppUser() {
		return this;
	}

	/*.................................................................................................................*/
	// each Runner should have its own interpreter for re-entrancy and parameter setting issues
	FileInterpreterI exporter = null;
	String lastInterpreterName = null;
	public FileInterpreterI getFileInterpreter(MesquiteModule module, String interpreterModuleName) {
		if (exporter == null || lastInterpreterName == null || !lastInterpreterName.equals(interpreterModuleName)) {
			exporter = (FileInterpreterI)hireNamedEmployee(FileInterpreterI.class, interpreterModuleName);
			if (exporter != null)
				lastInterpreterName = interpreterModuleName;
		}
		return exporter;
	}	
	/*.................................................................................................................*/
	public String getNameRefForAssocStrings() {
		return null;
	}
	/*.................................................................................................................*/
	public boolean showAssocStrings() {
		return false;
	}
	/*.................................................................................................................*/
	public void setUpRunner() { 

	}
	/*.................................................................................................................*/

	public MesquiteModule getModule() {
		return this;
	}

	/*.................................................................................................................*/
	public boolean mayHaveProblemsWithDeletingRunningOnReconnect() {
		return false;
	}
	/*.................................................................................................................*/
	public boolean needsHarvestLink() { //Debugg.println: does this also need to check if MacOS?
		return isReconnected() && isScriptBasedNoTerminal() && mayHaveProblemsWithDeletingRunningOnReconnect();
	}
	/*.................................................................................................................*/
	public String getTitleOfTextCommandLink() {
		if (needsHarvestLink())
			return "Harvest";
		return "";
	}
	/*.................................................................................................................*/
	public String getCommandOfTextCommandLink() {
		if (needsHarvestLink())
			return "harvest";
		return "";
	}
	/*.................................................................................................................*/
	public  String bootstrapOrJackknifeTreeListName()         {
		return "Bootstrap Trees";
	}

	public boolean getHasApp() {
		return hasApp;
	}
	public String getAppOfficialName() {
		return getExecutableName();
	}

	public void appChooserDialogBoxEntryChanged() {}

	public boolean getBuiltInExecutableAllowed() {
		return canUseLocalApp() && getHasApp();
	}

	/*.................................................................................................................*/
	public boolean canUseLocalApp() {
		return false;
	}

	/*.................................................................................................................*/
	public AppInformationFile getAppInfoFile() {
		return AppHarvester.getAppInfoFileForProgram(this);
	}
	/*.................................................................................................................*/
	public String getAppVersion()  {
		AppInformationFile appInfoFile = getAppInfoFile();
		if (externalProcRunner.useAppInAppFolder()) 
			if (appInfoFile!=null) {
				if (StringUtil.notEmpty(appInfoFile.getVersion())) {
					return appInfoFile.getVersion();
				}
			}
		return "";
	}
	/*.................................................................................................................*/
	public String getCitation()  {
		String addendum = "";
		AppInformationFile appInfoFile = getAppInfoFile();
		if (externalProcRunner.useAppInAppFolder()) 
			if (appInfoFile!=null) {
				if (StringUtil.notEmpty(appInfoFile.getCitation())) {
					addendum = " " + appInfoFile.getCitation();
				} else if (StringUtil.notEmpty(appInfoFile.getCitationURL())) {
					addendum = " For citation information about " + getProgramName() + " see: " + appInfoFile.getCitationURL();
				}
			}
		return super.getCitation() + addendum;
	}

	protected String getHelpString() {
		//can add more if needed!
		if (externalProcRunner!= null)
			return externalProcRunner.getHelpString(); //override if you want to add more, e.g. about script based analyses
		return "";
	}
	/*.................................................................................................................*/
	public String getHelpURL(ZephyrRunnerEmployer zephyrRunnerEmployer) {
		AppInformationFile appInfoFile = getAppInfoFile();
		if (externalProcRunner.useAppInAppFolder()) 
			if (appInfoFile!=null) {
				if (StringUtil.notEmpty(appInfoFile.getURL()))
					return appInfoFile.getURL();
			}
		if (zephyrRunnerEmployer!=null)
			return zephyrRunnerEmployer.getProgramURL();
		return "";
	}


	/*.................................................................................................................*
	public void examineAppsFolder(String programName) { 
		int numApps = AppHarvester.getNumAppsForProgram(programName);
		if (numApps==1) {
			setHasApp(true);

		} else if (numApps>1) {
			MesquiteMessage.warnUser("There is more than one " + getProgramName() + " app in the apps folder; please remove all but one copy, and restart Mesquite.");
		}
	}

	/*.................................................................................................................*/
	public void processUserClickingOnTextCommandLink(String command) {
		if ("harvest".equalsIgnoreCase(command)) {
			if (AlertDialog.query(containerOfModule(), "Harvest Trees?", "Are you sure you want to harvest the trees?  This should only be done if the run is complete; if this run is NOT complete, harvesting the trees now will prevent Mesquite from harvesting the final trees.", "Harvest", "Cancel")) {
				logln("User request to harvest trees");
				((LocalScriptRunner)externalProcRunner).deleteRunningFile();
			}
		}

	}

	/*.................................................................................................................*/
	public  CategoricalData getData(){
		return data;
	}

	public String getMessageIfUserAbortRequested () {
		if (externalProcRunner!=null)
			return externalProcRunner.getMessageIfUserAbortRequested();
		return "";
	}

	public String getMessageIfCloseFileRequested () {
		if (externalProcRunner!=null)
			return externalProcRunner.getMessageIfCloseFileRequested();
		return "";
	}
	public String getResamplingKindName() {
		return "Bootstrap";
	}

	public String getLogText() {
		return "Runner Text: "+ getName();
	}

	//ZQ this returns false if it is already reconnected; shoiuld return true if the file was opened and reconnected
	public boolean getReadyForReconnectionSave() {
		return  externalProcRunner!=null && externalProcRunner.getReadyForReconnectionSave();
	}

	/*====================================*/
	public String getFileCloseNotification(boolean fileIsDirty){
		boolean farEnoughAlongToReconnect = (externalProcRunner!=null && externalProcRunner.getReadyForReconnectionSave());
		if (!isReconnectable()) {
			return ("If you close the file now, the search will be stopped and you will be NOT able to reconnect to it through Mesquite later. (If you want reconnectability in future runs, use the \"Enable reconnection\" option.)");
		}
		else if (!farEnoughAlongToReconnect)
			return ("If you close the file now (even if you save it), the search will not "
					+ "be successful and you will be NOT able to reconnect to it through Mesquite later, as the process has not proceeded far enough to be reconnectible.  If you wish it to be reconnectible,"
					+ "then press cancel, and try again a bit later.");
		else if (fileIsDirty)
			return ("If you save the file now and permit the analysis to continue, you will be able to close the file and then later reconnect to the analysis by reopening this file, as long as you haven't moved the file or those files involved in the "+ getProgramName() 
			+ " search. \n" + getMessageIfCloseFileRequested());
		else
			return ("If you permit the analysis to continue, you will be able to close the file and later reconnect to the analysis by re-opening the file, as long as you haven't moved the file or those files involved in the "+ getProgramName() 
			+ " search. \n" + getMessageIfCloseFileRequested());
	}

	/*-----------------------------------------------------------------------------------*/
	public boolean queryWhetherToCloseFile(boolean fileIsDirty) {
		boolean askContinue = externalProcRunner.askAboutKillingRun();

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		String title= "Close File?";
		if (fileIsDirty && askContinue) //for some reason when not scripting, file is always dirty, but then later query will check on whether to save.
			title += " Save?";
		if (askContinue)
			title += " Let Analysis Continue?";
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(),  title ,buttonPressed); 

		dialog.addLabel("You have asked to close the file, but there is a run of "+ getProgramName() + " underway.");
		dialog.addBlankLine();
		dialog.addLargeOrSmallTextLabel(getFileCloseNotification(fileIsDirty));
		Checkbox sF = null;
		dialog.addBlankLine();
		if (fileIsDirty && askContinue)
			sF = dialog.addCheckBox("Save File Before Closing", true);
		Checkbox lR = null;
		if (askContinue)
			lR = dialog.addCheckBox("Let Analysis Continue", true);

		dialog.addBlankLine();
		dialog.defaultLabel = "Close File";
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			if (fileIsDirty && sF != null) {
				if (sF.getState()) {
					FileCoordinator fc = getFileCoordinator();
					fc.saveAllFiles();
				}
				else //here file is dirty, user asked not to save, so hope file should be flagged as quit despite dirtiness
					getProject().setIgnoreDirtWhenCloseRequested(true);  

			}
			if (askContinue) {
				if ( lR.getState())
					externalProcRunner.setDontKill(true);
			}
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*====================================*/




	public boolean getDirectProcessConnectionAllowed(){
		return true;
	}


	public boolean getConstrainedSearchAllowed() {
		return constrainSearchAllowed;
	}
	public void setConstainedSearchAllowed(boolean constrainSearchAllowed) {
		this.constrainSearchAllowed = constrainSearchAllowed;;
	}

	Checkbox deleteAnalysisDirectoryCheckBox =  null;

	public  boolean smallerIsBetter (){
		return true;
	}
	public  void addItemsToSOWHDialogPanel(ExtensibleDialog dialog){
	}

	public boolean SOWHoptionsChosen(){
		return true;
	}
	public void resetSOWHOptionsConstrained(){
	}
	public void resetSOWHOptionsUnconstrained(){
	}
	public String getSOWHDetailsObserved(){
		return "";
	}
	public String getSOWHDetailsSimulated(){
		return "";
	}

	/*.................................................................................................................*/
	public void reportNewTreeAvailable(){
	}

	/*.................................................................................................................*/

	public boolean localScriptRunsRequireTerminalWindow(){
		return false;
	}
	public String getExtraQueryOptionsTitle() {
		return extraQueryOptionsTitle;
	}
	public void setExtraQueryOptionsTitle(String extraQueryOptionsTitle) {
		this.extraQueryOptionsTitle = extraQueryOptionsTitle;
	}
	public boolean getUserAborted() {
		return userAborted;
	}
	public void setUserAborted(boolean userAborted) {
		this.userAborted = userAborted;
		if (externalProcRunner!=null)
			externalProcRunner.setUserAborted(userAborted);
	}

	public boolean errorsAreFatal(){
		return false;
	}

	public boolean allowStdErrRedirect() {
		return false;
	}
	/*.................................................................................................................*/
	public String getRunDetailsForHelp() {
		StringBuffer sb = new StringBuffer();
		sb.append("Analysis being conducted by " + getExecutableName()+"<br>");
		if (StringUtil.notEmpty(searchStartedDetails))
			sb.append("Analysis started " + searchStartedDetails +"<br>");
		if (data!=null)
			sb.append("Matrix: " + data.getName());
		return sb.toString();
	}

	/*.................................................................................................................*/
	public String getProgramURL() {
		return "";
	}

	public boolean isConstrainedSearch() {
		return constrainedSearch;
	}
	public void setConstrainedSearch(boolean constrainedSearch) {
		this.constrainedSearch = constrainedSearch;
	}

	public void storeRunnerPreferences() {
		if (externalProcRunner!=null)
			externalProcRunner.storeRunnerPreferences();
		storePreferences();
		if (treeInferer!=null)
			treeInferer.storePreferences();
	}
	int projectPanelSuppressed = 0;  //Debugg.println are there other things that need bailing on????
	protected void suppressProjectPanelReset(){
		if (getProject()==null)
			return;
		getProject().incrementProjectWindowSuppression();
		projectPanelSuppressed++;
	}
	protected void desuppressProjectPanelReset(){
		if (getProject()==null){
			projectPanelSuppressed = 0;
			return;
		}
		getProject().decrementProjectWindowSuppression();
		projectPanelSuppressed--;
	}
	public boolean isVerbose() {
		return verbose;
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	public boolean isReconnectable(){
		if (externalProcRunner != null)
			return externalProcRunner.isReconnectable();
		return false;
	}

	public String getRootNameForDirectory() {
		String name = getProgramName();
		if (isConstrainedSearch())
			name+=".Constrained";
		return name;
	}
	public void setRootNameForDirectoryInProcRunner(){
		externalProcRunner.setRootNameForDirectory(getRootNameForDirectory());
	}


	public boolean getRunInProgress() {
		return runInProgress;
	}
	public void setRunInProgress(boolean runInProgress) {
		this.runInProgress = runInProgress;
	}
	public void cleanupAfterSearch(){
		if (createdNewDataObject && data!=null) {
			data.dispose();
			data=null;
		}
	}

	public abstract boolean doMajRuleConsensusOfResults();
	public abstract boolean singleTreeFromResampling();

	public abstract void reconnectToRequester(MesquiteCommand command, MesquiteBoolean runSucceeded);
	public abstract String getProgramName();
	public abstract boolean queryOptions();

	String queryOptionsLabel = "";
	public String getLabelForQueryOptions(){
		return queryOptionsLabel;
	}
	public void setLabelForQueryOptions(String queryOptionsLabel){
		this.queryOptionsLabel= queryOptionsLabel;
	}

	public abstract String[] getLogFileNames();
	protected SimpleNamesTaxonNamer namer = new SimpleNamesTaxonNamer();

	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		return "";
	}

	public void endJob(){
		if (progIndicator!=null)
			progIndicator.goAway();
		while (projectPanelSuppressed>0)
			desuppressProjectPanelReset();
		super.endJob();
	}
	public void initialize (MesquiteModule ownerModule) {
		this.ownerModule= ownerModule;
		if (ownerModule instanceof ZephyrRunnerEmployer)
			this.zephyrRunnerEmployer = (ZephyrRunnerEmployer)ownerModule;
	}
	/*.................................................................................................................*/
	public String getProgramLocation() {
		if (externalProcRunner!=null)
			return externalProcRunner.getProgramLocation();
		return externalProcRunner.getDefaultProgramLocation();
	}
	/*.................................................................................................................*/
	public void setSearchDetails() {  // for annotation to tree block.  designed to be composed after the tree search started.  
		if (searchDetails!=null) {
			searchDetails.setLength(0);
			searchDetails.append("Mesquite version " + MesquiteTrunk.getMesquiteVersion() + ", build " + MesquiteTrunk.getBuildVersion()+"\n");
			searchDetails.append("Zephyr version " + getPackageIntroModule().getPackageVersion() + ", build " +  getPackageIntroModule().getPackageBuildNumber() +"\n\n");
			searchDetails.append("Trees acquired from " + getProgramName() + " using Mesquite's Zephyr package. \n");
			String version = getProgramVersion();
			if (StringUtil.notEmpty(version))
				searchDetails.append(getProgramName() + " [version "+ version +"] run on " + getProgramLocation() +" \n");
			else 
				searchDetails.append(getProgramName() + " run on " + getProgramLocation() +" \n");
			searchDetails.append("\nAnalysis started " + getDateAndTime()+ "\n");
			if (StringUtil.notEmpty(externalProcRunner.getDirectoryPath()))
				searchDetails.append("Results stored in folder: " + externalProcRunner.getDirectoryPath()+ "\n");
			//NOTE: if above string changes, "Results stored in folder:", then change also string in ProjectWindow, TreesRPanel, variable targetHeading
			searchStartedDetails = getDateAndTime();
		}
	}
	/*.................................................................................................................*/
	public void setExtraSearchDetails(String s) {   // for annotation to tree block;  can include things before search is started. 
		if (extraSearchDetails==null)
			extraSearchDetails = new StringBuffer();
		extraSearchDetails.setLength(0);
		extraSearchDetails.append(s);

	}
	/*.................................................................................................................*/
	public void appendToSearchDetails(String s) {   
		if (searchDetails!=null) {
			searchDetails.append(s);
		}
	}
	/*.................................................................................................................*/
	public void setUpdateWindow(boolean b) {   
		updateWindow=b;
	}
	/*.................................................................................................................*/
	public void setBootstrapAllowed(boolean b) {   
		bootstrapAllowed=b;
	}
	/*.................................................................................................................*/
	public void appendMatrixInformation() {   
		if (data!=null) {
			appendToSearchDetails("\nMatrix: " + data.getName() + "\n");
		}
	}
	/*.................................................................................................................*/
	public void appendToExtraSearchDetails(String s) {
		if (extraSearchDetails==null)
			extraSearchDetails = new StringBuffer();
		extraSearchDetails.append(s);
	}
	public StringBuffer getExtraSearchDetails(){
		return extraSearchDetails;
	}
	public void setAddendumToTreeBlockName(String s){
		if (StringUtil.blank(s) || s.equalsIgnoreCase("null"))
			return;
		if (addendumToTreeBlockName==null)
			addendumToTreeBlockName = new StringBuffer();
		addendumToTreeBlockName.setLength(0);
		addendumToTreeBlockName.append(s);
	}
	public void appendToAddendumToTreeBlockName(String s){
		if (StringUtil.blank(s) || s.equalsIgnoreCase("null"))
			return;
		if (addendumToTreeBlockName==null)
			addendumToTreeBlockName = new StringBuffer();
		addendumToTreeBlockName.append(s);
	}
	public String getAddendumToTreeBlockName(){
		String s= addendumToTreeBlockName.toString();
		if (StringUtil.blank(s))
			return "";
		return s;
	}
	public void prepareRunnerObject(Object obj){
	}
	/*.................................................................................................................*/
	abstract public String getExternalProcessRunnerModuleName();
	/*.................................................................................................................*/
	abstract public Class getExternalProcessRunnerClass();

	public int getCurrentRun() {
		return currentRun;
	}
	/*.................................................................................................................*/

	public boolean hireExternalProcessRunner() {
		if (externalProcRunner ==null) {
			externalProcRunner = (ExternalProcessRunner)hireNamedEmployee(getExternalProcessRunnerClass(), getExternalProcessRunnerModuleName());
			if (externalProcRunner==null)
				return false;
		}
		return true;
	}


	/*.................................................................................................................*/
	public void appendAdditionalSearchDetails() {

	}
	/*.................................................................................................................*/
	public String getPreflightLogFileName(){
		return "";	
	}
	/*.................................................................................................................*/
	public String getProgramVersion(){
		if (StringUtil.notEmpty(programVersion))
			return programVersion;
		return getAppVersion();
	}
	/*.................................................................................................................*/
	public void setProgramVersion(String programVersion){
		this.programVersion = programVersion;
	}
	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "";
	}
	/*.................................................................................................................*/
	public boolean initalizeTaxonNamer(Taxa taxa){
		namer.initialize(taxa);
		return true;
	}
	/*.................................................................................................................*/
	public TaxonNamer getTaxonNamer(){
		return namer;
	}
	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String command){
		return true;
	}
	/*.................................................................................................................*/
	public boolean initializeTaxa (Taxa taxa) {
		Taxa currentTaxa = this.taxa;
		this.taxa = taxa;
		if (taxa!=currentTaxa && taxa!=null) {
			if (!MesquiteThread.isScripting() && !queryTaxaOptions(taxa))
				return false;
		}
		return initalizeTaxonNamer(taxa);
	}

	public void setFileNames () {
	}


	public void initializeMonitoring () {
	}


	/*.................................................................................................................*/
	/** Override this to provide any subclass-specific initialization code needed before QueryOptions is called. */
	public boolean initializeJustBeforeQueryOptions(){
		return true;
	}

	/*.................................................................................................................*/
	protected StringBuffer searchDetails = new StringBuffer();
	protected String searchStartedDetails = "";
	protected StringBuffer extraSearchDetails = new StringBuffer();
	protected StringBuffer addendumToTreeBlockName = new StringBuffer();

	public String getSearchDetails(){
		return searchDetails.toString();
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (data != null)
			temp.addLine("recoverData #" + data.getAssignedIDNumber());
		temp.addLine("recoverSearchDetails " + ParseUtil.tokenize(searchDetails.toString()));
		temp.addLine("recoverSearchStartedDetails " + ParseUtil.tokenize(searchStartedDetails));
		temp.addLine("recoverExtraSearchDetails " + ParseUtil.tokenize(extraSearchDetails.toString()));
		temp.addLine("recoverAddendumToTreeBlockName " + ParseUtil.tokenize(addendumToTreeBlockName.toString()));
		if (externalProcRunner!=null)
			if (externalProcRunner.isScriptBased() && !externalProcRunner.isVisibleTerminal())
				temp.addLine("scriptBasedNoTerminal");
		if (tWindowMaker!= null)
			temp.addLine("getIntermTreeWindowMaker ", tWindowMaker);
		if (majRulesConsenser!= null)
			temp.addLine("majRulesConsenser ", majRulesConsenser);
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Recovers search details from previous run", "[search details]", commandName, "recoverSearchDetails")) {
			searchDetails.setLength(0);
			searchDetails.append(parser.getFirstToken(arguments));
		}
		else if (checker.compare(this.getClass(), "returns the window maker", "null", commandName, "getIntermTreeWindowMaker")) {
			if (this instanceof RemoteProcessCommunicator) { //for some reason when remote, the window doesn's thow properly if not made here
				if (tWindowMaker == null) 
					tWindowMaker = (TWindowMaker)hireNamedEmployee(TWindowMaker.class, "#ObedientTreeWindow");
				if (tWindowMaker != null) {
					MesquiteWindow w = tWindowMaker.getModuleWindow();
					if (w != null && w instanceof SimpleTreeWindow) {
						((SimpleTreeWindow)w).setWindowTitle("Inference in Progress: Consensus Tree");
						tWindowMaker.setWindowVisible(false);
					}
				}
				return tWindowMaker; }
			else
				return new MesquiteCommandAbsorber();

		}
		else if (checker.compare(this.getClass(), "Nonfunctional ; eats up consenser", "null", commandName, "majRulesConsenser")) {
			return new MesquiteCommandAbsorber();
		}
		else if (checker.compare(this.getClass(), "Specifies that is scriptBased with no terminal window", "[]", commandName, "scriptBasedNoTerminal")) {
			setScriptBasedNoTerminal(true);
		}
		else if (checker.compare(this.getClass(), "Recovers time the search started details from previous run", "[search started details]", commandName, "recoverSearchStartedDetails")) {
			searchStartedDetails=parser.getFirstToken(arguments);
			setHasBeenReconnected(true);
		}
		else if (checker.compare(this.getClass(), "Recovers data object when search monitoring resumes", "[matrix id]", commandName, "recoverData")) {
			data =  (CategoricalData)getProject().getCharacterMatrixByReference((MesquiteFile)null, parser.getFirstToken(arguments), false);
			if (data != null)
				taxa = data.getTaxa();
		}
		else if (checker.compare(this.getClass(), "Recovers extra search details from previous run", "[search details]", commandName, "recoverExtraSearchDetails")) {
			extraSearchDetails.setLength(0);
			extraSearchDetails.append(parser.getFirstToken(arguments));
		}
		else if (checker.compare(this.getClass(), "Recovers addendum to tree block name from previous run", "[addendum]", commandName, "recoverAddendumToTreeBlockName")) {
			addendumToTreeBlockName.setLength(0);
			String s = parser.getFirstToken(arguments);
			if (StringUtil.notEmpty(s) && !s.equalsIgnoreCase("null"))
				addendumToTreeBlockName.append(parser.getFirstToken(arguments));
		}
		else return super.doCommand(commandName, arguments, checker);
		return null;
	}	
	public boolean isReconnected() {
		return hasBeenReconnected;
	}
	public void setHasBeenReconnected(boolean hasBeenReconnected) {
		this.hasBeenReconnected = hasBeenReconnected;
	}

	public boolean isScriptBasedNoTerminal() {
		return scriptBasedNoTerminal;
	}
	public void setScriptBasedNoTerminal(boolean scriptBasedNoTerminal) {
		this.scriptBasedNoTerminal = scriptBasedNoTerminal;
	}


	/*.................................................................................................................*/
	protected boolean doQueryOptions() {
		return !optionsHaveBeenSet;
	}
	/*.................................................................................................................*/
	public boolean initializeGetTrees(Class requiredClassOfData, Taxa taxa, MCharactersDistribution matrix, MesquiteInteger statusResult) {

		if (matrix==null )
			return false;

		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return false;
		}
		createdNewDataObject = matrix.getParentData()==null;
		CharacterData cData = CharacterData.getData(this,  matrix, taxa);
		if (!CategoricalData.class.isInstance(cData))	{
			MesquiteMessage.discreetNotifyUser("Sorry, " + getProgramName() + " requires categorical data.");
			MesquiteInteger.setValue(statusResult, ResultCodes.INCOMPATIBLE_DATA);
			return false;
		}
		data = (CategoricalData)cData;
		if (!(requiredClassOfData.isInstance(data))){
			MesquiteMessage.discreetNotifyUser("Sorry, " + getProgramName() + " works only if given a full "+requiredClassOfData.getName()+" object");
			MesquiteInteger.setValue(statusResult, ResultCodes.INCOMPATIBLE_DATA);
			return false;
		}

		if (!initializeJustBeforeQueryOptions()) {
			MesquiteInteger.setValue(statusResult, ResultCodes.ERROR);
			return false;
		}
		if (doQueryOptions() && !MesquiteThread.isScripting()) 
			if (!queryOptions()){
				MesquiteInteger.setValue(statusResult, ResultCodes.USERCANCELONINITIALIZE);
				return false;
			}
		optionsHaveBeenSet = true;

		initializeMonitoring();
		data.incrementEditInhibition();
		rng = new Random(System.currentTimeMillis());
		unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		suppressProjectPanelReset();
		if (isVerbose()) 
			logln(getProgramName() + " analysis using data matrix " + data.getName());
		MesquiteInteger.setValue(statusResult, ResultCodes.NO_ERROR);
		return true;
	}

	/*.................................................................................................................*/
	public boolean runPreflightCommand (String preflightCommand) {

		boolean success = externalProcRunner.setPreflightInputFiles(preflightCommand);

		String preflightLogFileName = getPreflightLogFileName();


		// starting the process
		success = externalProcRunner.startExecution();


		// the process runs
		if (success) {
			String preflightFile = externalProcRunner.getPreflightFile(preflightLogFileName);
		}
		else {
		}

		return success;
	}
	/*.................................................................................................................*/
	protected void reportStdError() {
		if (externalProcRunner==null)
			return;
		String s = externalProcRunner.getStdErr();
		if (stdErrIsTrueError(s)){
			logln("\n*** ERROR REPORTED ***  \n");
			logln(s + "\n");
		}
	}
	/*.................................................................................................................*/
	public boolean stdErrIsTrueError(String stdErr) {
		if (StringUtil.notEmpty(stdErr)){
			return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	protected void reportStdOutput(String message) {
		if (externalProcRunner==null)
			return;
		String s = externalProcRunner.getStdOut();
		if (StringUtil.notEmpty(s) && isVerbose()){
			logln("\n"+message+ "\nContents of output file: ");
			logln(s + "\n");
		}
	}

	boolean useDiscreetAlert = false;

	/*.................................................................................................................*/
	public void setDiscreetAlert(boolean useDiscreetAlert) {
		this.useDiscreetAlert = useDiscreetAlert;
	}

	public boolean requiresLinuxTerminalCommands() {
		return false;
	}

	public String getPrefixForProgramCommand() {
		return "";
	}
	public String getSuffixForProgramCommand() {
		return "";
	}
	public boolean removeCommandSameCommandLineAsProgramCommand() {
		return false;
	}
	/*.................................................................................................................*/
	public boolean useDiscreetAlert() {
		return useDiscreetAlert;
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	//TODO: generalize to all programs (e.g., not in TNTRunner)
	public String getRunInformation() {
		StringBuffer sb = new StringBuffer(1000);
		sb.append("Trees acquired from " + getProgramName() + " using Mesquite's Zephyr package. \n");
		sb.append(getProgramName() + " run on " + getProgramLocation() +" \n");
		sb.append("Analysis started " + getDateAndTime()+ "\n");
		sb.append("User on originating (local) computer: " + MesquiteTrunk.getUserName()+ "\n");
		sb.append("------------------------------------------\n");
		if (taxa!=null)
			sb.append("Taxa: " + taxa.getName() + "\n");
		if (data!=null) {
			sb.append("Matrix: " + data.getName() + "\n");
			sb.append("Number of taxa analyzed: ");
			if (selectedTaxaOnly) {
				sb.append(data.numSelectedTaxa()+" (selected taxa only)\n");
			} else
				sb.append(data.getNumTaxa());
		}

		return sb.toString();
	}
	/*.................................................................................................................*/
	public String getRunInformation(Object arguments) {
		StringBuffer sb = new StringBuffer(1000);
		sb.append("\n\nArguments passed to " + getProgramName() + ": \n");
		if (arguments instanceof MesquiteString)
			sb.append(((MesquiteString)arguments).getValue());
		else if (arguments instanceof String)
			sb.append((String)arguments);

		return getRunInformation()+ sb.toString();
	}
	/*.................................................................................................................*/
	public boolean runVersionQueryOnExternalProcess (String programCommand, Object arguments, String versionFileName) {  // TODO:  need to override, temporarily,  StdOutFilename, as that's were it will go
		boolean success  = externalProcRunner.setProgramArgumentsAndInputFiles(programCommand,arguments, null, null, -1);
		if (!success){
			if (!beanWritten)
				postBean("failed, externalProcRunner.runVersionQueryOnExternalProcess | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten = true;
			return false;
		}
		String[] logFiles = new String[] {versionFileName};
		externalProcRunner.setOutputFileNamesToWatch(logFiles);

		return externalProcRunner.startExecution();

	}
	/*.................................................................................................................*/
	public boolean runProgramOnExternalProcess (String programCommand, Object arguments, String[] fileContents, String[] fileNames, String progTitle, int runInfoFileNumber) {
		runInProgress=true;

		/*  ============ SETTING UP THE RUN ============  */
		boolean success  = externalProcRunner.setProgramArgumentsAndInputFiles(programCommand,arguments, fileContents, fileNames, runInfoFileNumber);
		if (!success){
			// give message about failure
			if (!beanWritten)
				postBean("failed, externalProcRunner.setInputFiles | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten = true;
			return false;
		}
		logFileNames = getLogFileNames();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		logln("Analysis on: " + externalProcRunner.getProgramLocation());


		if (!MesquiteThread.pleaseSuppressProgressIndicatorsCurrentThread())
			progIndicator = new ProgressIndicator(getProject(),progTitle, getProgramName() + " Search", 0, true);
		if (progIndicator!=null){
			progIndicator.start();
		}
		externalProcRunner.setProgressIndicator(progIndicator);
		setSearchDetails();
		appendToSearchDetails(getExtraSearchDetails().toString());
		if (constrainedSearch) 
			MesquiteMessage.logCurrentTime("\nStart of constrained "+getProgramName()+" analysis: ");
		else 
			MesquiteMessage.logCurrentTime("\nStart of unconstrained "+getProgramName()+" analysis: ");

		timer.start();
		timer.fullReset();

		// starting the process

		success = externalProcRunner.startExecution();


		// the process runs
		if (success) {
			success = externalProcRunner.monitorExecution(progIndicator);
		}
		else if (!onlySetUpRun) {
			if (!beanWritten)
				postBean("failed, externalProcRunner.startExecution | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten=true;
			if (useDiscreetAlert())
				MesquiteMessage.discreetNotifyUser("The "+getProgramName()+" run encountered problems. ");  // better error message!
			else
				alert("The "+getProgramName()+" run encountered problems. ");  // better error message!
		}

		// the process completed
		if (isVerbose()) {
			logln("\n"+getProgramName()+" analysis completed at " + getDateAndTime());
			double totalTime= timer.timeSinceVeryStartInSeconds();
			if (totalTime>120.0)
				logln("Total time: " + StringUtil.secondsToHHMMSS((int)totalTime));
			else
				logln("Total time: " + totalTime  + " seconds");
			if (!success)
				if (userAborted)
					logln("Execution of "+getProgramName()+" stopped by user [1]");
				else
					logln("Execution of "+getProgramName()+" unsuccessful [1]");
		}
		if (progIndicator!=null)
			progIndicator.goAway();
		return success;
	}


	/*.................................................................................................................*/
	public Tree continueMonitoring(MesquiteCommand callBackCommand, MesquiteBoolean runSucceeded) {

		if (isVerbose()) 
			logln("Monitoring " + getProgramName() + " run begun.");

		String callBackArguments = callBackCommand.getDefaultArguments();
		String taxaID = parser.getFirstToken(callBackArguments);
		if (taxaID !=null)
			taxa = getProject().getTaxa(taxaID);
		//	getProject().incrementProjectWindowSuppression();

		initializeMonitoring();
		setFileNames();
		logFileNames = getLogFileNames();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		/*	MesquiteModule inferer = findEmployerWithDuty(TreeInferer.class);
		if (inferer != null)
			((TreeInferer)inferer).bringIntermediatesWindowToFront();*/
		boolean success = externalProcRunner.monitorExecution(progIndicator);
		if (runSucceeded!=null)
			runSucceeded.setValue(success);

		if (progIndicator!=null)
			progIndicator.goAway();
		//		if (getProject() != null)
		//			getProject().decrementProjectWindowSuppression();
		if (data != null)
			data.decrementEditInhibition();
		if (!isDoomed() && success)
			if (callBackCommand != null)
				callBackCommand.doItMainThread(null,  null,  this);

		return null;
	}	

	/*.................................................................................................................*/
	public boolean queryTaxaOptions(Taxa taxa) {
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (!taxa.anySelected())
			if (ssv==null || ssv.size()<=0)
				return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getProgramName()+" Taxon Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getProgramName()+" Taxon Options");

		boolean specifyOutgroup = false;

		Choice taxonSetChoice = null;
		Checkbox specifyOutgroupBox = null;

		if (ssv!=null && ssv.size()>0){
			taxonSetChoice = dialog.addPopUpMenu ("Outgroups: ", ssv, 0);
			specifyOutgroupBox = dialog.addCheckBox("specify outgroup", specifyOutgroup);
		}

		Checkbox selectedOnlyBox = null;

		if (taxa.anySelected())
			selectedOnlyBox = dialog.addCheckBox("selected taxa only", selectedTaxaOnly);
		else
			selectedTaxaOnly = false;


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			if (specifyOutgroupBox!=null) 
				specifyOutgroup = specifyOutgroupBox.getState();
			if (taxonSetChoice !=null) 
				outgroupTaxSetString = taxonSetChoice.getSelectedItem();
			if (!specifyOutgroup)
				outgroupTaxSetString="";
			if (selectedOnlyBox!=null)
				selectedTaxaOnly = selectedOnlyBox.getState();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	public void runFilesAvailable(int fileNum) {
	}
	/*.................................................................................................................*/
	public void readTreeFileForCurrentMultipleTrees(TreeVector trees, String treeFilePath, MesquiteBoolean readSuccess) {
	}
	/*.................................................................................................................*/
	public TreeVector basicRetrieveCurrentMultipleTrees(Taxa taxa, int out_treefile) {
		if (bootstrapOrJackknife()) {
			if (isVerbose()) 
				logln("Preparing to receive " + getProgramName() + " " + getResamplingKindName() + " trees.");
			suppressProjectPanelReset();
			CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
			CommandRecord scr = new CommandRecord(true);
			MesquiteThread.setCurrentCommandRecord(scr);

			String[] outputFilePaths = externalProcRunner.getOutputFilePaths();
			String treeFilePath = outputFilePaths[out_treefile];

			TreeVector trees = new TreeVector(taxa);
			MesquiteBoolean readSuccess = new MesquiteBoolean(false);
			readTreeFileForCurrentMultipleTrees(trees, treeFilePath, readSuccess);

			if (readSuccess.getValue())
				logln("  Reading of " + getProgramName() + " " + getResamplingKindName() + " trees succeeded.");
			else
				logln("  Reading of " + getProgramName() + " " + getResamplingKindName() + " trees failed.");

			MesquiteThread.setCurrentCommandRecord(oldCR);
			desuppressProjectPanelReset();
			return trees;
		}
		return null;
	}

	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return outputFilePaths;
	}
	/*.................................................................................................................*/
	public String getWindowTitle(){
		String s = getProgramName() + " inference in progress ";
		if (data !=null)
			s+= "["+data.getName() + "]";
		return s;
	}
	/*.................................................................................................................*/
	public void setTimeOfEarlyRep(int numReplicates) {
		if (!earlyRepsSet && numReplicates>0) {
			timeOfEarlyRep = timer.timeSinceVeryStartInSeconds() ;
			numEarlyReps = numReplicates;
			earlyRepsSet=true;
		}
	}
	/*.................................................................................................................*/
	public double getTimePerRep(int numReplicates) {
		if (numReplicates-numEarlyReps>0)
			return (timer.timeSinceVeryStartInSeconds() - timeOfEarlyRep)/(numReplicates-numEarlyReps);
		else
			return -1 ;
	}
	/*.................................................................................................................*/
	public String getTimeOfCompletion(int timeToCompletion) {
		return getFutureDateAndTime(timeToCompletion);

	}


	/*.................................................................................................................*/

	public void runFilesAvailable(boolean[] filesAvailable) {
		if ((progIndicator!=null && progIndicator.isAborted())) {
			setUserAborted(true);
			return;
		}
		String filePath = null;
		int fileNum=-1;
		String[] outputFilePaths = new String[filesAvailable.length];
		for (int i=0; i<outputFilePaths.length; i++)
			if (filesAvailable[i]){
				fileNum= i;
				break;
			}
		if (fileNum<0) return;
		runFilesAvailable(fileNum);
	}
	/*.................................................................................................................*/
	public void runFilesAvailable(){   // this should really only do the ones needed, not all of them.
		if (logFileNames==null)
			logFileNames = getLogFileNames();
		if (logFileNames==null)
			return;
		for (int i = 0; i<logFileNames.length; i++){
			runFilesAvailable(i);
		}
	}

}
