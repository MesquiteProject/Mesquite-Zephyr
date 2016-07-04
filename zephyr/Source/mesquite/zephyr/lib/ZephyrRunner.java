/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.Checkbox;
import java.awt.Choice;
import java.util.Random;

import mesquite.categ.lib.CategoricalData;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.TreeInferer;

public abstract class ZephyrRunner extends MesquiteModule implements ExternalProcessRequester, OutputFilePathModifier{
	
	protected TreeInferer treeInferer = null;

	String[] logFileNames;
	protected ExternalProcessRunner externalProcRunner;
	protected ProgressIndicator progIndicator;
	protected CategoricalData data;
	protected MesquiteTimer timer = new MesquiteTimer();
	protected Taxa taxa;
	protected String unique;
	protected Random rng;
	protected MesquiteModule ownerModule;
	protected ZephyrRunnerEmployer zephyrRunnerEmployer;
	protected boolean selectedTaxaOnly = false;
	protected boolean optionsHaveBeenSet = false;
	
	protected NameReference freqRef = NameReference.getNameReference("consensusFrequency");

	protected int currentRun=0;
	protected boolean[] completedRuns=null;
	protected int previousCurrentRun=0;
	protected boolean runInProgress = false;
	protected boolean updateWindow = false;
	protected boolean bootstrapAllowed = true;

	
	protected String outgroupTaxSetString = "";
	protected int outgroupTaxSetNumber = 0;

	public abstract Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore);
	public abstract Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore);
	public abstract boolean bootstrapOrJackknife();
	public abstract boolean showMultipleRuns();
	
	public TreeInferer getTreeInferer() {
		return treeInferer;
	}
	public void setTreeInferer(TreeInferer treeInferer) {
		this.treeInferer = treeInferer;
	}

	 public boolean stopExecution(){
		 return externalProcRunner.stopExecution();
	 }

	public String getResamplingKindName() {
		return "Bootstrap";
	}

	public void storeRunnerPreferences() {
		if (externalProcRunner!=null)
			externalProcRunner.storePreferences();
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

	public boolean getRunInProgress() {
		return runInProgress;
	}
	public void setRunInProgress(boolean runInProgress) {
		this.runInProgress = runInProgress;
	}

	public abstract boolean doMajRuleConsensusOfResults();
	public abstract boolean singleTreeFromResampling();

	public abstract void reconnectToRequester(MesquiteCommand command);
	public abstract String getProgramName();
	public abstract boolean queryOptions();
	
	public abstract String[] getLogFileNames();
	protected SimpleTaxonNamer namer = new SimpleTaxonNamer();

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
		this.zephyrRunnerEmployer = (ZephyrRunnerEmployer)ownerModule;
	}
	/*.................................................................................................................*/
	public void setSearchDetails() {  // for annotation to tree block.  designed to be composed after the tree search started.  
		if (searchDetails!=null) {
			searchDetails.setLength(0);
			searchDetails.append("Trees acquired from " + getProgramName() + " using Mesquite's Zephyr package. \n");
			searchDetails.append("Analysis started " + getDateAndTime()+ "\n");
			if (StringUtil.notEmpty(externalProcRunner.getDirectoryPath()))
					searchDetails.append("Results stored in folder: " + externalProcRunner.getDirectoryPath()+ "\n");
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
		temp.addLine("recoverExtraSearchDetails " + ParseUtil.tokenize(extraSearchDetails.toString()));
		temp.addLine("recoverAddendumToTreeBlockName " + ParseUtil.tokenize(addendumToTreeBlockName.toString()));
			
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Recovers search details from previous run", "[search details]", commandName, "recoverSearchDetails")) {
			searchDetails.setLength(0);
			searchDetails.append(parser.getFirstToken(arguments));
		}
		else if (checker.compare(this.getClass(), "Recovers data object when search monitoring resumes", "[matrix id]", commandName, "recoverData")) {
			data =  (CategoricalData)getProject().getCharacterMatrixByReference((MesquiteFile)null, parser.getFirstToken(arguments), false);
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
		return null;
	}	
	/*.................................................................................................................*/
	protected boolean doQueryOptions() {
		return !optionsHaveBeenSet;
	}
	/*.................................................................................................................*/
	public boolean initializeGetTrees(Class requiredClassOfData, Taxa taxa, MCharactersDistribution matrix) {
		if (matrix==null )
			return false;

		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return false;
		}
		data = (CategoricalData)CharacterData.getData(this,  matrix, taxa);
		if (!(requiredClassOfData.isInstance(data))){
			MesquiteMessage.discreetNotifyUser("Sorry, " + getProgramName() + " works only if given a full "+requiredClassOfData.getName()+" object");
			return false;
		}

		if (!initializeJustBeforeQueryOptions())
			return false;

		if (doQueryOptions() && !MesquiteThread.isScripting() && !queryOptions()){
			return false;
		}
		optionsHaveBeenSet = true;
		
		initializeMonitoring();
		data.incrementEditInhibition();
		rng = new Random(System.currentTimeMillis());
		unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		suppressProjectPanelReset();
		logln(getProgramName() + " analysis using data matrix " + data.getName());
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
		if (StringUtil.notEmpty(s)){
			logln("\nERROR REPORTED.  \nContents of standard error file: ");
			logln(s + "\n");
		}
	}
	/*.................................................................................................................*/
	protected void reportStdOutput(String message) {
		if (externalProcRunner==null)
			return;
		String s = externalProcRunner.getStdOut();
		if (StringUtil.notEmpty(s)){
			logln("\n"+message+ "\nContents of output file: ");
			logln(s + "\n");
		}
	}

	/*.................................................................................................................*/
	public boolean runProgramOnExternalProcess (String programCommand, Object arguments, String[] fileContents, String[] fileNames, String progTitle) {
		runInProgress=true;
		
		/*  ============ SETTING UP THE RUN ============  */
		boolean success  = externalProcRunner.setProgramArgumentsAndInputFiles(programCommand,arguments, fileContents, fileNames);
		if (!success){
			// give message about failure
			postBean("failed, externalProcRunner.setInputFiles", false);
			return false;
		}
		logFileNames = getLogFileNames();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		if (!MesquiteThread.pleaseSuppressProgressIndicatorsCurrentThread())
			progIndicator = new ProgressIndicator(getProject(),progTitle, getProgramName() + " Search", 0, true);
		if (progIndicator!=null){
			progIndicator.start();
		}
		setSearchDetails();
		appendToSearchDetails(getExtraSearchDetails().toString());
		MesquiteMessage.logCurrentTime("\nStart of "+getProgramName()+" analysis: ");
		
		timer.start();
		timer.fullReset();

		// starting the process
		success = externalProcRunner.startExecution();
		
		// the process runs
		if (success)
			success = externalProcRunner.monitorExecution();
		else {
			postBean("failed, externalProcRunner.startExecution", false);
			alert("The "+getProgramName()+" run encountered problems. ");  // better error message!
		}

		// the process completed
		logln("\n"+getProgramName()+" analysis completed at " + getDateAndTime());
		double totalTime= timer.timeSinceVeryStartInSeconds();
		if (totalTime>120.0)
			logln("Total time: " + StringUtil.secondsToHHMMSS((int)totalTime));
		else
			logln("Total time: " + totalTime  + " seconds");
		if (progIndicator!=null)
			progIndicator.goAway();
		if (!success)
			logln("Execution of "+getProgramName()+" unsuccessful [1]");
		return success;
	}
	

	/*.................................................................................................................*/
	public Tree continueMonitoring(MesquiteCommand callBackCommand) {
		
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
		boolean success = externalProcRunner.monitorExecution();
		
		
		if (progIndicator!=null)
			progIndicator.goAway();
//		if (getProject() != null)
//			getProject().decrementProjectWindowSuppression();
		if (data != null)
			data.decrementEditInhibition();
		if (!isDoomed())
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

	public void runFilesAvailable(boolean[] filesAvailable) {
		if ((progIndicator!=null && progIndicator.isAborted()))
			return;
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
