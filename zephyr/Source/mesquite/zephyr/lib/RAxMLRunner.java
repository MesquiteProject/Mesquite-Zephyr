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

public abstract class RAxMLRunner extends ZephyrRunner  implements ActionListener, ItemListener, ExternalProcessRequester  {

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

	long summaryFilePosition =0;



	protected long  randseed = -1;
	static String constraintfile = "none";

	protected  SingleLineTextField dnaModelField, proteinModelField, otherOptionsField;
	IntegerField seedField;
	protected javax.swing.JLabel commandLabel;
	protected SingleLineTextArea commandField;
	protected IntegerField numRunsField, bootStrapRepsField;
	protected Checkbox onlyBestBox, retainFilescheckBox, doBootstrapCheckbox;
	RadioButtons constraintButtons;
	RadioButtons threadingRadioButtons;
	//	int count=0;

	protected double finalValue = MesquiteDouble.unassigned;
	protected double[] finalValues = null;
	protected double[] optimizedValues = null;
	protected int runNumber = 0;

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
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "RAxML Options & Locations",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		//	dialog.addLabel("RAxML - Options and Locations");
		String helpString = "This module will prepare a matrix for RAxML, and ask RAxML do to an analysis.  A command-line version of RAxML must be installed. "
				+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
				+ "Mesquite will read in the trees found by RAxML, and, for non-bootstrap analyses, also read in the value of the RAxML score (-ln L) of the tree. " 
				+ "You can see the RAxML score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
				+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose RAxML Score.";

		dialog.appendToHelpString(helpString);
		dialog.setHelpURL(zephyrRunnerEmployer.getProgramURL());


		MesquiteTabbedPanel tabbedPanel = dialog.addMesquiteTabbedPanel();


		tabbedPanel.addPanel("RAxML Program Details", true);
		externalProcRunner.addItemsToDialogPanel(dialog);
		addRunnerOptions(dialog);
		if (treeInferer!=null) 
			treeInferer.addItemsToDialogPanel(dialog);

		tabbedPanel.addPanel("Search Replicates & Bootstrap", true);
		doBootstrapCheckbox = dialog.addCheckBox("do bootstrap analysis", doBootstrap);
		dialog.addHorizontalLine(1);
		dialog.addLabel("Bootstrap Options", Label.LEFT, false, true);
		doBootstrapCheckbox.addItemListener(this);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);
		dialog.addHorizontalLine(1);
		dialog.addLabel("Maximum Likelihood Tree Search Options", Label.LEFT, false, true);
		numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		checkEnabled(doBootstrap);

		tabbedPanel.addPanel("Character Models & Constraints", true);
		dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);
		dialog.addHorizontalLine(1);
		dialog.addLabel("Constraint tree:", Label.LEFT, false, true);
		constraintButtons = dialog.addRadioButtons (new String[]{"No Constraint", "Partial Resolution", "Skeletal Constraint"}, useConstraintTree);
		constraintButtons.addItemListener(this);

		/*		dialog.addHorizontalLine(1);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
		 */

		tabbedPanel.addPanel("Other options", true);
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
				randomIntSeed = seedField.getValue();
				bootstrapreps = bootStrapRepsField.getValue();
				onlyBest = onlyBestBox.getState();
				doBootstrap = doBootstrapCheckbox.getState();
				useConstraintTree = constraintButtons.getValue();
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
	/*.................................................................................................................*/
	public void setRAxMLSeed(long seed){
		this.randseed = seed;
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

	/*
	 * 
	 * -q multipleModelFileName
	 * 
	 *
If you have a pure DNA alignment with 1,000bp from two genes gene1 (positions 1���500) and gene2 (positions 501���1,000) the information in the multiple model file should look as follows:

DNA, gene1 = 1-500 
DNA, gene2 = 501-1000

If gene1 is scattered through the alignment, e.g. positions 1���200, and 800���1,000 you specify this with: 

DNA, gene1 = 1-200, 800-1,000
DNA, gene2 = 201-799

You can also assign distinct models to the codon positions, i.e. if you want a distinct model to be esti- mated for each codon position in gene1 you can specify:

DNA, gene1codon1 = 1-500\3 
DNA, gene1codon2 = 2-500\3 
DNA, gene1codon3 = 3-500\3 
DNA, gene2 = 501-1000

If you only need a distinct model for the 3rd codon position you can write:

DNA, gene1codon1andcodon2 = 1-500\3, 2-500\3 
DNA, gene1codon3 = 3-500\3 
DNA, gene2 = 501-1000

As already mentioned, for AA data you must specify the transition matrices for each partition:

JTT, gene1 = 1-500 
WAGF, gene2 = 501-800 
WAG, gene3 = 801-1000

The AA substitution model must be the first entry in each line and must be separated by a comma from the gene name, just like the DNA token above. You can not assign different models of rate heterogeneity to different partitions, i.e., it will be either CAT, GAMMA, GAMMAI etc. for all partitions, as specified with -m.
Finally, if you have a concatenated DNA and AA alignment, with DNA data at positions 1���500 and AA data at 501-1,000 with the WAG model the partition file should look as follows:

DNA, gene1 = 1-500 
WAG, gene2 = 501-1000

	 * 
	 * */
	/*.................................................................................................................*

	private String[] getRateModels(CharactersGroup[] parts){
		if (parts==null || parts.length==0 || parts.length>20)
			return null;
		String[] rateModels = new String[parts.length];
		for (int i=0; i<rateModels.length; i++)
			rateModels[i] = "JTT";

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Protein Rate Models",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Protein Rate Models");

		SingleLineTextField[] modelFields = new SingleLineTextField[parts.length];
		for (int i=0; i<rateModels.length; i++)
			modelFields[i] = dialog.addTextField(parts[i].getName()+":", rateModels[i], 20);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			for (int i=0; i<rateModels.length; i++)
				rateModels[i] = modelFields[i].getText();
		}
		dialog.dispose();
		if (buttonPressed.getValue()==0)
			return rateModels;
		return null;
	}


	private String getMultipleModelFileString(CharacterData data, boolean partByCodPos){
		boolean writeCodPosPartition = false;
		boolean writeStandardPartition = false;
		CharactersGroup[] parts =null;
		if (data instanceof DNAData)
			writeCodPosPartition = ((DNAData)data).someCoding();
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition == null && !writeCodPosPartition)
			return null;
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
			writeStandardPartition = parts!=null;
		}

		if (!writeStandardPartition && !writeCodPosPartition) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		CharInclusionSet incl = null;

		String codPosPart = "";
		boolean molecular = (data instanceof MolecularData);
		boolean nucleotides = (data instanceof DNAData);
		boolean protein = (data instanceof ProteinData);
		String standardPart = "";
		if (writeStandardPartition) {
			if (nucleotides) {
				for (int i=0; i<parts.length; i++) {
					String q = ListableVector.getListOfMatches((Listable[])characterPartition.getProperties(), parts[i], CharacterStates.toExternal(0), true, ",");
					if (q != null) {
						if (nucleotides)
							sb.append("DNA, " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
			} else if (protein) {
				String[] rateModels = getRateModels(parts);
				if (rateModels!=null) {for (int i=0; i<parts.length; i++) {
					String q = ListableVector.getListOfMatches((Listable[])characterPartition.getProperties(), parts[i], CharacterStates.toExternal(0), true, ",");
					if (q != null && i<rateModels.length) {
						sb.append(rateModels[i]+", " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
				}
			}
		} else if (writeCodPosPartition && partByCodPos) {//TODO: never accessed because in the only call of this method, partByCodPos is false.
			//codon positions if nucleotide
			int numberCharSets = 0;
			CodonPositionsSet codSet = (CodonPositionsSet)data.getCurrentSpecsSet(CodonPositionsSet.class);
			for (int iw = 0; iw<4; iw++){
				String locs = codSet.getListOfMatches(iw);
				if (!StringUtil.blank(locs)) {
					String charSetName = "";
					if (iw==0) 
						charSetName = StringUtil.tokenize("nonCoding");
					else 
						charSetName = StringUtil.tokenize("codonPos" + iw);			
					numberCharSets++;
					sb.append( "DNA, " + charSetName + " = " +  locs + "\n");
				}
			}
			//	String codPos = ((DNAData)data).getCodonsAsNexusCharSets(numberCharSets, charSetList); // equivalent to list
		}

		return sb.toString();
	}


	/*.................................................................................................................*
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
	/*.................................................................................................................*/
	public void initializeMonitoring(){
		if (finalValues==null) {
			if (bootstrapOrJackknife())
				finalValues = new double[getBootstrapreps()];
			else
				finalValues = new double[numRuns];
			DoubleArray.deassignArray(finalValues);
		}
		if (optimizedValues==null) {
			if (bootstrapOrJackknife())
				optimizedValues = new double[getBootstrapreps()];
			else
				optimizedValues = new double[numRuns];
			DoubleArray.deassignArray(optimizedValues);
		}
		outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		}
	}

	protected String multipleModelFileName;

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
	/*.................................................................................................................*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore) {
		if (!initializeGetTrees(CategoricalData.class, taxa, matrix))
			return null;

		//RAxML setup
		setRAxMLSeed(seed);
		isProtein = data instanceof ProteinData;

		// create local version of data file
		String tempDir = MesquiteFileUtil.createDirectoryForFiles(this, MesquiteFileUtil.IN_SUPPORT_DIR, "RAxML", "-Run.");  
		Debugg.println("tempDir " + tempDir);
		if (tempDir==null)
			return null;
		String dataFileName = getDataFileName();   //replace this with actual file name?
		String translationFileName = IOUtil.translationTableFileName;   
		String dataFilePath = tempDir +  dataFileName;
		Debugg.println("dataFilePath " + dataFilePath);
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
		if (StringUtil.blank(multipleModelFileContents)) 
			multipleModelFileName=null;

		String constraintTree = "";
		
		if (useConstraintTree>NOCONSTRAINT){
			getConstraintTreeSource();
			Tree constraint = null;
			if (constraintTreeTask != null)
				constraint = constraintTreeTask.getTree(taxa);
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
					return null;
			}
			}
		}
		//now establish the commands for invoking RAxML
		Object arguments = getProgramArguments(dataFileName, false);
		Object preflightArguments = getProgramArguments(dataFileName, true);

		//	String preflightCommand = externalProcRunner.getExecutableCommand()+" --flag-check " + ((MesquiteString)preflightArguments).getValue();
		String programCommand = externalProcRunner.getExecutableCommand();
		//programCommand += StringUtil.lineEnding();  

		//	if (preFlightSuccessful(preflightCommand)) {
		//	}
		
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
		fileNames[3] = "constraintTree.tre";

		numRunsCompleted = 0;
		completedRuns = new boolean[numRuns];
		for (int i=0; i<numRuns; i++) completedRuns[i]=false;
		summaryFilePosition=0;

		//----------//
		boolean success = runProgramOnExternalProcess (programCommand, arguments, fileContents, fileNames,  ownerModule.getName());

		if (!isDoomed()){

			if (success){  //David: abort here
				desuppressProjectPanelReset();
				return retrieveTreeBlock(trees, finalScore);   // here's where we actually process everything.
			}
		}
		desuppressProjectPanelReset();
		if (data != null)
			data.setEditorInhibition(false);
		return null;


	}	
	



	public  boolean showMultipleRuns() {
		return (!bootstrapOrJackknife() && numRuns>1);
	}


	/*.................................................................................................................*/
	public Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore){
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

		desuppressProjectPanelReset();
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

	protected static final int OUT_LOGFILE=0;
	protected static final int OUT_TREEFILE=1;
	protected static final int OUT_SUMMARYFILE=2;
	protected static final int WORKING_TREEFILE=3;

	/*.................................................................................................................*/

	public void runFilesAvailable(int fileNum) {

		String[] logFileNames = getLogFileNames();
		if ((progIndicator!=null && progIndicator.isAborted()) || logFileNames==null)
			return;
		String[] outputFilePaths = new String[logFileNames.length];
		outputFilePaths[fileNum] = externalProcRunner.getOutputFilePath(logFileNames[fileNum]);
		String filePath=outputFilePaths[fileNum];

		if (fileNum==OUT_LOGFILE && outputFilePaths.length>OUT_LOGFILE && !StringUtil.blank(outputFilePaths[OUT_LOGFILE]) && !bootstrapOrJackknife()) {   // screen log
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

		if (fileNum==WORKING_TREEFILE && outputFilePaths.length>WORKING_TREEFILE && !StringUtil.blank(outputFilePaths[WORKING_TREEFILE]) && !bootstrapOrJackknife() && showIntermediateTrees) {   // tree file
			String treeFilePath = filePath;

			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

			}
			else ((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, null);
		}

		if (fileNum==OUT_TREEFILE && outputFilePaths.length>OUT_TREEFILE && !StringUtil.blank(outputFilePaths[OUT_TREEFILE]) && !bootstrapOrJackknife() && showIntermediateTrees) {   // tree file
			String treeFilePath = filePath;

			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, outgroupSet);

			}
			else ((ZephyrTreeSearcher)ownerModule).newTreeAvailable(treeFilePath, null);
		}

		//David: if isDoomed() then module is closing down; abort somehow

		if (fileNum==OUT_SUMMARYFILE && outputFilePaths.length>OUT_SUMMARYFILE && !StringUtil.blank(outputFilePaths[OUT_SUMMARYFILE])) {   // info file
			if (MesquiteFile.fileExists(filePath)) {
				//	Debugg.println("\n\n ========================\nsummaryFilePosition before: " + summaryFilePosition);
				//String s = MesquiteFile.getFileLastContents(filePath,fPOS);
				String s = MesquiteFile.getFileContentsAsString(filePath);
				long lastLength = s.length();
				s = s.substring((int)summaryFilePosition);
				summaryFilePosition = lastLength;
				//	Debugg.println(" summaryFilePosition after: " + summaryFilePosition);
				//	Debugg.println(" s: " + s);
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
									logln("RAxML Run " + (runNumber+1) + ", final score ln L = " +token);
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
