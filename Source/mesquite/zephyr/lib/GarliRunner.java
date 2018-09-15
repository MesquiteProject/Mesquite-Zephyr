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

import javax.swing.*;
import javax.swing.event.*;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.io.lib.IOUtil;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.lib.*;

public abstract class GarliRunner extends ZephyrRunner implements ItemListener, ActionListener, ExternalProcessRequester, ConstrainedSearcherTreeScoreProvider {
	public static final String SCORENAME = "GARLIScore";

	boolean onlyBest = true;
	boolean doBootstrap = false;
	protected int numRuns = 5;
	int outgroupTaxSetNumber = 0;
	boolean preferencesSet = false;
	protected SingleLineTextField garliPathField = null;
	RadioButtons constraintButtons;
	TaxaSelectionSet outgroupSet = null;

	String ofprefix = "output";

	String dataFileName = null;
	int bootstrapreps = 100;

	boolean showConfigDetails = false;

	protected int previousCurrentRun=0;

	boolean linkModels = false;
	boolean subsetSpecificRates = false;

	static final int noPartition = 0;
	static final int partitionByCharacterGroups = 1;
	static final int partitionByCodonPosition = 2;
	static final String codpos1Subset = "1st Position";
	static final String codpos2Subset = "2nd Position";
	static final String codpos3Subset = "3rd Position";
	static final String nonCodingSubset = "Non-coding";

	int partitionScheme = partitionByCharacterGroups;
	int currentPartitionSubset = 0;

	protected long randseed = -1;
	double brlenweight = 0.2;
	double randnniweight = 0.1;
	double randsprweight = 0.3;
	double limsprweight = 0.6;
	int intervallength = 100;
	int intervalstostore = 5;
	int limsprrange = 6;
	int meanbrlenmuts = 5;
	int gammashapebrlen = 1000;
	int gammashapemodel = 1000;
	double uniqueswapbias = 0.1;
	double distanceswapbias = 1.0;
	int inferinternalstateprobs = 0;

	String ratematrix = "6rate";
	String statefrequencies = "estimate";
	String ratehetmodel = "gamma";
	int numratecats = 4;
	String invariantsites = "estimate";

	GarliCharModel[] charGroupModels;
	GarliCharModel[] codonPositionModels;
	GarliCharModel noPartitionModel;

	protected static final int DATAFILENUMBER = 0;
	protected static final int CONSTRAINTFILENUMBER = 1;
	protected static final int CONFIGFILENUMBER = 2;

	protected static final int MAINLOGFILE = 0;
	protected static final int CURRENTTREEFILEPATH = 1;
	protected static final int SCREENLOG = 2;
	protected static final int TREEFILE = 3;
	protected static final int BESTTREEFILE = 4;

	protected static final int NOCONSTRAINT = 0;
	protected static final int POSITIVECONSTRAINT = 1;
	protected static final int NEGATIVECONSTRAINT = 2;
	protected int useConstraintTree = NOCONSTRAINT;
	protected int SOWHConstraintTree = POSITIVECONSTRAINT;

	Tree constraint = null;



	/*
	 * [model0] datatype = nucleotide ratematrix = 6rate statefrequencies =
	 * estimate ratehetmodel = gamma numratecats = 4 invariantsites = none
	 * 
	 * [model1] datatype = nucleotide ratematrix = 2rate statefrequencies =
	 * estimate ratehetmodel = none numratecats = 1 invariantsites = none
	 */

	RadioButtons charPartitionButtons = null;
	Choice partitionChoice = null;
	Choice rateMatrixChoice = null;
	Choice stateFreqChoice = null;
	SingleLineTextField customMatrix;
	Choice invarSitesChoice = null;
	Choice rateHetChoice = null;
	Checkbox doBootstrapCheckbox = null;
	IntegerField numRateCatField = null;
	Checkbox onlyBestBox = null;

	// String rootDir;

	public void getEmployeeNeeds() { // This gets called on startup to harvest
		// information; override this and
		// inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(ExternalProcessRunner.class, getName() + "  needs a module to run an external process.", "");
	}

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		if (!hireExternalProcessRunner()){
			return sorry("Couldn't hire an external process runner");
		}
		externalProcRunner.setProcessRequester(this);


		return true;
	}
	/*.................................................................................................................*/
	public String getProgramURL() {
		return "https://code.google.com/p/garli/";
	}
	/* ................................................................................................................. */
	/** Returns the purpose for which the employee was hired (e.g., "to reconstruct ancestral states" or "for X axis"). */
	public String purposeOfEmployee(MesquiteModule employee) {
		if (employee instanceof OneTreeSource){
			return "for a source of a constraint tree for GARLI"; // to be overridden
		}
		return "for " + getName(); // to be overridden
	}
	public void intializeAfterExternalProcessRunnerHired() {
		loadPreferences();
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) {
		Snapshot temp = super.getSnapshot(file);
		temp.addLine("setExternalProcessRunner", externalProcRunner);
		return temp;
	}
	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(){
		String s = "";
		if (getRunInProgress()) {
			if (bootstrapOrJackknife()){
				s+="Bootstrap analysis<br>";
				s+="Bootstrap replicates completed: <b>";
				if (numRunsCompleted>bootstrapreps)
					s+=numRuns +" of " + bootstrapreps;
				else
					s+=numRunsCompleted +" of " + bootstrapreps;
			}
			else {
				s+="Search for ML Tree<br>";
				s+="Search replicates completed: <b>";
				if (numRunsCompleted>numRuns)
					s+=numRuns +" of " + numRuns;
				else
					s+=numRunsCompleted +" of " + numRuns;
			}
			s+="</b>";
		}
		return s;
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Hires the ExternalProcessRunner", "[name of module]", commandName, "setExternalProcessRunner")) {
			ExternalProcessRunner temp = (ExternalProcessRunner) replaceEmployee(ExternalProcessRunner.class, arguments,"External Process Runner", externalProcRunner);
			if (temp != null) {
				externalProcRunner = temp;
				parametersChanged();
			}
			externalProcRunner.setProcessRequester(this);
			return externalProcRunner;
		} else
			return super.doCommand(commandName, arguments, checker);
	}

	public void reconnectToRequester(MesquiteCommand command) {
		continueMonitoring(command);
	}
	/*.................................................................................................................*/

	public void appendToConfigFileGeneral(StringBuffer config) {
	}
	/*.................................................................................................................*/

	public String getGARLIConfigurationFile(CharacterData data) {
		StringBuffer sb = new StringBuffer();
		sb.append("[general] ");

		appendToConfigFileGeneral(sb);

		sb.append("\nlogevery = 10 \nsaveevery = 100 \nrefinestart = 1 \noutputeachbettertopology = 1");
		sb.append("\nenforcetermconditions = 1 \ngenthreshfortopoterm = 10000 \nscorethreshforterm = 0.05 \nsignificanttopochange = 0.01 \noutputphyliptree = 0  \nwritecheckpoints = 0 \nrestart = 0");

		sb.append("\n");


		sb.append("\noutputcurrentbesttree = 1");

		outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString, TaxaSelectionSet.class);
		if (outgroupSet != null) {
			sb.append("\noutgroups = " + outgroupSet.getListOfOnBits(" "));
		}

		sb.append("\n");
		if (linkModels)
			sb.append("\n\nlinkmodels = " + 1);
		else
			sb.append("\n\nlinkmodels = " + 0);
		if (subsetSpecificRates)
			sb.append("\nsubsetSpecificRates = " + 1);
		else
			sb.append("\nsubsetSpecificRates = " + 0);
		if (data instanceof ProteinData){
			sb.append("\ndatatype = aminoacid");
		}

		writeCharModels(sb, data);

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

		if (bootstrapOrJackknife())
			sb.append("\n\nbootstrapreps = " + bootstrapreps); // important to
		// be
		// user-editable
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
	public boolean isPrerelease(){
		return false;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference(String tag, String content) {
		if ("numRuns".equalsIgnoreCase(tag))
			numRuns = MesquiteInteger.fromString(content);
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootstrapreps = MesquiteInteger.fromString(content);
		if ("doBootstrap".equalsIgnoreCase(tag))
			doBootstrap = MesquiteBoolean.fromTrueFalseString(content);
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("showConfigDetails".equalsIgnoreCase(tag))
			showConfigDetails = MesquiteBoolean.fromTrueFalseString(content);

		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML() {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);
		StringUtil.appendXMLTag(buffer, 2, "doBootstrap", doBootstrap);
		StringUtil.appendXMLTag(buffer, 2, "showConfigDetails",
				showConfigDetails);

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public void preparePartitionChoice(Choice partitionChoice,
			int partitionScheme) {
		partitionChoice.removeAll();

		switch (partitionScheme) {
		case partitionByCharacterGroups:
			ZephyrUtil.setPartitionChoice(data, partitionChoice);
			break;
		case partitionByCodonPosition:
			partitionChoice.addItem(codpos1Subset);
			partitionChoice.addItem(codpos2Subset);
			partitionChoice.addItem(codpos3Subset);
			partitionChoice.addItem(nonCodingSubset);
			break;
		case noPartition:
			partitionChoice.addItem("All Characters");
			break;
		default:
			partitionChoice.addItem("All Characters");
		}
	}

	/*.................................................................................................................*/
	private void setUpCharModels(CategoricalData data) {
		if (data == null)
			return;
		CharactersGroup[] parts = null;
		CharacterPartition characterPartition = (CharacterPartition) data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition != null) {
			parts = characterPartition.getGroups();
		}
		if (parts != null) {
			int extra = 0;
			if (characterPartition.getAnyCurrentlyUnassigned())
				extra++;
			charGroupModels = new GarliCharModel[parts.length + extra];
			for (int i = 0; i < parts.length + extra; i++) {
				charGroupModels[i] = new GarliCharModel(data instanceof ProteinData);
			}
		}
		codonPositionModels = new GarliCharModel[4];
		for (int i = 0; i < 4; i++) {
			codonPositionModels[i] = new GarliCharModel(data instanceof ProteinData);
		}
		noPartitionModel = new GarliCharModel(data instanceof ProteinData);

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
		/*		MesquiteString arguments = (MesquiteString)getProgramArguments(getDataFileName(), false);
		if (arguments!=null && !arguments.isBlank()){
			appendToSearchDetails("\n" + getProgramName() + " command options: " + arguments.toString()); 
		} */

	}

	/*.................................................................................................................*/
	private void writeCharModels(StringBuffer sb, CharacterData data) {
		switch (partitionScheme) {
		case partitionByCharacterGroups:
			for (int i = 0; i < charGroupModels.length; i++) {
				charGroupModels[i].toStringBuffer(sb, i);
			}
			break;
		case partitionByCodonPosition:
			for (int i = 0; i < codonPositionModels.length; i++) {
				if (data instanceof DNAData)
					if (((DNAData)data).anyCodPos(i, false))   // only write if there are codpos of that sort
						codonPositionModels[i].toStringBuffer(sb, i);
			}
			break;
		case noPartition:
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
		case partitionByCharacterGroups:
			if (currentPartitionSubset >= 0 && currentPartitionSubset < charGroupModels.length) {
				charModel = charGroupModels[currentPartitionSubset];
			}
			break;
		case partitionByCodonPosition:
			if (currentPartitionSubset >= 0 && currentPartitionSubset < codonPositionModels.length) {
				charModel = codonPositionModels[currentPartitionSubset];
			}
			break;
		case noPartition:
			charModel = noPartitionModel;
			break;
		default:
			charModel = noPartitionModel;
		}

		if (rateMatrixChoice != null) {
			rateMatrixChoice.select(charModel.getRatematrixIndex());
		}
		if (stateFreqChoice != null) {
			stateFreqChoice.select(charModel.getStatefrequenciesIndex());
		}
		if (customMatrix != null) {
			customMatrix.setText(charModel.getRatematrix());
			if (rateMatrixChoice.getSelectedIndex() == 3) {
				customMatrix.setEditable(true);
				customMatrix.setBackground(Color.white);
			} else {
				customMatrix.setEditable(false);
				customMatrix.setBackground(ColorDistribution.veryLightGray);
			}
		}

		if (rateHetChoice != null) {
			rateHetChoice.select(charModel.getRatehetmodelIndex());
		}

		if (numRateCatField != null) {
			numRateCatField.getTextField().setText("" + charModel.getNumratecats());
		}

		if (invarSitesChoice != null) {
			invarSitesChoice.select(charModel.getInvariantsitesIndex());
		}
	}

	/*.................................................................................................................*/
	private void processCharacterModels() {
		int choiceValue = 0;
		GarliCharModel charModel = null;
		switch (partitionScheme) {
		case partitionByCharacterGroups:
			if (currentPartitionSubset >= 0 && currentPartitionSubset < charGroupModels.length) {
				charModel = charGroupModels[currentPartitionSubset];
			}
			break;
		case partitionByCodonPosition:
			if (currentPartitionSubset >= 0 && currentPartitionSubset < codonPositionModels.length) {
				charModel = codonPositionModels[currentPartitionSubset];
			}
			break;
		case noPartition:
			charModel = noPartitionModel;
			break;
		default:
			charModel = noPartitionModel;
		}

		if (rateMatrixChoice != null) {
			choiceValue = rateMatrixChoice.getSelectedIndex();
			charModel.setRatematrixIndex(choiceValue);
			if (data instanceof ProteinData){
				String val = rateMatrixChoice.getItem(choiceValue);
				charModel.setRatematrix(val);
			}
			else {

				switch (choiceValue) {
				case 0:
					charModel.setRatematrix("1rate");
					break;
				case 1:
					charModel.setRatematrix("2rate");
					break;
				case 2:
					charModel.setRatematrix("6rate");
					break;
					/*
					 * case 3 : charModel.setRatematrix("fixed"); break;
					 */
				case 3: // Custom
					charModel.setRatematrix(customMatrix.getText());
					break;
				default:
					charModel.setRatematrix("6rate");
				}
			}
		}

		if (stateFreqChoice != null) {
			choiceValue = stateFreqChoice.getSelectedIndex();
			charModel.setStatefrequenciesIndex(choiceValue);

			switch (choiceValue) {
			case 0:
				charModel.setStatefrequencies("equal");
				break;
			case 1:
				charModel.setStatefrequencies("empirical");
				break;
			case 2:
				charModel.setStatefrequencies("estimate");
				break;
			default:
				charModel.setStatefrequencies("estimate");

			}
		}

		if (rateHetChoice != null) {
			choiceValue = rateHetChoice.getSelectedIndex();
			charModel.setRatehetmodelIndex(choiceValue);
			switch (choiceValue) {
			case 0:
				charModel.setRatehetmodel("none");
				break;
			case 1:
				charModel.setRatehetmodel("gamma");
				break;
			case 2:
				charModel.setRatehetmodel("gammafixed");
				break;
			default:
				charModel.setRatehetmodel("gamma");
			}
		}

		if (numRateCatField != null) {

			numratecats = numRateCatField.getValue();
			// if (numratecats>1 && choiceValue==0)
			// numratecats=1;
			charModel.setNumratecats(numratecats);
		}

		if (invarSitesChoice != null) {
			choiceValue = invarSitesChoice.getSelectedIndex();
			charModel.setInvariantsitesIndex(choiceValue);
			switch (choiceValue) {
			case 0:
				charModel.setInvariantsites("none");
				break;
			case 1:
				charModel.setInvariantsites("estimate");
				break;
			case 2:
				charModel.setInvariantsites("fixed");
				break;
			default:
				charModel.setInvariantsites("estimate");
			}
		}
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions() {
		return "2.0";
	}
	public  boolean smallerIsBetter (){
		return false;
	}

	/*.................................................................................................................*/
	public void addRunnerOptions(ExtensibleDialog dialog) {
		externalProcRunner.addItemsToDialogPanel(dialog);
	}
	/*.................................................................................................................*/
	public void processRunnerOptions() {
		externalProcRunner.optionsChosen();
	}
	/*.................................................................................................................*/
	public  boolean showMultipleRuns() {
		return (!bootstrapOrJackknife() && numRuns>1);
	}
	/*.................................................................................................................*/
	public int minimumNumSearchReplicates() {
		return 1;
	}

	public void setConstrainedSearch(boolean constrainedSearch) {
		if (useConstraintTree==NOCONSTRAINT && constrainedSearch)
			useConstraintTree=POSITIVECONSTRAINT;  //TODO: 
		else if (useConstraintTree!=NOCONSTRAINT && !constrainedSearch)
			useConstraintTree = NOCONSTRAINT;
		this.constrainedSearch = constrainedSearch;
	}
	
	Checkbox useOptimizedScoreAsBestCheckBox =  null;
	RadioButtons SOWHConstraintButtons = null;
	public  void addItemsToSOWHDialogPanel(ExtensibleDialog dialog){
		if (SOWHConstraintTree==POSITIVECONSTRAINT)
			SOWHConstraintButtons = dialog.addRadioButtons (new String[]{"Positive constraint", "Negative constraint"}, 0);
		else if (SOWHConstraintTree==NEGATIVECONSTRAINT)
			SOWHConstraintButtons = dialog.addRadioButtons (new String[]{"Positive constraint", "Negative constraint"}, 1);

	}
	
	public boolean SOWHoptionsChosen(){
		if (SOWHConstraintButtons!=null) {
			int constraint = SOWHConstraintButtons.getValue();
			if (constraint==0)
				SOWHConstraintTree = POSITIVECONSTRAINT;
			else if (constraint==1)
				SOWHConstraintTree = NEGATIVECONSTRAINT;
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
		return sb.toString();
	}
	public String getSOWHDetailsSimulated(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for each simulated matrix: " + numRuns + "\n");
		if (SOWHConstraintTree==POSITIVECONSTRAINT)
			sb.append("Constraint type used in SOWH test: Positive constraint\n");
		else if (SOWHConstraintTree==NEGATIVECONSTRAINT)
			sb.append("Constraint type used in SOWH test: Negative constraint\n");
		return sb.toString();
	}
	public String getLogFileName(){
		return mainLogFileName;
	}

	Button setByModelNameButton;
	IntegerField numRunsField;
	IntegerField bootStrapRepsField;
	/*.................................................................................................................*/
	public abstract String queryOptionsDialogTitle();
	/*.................................................................................................................*/
	public boolean queryOptions() {
		if (!okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying Options")) // Debugg.println needs to check that options set well enough to proceed anyway

			return true;

		boolean closeWizard = false;

		if ((MesquiteTrunk.isMacOSXBeforeSnowLeopard())
				&& MesquiteDialog.currentWizard == null) {
			CommandRecord cRec = null;
			cRec = MesquiteThread.getCurrentCommandRecordDefIfNull(null);
			if (cRec != null) {
				cRec.requestEstablishWizard(true);
				closeWizard = true;
			}
		}

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		String title = queryOptionsDialogTitle();
		String extra = getExtraQueryOptionsTitle();
		if (StringUtil.notEmpty(extra))
			title += " ("+extra+")";
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), title, buttonPressed);

		// dialog.addLabel("GARLI - Options and Locations");

		String helpString = "This module will prepare a matrix for GARLI, and ask GARLI do to an analysis.  A command-line version of GARLI must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by GARLI, and, for non-bootstrap analyses, also read in the value of the GARLI score (-ln L) of the tree. "
				+ "You can see the GARLI score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose GARLI Score.";

		dialog.appendToHelpString(helpString);
		if (zephyrRunnerEmployer!=null)
			dialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());

		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();
		String extraLabel = getLabelForQueryOptions();
		if (StringUtil.notEmpty(extraLabel))
			dialog.addLabel(extraLabel);

		tabbedPanel.addPanel("GARLI Program Details", true);
		addRunnerOptions(dialog);
		dialog.addLabelSmallText("This version of Zephyr tested on the following GARLI version(s): " + getTestedProgramVersions());
		if (treeInferer!=null) 
			treeInferer.addItemsToDialogPanel(dialog);
		
		externalProcRunner.addNoteToBottomOfDialog(dialog);
		
		
		 bootStrapRepsField=null;

		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Replicates & Constraints", true);
			doBootstrapCheckbox = dialog.addCheckBox("do bootstrap analysis", doBootstrap);
			dialog.addHorizontalLine(1);
			dialog.addLabel("Bootstrap Options", Label.LEFT, false, true);
			doBootstrapCheckbox.addItemListener(this);
			bootStrapRepsField = dialog.addIntegerField("Bootstrap Reps", bootstrapreps, 8, 0, MesquiteInteger.infinite);
			dialog.addHorizontalLine(1);
		} else
			tabbedPanel.addPanel("Search Replicates", true);

		dialog.addLabel("Maximum Likelihood Tree Search Options", Label.LEFT, false, true);
		if (numRuns<minimumNumSearchReplicates())
			numRuns = minimumNumSearchReplicates();
		 numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, minimumNumSearchReplicates(), MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);

		if (getConstrainedSearchAllowed()) {
			dialog.addHorizontalLine(1);
			dialog.addLabel("Constraint tree:", Label.LEFT, false, true);
			constraintButtons = dialog.addRadioButtons (new String[]{"No Constraint", "Positive Constraint", "Negative Constraint"}, useConstraintTree);
			constraintButtons.addItemListener(this);
		}

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

		Checkbox linkModelsBox = dialog.addCheckBox("use same set of model parameters for all partition subsets",linkModels);
		Checkbox subsetSpecificRatesBox = dialog.addCheckBox("infer overall rate multipliers for each partition subset",subsetSpecificRates);

		dialog.addHorizontalLine(1);
		partitionChoice = dialog.addPopUpMenu("Edit model for this partition subset:",new String[] { "All Characters" }, 0);
		preparePartitionChoice(partitionChoice, partitionScheme);
		partitionChoice.addItemListener(this);

		if (data instanceof ProteinData)
			rateMatrixChoice = dialog.addPopUpMenu("Rate Matrix", new String[] {"poisson", "jones", "dayhoff", "wag", "mtmam", "mtrev" }, 2); 
		else{
			rateMatrixChoice = dialog.addPopUpMenu("Rate Matrix", new String[] {"Equal Rates", "2-Parameter", "GTR       ", "Custom" }, 2);  //corresponding to 1rate, 2rate, 6rate, custom
			customMatrix = dialog.addTextField("6rate", 20); // since 2 is selected

			// as default in
			// previous
			customMatrix.setEditable(false);
			customMatrix.setBackground(ColorDistribution.veryLightGray);
			setByModelNameButton = dialog.addAListenedButton("Set by Model Name...",null, this);
			setByModelNameButton.setActionCommand("setByModelName");
			stateFreqChoice = dialog.addPopUpMenu("State Frequencies", new String[] {"Equal", "Empirical", "Estimate" }, 2);  
		}
		rateMatrixChoice.addItemListener(this);
		invarSitesChoice = dialog.addPopUpMenu("Invariable Sites", new String[] {"none", "Estimate Proportion" }, 1);
		rateHetChoice = dialog.addPopUpMenu("Gamma Site-to-Site Rate Model",new String[] { "none", "Estimate Shape Parameter" }, 1);
		numRateCatField = dialog.addIntegerField("Number of Rate Categories for Gamma", numratecats, 4, 1, 20);


		tabbedPanel.addPanel("Other options", true);
		Checkbox showConfigDetailsBox = dialog.addCheckBox("show config file",
				showConfigDetails);

		tabbedPanel.cleanup();
		dialog.nullifyAddPanel();

		dialog.completeAndShowDialog(true);
		boolean acceptableOptions = false;

		if (buttonPressed.getValue() == 0) {
			boolean infererOK =  (treeInferer==null || treeInferer.optionsChosen());
			if (externalProcRunner.optionsChosen() && infererOK) {
				numRuns = numRunsField.getValue();
				if (bootstrapAllowed) {
					bootstrapreps = bootStrapRepsField.getValue();
					doBootstrap = doBootstrapCheckbox.getState();
				}
				onlyBest = onlyBestBox.getState();
				showConfigDetails = showConfigDetailsBox.getState();
				partitionScheme = charPartitionButtons.getValue();
				if (getConstrainedSearchAllowed()) {
					useConstraintTree = constraintButtons.getValue();
					if (useConstraintTree!=NOCONSTRAINT)
						setConstrainedSearch(true);
				}
				linkModels = linkModelsBox.getState();
				subsetSpecificRates = subsetSpecificRatesBox.getState();
				processRunnerOptions();

				// garliOptions = garliOptionsField.getText();

				processCharacterModels();

				storeRunnerPreferences();
				acceptableOptions=true;
			}
		}
		dialog.dispose();
		if (closeWizard)
			MesquiteDialog.closeWizard();

		return (acceptableOptions);
	}

	/*.................................................................................................................*/
	public void checkEnabled(boolean doBoot) {
		onlyBestBox.setEnabled(!doBoot);
		bootStrapRepsField.getTextField().setEnabled(doBoot);
	}

	/*.................................................................................................................*/
	public void setGarliSeed(long seed) {
		this.randseed = seed;
	}

	int count = 0;
	double finalValue = MesquiteDouble.unassigned;
	double[] finalValues = null;
	protected int runNumber = 0;

	/*.................................................................................................................*/
	public void initializeMonitoring() {
		if (finalValues == null) {
			if (bootstrapOrJackknife())
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numRuns];
			DoubleArray.deassignArray(finalValues);
		}
	}

	protected String configFileName;

	public int getCurrentRun() {
		return runNumber;
	}
	String mainLogFileName = ofprefix + ".log00.log";

	/*.................................................................................................................*/
	public String[] getLogFileNames() {
		String treeFileName;
		if (bootstrapOrJackknife())
			treeFileName = ofprefix + ".boot.tre";
		else
			treeFileName = ofprefix + ".best.tre";
		String currentTreeFilePath = ofprefix + ".best.current.tre";
		String allBestTreeFilePath = ofprefix + ".best.all.tre";
		mainLogFileName = ofprefix + ".log00.log";

		return new String[] { mainLogFileName, currentTreeFilePath,ofprefix + ".screen.log", treeFileName, allBestTreeFilePath };
	}

	/*.................................................................................................................*/
	public boolean initializeJustBeforeQueryOptions() {
		setUpCharModels(data);
		return true;
	}
	/*.................................................................................................................*/
	abstract public Object getProgramArguments(String dataFileName, String configFileName, boolean isPreflight) ;

	/* ================================================= */
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		finalValues=null;
		screenFile = null;
		screenFilePos=0;
		runNumber = 0;
		if (!initializeGetTrees(MolecularData.class, taxa, matrix))
			return null;
		//David: if isDoomed() then module is closing down; abort somehow
		setGarliSeed(seed);

		// create the data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this,MesquiteFileUtil.IN_SUPPORT_DIR, "GARLI", "-Run.");
		if (tempDir == null)
			return null;

		if (StringUtil.blank(dataFileName))
			dataFileName = "dataMatrix.nex"; // replace this with actual file name?

		String dataFilePath = tempDir + dataFileName;
		if (partitionScheme == noPartition)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, true, selectedTaxaOnly, false, false, false, false);
		else if (partitionScheme == partitionByCharacterGroups)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, true, selectedTaxaOnly, true, false, false, false);
		else if (partitionScheme == partitionByCodonPosition)
			ZephyrUtil.writeNEXUSFile(taxa, tempDir, dataFileName, dataFilePath, data, true, true, selectedTaxaOnly, true, false, true, false);

		setFileNames();

		// setting up the GARLI config file
		String config = getGARLIConfigurationFile(data);
		//		String configFilePath = tempDir+configFileName;
		//		MesquiteFile.putFileContents(configFilePath, config, true);
		if (!MesquiteThread.isScripting() && showConfigDetails) {
			config = MesquiteString.queryMultiLineString(getModuleWindow(),"GARLI Config File", "GARLI Config File", config, 30, false, true);
			if (StringUtil.blank(config))
				return null;
		}
		//getting constraint tree if requested
		String constraintTree = "";
		if (useConstraintTree>NOCONSTRAINT || isConstrainedSearch()){
			if (isConstrainedSearch() && useConstraintTree==NOCONSTRAINT)  //TODO: change  Debugg.println
				useConstraintTree=POSITIVECONSTRAINT;
			getConstraintTreeSource();
			constraint = null;
			if (constraintTreeTask != null)
				constraint = constraintTreeTask.getTree(taxa, "This will be the constraint tree");
			if (constraint == null){
				discreetAlert("Constraint tree is not available.");
				return null;
			}
			else {
				constraintTree = constraint.writeTreeSimple(MesquiteTree.BY_NUMBERS, false, false, false, false, ",");
				if (useConstraintTree == POSITIVECONSTRAINT){
					constraintTree = "+" + constraintTree+ ";";
					appendToExtraSearchDetails("\nPositive constraint using tree \"" + constraint.getName() + "\"");
					appendToAddendumToTreeBlockName("Constrained to tree \"" + constraint.getName() + "\"");
				}
				else if (useConstraintTree == NEGATIVECONSTRAINT){
					constraintTree = "-" + constraintTree+ ";";
					appendToExtraSearchDetails("\nNegative constraint using tree \"" + constraint.getName() + "\"");
					appendToAddendumToTreeBlockName("Constrained to oppose tree \"" + constraint.getName() + "\"");
				}

			}
		}
		setRootNameForDirectoryInProcRunner();


		// setting up the arrays of input file names and contents
		int numInputFiles = 4;
		String[] fileContents = new String[numInputFiles];
		String[] fileNames = new String[numInputFiles];
		for (int i = 0; i < numInputFiles; i++) {
			fileContents[i] = "";
			fileNames[i] = "";
		}
		fileContents[DATAFILENUMBER] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[DATAFILENUMBER] = dataFileName;
		if (isConstrainedSearch() && StringUtil.notEmpty(constraintTree)) {
			fileContents[CONSTRAINTFILENUMBER] = constraintTree;
			fileNames[CONSTRAINTFILENUMBER] = "constraintTree";
		}
		fileContents[CONFIGFILENUMBER] = config;
		fileNames[CONFIGFILENUMBER] = configFileName;
		fileContents[3] = getRunInformation();
		fileNames[3] = IOUtil.runInformationFileName;

		String GARLIcommand = externalProcRunner.getExecutableCommand();
		Object arguments = getProgramArguments(dataFileName, configFileName, false);
		completedRuns = new boolean[numRuns];
		for (int i=0; i<numRuns; i++) completedRuns[i]=false;


		if (updateWindow)
			parametersChanged(); //just a way to ping the coordinator to update the window
		
		boolean success = runProgramOnExternalProcess(GARLIcommand, arguments, fileContents, fileNames, ownerModule.getName());

		if (!isDoomed()){
			if (success) {
				desuppressProjectPanelReset();
				return retrieveTreeBlock(trees, finalScore); // here's where we actually process everything
			}
		}

		desuppressProjectPanelReset();
		if (data != null)
			data.decrementEditInhibition();
		externalProcRunner.finalCleanup();
		return null;
	}
	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore) {
		if (isVerbose())
			logln("Preparing to receive GARLI trees.");
		boolean success = false;
		taxa = treeList.getTaxa();
		finalScore.setValue(finalValue);

		suppressProjectPanelReset();

		FileCoordinator coord = getFileCoordinator();
		MesquiteFile tempDataFile = null;
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

		// read in the tree files
		TreesManager manager = (TreesManager) findElementManager(TreeVector.class);
		int oldnumTB = manager.getNumberTreeBlocks(taxa);
		if (onlyBest || numRuns == 1 || bootstrapOrJackknife())
			tempDataFile = (MesquiteFile) coord.doCommand("includeTreeFile", StringUtil.tokenize(outputFilePaths[TREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); // TODO: never scripting???
		else
			tempDataFile = (MesquiteFile) coord.doCommand("includeTreeFile",StringUtil.tokenize(outputFilePaths[BESTTREEFILE]) + " " + StringUtil.tokenize("#InterpretNEXUS") + " suppressImportFileSave useStandardizedTaxonNames taxa = " + coord.getProject().getTaxaReference(taxa), CommandChecker.defaultChecker); // TODO: never scripting???

		runFilesAvailable();

		MesquiteThread.setCurrentCommandRecord(oldCR);

		Tree t = null;
		int numTB = manager.getNumberTreeBlocks(taxa);
		if (numTB> oldnumTB){
			TreeVector tv = manager.getTreeBlock(taxa, numTB - 1);
			if (tv != null) {
				t = tv.getTree(0);
				ZephyrUtil.adjustTree(t, outgroupSet);

				if (t != null)
					success = true;

				if (treeList != null) {
					double bestScore = MesquiteDouble.unassigned;
					for (int i = 0; i < tv.getNumberOfTrees(); i++) {
						Tree newTree = tv.getTree(i);
						ZephyrUtil.adjustTree(newTree, outgroupSet);

						if (finalValues != null && i < finalValues.length
								&& MesquiteDouble.isCombinable(finalValues[i])) {
							MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
							s.setName(GarliRunner.SCORENAME);
							((Attachable) newTree).attachIfUniqueName(s);
						}

						treeList.addElement(newTree, false);

						if (finalValues != null && i < finalValues.length
								&& MesquiteDouble.isCombinable(finalValues[i]))
							if (MesquiteDouble.isUnassigned(bestScore))
								bestScore = finalValues[i]; // Debugg.println must refind final values, best score
							else if (bestScore < finalValues[i])
								bestScore = finalValues[i];
					}
					logln("Best score: " + bestScore);
					finalScore.setValue(bestScore);

				}
			}
			manager.deleteElement(tv); // get rid of temporary tree block
		}
		// int numTB = manager.getNumberTreeBlocks(taxa);

		desuppressProjectPanelReset();
		if (tempDataFile != null)
			tempDataFile.close();
		// deleteSupportDirectory();
		externalProcRunner.finalCleanup();
		finalValues=null;
		if (data != null)
			data.decrementEditInhibition();
		if (success) {
			if (!beanWritten)
				postBean("successful");
			beanWritten = true;
			return t;
		}
		if (!beanWritten)
			postBean("failed, retrieveTreeBlock");
		beanWritten=true;
		return null;
	}

	/*.................................................................................................................*/

	int numRunsCompleted = 0;
	long screenFilePos = 0;
	MesquiteFile screenFile = null;
	/*.................................................................................................................*/
	public boolean mpiVersion() {
		return false;
	}

	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {
		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted())) {
			setUserAborted(true);
			return;
		}
		if (logFileNames == null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath = outputFilePaths[fileNum];

		/*
		 * if (fileNum==1) filePath =
		 * getOutputFileToReadPath(outputFilePaths[fileNum]); else filePath =
		 * outputFilePaths[fileNum];
		 */

		if (fileNum == MAINLOGFILE && outputFilePaths.length > 0 && !StringUtil.blank(outputFilePaths[MAINLOGFILE]) && !bootstrapOrJackknife()) { // screen log
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath);
				if (!StringUtil.blank(s))
					if (progIndicator != null) {
						parser.setString(s);
						String gen = parser.getFirstToken(); // generation
						// number
						progIndicator.setText("Generation: " + gen + ", ln L = " + parser.getNextToken());
						progIndicator.spin();
					}
				count++;
			} else if (MesquiteTrunk.debugMode)
				MesquiteMessage.warnProgrammer("*** File does not exist (" + filePath + ") ***");
		}

		if (fileNum == CURRENTTREEFILEPATH && outputFilePaths.length > 1 && !StringUtil.blank(outputFilePaths[CURRENTTREEFILEPATH]) && !bootstrapOrJackknife()) {
			if (ownerModule instanceof NewTreeProcessor){ 
				String treeFilePath = filePath;
				if (taxa != null) {
					TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);
				} else
					((NewTreeProcessor)ownerModule).newTreeAvailable(treeFilePath, null);
			}
		}

		if (fileNum == SCREENLOG && outputFilePaths.length > 2 && !StringUtil.blank(outputFilePaths[SCREENLOG])) {
			if (screenFile == null) { // this is the output file
				if (MesquiteFile.fileExists(filePath)) {
					screenFile = MesquiteFile.open(true, filePath);
				}
				else if (MesquiteTrunk.debugMode)
					MesquiteMessage.warnProgrammer("*** File does not exist (" + filePath + ") ***");
			} 
			if (screenFile != null && MesquiteFile.fileExists(filePath)) {
				screenFile.openReading();
				if (!MesquiteLong.isCombinable(screenFilePos))
					screenFilePos = 0;
				screenFile.goToFilePosition(screenFilePos);
				boolean errorFound=false;
				String s = screenFile.readLine();
				while (s != null && !errorFound) { // &&
					// screenFile.getFilePosition()<screenFile.existingLength()-2)
					// {
					if (s.startsWith("Final score")) {
						parser.setString(s);
						String s1 = parser.getFirstToken(); // Final
						s1 = parser.getNextToken(); // score
						s1 = parser.getNextToken(); // =
						s1 = parser.getNextToken(); // number
						if (finalValues != null && runNumber < finalValues.length)
							finalValues[runNumber] = MesquiteDouble.fromString(s1);
						if (bootstrapOrJackknife())
							logln("GARLI bootstrap replicate " + (runNumber+1) + " of " + getTotalReps() + " completed, ln L = " + s1);
						else {
							logln("GARLI search replicate " + (runNumber+1) + " of " + getTotalReps() + " completed, ln L = " + s1);
							if (completedRuns != null && runNumber>=0 && runNumber<completedRuns.length)
								completedRuns[runNumber]=true;
						}
						runNumber++;
						numRunsCompleted++;
						double timePerRep = timer.timeSinceVeryStartInSeconds()/ numRunsCompleted; // this is time per rep
						int timeLeft = 0;
						if (bootstrapOrJackknife()) {
							timeLeft = (int) ((bootstrapreps - numRunsCompleted) * timePerRep);
						} else {
							timeLeft = (int) ((numRuns - numRunsCompleted) * timePerRep);
						}
						logln("  Running time so far " + StringUtil.secondsToHHMMSS((int) timer.timeSinceVeryStartInSeconds()) + ", approximate time remaining "+ StringUtil.secondsToHHMMSS(timeLeft));

					} else if (s.startsWith("ERROR:")) {
						MesquiteMessage.discreetNotifyUser("GARLI " + s);
						errorFound=true;
					}
					s = screenFile.readLine();
					count++;
				}

				screenFilePos = screenFile.getFilePosition();
				screenFile.closeReading();
				if (completedRuns != null && !bootstrapOrJackknife()) {  // let's see what the earliest non-completed run is
					for (int i=0; i<completedRuns.length; i++)
						if (!completedRuns[i]) {
							currentRun=i;
							break;
						}
				}

			}
		}

	}

	/*.................................................................................................................*/
	public boolean bootstrapOrJackknife() {
		if (!bootstrapAllowed)
			return false;
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

	/*.................................................................................................................*/
	public void setOfPrefix(String ofprefix) {
		this.ofprefix = ofprefix;
	}

	public Class getDutyClass() {
		return GarliRunner.class;
	}

	public String getName() {
		return "GARLI Runner";
	}

	/*.................................................................................................................*/
	public int getNumRuns() {
		return numRuns;
	}

	/*.................................................................................................................*/
	public int getTotalReps() {
		if (bootstrapOrJackknife())
			return getBootstrapreps();
		else
			return numRuns;
	}

	/*.................................................................................................................*/
	public boolean getOnlyBest() {
		return onlyBest;
	}

	/*.................................................................................................................*/
	protected OneTreeSource constraintTreeTask = null;
	protected OneTreeSource getConstraintTreeSource(){
		if (constraintTreeTask == null){
			constraintTreeTask = (OneTreeSource)hireEmployee(OneTreeSource.class, "Source of constraint tree");
		}
		return constraintTreeTask;
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("setByModelName")) {
			MesquiteString name = new MesquiteString();
			QueryDialogs.queryString(containerOfModule(), "Set by Model Name", "Model Name:", name);
			if (!name.isBlank()) {
				String modelName = name.getValue();
				 if (modelName.equalsIgnoreCase("JC")) {
					rateMatrixChoice.select("Equal Rates");
					customMatrix.setText("1rate");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("F81")) {
					rateMatrixChoice.select("Equal Rates");
					customMatrix.setText("1rate");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("K80")) {
					rateMatrixChoice.select("2-Parameter");
					customMatrix.setText("2rate");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("HKY")) {
					rateMatrixChoice.select("2-Parameter");
					customMatrix.setText("2rate");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TRNef") || modelName.equalsIgnoreCase("TRNe")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 0 2 0)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TrN")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 0 2 0)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TPM1")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 2 1 0)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TPM1uf") || modelName.equalsIgnoreCase("TPM1u")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 2 1 0)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TPM2")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 2 1 2)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TPM2uf") || modelName.equalsIgnoreCase("TPM2u")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 2 1 2)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TPM3")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 0 1 2)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TPM3uf") || modelName.equalsIgnoreCase("TPM3u")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 0 1 2)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TIM1ef") || modelName.equalsIgnoreCase("TIM1e")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 2 3 0)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TIM1")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 2 3 0)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TIM2ef") || modelName.equalsIgnoreCase("TIM2e")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 2 3 2)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TIM2")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 0 2 3 2)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TIM3ef") || modelName.equalsIgnoreCase("TIM3e")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 0 3 2)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TIM3")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 0 3 2)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("TVMef") || modelName.equalsIgnoreCase("TVMe")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 3 1 4)");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("TVM")) {
					rateMatrixChoice.select("Custom");
					customMatrix.setText("(0 1 2 3 1 4)");
					stateFreqChoice.select("Estimate");
				} else if (modelName.equalsIgnoreCase("SYM")) {
					rateMatrixChoice.select("GTR");
					customMatrix.setText("6rate");
					stateFreqChoice.select("Equal");
				} else if (modelName.equalsIgnoreCase("GTR")) {
					rateMatrixChoice.select("GTR");
					customMatrix.setText("6rate");
					stateFreqChoice.select("Estimate");
				} 

				if ("Custom".equalsIgnoreCase(rateMatrixChoice.getSelectedItem())) {
					customMatrix.setEditable(true);
					customMatrix.setBackground(Color.white);
				} else {
					customMatrix.setEditable(false);
					customMatrix.setBackground(ColorDistribution.veryLightGray);
				}

			}
		}
	}
	/*.................................................................................................................*/
	public void itemStateChanged(ItemEvent e) {
		if (charPartitionButtons.isAButton(e.getItemSelectable())) { // button for the partition scheme
			processCharacterModels();
			if (charPartitionButtons != null)
				partitionScheme = charPartitionButtons.getValue();
			if (partitionChoice != null)
				preparePartitionChoice(partitionChoice, partitionScheme);
		} else if (e.getItemSelectable() == partitionChoice) { // popup for which partition to edit
			processCharacterModels();
			if (partitionScheme == partitionByCodonPosition) {
				if (codpos1Subset.equalsIgnoreCase((String) e.getItem())) {
					currentPartitionSubset = 0;
					setCharacterModels();
				} else if (codpos2Subset.equalsIgnoreCase((String) e.getItem())) {
					currentPartitionSubset = 1;
					setCharacterModels();
				} else if (codpos3Subset.equalsIgnoreCase((String) e.getItem())) {
					currentPartitionSubset = 2;
					setCharacterModels();
				} else if (nonCodingSubset.equalsIgnoreCase((String) e
						.getItem())) {
					currentPartitionSubset = 3;
					setCharacterModels();
				}
			} else if (partitionScheme == partitionByCharacterGroups) {
				currentPartitionSubset = ZephyrUtil.getPartitionSubset(data, (String) e.getItem());
				setCharacterModels();
			} else
				setCharacterModels();

		} 
		else if (e.getItemSelectable() == constraintButtons && constraintButtons.getValue()>0){

			getConstraintTreeSource();

		}
		else if (e.getItemSelectable() == rateMatrixChoice) {
			if (data instanceof ProteinData){
			}
			else {
				String matrix = "";
				int choiceValue = rateMatrixChoice.getSelectedIndex();
				switch (choiceValue) {
				case 0:
					matrix = "1rate";
					break;
				case 1:
					matrix = "2rate";
					break;
				case 2:
					matrix = "6rate";
					break;
				case 3: // Custom
					if (customMatrix != null){
						matrix = customMatrix.getText();
						if (matrix == null || "1rate 2rate 6rate".indexOf(matrix) >= 0) // Debugg.println keep previous custom matrices remembered for users who switch back to them?

							matrix = "(a a a a a a)";
					}
					break;
				default:
					matrix = "6rate";
				}
				if (customMatrix != null){
					customMatrix.setText(matrix);
					if (choiceValue == 3) {
						customMatrix.setEditable(true);
						customMatrix.setBackground(Color.white);
					} else {
						customMatrix.setEditable(false);
						customMatrix.setBackground(ColorDistribution.veryLightGray);
					}
				}
			}
		} else if (e.getItemSelectable() == doBootstrapCheckbox) {
			checkEnabled(doBootstrapCheckbox.getState());
		}

	}

	public String getProgramName() {
		return "GARLI";
	}

	public String getExecutableName() {
		return "GARLI";
	}
	public String getConstraintTreeName() {
		if (constraint==null)
			return null;
		return constraint.getName();
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

}
/*.................................................................................................................*/
/*.................................................................................................................*/

class GarliCharModel {
	String ratematrix = "6rate";
	String statefrequencies = "estimate";
	String ratehetmodel = "gamma";
	int numratecats = 4;
	String invariantsites = "estimate";
	boolean ip = false;
	int ratematrixIndex = 2;
	int statefrequenciesIndex = 2;
	int ratehetmodelIndex = 1;
	int invariantsitesIndex = 1;

	public GarliCharModel(boolean protein){
		ip = protein;
		if (ip)
			ratematrix = "dayhoff";
	}
	public boolean isProtein() {
		return ip;
	}


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

	public void toStringBuffer(StringBuffer sb, int modelNumber) {
		sb.append("\n");
		if (modelNumber >= 0)
			sb.append("\n[model" + modelNumber + "]");

		if (ip){
			sb.append("\ndatatype = aminoacid");
		}

		sb.append("\nratematrix = " + ratematrix);
		sb.append("\nstatefrequencies = " + statefrequencies);

		sb.append("\nratehetmodel = " + ratehetmodel);
		if (numratecats > 1 && "none".equalsIgnoreCase(ratehetmodel))
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
	


	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}
	

}
