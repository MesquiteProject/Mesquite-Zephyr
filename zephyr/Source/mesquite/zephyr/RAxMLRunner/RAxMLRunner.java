package mesquite.zephyr.RAxMLRunner;
/* Mesquite source code.  Copyright 1997-2009 W. Maddison and D. Maddison.
Version 2.0, September 2007.
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.zephyr.RAxMLTrees.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;

/* TODO:
-b bootstrapRandomNumberSeed  // allow user to set seed

outgroups

 */

public class RAxMLRunner extends MesquiteModule  implements OutputFileProcessor, ShellScriptWatcher, ActionListener, ZephyrFilePreparer  {
	public static final String SCORENAME = "RAxMLScore";


	RAxMLTrees ownerModule;
	Random rng;
	String raxmlPath;
	boolean onlyBest = true;
	
	static final int threadingOther =0;
	static final int threadingMPI = 1;
	static final int threadingPThreads = 2;
	int threadingVersion = threadingOther;
	
//	long randomSeed = System.currentTimeMillis();
	int randomIntSeed = (int)System.currentTimeMillis();   // convert to int as RAxML doesn't like really big numbers
	
	boolean retainFiles = false;
	String MPIsetupCommand = "";
	int numProcessors = 2;
	boolean showIntermediateTrees = true;
	
	
	int numRuns = 5;
	int numRunsCompleted = 0;
	int run = 0;
	Taxa taxa;
	String outgroupTaxSetString = "";
	int outgroupTaxSetNumber = 0;
	boolean preferencesSet = false;
	boolean isProtein = false;

	int bootstrapreps = 0;
	int bootstrapSeed = Math.abs((int)System.currentTimeMillis());
	static String dnaModel = "GTRGAMMAI";
	static String proteinModel = "PROTGAMMAJTT";
	static String otherOptions = "";

	SimpleTaxonNamer namer = new SimpleTaxonNamer();


	long  randseed = -1;
	static String constraintfile = "none";
	MesquiteTimer timer = new MesquiteTimer();


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		rng = new Random(System.currentTimeMillis());
		if (randomIntSeed<0)
			randomIntSeed = -randomIntSeed;
		loadPreferences();
		if (!MesquiteThread.isScripting() && !queryOptions())
			return false;
		return true;
	}

	public void initialize (RAxMLTrees ownerModule) {
		this.ownerModule= ownerModule;
	}
	public boolean initializeTaxa (Taxa taxa) {
		Taxa currentTaxa = this.taxa;
		this.taxa = taxa;
		if (taxa!=currentTaxa && taxa!=null) {
			if (!MesquiteThread.isScripting() && !queryTaxaOptions(taxa))
				return false;
		}
		return true;
	}

	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("raxmlPath".equalsIgnoreCase(tag)) 
			raxmlPath = StringUtil.cleanXMLEscapeCharacters(content);
		if ("numRuns".equalsIgnoreCase(tag))
			numRuns = MesquiteInteger.fromString(content);
		if ("bootStrapReps".equalsIgnoreCase(tag))
			bootstrapreps = MesquiteInteger.fromString(content);
		if ("onlyBest".equalsIgnoreCase(tag))
			onlyBest = MesquiteBoolean.fromTrueFalseString(content);

	       if ("threadingVersion".equalsIgnoreCase(tag))
	    	   threadingVersion = MesquiteInteger.fromString(content);

		if ("MPIsetupCommand".equalsIgnoreCase(tag)) 
			MPIsetupCommand = StringUtil.cleanXMLEscapeCharacters(content);
       if ("numProcessors".equalsIgnoreCase(tag))
    	   numProcessors = MesquiteInteger.fromString(content);

		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "raxmlPath", raxmlPath);  
		StringUtil.appendXMLTag(buffer, 2, "bootStrapReps", bootstrapreps);  
		StringUtil.appendXMLTag(buffer, 2, "numRuns", numRuns);  
		StringUtil.appendXMLTag(buffer, 2, "onlyBest", onlyBest);  
		StringUtil.appendXMLTag(buffer, 2, "threadingVersion", threadingVersion);  
		StringUtil.appendXMLTag(buffer, 2, "MPIsetupCommand", MPIsetupCommand);  
		StringUtil.appendXMLTag(buffer, 2, "numProcessors", numProcessors);  

		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		if (!okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying Options"))  //Debugg.println needs to check that options set well enough to proceed anyway
			return true;
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "RAxML Options & Locations",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("RAxML - Options and Locations");
		String helpString = "This module will prepare a matrix for RAxML, and ask RAxML do to an analysis.  A command-line version of RAxML must be installed. "
			+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). "
			+ "Mesquite will read in the trees found by RAxML, and, for non-bootstrap analyses, also read in the value of the RAxML score (-ln L) of the tree. " 
			+ "You can see the RAxML score by choosing Taxa&Trees>List of Trees, and then in the List of Trees for that trees block, choose "
			+ "Columns>Number for Tree>Other Choices, and then in the Other Choices dialog, choose RAxML Score.";

		dialog.appendToHelpString(helpString);

		//PopUpPanelOfCards cardPanel = dialog.addPopUpPanelOfCards("");
		//Panel[] cardPanels = new Panel[3];

		//dialog.setAddPanel(cardPanel);

		//cardPanels[0] = cardPanel.addNewCard("RAxML Location");
		//dialog.setAddPanel(cardPanels[0]); 


		//if (!isProtein)
			dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		//else
			proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);
		dialog.addHorizontalLine(1);

		//cardPanels[1] = cardPanel.addNewCard("Search Replicates & Bootstrap");
		//dialog.setAddPanel(cardPanels[1]);
		numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);

		dialog.addHorizontalLine(1);
		//cardPanels[2] = cardPanel.addNewCard("Character Models");
		//dialog.setAddPanel(cardPanels[2]);
		//dialog.addHorizontalLine(1);
		otherOptionsField = dialog.addTextField("Other RAxML options:", otherOptions, 40);

		dialog.addHorizontalLine(1);
		raxMLPathField = dialog.addTextField("Path to RAxML:", raxmlPath, 40);
		Button RAxMLBrowseButton = dialog.addAListenedButton("Browse...",null, this);
		dialog.addHorizontalLine(1);
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"other", "MPI version", "PThreads version"}, threadingVersion);
		numProcessorsFiled = dialog.addIntegerField("Number of Processors", numProcessors, 8, 1, MesquiteInteger.infinite);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
		RAxMLBrowseButton.setActionCommand("RAxMLBrowse");
		dialog.addHorizontalLine(1);
		commandLabel = dialog.addLabel("");
		commandField = dialog.addSingleLineTextArea("", 2);
		dialog.addBlankLine();
		Button showCommand = dialog.addAListenedButton("Compose Command",null, this);
		showCommand.setActionCommand("composeRAxMLCommand");
		Button clearCommand = dialog.addAListenedButton("Clear",null, this);
		clearCommand.setActionCommand("clearCommand");

//		dialog.nullifyAddPanel();

		dialog.addHorizontalLine(1);
		retainFilescheckBox = dialog.addCheckBox("Retain Files", retainFiles);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			raxmlPath = raxMLPathField.getText();//TODO: What happens if this field is not left blank, but the 'Browse...' button is used?
			dnaModel = dnaModelField.getText();
			proteinModel = proteinModelField.getText();
			numRuns = numRunsField.getValue();
			randomIntSeed = seedField.getValue();
			bootstrapreps = bootStrapRepsField.getValue();
			onlyBest = onlyBestBox.getState();
			otherOptions = otherOptionsField.getText();
			threadingVersion = threadingRadioButtons.getValue();
			 MPIsetupCommand = MPISetupField.getText();
			 numProcessors = numProcessorsFiled.getValue(); //
			 retainFiles = retainFilescheckBox.getState();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0) && !StringUtil.blank(raxmlPath);
	}
	SingleLineTextField raxMLPathField, dnaModelField, proteinModelField, otherOptionsField, MPISetupField;
	IntegerField seedField;
	javax.swing.JLabel commandLabel;
	SingleLineTextArea commandField;
	IntegerField numProcessorsFiled, numRunsField, bootStrapRepsField;
	Checkbox onlyBestBox, retainFilescheckBox;
	RadioButtons threadingRadioButtons;
	
	/*.................................................................................................................*/
	public boolean queryTaxaOptions(Taxa taxa) {
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (ssv==null || ssv.size()<=0)
			return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "RAxML Outgroup Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("RAxML Outgroup Options");

		boolean specifyOutgroup = false;

		Choice taxonSetChoice = null;
		Checkbox specifyOutgroupBox = dialog.addCheckBox("specify outgroup", specifyOutgroup);
		taxonSetChoice = dialog.addPopUpMenu ("Outgroups: ", ssv, 0);


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			specifyOutgroup = specifyOutgroupBox.getState();
			outgroupTaxSetString = taxonSetChoice.getSelectedItem();
			if (!specifyOutgroup)
				outgroupTaxSetString="";
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("RAxMLBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			raxmlPath = MesquiteFile.openFileDialog("Choose RAxML", directoryName, fileName);
			if (StringUtil.notEmpty(raxmlPath))
				raxMLPathField.setText(raxmlPath);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("composeRAxMLCommand")) {
			
/*			String multipleModelFileContents = getMultipleModelFileString(data, false);
			String multipleModelFilePath = null;
			if (StringUtil.notEmpty(multipleModelFileContents)) {
				multipleModelFilePath = rootDir + "multipleModelFile.txt";
				MesquiteFile.putFileContents(multipleModelFilePath, multipleModelFileContents, true);
			}
*/
			String arguments = getArguments("fileName", proteinModelField.getText(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null);
			String command = getProgramCommand(threadingRadioButtons.getValue(), MPISetupField.getText(), numProcessorsFiled.getValue(), raxMLPathField.getText(), arguments, false);
			commandLabel.setText("This command will be used to run RAxML:");
			commandField.setText(command);
		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}
	/*.................................................................................................................*/
	public void setRAxMLSeed(long seed){
		this.randseed = seed;
	}

	ProgressIndicator progIndicator;
	int count=0;

	double finalValue = MesquiteDouble.unassigned;
	double[] finalValues = null;
	int runNumber = 0;

	public void prepareExportFile(FileInterpreterI exporter) {
		((InterpretPhylip)exporter).setTaxonNameLength(100);
		((InterpretPhylip)exporter).setTaxonNamer(namer);

	}
	/*.................................................................................................................*
	private boolean saveExportFile(Taxa taxa, String directoryPath, String fileName, String path, MolecularData data) {
		if (data==null)
			return false;


		FileCoordinator coord = getFileCoordinator();
		if (coord == null) 
			return false;

		incrementMenuResetSuppression();

		FileInterpreterI exporter=null;
		if (data instanceof DNAData)
			exporter = (FileInterpreterI)coord.findEmployeeWithName("#InterpretPhylipDNA");
		else if (data instanceof ProteinData)
			exporter = (FileInterpreterI)coord.findEmployeeWithName("#InterpretPhylipProtein");
		if (exporter!=null) {
			StringBuffer sb = ((InterpretPhylip)exporter).getDataFile(data);
			MesquiteFile.putFileContents(path, sb.toString(), true);
			decrementMenuResetSuppression();

			return true;
		}


		decrementMenuResetSuppression();
		return false;
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
					String q = ListableVector.getListOfMatches((Listable[])characterPartition.getProperties(), parts[i], CharacterStates.toExternal(0));
					if (q != null) {
						if (nucleotides)
							sb.append("DNA, " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
			} else if (protein) {
				String[] rateModels = getRateModels(parts);
				if (rateModels!=null) {for (int i=0; i<parts.length; i++) {
					String q = ListableVector.getListOfMatches((Listable[])characterPartition.getProperties(), parts[i], CharacterStates.toExternal(0));
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

	/*.................................................................................................................*/
	String getArguments(String fileName, String LOCproteinModel, String LOCdnaModel, String LOCotherOptions, int LOCbootstrapreps, int LOCbootstrapSeed, int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile){
		String arguments = " -s " + fileName + " -n file.out -m "; 

		if (isProtein) {
			if (StringUtil.blank(LOCproteinModel))
				arguments += "PROTGAMMAJTT";
			else
				arguments += LOCproteinModel;
		}
		else if (StringUtil.blank(LOCdnaModel))
			arguments += "GTRGAMMA";
		else
			arguments += LOCdnaModel;
		
		if (StringUtil.notEmpty(LOCMultipleModelFile))
			arguments += " -q " + ShellScriptUtil.protectForShellScript(LOCMultipleModelFile);
		
		arguments += " -p " + randomIntSeed;
			
			
		if (!StringUtil.blank(LOCotherOptions)) 
			arguments += " " + LOCotherOptions;

		if (LOCbootstrapreps>0) {
			arguments += " -# " + LOCbootstrapreps + " -b " + LOCbootstrapSeed;
		}
		else if (LOCnumRuns>1)
			arguments += " -# " + LOCnumRuns;
		
		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(LOCoutgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(LOCoutgroupTaxSetString,TaxaSelectionSet.class);
			if (outgroupSet!=null) 
				arguments += " -o " + outgroupSet.getStringList(",", true);
		}

		return arguments;
	}
	/*.................................................................................................................*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble[] finalScore) {
		if (matrix==null )
			return null;
	if (StringUtil.blank(raxmlPath))
			return null;
		if (!(matrix.getParentData() != null && matrix.getParentData() instanceof MolecularData)){
			MesquiteMessage.discreetNotifyUser("Sorry, RAxMLTree works only if given a full MolecularData object");
			return null;
		}
		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return null;
		}

		finalValues = new double[numRuns];
		DoubleArray.deassignArray(finalValues);

		setRAxMLSeed(seed);

		MolecularData data = (MolecularData)matrix.getParentData();
		isProtein = data instanceof ProteinData;

		getProject().incrementProjectWindowSuppression();
		
		Debugg.println(this.getName() + " using data matrix " + data.getID());

		data.setEditorInhibition(true);
		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
Debugg.println("RETAIN FILES " + retainFiles);
		String rootDir = ZephyrUtil.createDirectoryForFiles(this, ZephyrUtil.BESIDE_HOME_FILE, "RAxML");  //NOTE: retains files now always
		if (rootDir==null)
			return null;
		
		String fileName = "tempData" + MesquiteFile.massageStringToFilePathSafe(unique) + ".phy";   //replace this with actual file name?
		String filePath = rootDir +  fileName;

		boolean fileSaved = false;
		
		if (data instanceof DNAData)
			 fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipDNA", taxa, rootDir,  fileName,  filePath,  data);
		else if (data instanceof ProteinData)
			 fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipProtein", taxa, rootDir,  fileName,  filePath,  data);

		if (!fileSaved) return null;


		String runningFilePath = rootDir + "running" + MesquiteFile.massageStringToFilePathSafe(unique);
		//String outFilePath = rootDir + "tempTree" + MesquiteFile.massageStringToFilePathSafe(unique) + ".tre";

		StringBuffer shellScript = new StringBuffer(1000);

		String treeFileName;
		if (bootstrap())
			treeFileName = "RAxML_bootstrap.file.out";
		else 
			treeFileName = "RAxML_result.file.out";
		String logFileName = "RAxML_log.file.out";
		String summaryFilePath = rootDir+"RAxML_info.file.out";
		if (!bootstrap() && numRuns>1) {
			treeFileName+=".RUN.";
			logFileName+=".RUN.";
		}
		String treeFilePath = rootDir + treeFileName;
		String currentTreeFilePath =null;
		currentTreeFilePath = treeFilePath;
		String[] logFilePaths = {rootDir + logFileName, currentTreeFilePath, summaryFilePath};

		//./raxmlHPC -s infile.phy -n outfile.out -m GTRGAMMA -o outgroup1,outgroup2

		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir)+ StringUtil.lineEnding());
		shellScript.append("ls -la"+ StringUtil.lineEnding());
		
		String multipleModelFileContents = getMultipleModelFileString(data, false);//TODO: why is partByCodPos false?
		String multipleModelFilePath = null;
		if (StringUtil.notEmpty(multipleModelFileContents)) {
			multipleModelFilePath = rootDir + "multipleModelFile.txt";
			MesquiteFile.putFileContents(multipleModelFilePath, multipleModelFileContents, true);
		}
			

		String arguments = getArguments(fileName, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFilePath);
		/*" -s " + fileName + " -n file.out -m "; 

		if (isProtein) {
			if (StringUtil.blank(proteinModel))
				arguments += "PROTGAMMAJTT";
			else
				arguments += proteinModel;
		}
		else if (StringUtil.blank(dnaModel))
			arguments += "GTRGAMMA";
		else
			arguments += dnaModel;

		if (!StringUtil.blank(otherOptions)) 
			arguments += " " + otherOptions;

		if (bootstrap()) {
			arguments += " -# " + bootstrapreps + " -b " + bootstrapSeed;
		}
		else if (numRuns>1)
			arguments += " -# " + numRuns;
*/		
		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		//	if (outgroupSet!=null) 
		//		arguments += " -o " + outgroupSet.getStringList(",", true);
		}
		

		
		
		shellScript.append(getProgramCommand(threadingVersion, MPIsetupCommand,numProcessors, raxmlPath, arguments, true)+ StringUtil.lineEnding());

		shellScript.append(ShellScriptUtil.getRemoveCommand(runningFilePath));
		

		String scriptPath = rootDir + "raxmlScript" + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), true);


		progIndicator = new ProgressIndicator(getProject(),ownerModule.getName(), "RAxML Search", 0, true);
		if (progIndicator!=null){
			count = 0;
			progIndicator.start();
		}

		MesquiteMessage.logCurrentTime("Start of RAxML analysis: ");

		if (!bootstrap() && numRuns>1)
			logln("\nBeginning Run 1");
		else
			logln("");

		timer.start();
		numRunsCompleted = 0;
		boolean success = ShellScriptUtil.executeLogAndWaitForShell(scriptPath, "RAxML Tree", logFilePaths, this, this);
		logln("RAxML analysis completed at " + getDateAndTime());

		double totalTime= timer.timeSinceVeryStartInSeconds();
		if (totalTime>120.0)
			logln("Total time: " + StringUtil.secondsToHHMMSS((int)totalTime));
		else
			logln("Total time: " + totalTime  + " seconds");
		if (finalScore != null){
			if (finalScore.length==1)
				finalScore[0].setValue(finalValue);
			else
				for (int i=0; i<finalScore.length && i<finalValues.length; i++)
					finalScore[i].setValue(finalValues[i]);
		}

		if (progIndicator!=null)
			progIndicator.goAway();
		if (!success)
			logln("Execution of RAxML unsuccessful [1]");

		if (success){
			success = false;
			Tree t= null;
			MesquiteBoolean readSuccess = new MesquiteBoolean(false);
			if (bootstrap()) {
				t =readRAxMLTreeFile(trees, treeFilePath, "RAxML Bootstrap Tree", readSuccess, false);
				ZephyrUtil.adjustTree(t, outgroupSet);
			}
			else if (numRuns==1) {
				t =readRAxMLTreeFile(trees, treeFilePath, "RAxMLTree", readSuccess, true);
				ZephyrUtil.adjustTree(t, outgroupSet);
			}
			else if (numRuns>1) {
				TreeVector tv = new TreeVector(taxa);
				for (int run=0; run<numRuns; run++)
					if (MesquiteFile.fileExists(treeFilePath+run)) {
						String path =treeFilePath+run;
						t = readRAxMLTreeFile(tv, path, "RAxMLTree Run " + (run+1), readSuccess, true);
						if (trees!= null)
							trees.addElement(t, false);
					}
				if (trees !=null) {
					String summary = MesquiteFile.getFileContentsAsString(summaryFilePath);
					Parser parser = new Parser(summary);
					parser.setAllowComments(false);
					parser.allowComments = false;

					String line = parser.getRawNextDarkLine();
					int count = 0;
					logln("\nSummary of RAxML Search");

					while (!StringUtil.blank(line) && count < finalValues.length) {
						if (line.startsWith("Inference[")) {
							Parser subParser = new Parser();
							subParser.setString(line);
							String token = subParser.getFirstToken();
							while (!StringUtil.blank(token) && ! subParser.atEnd()){
								if (token.indexOf("likelihood")>=0) {
									token = subParser.getNextToken();
									finalValues[count] = -MesquiteDouble.fromString(token);
									finalScore[count].setValue(finalValues[count]);
									logln("RAxML Run " + (count+1) + " ln L = -" + finalValues[count]);
								}
								token = subParser.getNextToken();
							}
							count++;
						}
						parser.setAllowComments(false);
						line = parser.getRawNextDarkLine();
					}
					double bestScore =MesquiteDouble.unassigned;
					int bestRun = MesquiteInteger.unassigned;
					for (int i=0; i<trees.getNumberOfTrees(); i++) {
						Tree newTree = trees.getTree(i);
						ZephyrUtil.adjustTree(newTree, outgroupSet);
						if (MesquiteDouble.isCombinable(finalValues[i])){
							MesquiteDouble s = new MesquiteDouble(-finalValues[i]);
							s.setName(RAxMLRunner.SCORENAME);
							((Attachable)newTree).attachIfUniqueName(s);
						}

						//trees.addElement(newTree, false);

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
						t = trees.getTree(bestRun);
						ZephyrUtil.adjustTree(t, outgroupSet);

						String newName = t.getName() + " BEST";
						if (t instanceof AdjustableTree )
							((AdjustableTree)t).setName(newName);
					}
					if (MesquiteDouble.isCombinable(bestScore)){
						logln("Best score: " + bestScore);
					}
					//Only retain the best tree in tree block.
					if(trees.getTree(bestRun) != null && onlyBest){
						Tree bestTree = trees.getTree(bestRun);
						trees.removeAllElements(false);
						trees.addElement(bestTree, false);
					}
				} 

			}
			success = readSuccess.getValue();
			if (!success)
				logln("Execution of RAxML unsuccessful [2]");

			getProject().decrementProjectWindowSuppression();
			deleteSupportDirectory();
			data.setEditorInhibition(false);
			if (success) 
				return t;
			return null;
		} 
		deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();
		data.setEditorInhibition(false);
		return null;
	}	

	/*.................................................................................................................*/
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

	Parser parser = new Parser();
	long screenFilePos = 0;
	MesquiteFile screenFile = null;

	/*.................................................................................................................*/
	public String[] modifyOutputPaths(String[] outputFilePaths){
		if (numRuns==1)
			return outputFilePaths;
		String[] paths = new String[outputFilePaths.length];
		for (int i=0; i<outputFilePaths.length; i++)
			paths[i]=outputFilePaths[i];

		for (int i=0; i<=1; i++)
			for (int run=numRuns; run>=0; run--)
				if (MesquiteFile.fileExists(outputFilePaths[i]+run)) {
					paths[i]=outputFilePaths[i]+run;
					break;
				}

		return paths;
	}
	/*.................................................................................................................*/
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

	public void processOutputFile(String[] outputFilePaths, int fileNum) {
		if (progIndicator.isAborted())
			return;
		String filePath = null;
		filePath = getOutputFileToReadPath(outputFilePaths[fileNum]);

		if (fileNum==0 && outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0]) && !bootstrap()) {   // screen log
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
				count++;
			} 
		}

		if (fileNum==1 && outputFilePaths.length>1 && !StringUtil.blank(outputFilePaths[1]) && !bootstrap() && showIntermediateTrees) {   // tree file
			String treeFilePath = filePath;
			if (taxa != null) {
				TaxaSelectionSet outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
				ownerModule.newTreeAvailable(treeFilePath, outgroupSet);
				
			}
			else ownerModule.newTreeAvailable(treeFilePath, null);
		}

		if (fileNum==2 && outputFilePaths.length>2 && !StringUtil.blank(outputFilePaths[2])) {   // info file
			if (MesquiteFile.fileExists(filePath)) {
				String s = MesquiteFile.getFileLastContents(filePath,2);
				if (!StringUtil.blank(s)) {
					Parser parser = new Parser();
					parser.allowComments=false;
					parser.setString(s);
					String searchString = "";
					if (bootstrap())
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
								if (bootstrap()){
									logln("RAxML bootstrap replicate " + numRunsCompleted + " of " + bootstrapreps+" completed");
								}
								else
									logln("RAxML Run " + (runNumber+1) + ", final score ln L = " +token );
								//processOutputFile(outputFilePaths,1);
								foundRunInfo = true;
							}
							if (foundRunInfo)
								token="";
							else
								token = subParser.getNextToken();
						}
						if (numRunsCompleted>= numProcessors) {
							double timePerRep = timer.timeSinceVeryStartInSeconds()/numRunsCompleted;   //this is time per rep
							int timeLeft = 0;
							if (bootstrap()) {
								timeLeft = (int)((bootstrapreps- numRunsCompleted) * timePerRep);
							}
							else {
								timeLeft = (int)((numRuns- numRunsCompleted) * timePerRep);
							}
							double timeSoFar = timer.timeSinceVeryStartInSeconds();
							logln("   Run time " +  StringUtil.secondsToHHMMSS((int)timeSoFar)  + ", approximate time remaining " + StringUtil.secondsToHHMMSS(timeLeft));
							logln("    Average time per replicate:  " +  StringUtil.secondsToHHMMSS((int)timePerRep));
							logln("    Estimated total time:  " +  StringUtil.secondsToHHMMSS((int)(timeSoFar+timeLeft))+"\n");
 							//Debugg.println("     numRunsCompleted: " + numRunsCompleted);
							//Debugg.println("     timer.timeSinceVeryStartInSeconds(): " + timer.timeSinceVeryStartInSeconds());
							//Debugg.println("     timePerRep: " + timePerRep);
							//Debugg.println("     bootstrapreps: " + bootstrapreps);
							//Debugg.println("     timeLeft: " + timeLeft);
						}

						if (!bootstrap() && runNumber+1<numRuns) {
							logln("");
							logln("Beginning Run " + (runNumber+2));
						}
					}
				}

			} 
		}

	}
	

	/*.................................................................................................................*/

	public void processCompletedOutputFiles(String[] outputFilePaths) {
		runNumber=0;
		screenFilePos = 0;
		if (outputFilePaths.length>0 && !StringUtil.blank(outputFilePaths[0])) {
			if (!bootstrap()) {
				String s = MesquiteFile.getFileLastContents(outputFilePaths[0]);
				if (!StringUtil.blank(s)) {
					parser.setString(s);
					parser.getFirstToken();
					finalValue = MesquiteDouble.fromString(parser.getNextToken());
				}
			}
			ZephyrUtil.copyLogFile(this, "RAxML", outputFilePaths[2]);

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


	public boolean bootstrap() {
		return bootstrapreps>0;
	}
	public int getBootstrapreps() {
		return bootstrapreps;
	}

	public void setBootstrapreps(int bootstrapreps) {
		this.bootstrapreps = bootstrapreps;
	}

	/*.................................................................................................................*/
	public void setRAxMLPath(String RAxMLPath){
		this.raxmlPath = RAxMLPath;
	}

	public Class getDutyClass() {
		return RAxMLRunner.class;
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



}
