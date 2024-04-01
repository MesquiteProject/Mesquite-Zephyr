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

import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.Bits;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.io.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public abstract class IQTreeRunner extends ZephyrRunner  implements ActionListener, ItemListener, ExternalProcessRequester, ConstrainedSearcherTreeScoreProvider  {

	boolean onlyBest = true;

	protected	int randomIntSeed = (int)System.currentTimeMillis();  
	protected long  randseed = -1;

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

	protected int numUFBootRuns = 1;
	protected int numSearchRuns = 1;
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
	protected boolean useConstraintTree = false;
	protected static int minUFBootstrapReps=1000;
	protected static String CONSTRAINTTREEFILENAME =  "constraintTree.tre";

	protected static final int STANDARDBOOTSTRAP = 0;
	protected static final int ULTRAFASTBOOTSTRAP = 1;
	protected static final int STANDARDSEARCH = 2;
	protected int searchStyle = STANDARDSEARCH;
	protected RadioButtons searchStyleButtons = null;
	
	protected boolean doALRT = false;
	protected int alrtReps=1000;


	protected RadioButtons charPartitionButtons = null;

	protected boolean importBestPartitionScheme = true;


	long summaryFilePosition =0;



	//static String constraintfile = "none";

	protected  SingleLineTextField substitutionModelField, otherOptionsField;

	IntegerField seedField;
	protected javax.swing.JLabel commandLabel;
	protected SingleLineTextArea commandField;
	protected IntegerField numSearchRunsField,numUFBootRunsField, bootStrapRepsField, alrtRepsField;
	protected Checkbox onlyBestBox, retainFilescheckBox, useConstraintTreeCheckbox, importBestPartitionSchemeCheckbox, alrtBox;
	//	int count=0;

	protected double finalValue = MesquiteDouble.unassigned;
	protected double optimizedValue = MesquiteDouble.unassigned;
	protected double[] finalValues = null;
//	protected double[] optimizedValues = null;
	protected int runNumber = 0;



	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		if (randomIntSeed<0)
			randomIntSeed = -randomIntSeed;
		if (!hireExternalProcessRunner()){
			return sorry("Couldn't hire an external process runner");
		}
		externalProcRunner.setProcessRequester(this);
		setUpRunner();

		return true;
	}
	/*.................................................................................................................*/
	public void setUpRunner() { 
		
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
			
		}
		else
			return super.doCommand(commandName, arguments, checker);
	}	
	public void reconnectToRequester(MesquiteCommand command, MesquiteBoolean runSucceeded){
		continueMonitoring(command,  runSucceeded);
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
			numSearchRuns = MesquiteInteger.fromString(content);
		if ("numUFBootRuns".equalsIgnoreCase(tag))
			numUFBootRuns = MesquiteInteger.fromString(content);
		if ("partitionScheme".equalsIgnoreCase(tag))
			partitionScheme = MesquiteInteger.fromString(content);
		if ("partitionLinkage".equalsIgnoreCase(tag))
			partitionLinkage = MesquiteInteger.fromString(content);		
		
		
		

		if ("bootStrapReps".equalsIgnoreCase(tag)){
			bootstrapreps = MesquiteInteger.fromString(content);
			if (bootstrapreps<1) bootstrapreps=1;
		}
		if ("alrtReps".equalsIgnoreCase(tag)){
			alrtReps = MesquiteInteger.fromString(content);
			if (alrtReps<1) alrtReps=1;
		}
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);
		if ("doALRT".equalsIgnoreCase(tag))
			doALRT = MesquiteBoolean.fromTrueFalseString(content);
		if ("modelOption".equalsIgnoreCase(tag))
			modelOption = MesquiteInteger.fromString(content);
		if ("searchStyle".equalsIgnoreCase(tag))
			searchStyle = MesquiteInteger.fromString(content);
		if ("substitutionModel".equalsIgnoreCase(tag))
			substitutionModel = StringUtil.cleanXMLEscapeCharacters(content);
		if ("importBestPartitionScheme".equalsIgnoreCase(tag))
			importBestPartitionScheme = MesquiteBoolean.fromTrueFalseString(content);


		preferencesSet = true;
	}

	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numSearchRuns);  
		StringUtil.appendXMLTag(buffer, 2, "numUFBootRuns", numUFBootRuns);  
		StringUtil.appendXMLTag(buffer, 2, "partitionScheme", partitionScheme);  
		StringUtil.appendXMLTag(buffer, 2, "partitionLinkage", partitionLinkage);  
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);  
		StringUtil.appendXMLTag(buffer, 2, "doALRT", doALRT);  
		StringUtil.appendXMLTag(buffer, 2, "alrtReps", alrtReps);  
		StringUtil.appendXMLTag(buffer, 2, "searchStyle", searchStyle);  
		StringUtil.appendXMLTag(buffer, 2, "modelOption", modelOption);  
		StringUtil.appendXMLTag(buffer, 2, "substitutionModel", substitutionModel);  
		StringUtil.appendXMLTag(buffer, 2, "importBestPartitionScheme", importBestPartitionScheme);  

		preferencesSet = true;
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public String searchStyleName(int searchStyle) {
		switch (searchStyle) {
		case STANDARDBOOTSTRAP:
			return "standardBootstrap";
		case ULTRAFASTBOOTSTRAP:
			return "ultrafastBootstrap";
		case STANDARDSEARCH:
			return "standardSearch";
		default:
			return"";
		}
	}
	/*.................................................................................................................*/
	public int getSearchStyleFromName(String searchName) {
		if (StringUtil.blank(searchName))
				return STANDARDSEARCH;
		if (searchName.equalsIgnoreCase("standardBootstrap"))
			return STANDARDBOOTSTRAP;
		if (searchName.equalsIgnoreCase("ultrafastBootstrap"))
			return ULTRAFASTBOOTSTRAP;
		if (searchName.equalsIgnoreCase("standardSearch"))
			return STANDARDSEARCH;			
		return STANDARDSEARCH;
	}
	

	/*.................................................................................................................*/
	public String getNameRefForAssocStrings() {
		if (searchStyle == ULTRAFASTBOOTSTRAP) {
			if (doALRT) 
				return "'"+IQTreeALRTUFBoot.getName()+"'";
			else
				return "'"+IQTreeUFBoot.getName()+"'";
		}
		return null;
	}
	/*.................................................................................................................*/
	public boolean showAssocStrings() {
		return true;
	}

	/*.................................................................................................................*/
	public  String bootstrapOrJackknifeTreeListName() {
		if (searchStyle==ULTRAFASTBOOTSTRAP)
			return "UF Bootstrap Tree";
		else
			return "Bootstrap Trees";
	}

	public String getResamplingKindName() {
		if (searchStyle==ULTRAFASTBOOTSTRAP)
			return "UF Bootstrap";
		else
			return "Bootstrap";
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
		if (searchStyle==STANDARDBOOTSTRAP){
			appendToSearchDetails("   Standard bootstrap analysis\n");
			appendToSearchDetails("   "+bootstrapreps + " bootstrap replicates");
		} else if (searchStyle==ULTRAFASTBOOTSTRAP){
			appendToSearchDetails("   Ultrafast bootstrap analysis\n");
			appendToSearchDetails("       "+bootstrapreps + " bootstrap replicates per run\n");
			appendToSearchDetails("       "+numUFBootRuns + " run");
			if (numUFBootRuns>1)
				appendToSearchDetails("s");
			if (doALRT)
				appendToSearchDetails("\n       SH-aLRT run with " + alrtReps + " replicates");
		} else {
			appendToSearchDetails("   Search for maximum-likelihood tree\n");
			appendToSearchDetails("   "+numSearchRuns + " search replicate");
			if (numSearchRuns>1)
				appendToSearchDetails("s");
		}
	}

	/*.................................................................................................................*/
	public String getTestedProgramVersions(){
		return "1.6.4-1.6.12, 2.2.0";
	}
	public abstract void addRunnerOptions(ExtensibleDialog dialog);
	public abstract void processRunnerOptions();
	/*.................................................................................................................*/
	public int minimumNumSearchReplicates() {
		return 1;
	}
	/*.................................................................................................................*/
	public int minimumNumBootstrapReplicates() {
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
		dialog.setHelpURL(getHelpURL(zephyrRunnerEmployer));


		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();
		String extraLabel = getLabelForQueryOptions();
		if (StringUtil.notEmpty(extraLabel))
			dialog.addLabel(extraLabel);

		tabbedPanel.addPanel(getExecutableName()+" Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);
		addRunnerOptions(dialog);
		if (treeInferer!=null) {
			treeInferer.addItemsToDialogPanel(dialog);
		}
		//Checkbox onlySetUpRunBox = dialog.addCheckBox("set up files but do not start inference", onlySetUpRun);
		externalProcRunner.addNoteToBottomOfDialog(dialog);

		if (bootstrapAllowed) {
			tabbedPanel.addPanel("Replicates", true);
			searchStyleButtons = dialog.addRadioButtons(new String[] {"standard bootstrap analysis", "ultrafast bootstrap analysis","search for ML tree"}, searchStyle);
			dialog.addHorizontalLine(1);
			dialog.addLabel("Bootstrap Options", Label.LEFT, false, true);
			searchStyleButtons.addItemListener(this);
			if (bootstrapreps< minimumNumBootstrapReplicates())
				bootstrapreps = minimumNumBootstrapReplicates();
			bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, minimumNumBootstrapReplicates(), MesquiteInteger.infinite);
			numUFBootRunsField = dialog.addIntegerField("Number of Runs for Ultrafast Bootstrap", numUFBootRuns, 8, 1, MesquiteInteger.infinite);
			alrtBox = dialog.addCheckBox("do SH-aLRT analysis", doALRT);
			alrtRepsField = dialog.addIntegerField("Number of Reps for SH-aLRT", alrtReps, 8, 1, MesquiteInteger.infinite);
			seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);
			dialog.addHorizontalLine(1);
		}
		else 
			tabbedPanel.addPanel("Replicates", true);
		dialog.addLabel("Maximum Likelihood Tree Search Options", Label.LEFT, false, true);
		if (numSearchRuns< minimumNumSearchReplicates())
			numSearchRuns = minimumNumSearchReplicates();
		numSearchRunsField = dialog.addIntegerField("Number of Search Replicates", numSearchRuns, 8, minimumNumSearchReplicates(), MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		checkEnabled(searchStyle);

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
		importBestPartitionSchemeCheckbox = dialog.addCheckBox("import best partition scheme", importBestPartitionScheme);
		checkEnablingOfImportOption(substitutionModel);


		/*		dialog.addHorizontalLine(1);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
		 */

		if (getConstrainedSearchAllowed())
			tabbedPanel.addPanel("Constraints & Other options", true);
		else
			tabbedPanel.addPanel("Other options", true);
		if (getConstrainedSearchAllowed()) {
			useConstraintTreeCheckbox = dialog.addCheckBox("use topological constraint", useConstraintTree);
			dialog.addHorizontalLine(1);
		}
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
				modelOption = modelOptionChoice.getSelectedIndex();
				substitutionModel = substitutionModelField.getText();
				partitionLinkage = partitionLinkageChoice.getSelectedIndex();
				numSearchRuns = numSearchRunsField.getValue();
				if (bootstrapAllowed) {
					searchStyle = searchStyleButtons.getValue();
					randomIntSeed = seedField.getValue();
					bootstrapreps = bootStrapRepsField.getValue();
					numUFBootRuns = numUFBootRunsField.getValue();
					doALRT = alrtBox.getState();
					alrtReps = alrtRepsField.getValue();
					if (searchStyle==ULTRAFASTBOOTSTRAP && bootstrapreps<minUFBootstrapReps) {
						bootstrapreps = minUFBootstrapReps;
						MesquiteMessage.discreetNotifyUser("Minimum number of bootstrap replicates for ultrafast bootstrap is " + minUFBootstrapReps + ". Number of replicates reset to " + minUFBootstrapReps);
					}

				} else
					searchStyle=STANDARDSEARCH;
				onlyBest = onlyBestBox.getState();

				if (getConstrainedSearchAllowed()) {
					useConstraintTree = useConstraintTreeCheckbox.getState();
					if (useConstraintTree)
						setConstrainedSearch(true);
				}
				partitionScheme = charPartitionButtons.getValue();
				importBestPartitionScheme = importBestPartitionSchemeCheckbox.getState();
				otherOptions = otherOptionsField.getText();
				processRunnerOptions();
				storeRunnerPreferences();
				acceptableOptions = true;
//				onlySetUpRun = onlySetUpRunBox.getState();
				externalProcRunner.setOnlySetUpRun(onlySetUpRun);
			}
				
		}
		dialog.dispose();
		return (acceptableOptions);
	}
	/*.................................................................................................................*/

	public void checkEnabled(int searchStyle) {
		onlyBestBox.setEnabled(searchStyle==STANDARDSEARCH || !bootstrapAllowed);
		numSearchRunsField.getTextField().setEnabled(searchStyle==STANDARDSEARCH || !bootstrapAllowed);
		if (alrtBox!=null)
			alrtBox.setEnabled(searchStyle==ULTRAFASTBOOTSTRAP);
		if (numUFBootRunsField!=null)
			numUFBootRunsField.getTextField().setEnabled(searchStyle==ULTRAFASTBOOTSTRAP);
		if (bootStrapRepsField!=null)
			bootStrapRepsField.getTextField().setEnabled(searchStyle!=STANDARDSEARCH);
		if (seedField!=null)
			seedField.getTextField().setEnabled(searchStyle!=STANDARDSEARCH);
	
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
		case 3:  //MFPOption
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
	/*.................................................................................................................*/
	public void checkEnablingOfImportOption(String modelFieldText){
		importBestPartitionSchemeCheckbox.setEnabled(expectSchemeFile(modelFieldText));
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getItemSelectable() == modelOptionChoice){
			int selected = modelOptionChoice.getSelectedIndex();
			String modelName =getModel(selected); 
			substitutionModelField.setText(modelName);
			checkEnablingOfImportOption(modelName);
			
		}
		else if (e.getItemSelectable() == useConstraintTreeCheckbox && useConstraintTreeCheckbox.getState()){

			getConstraintTreeSource();

		} else if (searchStyleButtons.isAButton(e.getItemSelectable())) {
			checkEnabled (searchStyleButtons.getValue());
			int bootreps = bootStrapRepsField.getValue();
			int searchStyleLocal = searchStyleButtons.getValue();

			if (searchStyleLocal==ULTRAFASTBOOTSTRAP && bootreps<minUFBootstrapReps) {
				bootStrapRepsField.setValue(minUFBootstrapReps);
			}

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
		sb.append("Number of search replicates for observed matrix: " + numSearchRuns);
		sb.append("\nModel of Evolution: " +substitutionModel);
		return sb.toString();
	}
	public String getSOWHDetailsSimulated(){
		StringBuffer sb = new StringBuffer();
		sb.append("Number of search replicates for each simulated matrix: " + numSearchRuns + "\n");
		sb.append("\nModel of Evolution: " +substitutionModel);
		return sb.toString();
	}

	public static final NameReference IQTreeALRTUFBoot = NameReference.getNameReference("IQ-TREE SH-aLRT/UF Boot"); 
	public static final NameReference IQTreeUFBoot = NameReference.getNameReference("IQ-TREE UFBoot"); 
	public static final NameReference IQTreeALRT = NameReference.getNameReference("IQ-TREE alrt"); 

	/*.................................................................................................................*/
	private NameReference[] getNameRefsForNodeLabels() {
		if (searchStyle==ULTRAFASTBOOTSTRAP) {
			 if (doALRT)
				 return new NameReference[] {IQTreeALRTUFBoot};
			 else
				 return new NameReference[] {IQTreeUFBoot};
			// return new NameReference[] {IQTreeALRT, IQTreeUFBoot};
		} else
			return null;
	}
	/*.................................................................................................................*/
	private Tree readTreeFile(TreeVector trees, String treeFilePath, String treeName, MesquiteBoolean success, boolean lastTree) {
		Tree t =null;
		NameReference[] nameReferences = getNameRefsForNodeLabels();
		if (lastTree) {
			String s = MesquiteFile.getFileLastContents(treeFilePath);
			t =  ZephyrUtil.readPhylipTree(s,taxa,false, namer);

			if (t!=null) {
				ZephyrUtil.reinterpretNodeLabels(t, t.getRoot(), nameReferences, true, 100.0);
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
					ZephyrUtil.reinterpretNodeLabels(t, t.getRoot(), nameReferences, true, 100.0);
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
			if (searchStyle==STANDARDBOOTSTRAP)
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numSearchRuns];
			DoubleArray.deassignArray(finalValues);
			finalValue = MesquiteDouble.unassigned;
		}
		outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		}
	}

	//protected String multipleModelFileName;

	/*.................................................................................................................*/
	public void setFileNames () {
	//	multipleModelFileName = "multipleModelFile.txt";

	}




	TaxaSelectionSet outgroupSet;

	/*.................................................................................................................*/
	public boolean preFlightSuccessful(String preflightCommand){
		return runPreflightCommand(preflightCommand);
	}

	/*.................................................................................................................*/
	public abstract Object getProgramArguments(String dataFileName, String setsFileName, boolean isPreflight, int numParts);

	protected int numPartsInStartingPartition = -1;

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
	


	protected static final int DATAFILENUMBER = 0;
	protected static final int PARTITIONFILENUMBER = 1;
	protected static final int CONSTRAINTFILENUMBER = 2;

	/*.................................................................................................................*/
	public synchronized Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		finalValues=null;
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
		MesquiteInteger numParts = new MesquiteInteger(1);

		if (partitionScheme == partitionByCharacterGroups) {
			ZephyrUtil.writeNEXUSSetsBlock(taxa, tempDir, setsFileName, setsFilePath, data,  false,  false, false, numParts);
		}
		else if (partitionScheme == partitionByCodonPosition) {
			ZephyrUtil.writeNEXUSSetsBlock(taxa, tempDir, setsFileName, setsFilePath, data,  true,  false, false, numParts);
		}


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


		Object arguments = getProgramArguments(dataFileName, setsFileName, false, numParts.getValue());
		Object preflightArguments = getProgramArguments(dataFileName, setsFileName, true,numParts.getValue());

		//	String preflightCommand = externalProcRunner.getExecutableCommand()+" --flag-check " + ((MesquiteString)preflightArguments).getValue();
		String programCommand = externalProcRunner.getExecutableCommand();
		//programCommand += StringUtil.lineEnding();  
 
		//	if (preFlightSuccessful(preflightCommand)) {
		//	}
		
		if (StringUtil.blank(programCommand)) {
			MesquiteMessage.discreetNotifyUser("Path to IQ-TREE not specified!");
			return null;
		}
		

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
		fileContents[DATAFILENUMBER] = MesquiteFile.getFileContentsAsString(dataFilePath);
		fileNames[DATAFILENUMBER] = dataFileName;
		if (partitionScheme != noPartition) {
			fileContents[PARTITIONFILENUMBER] = MesquiteFile.getFileContentsAsString(setsFilePath);
			fileNames[PARTITIONFILENUMBER] = setsFileName;
		}
		//fileContents[2] = translationTable;
		//fileNames[2] = translationFileName;
		fileContents[CONSTRAINTFILENUMBER] = constraintTree;
		fileNames[CONSTRAINTFILENUMBER] = CONSTRAINTTREEFILENAME;
		fileContents[3] = getRunInformation(arguments);
		fileNames[3] = runInformationFileName;
		int runInformationFileNumber = 3;

		switch (searchStyle) {
		case STANDARDSEARCH: 
			numRuns=numSearchRuns;
			break;
		case STANDARDBOOTSTRAP: 
			numRuns=1;
			break;
		case ULTRAFASTBOOTSTRAP: 
			numRuns=numUFBootRuns;
			break;
		}

		numRunsCompleted = 0;
		completedRuns = new boolean[numRuns];
		for (int i=0; i<numRuns; i++) completedRuns[i]=false;
		summaryFilePosition=0;

		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, fileContents, fileNames,  ownerModule.getName(), runInformationFileNumber);

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
		externalProcRunner.setLeaveAnalysisDirectoryIntact(true);  // we don't want to delete the directory here
		externalProcRunner.finalCleanup();
		cleanupAfterSearch();
		return null;

	}	




	public  boolean showMultipleRuns() {
		return (numRuns>1);
	}


	/*.................................................................................................................*/


	public boolean bootstrapOrJackknife() {
		return searchStyle==STANDARDBOOTSTRAP || searchStyle==ULTRAFASTBOOTSTRAP;
	}
	public  boolean doMajRuleConsensusOfResults(){
		return searchStyle==STANDARDBOOTSTRAP;
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
		return -2100;  
	}

	public String getName() {
		return "IQ-TREE Runner";
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
	private CharactersGroup makeGroup(String name, mesquite.lib.characters.CharacterData data, MesquiteFile file){
		CharactersGroupVector groups = (CharactersGroupVector)data.getProject().getFileElement(CharactersGroupVector.class, 0);
		String groupName = name;
		int count = 1;
		CharactersGroup group= groups.findGroup(groupName);  
		while (group!=null) {  // group is not null; therefore this group name already exists.  Have to make a new one because of the way IQTree gives names different groups with the same name.
										// first step is to find an available name
			count++;
			groupName = name + "_" + count;
			group= groups.findGroup(groupName);  
		}
		group = new CharactersGroup();
		group.setName(groupName);
		group.addToFile(file, getProject(), null);
		if (groups.indexOf(group)<0) 
			groups.addElement(group, false);
		
		return group;
	}

	public  void processSpecSet (String command, String firstToken, Object obj, MesquiteInteger startCharT, boolean hasSpecificationTokens, Bits[] bitsArray, String[] charSetNames) {

		String token = firstToken;   // this will be the name of the first part
		Object specification = null;
		if (!(obj instanceof Bits))
			specification = makeGroup(token, data, getProject().getHomeFile());
		String whitespaceString = Parser.defaultWhitespace;
		String punctuationString = "(){}:,;-<>=\\*/\''\"[]";  // took + out of it


		int lastChar = -1;
		boolean join = false;
		boolean nextIsCharList = !hasSpecificationTokens;
		//Bits bits = new Bits(numChars);
		while (token !=null && !token.equals(";") && token.length()>0) {
			if (token.equals("-")) {
				if (lastChar!=-1)
					join = true;
			}
			else {
				if (token != null && token.equals("."))
					token = Integer.toString(data.getNumChars());
				if (token.startsWith("-")) {
					if (lastChar!=-1)
						join = true;
					token = token.substring(1, token.length());
				}
				if (token.equals(":")) {
					nextIsCharList = true;
				}
				else if (token.equals(","))
					nextIsCharList=false;
				else if (nextIsCharList) {
					int whichChar = CharacterStates.toInternal(MesquiteInteger.fromString(token, false));
					if (MesquiteInteger.isCombinable(whichChar) && whichChar>=0) {
						if (whichChar>= data.getNumChars())
							whichChar = data.getNumChars()-1;

						if (join) {
							int skip = 1;
							//check here if next char is "\"; if so then need to skip
							int temp = startCharT.getValue();
							token = ParseUtil.getToken(command, startCharT, whitespaceString, punctuationString); 
							if (token!=null && token.equals("\\")){
								token = ParseUtil.getToken(command, startCharT, whitespaceString, punctuationString); 
								int tSkip = MesquiteInteger.fromString(token, false);
								if (MesquiteInteger.isCombinable(tSkip))
									skip = tSkip;
							}
							else
								startCharT.setValue(temp);
							for (int j = lastChar; j<=whichChar; j += skip) {
								if (obj instanceof Bits)
									((Bits)obj).setBit(j, true);
								else
									((CharacterPartition)obj).setProperty(specification,j);
							}
							join = false;
							lastChar = -1;
						}
						else {
							lastChar = whichChar;
							if (obj instanceof Bits)
								((Bits)obj).setBit(whichChar, true);
							else
								((CharacterPartition)obj).setProperty(specification,whichChar);
						}
					} else {
						if (bitsArray!=null && charSetNames!=null) {
							Bits bitsSet = null;
							for (int i=0; i<charSetNames.length && i<bitsArray.length; i++)
								if (token.equalsIgnoreCase(charSetNames[i])) { // found charset bits
									bitsSet = bitsArray[i];
									break;
								}
							if (bitsSet!=null) {
								for (whichChar = 0; whichChar<data.getNumChars(); whichChar++) {
									if (bitsSet.isBitOn(whichChar))
										if (obj instanceof Bits)
											((Bits)obj).setBit(whichChar, true);
										else
											((CharacterPartition)obj).setProperty(specification,whichChar);
								}
							}
						}
					}
				}
				else {
					specification =  makeGroup(token, data, getProject().getHomeFile());
					nextIsCharList = true;
				}
			}
			token = ParseUtil.getToken(command, startCharT, whitespaceString, punctuationString); 
		}

	}

	public  void addCharPartionFromIQTree (String command, String firstToken, MesquiteInteger startCharT, boolean hasSpecificationTokens, Bits[] bitsArray, String[] charSetNames) {
		CharacterPartition characterPartition= new CharacterPartition("IQ-TREE "+ StringUtil.getDateDayOnly()+"."+partitionNumber, data.getNumChars(), null, data);
		partitionNumber++;
		characterPartition.setNexusBlockStored("SETS");
		processSpecSet (command,  firstToken, characterPartition,  startCharT,  hasSpecificationTokens, bitsArray, charSetNames);
		characterPartition.addToFile(getProject().getHomeFile(), getProject(), null);    
		data.storeSpecsSet(characterPartition, CharacterPartition.class); 
	}

	static int partitionNumber = 1;

	void processSchemeFile(String contents) {
		int numLines = StringUtil.getNumberOfLines(contents);
		Bits[] bitsArray = new Bits[numLines];
		String[] charSetNames = new String[numLines];
		Parser parser = new Parser(contents);
		String line = parser.getRawNextDarkLine();
		Parser subparser = new Parser (line);
		String token = subparser.getFirstToken();
		String partitionName = "";
		boolean setsBlockFound = false;
		int countCharSets = -1;
		parser.setLineEndString(";");


		if ("#nexus".equalsIgnoreCase(token))
			while (StringUtil.notEmpty(line)) {
				line = line.toLowerCase();
				if (line.indexOf("begin sets")>=0)
					setsBlockFound = true;
				if (setsBlockFound) {
					token =  subparser.getFirstToken();
					if ("charset".equalsIgnoreCase(token)) {  // found a charset
						countCharSets++;
						token = subparser.getNextToken();  // name of charset.
						charSetNames[countCharSets]=token;
						token = subparser.getNextToken();  // =
						String remaining = subparser.getRemaining();  // the character list
						Parser subsubparser = new Parser(remaining);
						bitsArray[countCharSets] = new Bits(data.getNumChars());
						MesquiteInteger startCharT = new MesquiteInteger(0);
						processSpecSet(remaining,  subsubparser.getFirstToken(), bitsArray[countCharSets],  startCharT,  false, bitsArray, charSetNames);
						//fillCharSetBits(bitsArray[countCharSets], token);
					}
					if ("charpartition".equalsIgnoreCase(token)) {  // found the partition
						partitionName = subparser.getNextToken();  // name of partition (ignore).
						token = subparser.getNextToken();  // =
						String remaining = subparser.getRemaining();  // the character list
						Parser subsubparser = new Parser(remaining);
						MesquiteInteger startCharT = new MesquiteInteger(0);
						subsubparser.setPunctuationString("(){}:,;-<>=\\*/\''\"[]");  //// took + out of it
						
						addCharPartionFromIQTree (remaining, subsubparser.getFirstToken(),  startCharT,  true, bitsArray, charSetNames);
					}
				}
				line = parser.getRawNextDarkLine();
				subparser.setString(line);
			}
	}

	/*.................................................................................................................*/
	boolean expectSchemeFile(String subModel) {
		if (subModel==null)
			return false;
		return (subModel.equalsIgnoreCase("TESTMERGEONLY")||subModel.equalsIgnoreCase("TESTMERGE")||subModel.equalsIgnoreCase("MF+MERGE")||subModel.equalsIgnoreCase("MFP+MERGE"));
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
			if (numSearchRuns>1 && !onlyBest) {
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
				finalScore.setValue(finalValue);

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

		if (expectSchemeFile(substitutionModel) && importBestPartitionScheme) {
			String contents = MesquiteFile.getFileContentsAsString(outputFilePaths[OUT_SCHEMEFILE]);
			if (StringUtil.notEmpty(contents))
				processSchemeFile(contents);
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
		if (success) {
			if (!beanWritten)
				if (bootstrapOrJackknife())
					postBean("successful, bootstrap | "+externalProcRunner.getDefaultProgramLocation());
				else 
					postBean("successful, ML tree | "+externalProcRunner.getDefaultProgramLocation());
			beanWritten=true;
			return t;
		}
		reportStdError();
		if (!beanWritten)
			postBean("failed, retrieveTreeBlock | "+externalProcRunner.getDefaultProgramLocation());
		beanWritten = true;
		return null;
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

		if (fileNum==OUT_LOGFILE && outputFilePaths.length>OUT_LOGFILE && !StringUtil.blank(outputFilePaths[OUT_LOGFILE]) && !bootstrapOrJackknife()) {   // screen log
			String newFilePath = filePath;
			if (numSearchRuns>1) newFilePath+=fileNum;
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
				reportNewTreeAvailable();
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
					if (searchStyle==STANDARDBOOTSTRAP || numRuns>1) {
						numRunsCompleted=StringUtil.getNumberOfLines(s);
						currentRun=numRunsCompleted;
						if (externalProcRunner.canCalculateTimeRemaining(numRunsCompleted)) {
							double timePerRep = timer.timeSinceVeryStartInSeconds()/numRunsCompleted;   //this is time per rep
							int timeLeft = 0;
							if (searchStyle==STANDARDBOOTSTRAP) {
								logln("\n"+getExecutableName()+" bootstrap replicate " + numRunsCompleted + " of " + bootstrapreps+" completed");
								timeLeft = (int)((bootstrapreps- numRunsCompleted) * timePerRep);
							}
							else if (searchStyle==ULTRAFASTBOOTSTRAP) {
								logln("\n"+getExecutableName()+" ultrafast bootstrap run " + numRunsCompleted + " of " + numRuns+" completed");
								timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
							}
							else {
								logln("\n"+getExecutableName()+" search replicate " + numRunsCompleted + " of " + numRuns+" completed");
								timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
							}
							double timeSoFar = timer.timeSinceVeryStartInSeconds();
							if (isVerbose()){
								logln("   Run time " +  StringUtil.secondsToHHMMSS((int)timeSoFar)  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
								logln("    Average time per replicate:  " +  StringUtil.secondsToHHMMSS((int)timePerRep));
								logln("    Estimated total time:  " +  StringUtil.secondsToHHMMSS((int)(timeSoFar+timeLeft))+"\n");
							}
						} else {  // at least report the number of reps
							logln("");
							if (searchStyle==STANDARDBOOTSTRAP) 
								logln("\n"+getExecutableName()+" bootstrap replicate " + numRunsCompleted + " of " + bootstrapreps+" completed");
							else if (searchStyle==ULTRAFASTBOOTSTRAP) 
								logln("\n"+getExecutableName()+" ultrafast bootstrap run " + numRunsCompleted + " of " + numRuns+" completed");
							else 
								logln("\n"+getExecutableName()+" search replicate " + numRunsCompleted + " of " + numRuns+" completed");
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
	//protected static final int BESTTREEFILE = 4;
	protected static final int OUT_SCHEMEFILE = 4;

	/*.................................................................................................................*/
	public String[] getLogFileNames(){
		String treeFileName;
		String workingTreeFileName;
		String summaryFileName;
		String logFileName;
		String schemeFileName;
		if (bootstrapOrJackknife()) {
			if (searchStyle==ULTRAFASTBOOTSTRAP) {
				treeFileName = getOutputFilePrefix()+".treefile";
				summaryFileName = getOutputFilePrefix()+".runtrees";
			}
			else {
				treeFileName = getOutputFilePrefix()+".boottrees";
				summaryFileName = treeFileName;
			}
			workingTreeFileName= treeFileName;
		}
		else  {
			treeFileName = getOutputFilePrefix()+".treefile";
			if (numSearchRuns>1)
				workingTreeFileName = getOutputFilePrefix()+".runtrees";
			else
				workingTreeFileName = treeFileName;
			summaryFileName = workingTreeFileName;
		}
		logFileName = getOutputFilePrefix()+".log";
		schemeFileName = getOutputFilePrefix()+".best_scheme.nex";

		return new String[]{logFileName, treeFileName, summaryFileName, workingTreeFileName, schemeFileName};
	}
	/*.................................................................................................................*/
	public String getPreflightLogFileNames(){
		return getDataFileName()+".log";
	}
	public String getProgramName() {
		return "IQ-TREE";
	}




}
