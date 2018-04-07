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

public abstract class IQTreeRunner extends ZephyrRunner  implements ActionListener, ItemListener, ExternalProcessRequester, ConstrainedSearcherTreeScoreProvider  {

	boolean onlyBest = true;

	protected	int randomIntSeed = (int)System.currentTimeMillis();   // convert to int as RAxML doesn't like really big numbers

	protected static final int noPartition = 0;
	protected static final int partitionByCharacterGroups = 1;
	protected static final int partitionByCodonPosition = 2;
	protected int partitionScheme = partitionByCharacterGroups;
	protected int currentPartitionSubset = 0;
	protected 	Choice modelOptionChoice;

	protected static final int qPartitionLinkage = 0;
	protected static final int sppPartitionLinkage = 1;
	protected static final int spPartitionLinkage = 2;
	protected int partitionLinkage = sppPartitionLinkage;
	protected 	Choice partitionLinkageChoice;

	//	boolean retainFiles = false;
	//	String MPIsetupCommand = "";
	boolean showIntermediateTrees = true;

	protected int numRuns = 1;
	protected int numRunsCompleted = 0;
	protected int run = 0;
	protected boolean preferencesSet = false;
	protected boolean isProtein = false;

	protected int bootstrapreps = 100;
	protected int bootstrapSeed = Math.abs((int)System.currentTimeMillis());
	protected static int MFPOption=3;
	protected static int modelOption = MFPOption;
	protected static String substitutionModel = "MFP";
	protected static String otherOptions = "";
	protected boolean doBootstrap = false;
	protected boolean useConstraintTree = false;
	protected boolean doUFBootstrap = false;
	protected static int minUFBootstrapReps=1000;

	protected RadioButtons charPartitionButtons = null;


	long summaryFilePosition =0;



	protected long  randseed = -1;
	static String constraintfile = "none";

	protected  SingleLineTextField substitutionModelField, otherOptionsField;

	IntegerField seedField;
	protected javax.swing.JLabel commandLabel;
	protected SingleLineTextArea commandField;
	protected IntegerField numRunsField, bootStrapRepsField;
	protected Checkbox onlyBestBox, retainFilescheckBox, doBootstrapCheckbox, useConstraintTreeCheckbox, doUFBootstrapCheckbox;
	//	int count=0;

	protected double finalValue = MesquiteDouble.unassigned;
	protected double optimizedValue = MesquiteDouble.unassigned;
	protected double[] finalValues = null;
	protected double[] optimizedValues = null;
	protected int runNumber = 0;



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
		if ("partitionScheme".equalsIgnoreCase(tag))
			partitionScheme = MesquiteInteger.fromString(content);
		if ("partitionLinkage".equalsIgnoreCase(tag))
			partitionLinkage = MesquiteInteger.fromString(content);		

		if ("bootStrapReps".equalsIgnoreCase(tag)){
			bootstrapreps = MesquiteInteger.fromString(content);
			if (bootstrapreps<1) bootstrapreps=1;
		}
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("doBootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("doUFBootstrap".equalsIgnoreCase(tag))
			doUFBootstrap = MesquiteBoolean.fromTrueFalseString(content);


		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);  
		StringUtil.appendXMLTag(buffer, 2, "partitionScheme", partitionScheme);  
		StringUtil.appendXMLTag(buffer, 2, "partitionLinkage", partitionLinkage);  
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);  
		StringUtil.appendXMLTag(buffer, 2, "doBootstrap", doBootstrap);  
		StringUtil.appendXMLTag(buffer, 2, "doUFBootstrap", doUFBootstrap);  
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
		return "1.6.0";
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
	public abstract String queryOptionsDialogTitle();


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
		String title = queryOptionsDialogTitle();
		String extra = getExtraQueryOptionsTitle();
		if (StringUtil.notEmpty(extra))
			title += " ("+extra+")";
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), title,buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()

		String helpString = "This module will prepare a matrix for "+getExecutableName()+", and ask "+getExecutableName()+" do to an analysis.  A command-line version of "+getExecutableName()+" must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by "+getExecutableName()+", and, for non-bootstrap analyses, also read in the value of the "+getExecutableName()+" score (-ln L) of the tree. " 
				+ "You can see the "+getExecutableName()+" score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose "+getExecutableName()+" Score.";

		dialog.appendToHelpString(helpString);
		if (zephyrRunnerEmployer!=null)
			dialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());


		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();
		String extraLabel = getLabelForQueryOptions();
		if (StringUtil.notEmpty(extraLabel))
			dialog.addLabel(extraLabel);

		tabbedPanel.addPanel(getExecutableName()+" Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);
		addRunnerOptions(dialog);
		if (treeInferer!=null) 
			treeInferer.addItemsToDialogPanel(dialog);
		externalProcRunner.addNoteToBottomOfDialog(dialog);

		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Replicates", true);
			doBootstrapCheckbox = dialog.addCheckBox("do bootstrap analysis", doBootstrap);
			doUFBootstrapCheckbox = dialog.addCheckBox("ultrafast bootstrap", doUFBootstrap);
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
		if (!data.hasCharacterGroups()) {
			if (partitionScheme == partitionByCharacterGroups)
				partitionScheme = noPartition;
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			if (partitionScheme == partitionByCodonPosition)
				partitionScheme = noPartition;
		}
		if (data instanceof ProteinData)
			charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups" }, partitionScheme);
		else
			charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups","use codon positions" }, partitionScheme);

		charPartitionButtons.addItemListener(this);
		if (!data.hasCharacterGroups()) {
			charPartitionButtons.setEnabled(1, false);
		}
		if (!(data instanceof DNAData && ((DNAData) data).someCoding())) {
			charPartitionButtons.setEnabled(2, false);
		}
		partitionLinkageChoice = dialog.addPopUpMenu("Partition linkages", partitionLinkageStrings(), partitionLinkage); 

		dialog.addHorizontalLine(1);

		modelOptionChoice = dialog.addPopUpMenu("Model option", modelStrings(), modelOption); 
		modelOptionChoice.addItemListener(this);
		substitutionModelField = dialog.addTextField("Substitution model:", substitutionModel, 20);

		if (getConstrainedSearchAllowed()) {
			dialog.addHorizontalLine(1);
			useConstraintTreeCheckbox = dialog.addCheckBox("use topological constraint", useConstraintTree);
		}

		/*		dialog.addHorizontalLine(1);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
		 */

		tabbedPanel.addPanel("Other options", true);
		otherOptionsField = dialog.addTextField("Other "+getExecutableName()+" options:", otherOptions, 60);

		dialog.addHorizontalLine(1);

		commandLabel = dialog.addLabel("");
		commandField = dialog.addSingleLineTextArea("", 2);
		dialog.addBlankLine();
		Button showCommand = dialog.addAListenedButton("Compose Command",null, this);
		showCommand.setActionCommand(composeProgramCommand);
		Button clearCommand = dialog.addAListenedButton("Clear",null, this);
		clearCommand.setActionCommand("clearCommand");

		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.addHorizontalLine(1);
		//		retainFilescheckBox = dialog.addCheckBox("Retain Files", retainFiles);

		dialog.completeAndShowDialog(true);
		boolean acceptableOptions = false;


		if (buttonPressed.getValue()==0)  {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (externalProcRunner.optionsChosen() && infererOK) {
				//modelOption = modelOptionChoice.getIndex();
				substitutionModel = substitutionModelField.getText();
				partitionLinkage = partitionLinkageChoice.getSelectedIndex();
				numRuns = numRunsField.getValue();
				if (bootstrapAllowed) {
					doBootstrap = doBootstrapCheckbox.getState();
					doUFBootstrap = doUFBootstrapCheckbox.getState();
					randomIntSeed = seedField.getValue();
					bootstrapreps = bootStrapRepsField.getValue();
					if (doBootstrap && bootstrapreps<minUFBootstrapReps) {
						bootstrapreps = minUFBootstrapReps;
						MesquiteMessage.discreetNotifyUser("Minimum number of bootstrap replicates for ultrafast bootstrap is " + minUFBootstrapReps + ". Number of replicates reset to " + minUFBootstrapReps);
					}

				} else
					doBootstrap=false;
				onlyBest = onlyBestBox.getState();

				if (getConstrainedSearchAllowed()) {
					useConstraintTree = useConstraintTreeCheckbox.getState();
					if (useConstraintTree)
						setConstrainedSearch(true);
				}
				partitionScheme = charPartitionButtons.getValue();
				otherOptions = otherOptionsField.getText();
				processRunnerOptions();
				storeRunnerPreferences();
				acceptableOptions = true;
			}
		}
		dialog.dispose();
		return (acceptableOptions);
	}
	public void checkEnabled(boolean doBoot) {
		onlyBestBox.setEnabled(!doBoot);
	}
	/* ................................................................................................................. */
	/** Returns the purpose for which the employee was hired (e.g., "to reconstruct ancestral states" or "for X axis"). */
	public String purposeOfEmployee(MesquiteModule employee) {
		if (employee instanceof OneTreeSource){
			return "for a source of a constraint tree for " + getExecutableName(); // to be overridden
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

	private String[] partitionLinkageStrings() {
		return new String[] {
				"Edge-linked partition model (no partition-specific rates) [-q]",
				"Edge-linked partition model (with partition-specific rates) [-spp]",
				"Edge-unlinked partition model [-sp]",
		};

	}
	private String[] modelStrings() {
		return new String[] {
				"Standard model selection (like jModelTest, ProtTest)",
				"Standard model selection followed by tree inference",
				"Extended model selection with FreeRate heterogeneity",
				"Extended model selection followed by tree inference",
				"Find best partition scheme (like PartitionFinder)",
				"Find best partition scheme followed by tree inference",
				"Find best partition scheme inc FreeRate heterogeneity",
				"Find best partition scheme inc FreeRate heterogeneity followed by tree inference",
				"Please enter the model name yourself:"
		};

	}
	private String getModel(int index) {
		switch (index) {
		case 0: 
			return "TESTONLY";
		case 1: 
			return "TEST";
		case 2: 
			return "MF";
		case 3: 
			return "MFP";
		case 4: 
			return "TESTMERGEONLY";
		case 5: 
			return "TESTMERGE";
		case 6: 
			return "MF+MERGE";
		case 7: 
			return "MFP+MERGE";
		case 8: 
			return "";
		default:
			return"";
		}

	}
	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == doBootstrapCheckbox){
			checkEnabled (doBootstrapCheckbox.getState());

		}
		else if (e.getItemSelectable() == modelOptionChoice){
			int selected = modelOptionChoice.getSelectedIndex();
			substitutionModelField.setText(getModel(selected));
		}
		else if (e.getItemSelectable() == useConstraintTreeCheckbox && useConstraintTreeCheckbox.getState()){

			getConstraintTreeSource();

		}
	}

	/*.................................................................................................................*/
	public void setProgramSeed(long seed){
		this.randseed = seed;
	}

	public String getLogFileName(){
		return getOutputFilePrefix() + ".log";
	}

	Checkbox useOptimizedScoreAsBestCheckBox =  null;
	RadioButtons SOWHConstraintButtons = null;
	public void resetSOWHOptionsConstrained(){
		useConstraintTree = true;
	}
	public void resetSOWHOptionsUnconstrained(){
		useConstraintTree = false;
	}
	public String getSOWHDetailsObserved(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for observed matrix: " + numRuns);
		sb.append("\nModel of Evolution: " +substitutionModel);
		return sb.toString();
	}
	public String getSOWHDetailsSimulated(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for each simulated matrix: " + numRuns + "\n");
		sb.append("\nModel of Evolution: " +substitutionModel);
		return sb.toString();
	}

	/*.................................................................................................................*/
	private Tree readTreeFile(TreeVector trees, String treeFilePath, String treeName, MesquiteBoolean success, boolean lastTree) {
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




	TaxaSelectionSet outgroupSet;

	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	/*.................................................................................................................*/
	public abstract Object getProgramArguments(String dataFileName, String setsFileName, boolean isPreflight);


	//String arguments;
	/*.................................................................................................................*/
	public String getDataFileName() {
		return "dataMatrix.nex";
	}
	/*.................................................................................................................*/
	public String getSetsFileName() {
		return "setsBlock.nex";
	}
	/*.................................................................................................................*/
	public String getOutputFilePrefix() {
		return "iqtreeAnalysis";
	}
	public void setConstrainedSearch(boolean constrainedSearch) {
		useConstraintTree = constrainedSearch;
		this.constrainedSearch = constrainedSearch;
	}


	/*.................................................................................................................*/
	public synchronized Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		finalValues=null;
		optimizedValues =null;
		if (!initializeGetTrees(CategoricalData.class, taxa, matrix))
			return null;

		// setup
		setProgramSeed(seed);
		isProtein = data instanceof ProteinData;

		// create local version of data file; this will then be copied over to the running location

		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, getExecutableName(), "-Run.");  
		if (tempDir==null)
			return null;
		String dataFileName = getDataFileName();   //replace this with actual file name?
		String setsFileName = getSetsFileName();   //replace this with actual file name?


		if (StringUtil.blank(dataFileName))
			dataFileName = "dataMatrix.nex"; // replace this with actual file name?

		String dataFilePath = tempDir + dataFileName;
		ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, true, selectedTaxaOnly, false, false, false, false);

		String setsFilePath = tempDir + setsFileName;

		if (partitionScheme == partitionByCharacterGroups)
			ZephyrUtil.writeNEXUSSetsBlock(taxa, tempDir, setsFileName, setsFilePath, data,  false,  false, false);
		else if (partitionScheme == partitionByCodonPosition)
			ZephyrUtil.writeNEXUSSetsBlock(taxa, tempDir, setsFileName, setsFilePath, data,  true,  false, false);


		/*
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
		 */

		setFileNames();

		String multipleModelFileContents = IOUtil.getMultipleModelRAxMLString(this, data, false);//TODO: why is partByCodPos false?  
		//Debugg.println: David: could there be a choice for partByCodPos?  I'd like that
		//Debugg.println("multipleModelFileContents " + multipleModelFileContents);

		if (StringUtil.blank(multipleModelFileContents)) 
			multipleModelFileName=null;

		String constraintTree = "";

		if ( isConstrainedSearch()){
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
			else if (useConstraintTree){
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
		//now establish the commands for invoking IQ-TREE


		Object arguments = getProgramArguments(dataFileName, setsFileName, false);
		Object preflightArguments = getProgramArguments(dataFileName, setsFileName, true);

		//	String preflightCommand = externalProcRunner.getExecutableCommand()+" --flag-check " + ((MesquiteString)preflightArguments).getValue();
		String programCommand = externalProcRunner.getExecutableCommand();
		//programCommand += StringUtil.lineEnding();  

		//	if (preFlightSuccessful(preflightCommand)) {
		//	}

		if (updateWindow)
			parametersChanged(); //just a way to ping the coordinator to update the window

		//setting up the arrays of input file names and contents
		int numInputFiles = 3;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i=0; i<numInputFiles; i++){
			fileContents[i]="";
			fileNames[i]="";
		}
		fileContents[0] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[0] = dataFileName;
		if (partitionScheme != noPartition) {
			fileContents[1] = MesquiteFile.getFileContentsAsString(setsFilePath);
			fileNames[1] = setsFileName;
		}
		//fileContents[2] = translationTable;
		//fileNames[2] = translationFileName;
		fileContents[2] = constraintTree;
		fileNames[2] = constraintTreeFileName;

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
		return IQTreeRunner.class;
	}

	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	public String getName() {
		return "IQ-Tree Runner";
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

	public boolean allowStdErrRedirect() {
		return true;
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}

	/*.................................................................................................................*/
	public synchronized Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore){
		if (isVerbose()) 
			logln("Preparing to receive "+getExecutableName()+" trees.");
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
			t =readTreeFile(treeList, treeFilePath, getExecutableName()+" Bootstrap Tree", readSuccess, false);
			ZephyrUtil.adjustTree(t, outgroupSet);
		}
		else  {
			if (numRuns>1 && !onlyBest) {
				treeFilePath = outputFilePaths[WORKING_TREEFILE];
				t =readTreeFile(treeList, treeFilePath, getExecutableName()+" Tree", readSuccess, false);

				String treeFileString = MesquiteFile.getFileContentsAsString(outputFilePaths[WORKING_TREEFILE]);
				Parser parser = new Parser(treeFileString);
				parser.setAllowComments(false);
				parser.allowComments = false;
				String line = parser.getRawNextDarkLine();
				while (!StringUtil.blank(line)) {

					//[ lh=-6568.409367 ]
					Parser subParser = new Parser(line);
					subParser.setAllowComments(false);
					String token = subParser.getFirstToken();   // should be "["
					token = subParser.getNextToken();   // should be "lh"
					token = subParser.getNextToken();   // should be "="
					token = subParser.getNextToken();   // should be value
					token = StringUtil.stripBoundingWhitespace(token);
					if (count<finalValues.length)
						finalValues[count] = -MesquiteDouble.fromString(token);
					count++;

					parser.setAllowComments(false);
					line = parser.getRawNextDarkLine();
				}



				double bestScore =MesquiteDouble.unassigned;
				int bestRun = MesquiteInteger.unassigned;
				for (int i=0; i<treeList.getNumberOfTrees() && i<finalValues.length; i++) {
					Tree newTree = treeList.getTree(i);
					String newName = newTree.getName() + " run " + (i+1);
					if (newTree instanceof AdjustableTree )
						((AdjustableTree)newTree).setName(newName);
					ZephyrUtil.adjustTree(newTree, outgroupSet);
					if (MesquiteDouble.isCombinable(finalValues[i])){
						MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
						s.setName(IOUtil.IQTREESCORENAME);
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
				finalValue = bestScore;

			} else {
				t =readTreeFile(treeList, treeFilePath, getExecutableName()+" Tree", readSuccess, true);
				ZephyrUtil.adjustTree(t, outgroupSet);
				String summary = MesquiteFile.getFileContentsAsString(outputFilePaths[OUT_LOGFILE]);
				Parser parser = new Parser(summary);
				parser.setAllowComments(false);
				parser.allowComments = false;
				String line = parser.getRawNextDarkLine();
				while (!StringUtil.blank(line) && count < 1) {
					if (line.indexOf("BEST SCORE FOUND")>=0) {
						Parser subParser = new Parser(line);
						String token = subParser.getFirstToken();   // should be "BEST"
						token = subParser.getNextToken();   // should be "SCORE"
						token = subParser.getNextToken();   // should be "FOUND"
						token = subParser.getNextToken();   // should be ":"
						token = subParser.getRemaining();
						token = StringUtil.stripBoundingWhitespace(token);
						finalValue = -MesquiteDouble.fromString(token);
						finalScore.setValue(finalValue);
						count++;
					}
					parser.setAllowComments(false);
					line = parser.getRawNextDarkLine();
				}
			}
			logln("Best score: " + finalValue);
		}

		MesquiteThread.setCurrentCommandRecord(oldCR);
		success = readSuccess.getValue();
		if (!success)
			logln("Execution of IQ-TREE unsuccessful [2]");

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
					postBean("successful, bootstrap");
				else 
					postBean("successful, ML tree");
			beanWritten=true;
			return t;
		}
		reportStdError();
		if (!beanWritten)
			postBean("failed, retrieveTreeBlock");
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

		//DavidCheck: if isDoomed() then module is closing down; abort somehow

		if (fileNum==OUT_SUMMARYFILE && outputFilePaths.length>OUT_SUMMARYFILE && !StringUtil.blank(outputFilePaths[OUT_SUMMARYFILE])) {   // info file
			if (MesquiteFile.fileExists(filePath)) {
				//String s = MesquiteFile.getFileLastContents(filePath,fPOS);
				String s = MesquiteFile.getFileContentsAsString(filePath);
				if (!StringUtil.blank(s)) {
					if (bootstrapOrJackknife() || numRuns>1) {
						numRunsCompleted=StringUtil.getNumberOfLines(s);
						currentRun=numRunsCompleted;
						if (externalProcRunner.canCalculateTimeRemaining(numRunsCompleted)) {
							double timePerRep = timer.timeSinceVeryStartInSeconds()/numRunsCompleted;   //this is time per rep
							int timeLeft = 0;
							if (bootstrapOrJackknife()) {
								logln(getExecutableName()+" bootstrap replicate " + numRunsCompleted + " of " + bootstrapreps+" completed");
								timeLeft = (int)((bootstrapreps- numRunsCompleted) * timePerRep);
							}
							else {
								logln(getExecutableName()+"  search replicate " + numRunsCompleted + " of " + numRuns+" completed");
								timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
							}
							double timeSoFar = timer.timeSinceVeryStartInSeconds();
							if (isVerbose()){
								logln("   Run time " +  StringUtil.secondsToHHMMSS((int)timeSoFar)  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
								logln("    Average time per replicate:  " +  StringUtil.secondsToHHMMSS((int)timePerRep));
								logln("    Estimated total time:  " +  StringUtil.secondsToHHMMSS((int)(timeSoFar+timeLeft))+"\n");
							}
						}
					}

				}

			} 
		}

	}

	protected static final int OUT_LOGFILE=0;
	protected static final int OUT_TREEFILE=1;
	protected static final int OUT_SUMMARYFILE=2;
	protected static final int WORKING_TREEFILE=3;
	protected static final int BESTTREEFILE = 4;


	/*.................................................................................................................*/
	public String[] getLogFileNames(){
		String treeFileName;
		String workingTreeFileName;
		String summaryFileName;
		String logFileName;
		if (bootstrapOrJackknife()) {
			if (doUFBootstrap)
				treeFileName = getOutputFilePrefix()+".ufboot";
			else
				treeFileName = getOutputFilePrefix()+".boottrees";
			workingTreeFileName= treeFileName;
			summaryFileName = treeFileName;
		}
		else  {
			treeFileName = getOutputFilePrefix()+".treefile";
			if (numRuns>1)
				workingTreeFileName = getOutputFilePrefix()+".runtrees";
			else
				workingTreeFileName = treeFileName;
			summaryFileName = workingTreeFileName;
		}
		logFileName = getOutputFilePrefix()+".log";

		return new String[]{logFileName, treeFileName, summaryFileName, workingTreeFileName};
	}
	/*.................................................................................................................*/
	public String getPreflightLogFileNames(){
		return getDataFileName()+".log";
	}
	public String getProgramName() {
		return "IQ-TREE";
	}




}
