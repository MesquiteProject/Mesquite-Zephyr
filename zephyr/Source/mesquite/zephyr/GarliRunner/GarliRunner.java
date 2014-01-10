package mesquite.zephyr.GarliRunner;
/* Zephyr source code.  Copyright 2009-2013 D. Maddison and W. Maddison.
Version 0.9, December 2013.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.GarliTrees.*;
import mesquite.zephyr.lib.*;

/* TODO:
 * 	- get it so that either the shell doesn't pop to the foreground, or the runs are all done in one shell script, rather than a shell script for each
 */

public class GarliRunner extends ZephyrRunner  implements ActionListener, ItemListener, ExternalProcessRequester {
	public static final String SCORENAME = "GARLIScore";

	GarliTrees ownerModule;
	Random rng;
	boolean onlyBest = true;
	int numRuns = 5;
	Taxa taxa;
	String outgroupTaxSetString = "";
	int outgroupTaxSetNumber = 0;
	boolean preferencesSet = false;
	SingleLineTextField garliPathField =  null;
	SingleLineTextField constraintFileField = null;
	TaxaSelectionSet outgroupSet = null;

	String ofprefix = "output";

	String dataFileName = null;
	int bootstrapreps = 0;

	boolean showConfigDetails = false;

	boolean linkModels = false;
	boolean subsetSpecificRates = false;

	static final int noPartition = 0;
	static final int partitionByCharacterGroups = 1;
	static final int partitionByCodonPosition = 2;
	static final String codpos1Subset= "1st Position";
	static final String codpos2Subset= "2nd Position";
	static final String codpos3Subset= "3rd Position";
	static final String nonCodingSubset= "Non-coding";


	int partitionScheme = partitionByCharacterGroups;
	int currentPartitionSubset = 0;

	long  randseed = -1;
	double brlenweight = 0.2;
	double randnniweight = 0.1;
	double randsprweight = 0.3 ;
	double limsprweight =  0.6;
	int intervallength = 100;
	int intervalstostore = 5;
	int limsprrange = 6;
	int meanbrlenmuts = 5;
	int gammashapebrlen = 1000;
	int gammashapemodel = 1000;
	double uniqueswapbias = 0.1;
	double distanceswapbias = 1.0;
	int inferinternalstateprobs = 0;
	String constraintfile = "";

	String ratematrix = "6rate";
	String statefrequencies = "estimate";
	String ratehetmodel = "gamma";
	int numratecats = 4;
	String invariantsites = "estimate";

	GarliCharModel[] charGroupModels;
	GarliCharModel[] codonPositionModels;
	GarliCharModel noPartitionModel;

	/*
	 *  [model0]
 datatype = nucleotide
 ratematrix = 6rate
 statefrequencies = estimate
 ratehetmodel = gamma
 numratecats = 4
 invariantsites = none

 [model1]
 datatype = nucleotide
 ratematrix = 2rate
 statefrequencies = estimate
 ratehetmodel = none
 numratecats = 1
 invariantsites = none
	 */


	MolecularData data = null;
	RadioButtons charPartitionButtons = null;
	Choice partitionChoice = null;
	Choice rateMatrixChoice = null;
	SingleLineTextField customMatrix;
	Choice invarSitesChoice= null;
	Choice rateHetChoice= null;
	IntegerField numRateCatField = null;
//	String rootDir;
	ExternalProcessRunner externalProcRunner;

	MesquiteTimer timer = new MesquiteTimer();

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class, getName() + "  needs a module to run an external process.","");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		rng = new Random(System.currentTimeMillis());
		externalProcRunner = (ExternalProcessRunner)hireEmployee(ExternalProcessRunner.class, "External Process Runner (for " + getName() + ")"); 
		if (externalProcRunner==null){
			return sorry("Couldn't find an external process runner");
		}
		externalProcRunner.setProcessRequester(this);
		
		return true;
	}

	public void intializeAfterExternalProcessRunnerHired() {
		loadPreferences();
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
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
		return null;
	}	

	public void reconnectToRequester(MesquiteCommand command){
		continueMonitoring(command);
	}


	public void initialize (ZephyrTreeSearcher ownerModule) {
		this.ownerModule= (GarliTrees)ownerModule;
	}
	public boolean initializeTaxa (Taxa taxa) {
		Debugg.println(this.getName() + " using taxa " + taxa.getID());
		Taxa currentTaxa = this.taxa;
		this.taxa = taxa;
		if (taxa!=currentTaxa && taxa!=null) {
			if (!MesquiteThread.isScripting() && !queryTaxaOptions(taxa))
				return false;
		}
		return true;
	}

	public String getGARLIConfigurationFile(){
		StringBuffer sb = new StringBuffer();
		//sb.append("\n = " + nnnnn);
		sb.append("[general] ");
		sb.append("\ndatafname=" + dataFileName); 
		sb.append("\nofprefix=" + ofprefix);
		sb.append("\noutputcurrentbesttree = 1");
		//sb.append("\noutputcurrentbesttree = 1");

		if (StringUtil.blank(constraintfile))
			sb.append("\nconstraintfile = none");  
		else
			sb.append("\nconstraintfile = constraint");  //important to be user-editable
		sb.append("\n\nrandseed = " + randseed); //important to be user-editable


		String garliGeneralOptions = "\nstreefname = random \navailablememory = 512 \nlogevery = 10 \nsaveevery = 100 \nrefinestart = 1 \noutputeachbettertopology = 1"
				+ "\nenforcetermconditions = 1 \ngenthreshfortopoterm = 10000 \nscorethreshforterm = 0.05 \nsignificanttopochange = 0.01 \noutputphyliptree = 0 \noutputmostlyuselessfiles = 0 \nwritecheckpoints = 0 \nrestart = 0";
		sb.append(garliGeneralOptions);

		sb.append("\n");

		outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		if (outgroupSet!=null) {
			sb.append("\noutgroups = " + outgroupSet.getListOfOnBits(" "));
		}
		sb.append("\nsearchreps = " + numRuns);

		sb.append("\n");
		if (linkModels)
			sb.append("\n\nlinkmodels = " + 1);
		else
			sb.append("\n\nlinkmodels = " + 0);
		if (subsetSpecificRates)
			sb.append("\nsubsetSpecificRates = " + 1);
		else
			sb.append("\nsubsetSpecificRates = " + 0);
		writeCharModels(sb);

		sb.append("\n");

		String garliMasterOptions = "\n\n[master] \nnindivs = 4 \nholdover = 1 \nselectionintensity = .5 \nholdoverpenalty = 0 \nstopgen = 5000000 \nstoptime = 5000000"
				+ "\n\nstartoptprec = .5 \nminoptprec = .01 \nnumberofprecreductions = 20 \ntreerejectionthreshold = 50.0 \ntopoweight = 1.0 \nmodweight = .05";
		sb.append(garliMasterOptions);

		sb.append("\n\nbrlenweight = " + brlenweight);
		sb.append("\nrandnniweight = " + randnniweight);
		sb.append("\nrandsprweight = " + randsprweight);
		sb.append("\nlimsprweight = " + limsprweight);
		sb.append("\nintervallength = " + intervallength);
		sb.append("\nintervalstostore = " + intervalstostore);

		sb.append("\n\nlimsprrange = " + limsprrange);
		sb.append("\nmeanbrlenmuts = " + meanbrlenmuts);
		sb.append("\ngammashapebrlen = " + gammashapebrlen);
		sb.append("\ngammashapemodel = " + gammashapemodel);
		sb.append("\nuniqueswapbias = " + uniqueswapbias);		
		sb.append("\ndistanceswapbias = " + distanceswapbias);

		sb.append("\n\nbootstrapreps = " + bootstrapreps); //important to be user-editable
		sb.append("\ninferinternalstateprobs = " + inferinternalstateprobs);
		return sb.toString();
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
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootstrapreps = MesquiteInteger.fromString(content);
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("showConfigDetails".equalsIgnoreCase(tag))
			showConfigDetails = MesquiteBoolean.fromTrueFalseString(content);

		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);  
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);  
		StringUtil.appendXMLTag(buffer, 2, "showConfigDetails", showConfigDetails);  

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public void preparePartitionChoice (Choice partitionChoice, int partitionScheme) {
		partitionChoice.removeAll();

		switch (partitionScheme) {
		case partitionByCharacterGroups : 
			ZephyrUtil.setPartitionChoice(data, partitionChoice);
			break;
		case partitionByCodonPosition : 
			partitionChoice.addItem(codpos1Subset);
			partitionChoice.addItem(codpos2Subset);
			partitionChoice.addItem(codpos3Subset);
			partitionChoice.addItem(nonCodingSubset);
			break;
		case noPartition : 
			partitionChoice.addItem("All Characters");
			break;
		default:
			partitionChoice.addItem("All Characters");
		}
	}

	/*.................................................................................................................*/
	private void setUpCharModels(MolecularData data) {
		if (data==null)
			return;
		CharactersGroup[] parts =null;
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
		}
		if (parts!=null){
			charGroupModels = new GarliCharModel[parts.length];
			for (int i=0; i<parts.length; i++) {
				charGroupModels[i] = new GarliCharModel();
			}
		}
		codonPositionModels = new GarliCharModel[4];
		for (int i=0; i<4; i++) {
			codonPositionModels[i] = new GarliCharModel();
		}
		noPartitionModel = new GarliCharModel();


	}
	/*.................................................................................................................*/
	private void writeCharModels(StringBuffer sb) {
		switch (partitionScheme) {
		case partitionByCharacterGroups : 
			for (int i=0; i<charGroupModels.length; i++) {
				charGroupModels[i].toStringBuffer(sb, i);
			}
			break;
		case partitionByCodonPosition : 
			for (int i=0; i<codonPositionModels.length; i++) {
				codonPositionModels[i].toStringBuffer(sb, i);
			}
			break;
		case noPartition : 
			noPartitionModel.toStringBuffer(sb, -1);
			break;
		default:
			noPartitionModel.toStringBuffer(sb, -1);
		}
	}

	/*.................................................................................................................*/
	private void setCharacterModels() {
		GarliCharModel charModel = null;
		switch (partitionScheme) {
		case partitionByCharacterGroups : 
			if (currentPartitionSubset>=0 && currentPartitionSubset<charGroupModels.length) {
				charModel = charGroupModels[currentPartitionSubset];
			}
			break;
		case partitionByCodonPosition : 
			if (currentPartitionSubset>=0 && currentPartitionSubset<codonPositionModels.length) {
				charModel = codonPositionModels[currentPartitionSubset];
			}
			break;
		case noPartition : 
			charModel = noPartitionModel;
			break;
		default:
			charModel = noPartitionModel;
		}


		if (rateMatrixChoice!=null) {
			rateMatrixChoice.select(charModel.getRatematrixIndex());
		}
		if (customMatrix!=null){
			customMatrix.setText(charModel.getRatematrix());
			if (rateMatrixChoice.getSelectedIndex() == 3){
				customMatrix.setEditable(true);
				customMatrix.setBackground(Color.white);
			}
			else {
				customMatrix.setEditable(false);
				customMatrix.setBackground(ColorDistribution.veryLightGray);
			}
		}

		if (rateHetChoice!=null) {
			rateHetChoice.select(charModel.getRatehetmodelIndex());
		}

		if (numRateCatField!=null) {
			numRateCatField.getTextField().setText(""+charModel.getNumratecats());
		}

		if (invarSitesChoice!=null) {
			invarSitesChoice.select(charModel.getInvariantsitesIndex());
		}
	}

	/*.................................................................................................................*/
	private void processCharacterModels() {
		int choiceValue=0;
		GarliCharModel charModel = null;
		switch (partitionScheme) {
		case partitionByCharacterGroups : 
			if (currentPartitionSubset>=0 && currentPartitionSubset<charGroupModels.length) {
				charModel = charGroupModels[currentPartitionSubset];
			}
			break;
		case partitionByCodonPosition : 
			if (currentPartitionSubset>=0 && currentPartitionSubset<codonPositionModels.length) {
				charModel = codonPositionModels[currentPartitionSubset];
			}
			break;
		case noPartition : 
			charModel = noPartitionModel;
			break;
		default:
			charModel = noPartitionModel;
		}


		if (rateMatrixChoice!=null) {
			choiceValue = rateMatrixChoice.getSelectedIndex();
			charModel.setRatematrixIndex(choiceValue);
			switch (choiceValue) {
			case 0 : 
				charModel.setRatematrix("1rate");
				break;
			case 1 : 
				charModel.setRatematrix("2rate");
				break;
			case 2 : 
				charModel.setRatematrix("6rate");
				break;
				/*case 3 : 
				charModel.setRatematrix("fixed");
				break;*/
			case 3 :  //Custom
				charModel.setRatematrix(customMatrix.getText());
				break;
			default:
				charModel.setRatematrix("6rate");
			}
		}

		if (rateHetChoice!=null) {
			choiceValue = rateHetChoice.getSelectedIndex();
			charModel.setRatehetmodelIndex(choiceValue);
			switch (choiceValue) {
			case 0 : 
				charModel.setRatehetmodel("none");
				break;
			case 1 : 
				charModel.setRatehetmodel("gamma");
				break;
			case 2 : 
				charModel.setRatehetmodel("gammafixed");
				break;
			default:
				charModel.setRatehetmodel("gamma");
			}
		}

		if (numRateCatField!=null) {

			numratecats = numRateCatField.getValue();
			//	if (numratecats>1 && choiceValue==0)
			//		numratecats=1;
			charModel.setNumratecats(numratecats);
		}

		if (invarSitesChoice!=null) {
			choiceValue = invarSitesChoice.getSelectedIndex();
			charModel.setInvariantsitesIndex(choiceValue);
			switch (choiceValue) {
			case 0 : 
				charModel.setInvariantsites("none");
				break;
			case 1 : 
				charModel.setInvariantsites("estimate");
				break;
			case 2 : 
				charModel.setInvariantsites("fixed");
				break;
			default:
				charModel.setInvariantsites("estimate");
			}
		}
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
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "GARLI Options & Locations",buttonPressed); 

		//dialog.addLabel("GARLI - Options and Locations");

		String helpString = "This module will prepare a matrix for GARLI, and ask GARLI do to an analysis.  A command-line version of GARLI must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by GARLI, and, for non-bootstrap analyses, also read in the value of the GARLI score (-ln L) of the tree. " 
				+ "You can see the GARLI score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose GARLI Score.";

		dialog.appendToHelpString(helpString);

		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();

		tabbedPanel.addPanel("GARLI Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);

		tabbedPanel.addPanel("Search Replicates & Bootstrap", true);
		IntegerField numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
		Checkbox onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		dialog.addHorizontalLine(1);
		IntegerField bootStrapRepsField = dialog.addIntegerField("Bootstrap Reps", bootstrapreps, 8, 0, MesquiteInteger.infinite);

		tabbedPanel.addPanel("Character Models", true);
		if (!data.hasCharacterGroups()) {
			if (partitionScheme == partitionByCharacterGroups)
				partitionScheme = noPartition;
		}
		if (!(data instanceof DNAData  &&  ((DNAData)data).someCoding())) {
			if (partitionScheme == partitionByCodonPosition)
				partitionScheme = noPartition;
		}
		charPartitionButtons = dialog.addRadioButtons(new String[] {"don't partition", "use character groups", "use codon positions"},partitionScheme);
		charPartitionButtons.addItemListener(this);
		if (!data.hasCharacterGroups()) {
			charPartitionButtons.setEnabled(1,false);
		}
		if (!(data instanceof DNAData  &&  ((DNAData)data).someCoding())) {
			charPartitionButtons.setEnabled(2,false);
		}

		Checkbox linkModelsBox = dialog.addCheckBox("use same set of model parameters for all partition subsets", linkModels);
		Checkbox subsetSpecificRatesBox = dialog.addCheckBox("infer overall rate multipliers for each partition subset", subsetSpecificRates);

		dialog.addHorizontalLine(1);
		partitionChoice = dialog.addPopUpMenu ("Edit model for this partition subset:",  new String[] {"All Characters"}, 0);
		preparePartitionChoice(partitionChoice, partitionScheme);
		partitionChoice.addItemListener(this);

		rateMatrixChoice = dialog.addPopUpMenu ("Rate Matrix",  new String[] {"Equal Rates", "2-Parameter", "GTR       ", "Custom"}, 2);
		rateMatrixChoice.addItemListener(this);
		customMatrix = dialog.addTextField("6rate", 20); //since 2 is selected as default in previous
		customMatrix.setEditable(false);
		customMatrix.setBackground(ColorDistribution.veryLightGray);

		invarSitesChoice= dialog.addPopUpMenu ("Invariant Sites",  new String[] {"none", "Estimate Proportion"}, 1);
		rateHetChoice= dialog.addPopUpMenu ("Gamma Site-to-Site Rate Model",  new String[] {"none", "Estimate Shape Parameter"}, 1);
		numRateCatField = dialog.addIntegerField("Number of Rate Categories for Gamma", numratecats, 4, 1, 20);

		tabbedPanel.addPanel("Constraint File", true);
		constraintFileField = dialog.addTextField("Path to Constraint File:", constraintfile, 40);
		Button constraintFileBrowseButton = dialog.addAListenedButton("Browse...",null, this);
		constraintFileBrowseButton.setActionCommand("constraintBrowse");

		tabbedPanel.addPanel("Other options", true);
		Checkbox showConfigDetailsBox = dialog.addCheckBox("show config file", showConfigDetails);

		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.completeAndShowDialog(true);

		if (buttonPressed.getValue()==0)  {
			if (externalProcRunner.optionsChosen()) {
				constraintfile = constraintFileField.getText();
				numRuns = numRunsField.getValue();
				bootstrapreps = bootStrapRepsField.getValue();
				onlyBest = onlyBestBox.getState();
				showConfigDetails = showConfigDetailsBox.getState();
				partitionScheme = charPartitionButtons.getValue();
				linkModels = linkModelsBox.getState();
				subsetSpecificRates = subsetSpecificRatesBox.getState();

				//garliOptions = garliOptionsField.getText();

				processCharacterModels();

				storePreferences();
				externalProcRunner.storePreferences();
			}
		}
		dialog.dispose();
		if (closeWizard) 
			MesquiteDialog.closeWizard();

		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public boolean queryTaxaOptions(Taxa taxa) {
		Debugg.println(this.getName() + " using taxa " + taxa.getID());
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (ssv==null || ssv.size()<=0)
			return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "GARLI Outgroup Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("GARLI Outgroup Options");


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
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("constraintBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			constraintfile = MesquiteFile.openFileDialog("Choose Constraint File", directoryName, fileName);
			if (StringUtil.notEmpty(constraintfile))
				constraintFileField.setText(constraintfile);
		}
	}

	/*.................................................................................................................*/
	public void setGarliSeed(long seed){
		this.randseed = seed;
	}

	ProgressIndicator progIndicator;
	int count=0;

	double finalValue = MesquiteDouble.unassigned;
	double[] finalValues = null;
	int runNumber = 0;

	String treeFileName;
	String treeFilePath;
	String currentTreeFilePath;
	String allBestTreeFilePath;
	String[] logFileNames;
	String constraintFilePath;
	String configFileName;

	/*.................................................................................................................*/
	private void initializeMonitoring(){
		if (finalValues==null) {
			if (bootstrap())
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numRuns];
			DoubleArray.deassignArray(finalValues);
		}
	}
	static final int MAINLOGFILE =0;
	static final int CURRENTTREEFILEPATH=1;
	static final int SCREENLOG=2;
	static final int TREEFILE=3;
	static final int BESTTREEFILE=4;
	/*.................................................................................................................*/
	private void setFilePaths () {
		configFileName =  "garli.conf";

		if (bootstrap())
			treeFileName = ofprefix+".boot.tre";
		else
			treeFileName = ofprefix+".best.tre";
		treeFilePath = treeFileName;
		currentTreeFilePath =  ofprefix+".best.current.tre";
		allBestTreeFilePath = ofprefix+".best.all.tre";
		String mainLogFileName = ofprefix+".log00.log";

		String[] logFileNamesLocal = {mainLogFileName, currentTreeFilePath, ofprefix+".screen.log", treeFileName, allBestTreeFilePath};
		logFileNames = logFileNamesLocal;

		constraintFilePath = "constraint";
	}
	/*=================================================*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {

		if (matrix==null )
			return null;
		if (!(matrix.getParentData() != null && matrix.getParentData() instanceof MolecularData)){
			MesquiteMessage.discreetNotifyUser("Sorry, GARLI Tree inference works only if given a full MolecularData object");
			return null;
		}
		data = (MolecularData)matrix.getParentData();

		setUpCharModels(data);

		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return null;
		}
		
		if (!MesquiteThread.isScripting() && !queryOptions()){
			return null;
		}


	
		initializeMonitoring();
		setGarliSeed(seed);
		getProject().incrementProjectWindowSuppression();
		data.setEditorInhibition(true);

		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());

		// create the data file
		String tempDir = ZephyrUtil.createDirectoryForFiles(this, ZephyrUtil.IN_SUPPORT_DIR, "GARLI");
		if (tempDir==null)
			return null;
		dataFileName = "tempData" + MesquiteFile.massageStringToFilePathSafe(unique) + ".nex";   //replace this with actual file name?
		String dataFilePath = tempDir +  dataFileName;
		if (partitionScheme==noPartition)
			ZephyrUtil.writeNEXUSFile(taxa,  tempDir,  dataFileName,  dataFilePath,  data, true, false, false);
		else if (partitionScheme==partitionByCharacterGroups)
			ZephyrUtil.writeNEXUSFile(taxa,  tempDir,  dataFileName,  dataFilePath,  data, true, true, false);
		else if (partitionScheme==partitionByCodonPosition)
			ZephyrUtil.writeNEXUSFile(taxa,  tempDir,  dataFileName,  dataFilePath,  data, true, true, true);

		setFilePaths();

		//setting up the GARLI config file
		String config = getGARLIConfigurationFile();
		if (!MesquiteThread.isScripting() && showConfigDetails) {
			config = MesquiteString.queryMultiLineString(getModuleWindow(), "GARLI Config File", "GARLI Config File2", config, 30, false, true);
			if (StringUtil.blank(config))
				return null;
		}

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
		if (StringUtil.notEmpty(constraintfile)) {
			fileContents[1] = MesquiteFile.getFileContentsAsString(constraintfile);
			fileNames[1] = "constraint";
		}
		fileContents[2] = config;
		fileNames[2] = configFileName;

		String GARLIcommand = externalProcRunner.getExecutableCommand();
		if (externalProcRunner.isWindows())
			GARLIcommand += " --batch " + configFileName+StringUtil.lineEnding();  
		else
			GARLIcommand += StringUtil.lineEnding();  // GARLI command is very simple as all of the arguments are in the config file


		/*  ============ Setting up the run ============  */
		boolean success = externalProcRunner.setInputFiles(GARLIcommand,fileContents, fileNames);
		if (!success){
			// give message about failure
			return null;
		}

		externalProcRunner.setOutputFileNamesToWatch(logFileNames);


		progIndicator = new ProgressIndicator(getProject(),ownerModule.getName(), "GARLI Search", 0, true);
		if (progIndicator!=null){
			count = 0;
			progIndicator.start();
		}
		MesquiteMessage.logCurrentTime("Start of " + getExecutableName() + " analysis: ");
		timer.start();


		/*  ============ STARTING THE PROCESS ============  */
		success = externalProcRunner.startExecution();

		/*  ============ THE PROCESS RUNS ============  */

		if (success)
			success = externalProcRunner.monitorExecution();
		else
			alert("The GARLI run encountered problems. ");  // better error message!

		/*  ============ THE PROCESS COMPLETES ============  */

		logln("GARLI analysis completed at " + getDateAndTime());
		logln("Total time: " + timer.timeSinceVeryStartInSeconds() + " seconds");
		if (progIndicator!=null)
			progIndicator.goAway();

		if (success){
			getProject().decrementProjectWindowSuppression();
			return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
		}

		getProject().decrementProjectWindowSuppression();
		data.setEditorInhibition(false);
		return null;
	}	
	
	
	/*.................................................................................................................*/
	public Tree continueMonitoring(MesquiteCommand callBackCommand) {
		logln("Monitoring GARLI run begun.");
		getProject().incrementProjectWindowSuppression();

		initializeMonitoring();
		setFilePaths();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		/*	MesquiteModule inferer = findEmployerWithDuty(TreeInferer.class);
		if (inferer != null)
			((TreeInferer)inferer).bringIntermediatesWindowToFront();*/
		boolean success = externalProcRunner.monitorExecution();

		if (progIndicator!=null)
			progIndicator.goAway();

		//deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();
		if (data != null)
			data.setEditorInhibition(false);
		if (callBackCommand != null)
			callBackCommand.doItMainThread(null,  null,  this);
		return null;
	}	

	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore){
		logln("Preparing to receive GARLI trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		finalScore.setValue(finalValue);

		getProject().incrementProjectWindowSuppression();
		FileCoordinator coord = getFileCoordinator();
		MesquiteFile tempDataFile = null;
		CommandRecord oldCR = MesquiteThread.getCurrentCommandRecord();
		CommandRecord scr = new CommandRecord(true);
		MesquiteThread.setCurrentCommandRecord(scr);

		// define file paths and set tree files as needed. 
		setFilePaths();
		String[] outputFilePaths = externalProcRunner.getOutputFilePaths();

		// read in the tree files
		if (onlyBest || numRuns==1 || bootstrap())
			tempDataFile = (MesquiteFile)coord.doCommand("includeTreeFile", StringUtil.tokenize(outputFilePaths[TREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); //TODO: never scripting???
		else
			tempDataFile = (MesquiteFile)coord.doCommand("includeTreeFile", StringUtil.tokenize(outputFilePaths[BESTTREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); //TODO: never scripting???

		runFilesAvailable();

		MesquiteThread.setCurrentCommandRecord(oldCR);

		TreesManager manager = (TreesManager)findElementManager(TreeVector.class);
		Tree t =null;
		int numTB = manager.getNumberTreeBlocks(taxa);
		TreeVector tv = manager.getTreeBlock(taxa,numTB-1);
		if (tv!=null) {
			t = tv.getTree(0);
			ZephyrUtil.adjustTree(t, outgroupSet);

			if (t!=null)
				success=true;

			if (treeList !=null) {
				double bestScore =MesquiteDouble.unassigned;
				for (int i=0; i<tv.getNumberOfTrees(); i++) {
					Tree newTree = tv.getTree(i);
					ZephyrUtil.adjustTree(newTree, outgroupSet);

					if (finalValues!=null && i<finalValues.length && MesquiteDouble.isCombinable(finalValues[i])){
						MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
						s.setName(GarliRunner.SCORENAME);
						((Attachable)newTree).attachIfUniqueName(s);
					}

					treeList.addElement(newTree, false);

					if (finalValues!=null && i<finalValues.length && MesquiteDouble.isCombinable(finalValues[i]))
						if (MesquiteDouble.isUnassigned(bestScore))
							bestScore = finalValues[i];  //Debugg.println must refind final values, best score
						else if (bestScore<finalValues[i])
							bestScore = finalValues[i];
				}
				logln("Best score: " + bestScore);

			} 
		}
		//int numTB = manager.getNumberTreeBlocks(taxa);

		getProject().decrementProjectWindowSuppression();
		if (tempDataFile!=null)
			tempDataFile.close();
		//deleteSupportDirectory();
		if (data != null)
			data.setEditorInhibition(false);
		manager.deleteElement(tv);  // get rid of temporary tree block
		if (success)
			return t;
		return null;
	}	

	Parser parser = new Parser();
	long screenFilePos = 0;
	MesquiteFile screenFile = null;

	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		return outputFilePaths;
	}
	/*.................................................................................................................*/
	public String getOutputFileToReadPath(String originalPath) {
		File file = new File(originalPath);
		File fileCopy = new File(originalPath + "2");
		if (file.renameTo(fileCopy))
			return originalPath + "2";
		return originalPath;
	}

	int numRunsCompleted = 0;
	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {
		if ((progIndicator!=null && progIndicator.isAborted()))
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];

		/*		if (fileNum==1)
			filePath = getOutputFileToReadPath(outputFilePaths[fileNum]);
		else 
			filePath = outputFilePaths[fileNum];
		 */

		if (fileNum==MAINLOGFILE && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[MAINLOGFILE]) && !bootstrap()) {   // screen log
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator!=null) {
						parser.setString(s);
						String gen = parser.getFirstToken(); // generation number
						progIndicator.setText("Generation: " + gen + ", ln L = " + parser.getNextToken());
						progIndicator.spin();		

					}
				count++;
			} else
				Debugg.println("*** File does not exist (" + filePath + ") ***");
		}

		if (fileNum==CURRENTTREEFILEPATH && outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[CURRENTTREEFILEPATH]) && !bootstrap()) {
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				ownerModule.newTreeAvailable(treeFilePath, outgroupSet);
			}
			else ownerModule.newTreeAvailable(treeFilePath, null);
		}

		if (fileNum==SCREENLOG && outputFilePaths.length>2 && !StringUtil.blank(outputFilePaths[SCREENLOG])) {
			if (screenFile==null) {   //this is the output file
				if (MesquiteFile.fileExists(filePath))
					screenFile = MesquiteFile.open(true, filePath);
				else
					Debugg.println("*** File does not exist (" + filePath + ") ***");
			}
			if (screenFile!=null) {
				screenFile.openReading();
				if (!MesquiteLong.isCombinable(screenFilePos))
					screenFilePos=0;
				screenFile.goToFilePosition(screenFilePos);
				String s = screenFile.readLine();
				while (s!=null){ //  && screenFile.getFilePosition()<screenFile.existingLength()-2) {
					if (s.startsWith("Final score")) {
						parser.setString(s);
						String s1 = parser.getFirstToken(); // Final
						s1 = parser.getNextToken(); // score
						s1 = parser.getNextToken(); // =
						s1 = parser.getNextToken(); // number
						if (finalValues!=null && runNumber<finalValues.length)
							finalValues[runNumber] = MesquiteDouble.fromString(s1);
						runNumber++;
						if (bootstrap())
							logln("GARLI bootstrap replicate " + runNumber + " of " + getTotalReps() + ", ln L = " + s1);
						else
							logln("GARLI search replicate " + runNumber + " of " + getTotalReps() +  ", ln L = " + s1);
						numRunsCompleted++;
						double timePerRep = timer.timeSinceVeryStartInSeconds()/numRunsCompleted;   //this is time per rep
						int timeLeft = 0;
						if (bootstrap()) {
							timeLeft = (int)((bootstrapreps- numRunsCompleted) * timePerRep);
						}
						else {
							timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
						}
						logln("  Running time so far " +  StringUtil.secondsToHHMMSS((int)timer.timeSinceVeryStartInSeconds())  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));

					}
					else if (s.startsWith("ERROR:"))
						MesquiteMessage.discreetNotifyUser("GARLI " + s);
					s = screenFile.readLine();
					count++;
				}

				screenFilePos = screenFile.getFilePosition();
				screenFile.closeReading();
			}
		}

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
		for (int i = 0; i<logFileNames.length; i++){
			runFilesAvailable(i);
		}
	}

	/*.................................................................................................................*

	public void processCompletedOutputFiles(String[] outputFilePaths) {
		runNumber=0;
		screenFilePos = 0;
		if (outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[MAINLOGFILE])) {
			if (!bootstrap()) {
				String s = MesquiteFile.getFileLastContents(outputFilePaths[MAINLOGFILE]);
				if (!StringUtil.blank(s)) {
					parser.setString(s);
					parser.getFirstToken();
					finalValue = MesquiteDouble.fromString(parser.getNextToken());
				}
			}
			ZephyrUtil.copyLogFile(this, getExecutableName(), outputFilePaths[SCREENLOG]);

		}
	}
	/*.................................................................................................................*

	public boolean continueShellProcess(Process proc){
		if (progIndicator!=null && progIndicator.isAborted()) {
			try {
				proc.destroy();
			}
			catch (Exception e) {
				MesquiteMessage.warnProgrammer("EXCEPTION ");
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}
	/*.................................................................................................................*/


	public boolean bootstrap() {
		return bootstrapreps>0;
	}
	public int getBootstrapreps() {
		return bootstrapreps;
	}

	public void setBootstrapreps(int bootstrapreps) {
		this.bootstrapreps = bootstrapreps;
	}

	/*.................................................................................................................*
	public void setNumRateCats(int numratecats){
		this.numratecats = numratecats;
	}
	/*.................................................................................................................*
	public int getNumRateCats(){
		return numratecats;
	}
	/*.................................................................................................................*
	public void setRateMatrix(String ratematrix){
		this.ratematrix = ratematrix;
	}
	/*.................................................................................................................*
	public String getRateMatrix(){
		return ratematrix;
	}
	/*.................................................................................................................*
	public void setRateHetModel(String ratehetmodel){
		this.ratehetmodel = ratehetmodel;
	}
	/*.................................................................................................................*
	public String getRateHetModel(){
		return ratehetmodel;
	}
	/*.................................................................................................................*
	public void setInvariantSites(String invariantsites){
		this.invariantsites = invariantsites;
	}
	/*.................................................................................................................*
	public String getInvariantSites(){
		return invariantsites;
	}
	/*.................................................................................................................*/
	public void setOfPrefix(String ofprefix){
		this.ofprefix = ofprefix;
	}

	public Class getDutyClass() {
		return GarliRunner.class;
	}

	public String getName() {
		return "GARLI Runner";
	}

	/*.................................................................................................................*/
	public int getNumRuns(){
		return numRuns;
	}
	/*.................................................................................................................*/
	public int getTotalReps(){
		if (bootstrap())
			return getBootstrapreps();
		else
			return numRuns;
	}
	/*.................................................................................................................*/
	public boolean getOnlyBest(){
		return onlyBest;
	}

	public void itemStateChanged(ItemEvent e) {
		if (charPartitionButtons.isAButton(e.getItemSelectable())){   // button for the partition scheme
			processCharacterModels();
			if (charPartitionButtons!=null)
				partitionScheme = charPartitionButtons.getValue();
			if (partitionChoice!=null)
				preparePartitionChoice(partitionChoice, partitionScheme);
		} else if (e.getItemSelectable() == partitionChoice){   // popup for which partition to edit
			processCharacterModels();
			if (partitionScheme == partitionByCodonPosition) {
				if (codpos1Subset.equalsIgnoreCase((String)e.getItem())) {
					currentPartitionSubset = 0;
					setCharacterModels();
				} else if (codpos2Subset.equalsIgnoreCase((String)e.getItem())) {
					currentPartitionSubset = 1;
					setCharacterModels();
				}
				else if (codpos3Subset.equalsIgnoreCase((String)e.getItem())) {
					currentPartitionSubset = 2;
					setCharacterModels();
				} else if (nonCodingSubset.equalsIgnoreCase((String)e.getItem())) {
					currentPartitionSubset = 3;
					setCharacterModels();
				}
			} else if (partitionScheme == partitionByCharacterGroups) {
				currentPartitionSubset = ZephyrUtil.getPartitionSubset(data, (String)e.getItem());
				setCharacterModels();
			} else
				setCharacterModels();

		}
		else if (e.getItemSelectable() == rateMatrixChoice){
			String matrix = "";
			int choiceValue = rateMatrixChoice.getSelectedIndex();
			switch (choiceValue) {
			case 0 : 
				matrix = "1rate";
				break;
			case 1 : 
				matrix = "2rate";
				break;
			case 2 : 
				matrix = "6rate";
				break;
			case 3 :  //Custom  
				matrix = customMatrix.getText();
				if (matrix == null || "1rate 2rate 6rate".indexOf(matrix)>=0)  //Debugg.println  keep previous custom matrices remembered for users who switch back to them?
					matrix = "(a a a a a a)";  
				break;
			default:
				matrix = "6rate";
			}
			customMatrix.setText(matrix);
			if (choiceValue == 3){
				customMatrix.setEditable(true);
				customMatrix.setBackground(Color.white);
			}
			else {
				customMatrix.setEditable(false);
				customMatrix.setBackground(ColorDistribution.veryLightGray);
			}
		}

	}


	public String getExecutableName() {
		return "GARLI";
	}


	public void runFailed(String message) {
		// TODO Auto-generated method stub

	}

	public void runFinished(String message) {
		// TODO Auto-generated method stub

	}


}




class GarliCharModel {
	String ratematrix = "6rate";
	String statefrequencies = "estimate";
	String ratehetmodel = "gamma";
	int numratecats = 4;
	String invariantsites = "estimate";

	int ratematrixIndex = 2;
	int statefrequenciesIndex = 0;
	int ratehetmodelIndex = 1;
	int invariantsitesIndex = 1;



	public String getRatematrix() {
		return ratematrix;
	}
	public void setRatematrix(String ratematrix) {
		this.ratematrix = ratematrix;
	}
	public String getStatefrequencies() {
		return statefrequencies;
	}
	public void setStatefrequencies(String statefrequencies) {
		this.statefrequencies = statefrequencies;
	}
	public String getRatehetmodel() {
		return ratehetmodel;
	}
	public void setRatehetmodel(String ratehetmodel) {
		this.ratehetmodel = ratehetmodel;
	}
	public int getNumratecats() {
		return numratecats;
	}
	public void setNumratecats(int numratecats) {
		this.numratecats = numratecats;
	}
	public String getInvariantsites() {
		return invariantsites;
	}
	public void setInvariantsites(String invariantsites) {
		this.invariantsites = invariantsites;
	}

	public void toStringBuffer (StringBuffer sb, int modelNumber) {
		sb.append("\n");
		if (modelNumber>=0)
			sb.append("\n[model"+modelNumber+"]");

		sb.append("\nratematrix = " + ratematrix);
		sb.append("\nstatefrequencies = " + statefrequencies);
		sb.append("\nratehetmodel = " + ratehetmodel);
		if (numratecats>1 && "none".equalsIgnoreCase(ratehetmodel))
			sb.append("\nnumratecats = 1");
		else 
			sb.append("\nnumratecats = " + numratecats);
		sb.append("\ninvariantsites = " + invariantsites);

	}
	public int getRatematrixIndex() {
		return ratematrixIndex;
	}
	public void setRatematrixIndex(int ratematrixIndex) {
		this.ratematrixIndex = ratematrixIndex;
	}
	public int getStatefrequenciesIndex() {
		return statefrequenciesIndex;
	}
	public void setStatefrequenciesIndex(int statefrequenciesIndex) {
		this.statefrequenciesIndex = statefrequenciesIndex;
	}
	public int getRatehetmodelIndex() {
		return ratehetmodelIndex;
	}
	public void setRatehetmodelIndex(int ratehetmodelIndex) {
		this.ratehetmodelIndex = ratehetmodelIndex;
	}
	public int getInvariantsitesIndex() {
		return invariantsitesIndex;
	}
	public void setInvariantsitesIndex(int invariantsitesIndex) {
		this.invariantsitesIndex = invariantsitesIndex;
	}


}

