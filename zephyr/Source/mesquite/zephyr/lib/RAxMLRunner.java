/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

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
import mesquite.io.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public abstract class RAxMLRunner extends ZephyrRunner  implements ActionListener, ItemListener, ExternalProcessRequester, ConstrainedSearcherTreeScoreProvider  {

	boolean onlyBest = true;

	boolean RAxML814orLater = false;

	protected	int randomIntSeed = (int)System.currentTimeMillis();   // convert to int as RAxML doesn't like really big numbers

	//	boolean retainFiles = false;
	//	String MPIsetupCommand = "";
	boolean showIntermediateTrees = true;

	protected int numRuns = 5;
	protected int numRunsCompleted = 0;
	protected int run = 0;
	protected boolean preferencesSet = false;
	protected boolean isProtein = false;

	protected boolean nobfgs = false;
	protected int bootstrapreps = 100;
	protected int bootstrapSeed = Math.abs((int)System.currentTimeMillis());
	protected static String dnaModel = "GTRGAMMAI";
	protected static String proteinModel = "PROTGAMMAJTT";
	protected static String otherOptions = "";
	protected boolean doBootstrap = false;
	protected static final int NOCONSTRAINT = 0;
	protected static final int MONOPHYLY = 1;
	protected static final int SKELETAL = 2;
	protected int useConstraintTree = NOCONSTRAINT;
	protected int SOWHConstraintTree = MONOPHYLY;


	long summaryFilePosition =0;



	protected long  randseed = -1;
	static String constraintfile = "none";

	protected  SingleLineTextField dnaModelField, proteinModelField, otherOptionsField;
	IntegerField seedField;
	protected javax.swing.JLabel commandLabel;
	protected SingleLineTextArea commandField;
	protected IntegerField numRunsField, bootStrapRepsField;
	protected Checkbox onlyBestBox, retainFilescheckBox, doBootstrapCheckbox, nobfgsCheckBox;
	RadioButtons constraintButtons;
	RadioButtons threadingRadioButtons;
	//	int count=0;

	protected double finalValue = MesquiteDouble.unassigned;
	protected double optimizedValue = MesquiteDouble.unassigned;
	protected double[] finalValues = null;
	protected double[] optimizedValues = null;
	protected int runNumber = 0;
	
	protected boolean useOptimizedScoreAsBest = false;

	protected static final int OUT_LOGFILE=0;
	protected static final int OUT_TREEFILE=1;
	protected static final int OUT_SUMMARYFILE=2;
	protected static final int WORKING_TREEFILE=3;


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		if (randomIntSeed<0)
			randomIntSeed = -randomIntSeed;
		if (!hireExternalProcessRunner()){
			return sorry("Couldn't hire an external process runner");
		}
		externalProcRunner.setProcessRequester(this);

		return true;
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
		}
		 else
			return super.doCommand(commandName, arguments, checker);
	}	
	public void reconnectToRequester(MesquiteCommand command){
		continueMonitoring(command);
	}
	
	public void setConstraintTreeType (int useConstraintTree) {
		this.useConstraintTree = useConstraintTree;
	}
	
	public int getConstraintTreeType() {
		return useConstraintTree;
	}



	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("numRuns".equalsIgnoreCase(tag))
			numRuns = MesquiteInteger.fromString(content);
		if ("bootStrapReps".equalsIgnoreCase(tag)){
			bootstrapreps = MesquiteInteger.fromString(content);
			if (bootstrapreps<1) bootstrapreps=1;
		}
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("nobfgs".equalsIgnoreCase(tag))
			nobfgs = MesquiteBoolean.fromTrueFalseString(content);
		if ("doBootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);


		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);  
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);  
		StringUtil.appendXMLTag(buffer, 2, "doBootstrap", doBootstrap);  
		StringUtil.appendXMLTag(buffer, 2, "nobfgs", nobfgs);  
		//StringUtil.appendXMLTag(buffer, 2, "MPIsetupCommand", MPIsetupCommand);  

		preferencesSet = true;
		return buffer.toString();
	}

	 /*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+="Bootstrap analysis<br>";
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
	public String getTestedProgramVersions(){
		return "8.0.0 and 8.1.4";
	}
	public abstract void addRunnerOptions(ExtensibleDialog dialog);
	public abstract void processRunnerOptions();
	/*.................................................................................................................*/
	public int minimumNumSearchReplicates() {
		return 1;
	}
	public String getConstraintTreeName() {
		if (constraint==null)
			return null;
		return constraint.getName();
	}


	/*.................................................................................................................*/
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
		String title = "RAxML Options & Locations";
		String extra = getExtraQueryOptionsTitle();
		if (StringUtil.notEmpty(extra))
			title += " ("+extra+")";
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), title,buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//	dialog.addLabel("RAxML - Options and Locations");
		String helpString = "This module will prepare a matrix for RAxML, and ask RAxML do to an analysis.  A command-line version of RAxML must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by RAxML, and, for non-bootstrap analyses, also read in the value of the RAxML score (-ln L) of the tree. " 
				+ "You can see the RAxML score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose RAxML Score.";

		dialog.appendToHelpString(helpString);
		if (zephyrRunnerEmployer!=null)
			dialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());


		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();


		tabbedPanel.addPanel("RAxML Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);
		addRunnerOptions(dialog);
		if (treeInferer!=null) 
			treeInferer.addItemsToDialogPanel(dialog);

		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Replicates", true);
			doBootstrapCheckbox = dialog.addCheckBox("do bootstrap analysis", doBootstrap);
			dialog.addHorizontalLine(1);
			dialog.addLabel("Bootstrap Options", Label.LEFT, false, true);
			doBootstrapCheckbox.addItemListener(this);
			bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 1, MesquiteInteger.infinite);
			seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);
			dialog.addHorizontalLine(1);
		}
		else 
			tabbedPanel.addPanel("Replicates", true);
		dialog.addLabel("Maximum Likelihood Tree Search Options", Label.LEFT, false, true);
		if (numRuns< minimumNumSearchReplicates())
			numRuns = minimumNumSearchReplicates();
		numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, minimumNumSearchReplicates(), MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		checkEnabled(doBootstrap);

		if (getConstrainedSearchAllowed())
			tabbedPanel.addPanel("Character Models & Constraints", true);
		else
			tabbedPanel.addPanel("Character Models", true);
		dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);

		if (getConstrainedSearchAllowed()) {
			dialog.addHorizontalLine(1);
			dialog.addLabel("Constraint tree:", Label.LEFT, false, true);
			constraintButtons = dialog.addRadioButtons (new String[]{"No Constraint", "Partial Resolution", "Skeletal Constraint"}, useConstraintTree);
			constraintButtons.addItemListener(this);
		}

		/*		dialog.addHorizontalLine(1);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
		 */

		tabbedPanel.addPanel("Other options", true);
		nobfgsCheckBox = dialog.addCheckBox("no bfgs option", nobfgs);
		otherOptionsField = dialog.addTextField("Other RAxML options:", otherOptions, 40);


		commandLabel = dialog.addLabel("");
		commandField = dialog.addSingleLineTextArea("", 2);
		dialog.addBlankLine();
		Button showCommand = dialog.addAListenedButton("Compose Command",null, this);
		showCommand.setActionCommand("composeRAxMLCommand");
		Button clearCommand = dialog.addAListenedButton("Clear",null, this);
		clearCommand.setActionCommand("clearCommand");

		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.addHorizontalLine(1);
		//		retainFilescheckBox = dialog.addCheckBox("Retain Files", retainFiles);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (externalProcRunner.optionsChosen() && infererOK) {
				dnaModel = dnaModelField.getText();
				proteinModel = proteinModelField.getText();
				numRuns = numRunsField.getValue();
				if (bootstrapAllowed) {
					doBootstrap = doBootstrapCheckbox.getState();
					randomIntSeed = seedField.getValue();
					bootstrapreps = bootStrapRepsField.getValue();
				} else
					doBootstrap=false;
				onlyBest = onlyBestBox.getState();
				nobfgs = nobfgsCheckBox.getState();

				if (getConstrainedSearchAllowed()) {
					useConstraintTree = constraintButtons.getValue();
					if (useConstraintTree!=NOCONSTRAINT)
						setConstrainedSearch(true);
				}
				otherOptions = otherOptionsField.getText();
				processRunnerOptions();
				storeRunnerPreferences();
			}
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	public void checkEnabled(boolean doBoot) {
		onlyBestBox.setEnabled(!doBoot);
	}
	/* ................................................................................................................. */
	/** Returns the purpose for which the employee was hired (e.g., "to reconstruct ancestral states" or "for X axis"). */
	public String purposeOfEmployee(MesquiteModule employee) {
		if (employee instanceof OneTreeSource){
			return "for a source of a constraint tree for RAxML"; // to be overridden
		}
		return "for " + getName(); // to be overridden
	}

	protected OneTreeSource constraintTreeTask = null;
	protected OneTreeSource getConstraintTreeSource(){
		if (constraintTreeTask == null){
			constraintTreeTask = (OneTreeSource)hireEmployee(OneTreeSource.class, "Source of constraint tree");
		}
		return constraintTreeTask;
	}
	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == doBootstrapCheckbox){
			checkEnabled (doBootstrapCheckbox.getState());

		}
		else if (e.getItemSelectable() == constraintButtons && constraintButtons.getValue()>0){
			
			getConstraintTreeSource();

		}
	}
	public boolean getUseOptimizedScoreAsBest() {
		return useOptimizedScoreAsBest;
	}

	public void setUseOptimizedScoreAsBest(boolean useOptimizedScoreAsBest) {
		this.useOptimizedScoreAsBest = useOptimizedScoreAsBest;
	}

	/*.................................................................................................................*/
	public void setRAxMLSeed(long seed){
		this.randseed = seed;
	}

	Checkbox useOptimizedScoreAsBestCheckBox =  null;
	RadioButtons SOWHConstraintButtons = null;
	public  void addItemsToSOWHDialogPanel(ExtensibleDialog dialog){
		useOptimizedScoreAsBestCheckBox = dialog.addCheckBox("use final gamma-based optimized RAxML score", useOptimizedScoreAsBest);
		if (SOWHConstraintTree==MONOPHYLY)
			SOWHConstraintButtons = dialog.addRadioButtons (new String[]{"Partial Resolution constraint", "Skeletal constraint"}, 0);
		else if (SOWHConstraintTree==SKELETAL)
			SOWHConstraintButtons = dialog.addRadioButtons (new String[]{"Partial Resolution constraint", "Skeletal constraint"}, 1);

	}
	
	public boolean SOWHoptionsChosen(){
		if (useOptimizedScoreAsBestCheckBox!=null)
			useOptimizedScoreAsBest = useOptimizedScoreAsBestCheckBox.getState();
		if (SOWHConstraintButtons!=null) {
			int constraint = SOWHConstraintButtons.getValue();
			if (constraint==0)
				SOWHConstraintTree = MONOPHYLY;
			else if (constraint==1)
				SOWHConstraintTree = SKELETAL;
			useConstraintTree = SOWHConstraintTree;
		}
		return true;
	}
	public void resetSOWHOptionsConstrained(){
		useConstraintTree = SOWHConstraintTree;
	}
	public void resetSOWHOptionsUnconstrained(){
		useConstraintTree = NOCONSTRAINT;
	}
	public String getSOWHDetailsObserved(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for observed matrix: " + numRuns);
		sb.append("\nDNA model: " + dnaModel);
		if (useOptimizedScoreAsBest)
			sb.append("\nUsing final gamma-based scores\n");
		return sb.toString();
	}
	public String getSOWHDetailsSimulated(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for each simulated matrix: " + numRuns + "\n");
		if (SOWHConstraintTree==MONOPHYLY)
			sb.append("Constraint type used in SOWH test: Regular constraint\n");
		else if (SOWHConstraintTree==SKELETAL)
			sb.append("Constraint type used in SOWH test: Skeletal constraint\n");
		sb.append("DNA model: " + dnaModel);
		if (useOptimizedScoreAsBest)
			sb.append("\nUsing final gamma-based scores\n");
		return sb.toString();
	}

	/*.................................................................................................................*/
	private Tree readRAxMLTreeFile(TreeVector trees, String treeFilePath, String treeName, MesquiteBoolean success, boolean lastTree) {
		Tree t =null;
		if (lastTree) {
			String s = MesquiteFile.getFileLastContents(treeFilePath);
			t =  ZephyrUtil.readPhylipTree(s,taxa,false, namer);

			if (t!=null) {
				if (success!=null)
					success.setValue(true);
				if (t instanceof AdjustableTree )
					((AdjustableTree)t).setName(treeName);

				if (trees!=null)
					trees.addElement(t, false);
			}
		}
		else {
			String contents = MesquiteFile.getFileContentsAsString(treeFilePath);
			Parser parser = new Parser(contents);

			String s = parser.getRawNextDarkLine();

			while (!StringUtil.blank(s)) {
				t = ZephyrUtil.readPhylipTree(s,taxa,false, namer);

				if (t!=null) {
					if (success!=null)
						success.setValue(true);
					if (t instanceof AdjustableTree )
						((AdjustableTree)t).setName(treeName);

					if (trees!=null)
						trees.addElement(t, false);
				}
				s = parser.getRawNextDarkLine();
			}
		}
		return t;

	}

	/*.................................................................................................................*/
	public void initializeMonitoring(){
		//Debugg.println("||||||||||   initializeMonitoring called");
		if (finalValues==null) {
			if (bootstrapOrJackknife())
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numRuns];
			DoubleArray.deassignArray(finalValues);
			finalValue = MesquiteDouble.unassigned;
		}
		if (optimizedValues==null) {
			if (bootstrapOrJackknife())
				optimizedValues = new double[getBootstrapreps()];
			else
				optimizedValues = new double[numRuns];
			DoubleArray.deassignArray(optimizedValues);
			optimizedValue = MesquiteDouble.unassigned;
		}
		outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		}
	}

	protected String multipleModelFileName;
	protected String constraintTreeFileName="constraintTree.tre";

	/*.................................................................................................................*/
	public void setFileNames () {
		multipleModelFileName = "multipleModelFile.txt";

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

	/*.................................................................................................................*/
	public abstract Object getProgramArguments(String dataFileName, boolean isPreflight);


	//String arguments;
	/*.................................................................................................................*/
	public String getDataFileName() {
		return "data.phy";
	}
	public void setConstrainedSearch(boolean constrainedSearch) {
		if (useConstraintTree==NOCONSTRAINT && constrainedSearch)
			useConstraintTree=MONOPHYLY;
		else if (useConstraintTree!=NOCONSTRAINT && !constrainedSearch)
			useConstraintTree = NOCONSTRAINT;
		this.constrainedSearch = constrainedSearch;
	}


	/*.................................................................................................................*/
	public synchronized Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		finalValues=null;
		optimizedValues =null;
		if (!initializeGetTrees(new Class[] {CategoricalData.class}, taxa, matrix))
			return null;

		//RAxML setup
		setRAxMLSeed(seed);
		isProtein = data instanceof ProteinData;

		// create local version of data file; this will then be copied over to the running location
		
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "RAxML", "-Run.");  
		if (tempDir==null)
			return null;
		String dataFileName = getDataFileName();   //replace this with actual file name?
		String translationFileName = IOUtil.translationTableFileName;   
		String dataFilePath = tempDir +  dataFileName;
		FileInterpreterI exporter = null;
		if (data instanceof DNAData)
			exporter = ZephyrUtil.getFileInterpreter(this,"#InterpretPhylipDNA");
		else if (data instanceof ProteinData)
			exporter = ZephyrUtil.getFileInterpreter(this,"#InterpretPhylipProtein");
		if (exporter==null)
			return null;
		((InterpretPhylip)exporter).setTaxonNameLength(100);
		String translationTable = namer.getTranslationTable(taxa);
		((InterpretPhylip)exporter).setTaxonNamer(namer);

		boolean fileSaved = false;
		if (data instanceof DNAData)
			fileSaved = ZephyrUtil.saveExportFile(this,exporter,  dataFilePath,  data, selectedTaxaOnly);
		else if (data instanceof ProteinData)
			fileSaved = ZephyrUtil.saveExportFile(this, exporter,  dataFilePath,  data, selectedTaxaOnly);
		if (!fileSaved) return null;

		setFileNames();

		String multipleModelFileContents = IOUtil.getMultipleModelRAxMLString(this, data, false);//TODO: why is partByCodPos false?  
		//Debugg.println: David: could there be a choice for partByCodPos?  I'd like that
		//Debugg.println("multipleModelFileContents " + multipleModelFileContents);
		
		if (StringUtil.blank(multipleModelFileContents)) 
			multipleModelFileName=null;

		String constraintTree = "";
		
		if ((useConstraintTree>NOCONSTRAINT || isConstrainedSearch())){
			if (isConstrainedSearch() && useConstraintTree==NOCONSTRAINT)  //TODO: change  Debugg.println
				useConstraintTree=MONOPHYLY;
			if (constraint==null) { // we don't have one
				getConstraintTreeSource();
				if (constraintTreeTask != null){
					constraint = constraintTreeTask.getTree(taxa, "This will be the constraint tree");
				}
			}
			if (constraint == null){
				discreetAlert("Constraint tree is not available.");
				return null;
			}
			else if (useConstraintTree == SKELETAL){
				if (!constraint.hasPolytomies(constraint.getRoot())){
					constraintTree = constraint.writeTreeByT0Names(false) + ";";
					appendToExtraSearchDetails("\nSkeletal constraint using tree \"" + constraint.getName() + "\"");
					appendToAddendumToTreeBlockName("Constrained by tree \"" + constraint.getName() + "\"");
				}
				else {
					discreetAlert("Constraint tree cannot be used as a skeletal constraint because it has polytomies");
					constraint=null;
					if (constraintTreeTask != null)
						constraintTreeTask.reset();  //WAYNECHECK: added reset as otherwise you got into an infinite loop as it never allowed you to reset the context tree
					return null;
				}
			}
			else if (useConstraintTree == MONOPHYLY){
				if (constraint.hasPolytomies(constraint.getRoot())){
						constraintTree = constraint.writeTreeByT0Names(false) + ";";
						appendToExtraSearchDetails("\nPartial resolution constraint using tree \"" + constraint.getName() + "\"");
						appendToAddendumToTreeBlockName("Constrained by tree \"" + constraint.getName() + "\"");
			}
				else {
					discreetAlert("Constraint tree cannot be used as a partial resolution constraint because it is strictly dichotomous");
					constraint=null;
					if (constraintTreeTask != null)
						constraintTreeTask.reset();
					return null;
			}
			}
		}
		setRootNameForDirectoryInProcRunner();
		//now establish the commands for invoking RAxML
		Object arguments = getProgramArguments(dataFileName, false);
		Object preflightArguments = getProgramArguments(dataFileName, true);

		//	String preflightCommand = externalProcRunner.getExecutableCommand()+" --flag-check " + ((MesquiteString)preflightArguments).getValue();
		String programCommand = externalProcRunner.getExecutableCommand();
		//programCommand += StringUtil.lineEnding();  

		//	if (preFlightSuccessful(preflightCommand)) {
		//	}
		
		if (updateWindow)
			parametersChanged(); //just a way to ping the coordinator to update the window

		//setting up the arrays of input file names and contents
		int numInputFiles = 4;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i=0; i<numInputFiles; i++){
			fileContents[i]="";
			fileNames[i]="";
		}
		fileContents[0] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[0] = dataFileName;
		fileContents[1] = multipleModelFileContents;
		fileNames[1] = multipleModelFileName;
		fileContents[2] = translationTable;
		fileNames[2] = translationFileName;
		fileContents[3] = constraintTree;
		fileNames[3] = constraintTreeFileName;

		numRunsCompleted = 0;
		completedRuns = new boolean[numRuns];
		for (int i=0; i<numRuns; i++) completedRuns[i]=false;
		summaryFilePosition=0;

		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, fileContents, fileNames,  ownerModule.getName());

		MesquiteFile.deleteDirectory(tempDir);
		if (!isDoomed()){

			if (success){  //David: abort here
				desuppressProjectPanelReset();
				return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
			} else {
				reportStdError();
			}
		}
		desuppressProjectPanelReset();
		if (data != null)
			data.decrementEditInhibition();
		externalProcRunner.finalCleanup();
		cleanupAfterSearch();
		return null;

	}	
	



	public  boolean showMultipleRuns() {
		return (!bootstrapOrJackknife() && numRuns>1);
	}


	/*.................................................................................................................*/
	public synchronized Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore){
		if (isVerbose()) 
			logln("Preparing to receive RAxML trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		finalScore.setValue(finalValue);

		suppressProjectPanelReset();
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed. 
		setFileNames();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();
		if (completedRuns == null){
			completedRuns = new boolean[numRuns];
			for (int i=0; i<numRuns; i++) completedRuns[i]=false;
		}

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
			String summary = MesquiteFile.getFileContentsAsString(outputFilePaths[OUT_SUMMARYFILE]);
			Parser parser = new Parser(summary);
			parser.setAllowComments(false);
			parser.allowComments = false;
			String line = parser.getRawNextDarkLine();
			while (!StringUtil.blank(line) && count < 1) {
				if (line.startsWith("Inference[")) {
					Parser subParser = new Parser();
					subParser.setString(line);
					String token = subParser.getFirstToken();   // should be "Inference"
					while (!StringUtil.blank(token) && ! subParser.atEnd()){
						if (token.indexOf("likelihood")>=0) {
							token = subParser.getNextToken();
							finalValue = -MesquiteDouble.fromString(token);
							if (!useOptimizedScoreAsBest)
								finalScore.setValue(finalValue);
						}
						token = subParser.getNextToken();
					}
					count++;
				}
				parser.setAllowComments(false);
				line = parser.getRawNextDarkLine();
			}
			while (!StringUtil.blank(line)) {
				if (line.startsWith("Final GAMMA-based Score of best tree")) {
					Parser subParser = new Parser();
					subParser.setString(line);
					String token = subParser.getFirstToken();   // should be "Final"
					//count = 0;
					while (!StringUtil.blank(token)) {
						token = subParser.getNextToken();
						if (token.equalsIgnoreCase("tree")) {
							token = subParser.getNextToken();  // should be value
							break;
						}
					}
					optimizedValue = -MesquiteDouble.fromString(token);
					if (useOptimizedScoreAsBest)
						finalScore.setValue(optimizedValue);
					break;
				} 
				parser.setAllowComments(false);
				line = parser.getRawNextDarkLine();
			}
	
			
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
				if (isVerbose()) 
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
						if (isVerbose())
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
					if (MesquiteDouble.isCombinable(optimizedValues[i]))
						if (MesquiteDouble.isUnassigned(optimizedValue)) {
							optimizedValue = optimizedValues[i];
						}
						else if (optimizedValue>optimizedValues[i]) {
							optimizedValue = optimizedValues[i];
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
					if (!useOptimizedScoreAsBest)
						finalScore.setValue(bestScore);
					else
						finalScore.setValue(optimizedValue);
				} else
						finalScore.setValue(optimizedValue);

				//Only retain the best tree in tree block.
				if(treeList.getTree(bestRun) != null && onlyBest){
					Tree bestTree = treeList.getTree(bestRun);
					treeList.removeAllElements(false);
					treeList.addElement(bestTree, false);
				}
			} 
			tv.dispose();
			tv =null;
		}
		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success)
			logln("Execution of RAxML unsuccessful [2]");

		desuppressProjectPanelReset();
		if (data!=null)
			data.decrementEditInhibition();
		//	manager.deleteElement(tv);  // get rid of temporary tree block
		externalProcRunner.finalCleanup();
		cleanupAfterSearch();
		finalValues=null;
		optimizedValues=null;
		if (success) {
			if (!beanWritten)
				if (bootstrapOrJackknife())
					postBean("successful, bootstrap", false);
				else 
					postBean("successful, ML tree", false);
			beanWritten=true;
			return t;
		}
		reportStdError();
		if (!beanWritten)
			postBean("failed, retrieveTreeBlock", false);
		beanWritten = true;
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

	protected static final int OUT_LOGFILE=0;
	protected static final int OUT_TREEFILE=1;
	protected static final int OUT_SUMMARYFILE=2;
	protected static final int WORKING_TREEFILE=3;

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

		if (fileNum==OUT_LOGFILE && outputFilePaths.length>OUT_LOGFILE && !StringUtil.blank(outputFilePaths[OUT_LOGFILE]) && !bootstrapOrJackknife()) {   // screen log
			String newFilePath = filePath;
			if (numRuns>1) newFilePath+=fileNum;
			if (MesquiteFile.fileExists(newFilePath)) {
				String s = MesquiteFile.getFileLastContents(newFilePath);
				if (!StringUtil.blank(s))
					if (progIndicator!=null) {
						parser.setString(s);
						String gen = parser.getFirstToken(); 
						String lnL = parser.getNextToken();
						progIndicator.setText("ln L = " + lnL);
						if (isVerbose())
							logln("    ln L = " + lnL);
						else
							log(".");
						progIndicator.spin();		

					}
				//				count++;
			} 
		}

		if (fileNum==WORKING_TREEFILE && outputFilePaths.length>WORKING_TREEFILE && !StringUtil.blank(outputFilePaths[WORKING_TREEFILE]) && !bootstrapOrJackknife() && showIntermediateTrees) {   // tree file
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;

				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

				}
				else ((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
			}
		}

		if (fileNum==OUT_TREEFILE && outputFilePaths.length>OUT_TREEFILE && !StringUtil.blank(outputFilePaths[OUT_TREEFILE]) && !bootstrapOrJackknife() && showIntermediateTrees) {   // tree file
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;

				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

				}
				else ((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
			}
		}

		//David: if isDoomed() then module is closing down; abort somehow

		if (fileNum==OUT_SUMMARYFILE && outputFilePaths.length>OUT_SUMMARYFILE && !StringUtil.blank(outputFilePaths[OUT_SUMMARYFILE])) {   // info file
			if (MesquiteFile.fileExists(filePath)) {
				//String s = MesquiteFile.getFileLastContents(filePath,fPOS);
				String s = MesquiteFile.getFileContentsAsString(filePath);
				long lastLength = s.length();
				s = s.substring((int)summaryFilePosition);
				summaryFilePosition = lastLength;
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
								else {
									if (isVerbose())
										logln("\nRAxML Run " + (runNumber+1) + ", final score ln L = " +token);
									if (completedRuns != null && runNumber<completedRuns.length)
										completedRuns[runNumber]=true;
								}
								//processOutputFile(outputFilePaths,1);
								foundRunInfo = true;
							}
							if (foundRunInfo)
								token="";
							else
								token = subParser.getNextToken();
						}
						if (completedRuns !=null){
							for (int i=0; i<completedRuns.length; i++)
								if (!completedRuns[i]) {
									currentRun=i;
									break;
								}
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
							if (isVerbose()){
								logln("   Run time " +  StringUtil.secondsToHHMMSS((int)timeSoFar)  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
								logln("    Average time per replicate:  " +  StringUtil.secondsToHHMMSS((int)timePerRep));
								logln("    Estimated total time:  " +  StringUtil.secondsToHHMMSS((int)(timeSoFar+timeLeft))+"\n");
							}
						}

						if (!bootstrapOrJackknife() && runNumber+1<numRuns) {
							if (isVerbose()) {
								logln("");
								logln("Beginning Run " + (runNumber+2));
							}
						}
					}
				}

			} 
		}

	}
	/*.................................................................................................................*/


	public boolean bootstrapOrJackknife() {
		return doBootstrap;
	}
	public  boolean doMajRuleConsensusOfResults(){
		return bootstrapOrJackknife();
	}

	public boolean singleTreeFromResampling() {
		return false;
	}


	public int getBootstrapreps() {
		return bootstrapreps;
	}

	public void setBootstrapreps(int bootstrapreps) {
		this.bootstrapreps = bootstrapreps;
	}


	public Class getDutyClass() {
		return RAxMLRunner.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "RAxML Runner";
	}

	/*.................................................................................................................*/
	public int getNumRuns(){
		return numRuns;
	}
	/*.................................................................................................................*/
	public boolean getOnlyBest(){
		return onlyBest;
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}


	public void intializeAfterExternalProcessRunnerHired() {
		loadPreferences();
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
