/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.RAxMLExporter;

import java.awt.*;
import java.awt.event.ActionEvent;

import mesquite.categ.lib.*;
import mesquite.io.lib.InterpretPhylip;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.zephyr.RAxMLRunnerLocalOld.*;
import mesquite.zephyr.lib.*;

/*TODO: Eventually will become a module to run RAxML on a remote cluster.
 * To accomplish this, perhaps RAxMLRunner would become an abstract superclass for two modules,
 * RAxMLLocal, which will function as RAxMLRunner does now, and RAxMLRemote, which will
 * replace this module.*/

//TODO: add functionality to commandField

public class RAxMLExporter extends RAxMLRunnerLocalOld {
	String baseFileName = "untitled";
	String directoryPath;
	static final int threadingOther =0;
	static final int threadingMPI = 1;
	static final int threadingPThreads = 2;
	int threadingVersion = threadingOther;

	int randomIntSeed = (int)System.currentTimeMillis();   // convert to int as RAxML doesn't like really big numbers
	boolean retainFiles = false;
	String MPIsetupCommand = "";
	int numProcessors = 2;
		
	int numRuns = 5;
	Taxa taxa;
	String outgroupTaxSetString = "";
	boolean isProtein = false;

	int bootstrapreps = 0;
	int bootstrapSeed = Math.abs((int)System.currentTimeMillis());
	static String dnaModel = "GTRGAMMAI";
	static String proteinModel = "PROTGAMMAJTT";
	static String otherOptions = "";
	TaxonNameShortener namer = new TaxonNameShortener();
	
	SingleLineTextField raxMLPathField, dnaModelField, proteinModelField, otherOptionsField, MPISetupField;
	IntegerField seedField;
	javax.swing.JLabel commandLabel;
	SingleLineTextArea commandField;
	IntegerField numProcessorsFiled, numRunsField, bootStrapRepsField;
	RadioButtons threadingRadioButtons;

	/*.................................................................................................................*/
	/**A dialog for various RAxML options and file information, called by startJob.*/
	public boolean queryOptions() { //over-riding RAxMLRunner's method 
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "RAxML Options",buttonPressed);
		dialog.addLabel("RAxML - Options");
		String helpString = "This module will prepare a matrix and command file for RAxML.  If the matrix includes partitions (groups), this module will also "
			+ "produce a partitions file.  RAxML must be run separately.  You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). ";

		dialog.appendToHelpString(helpString);

		SingleLineTextField baseFileNameField;
		baseFileNameField = dialog.addTextField("Base name for files:", baseFileName, 30);

		dialog.addBlankLine();//To vertically separate the base name for files and the destination directory.
		Button directoryBrowseButton = dialog.addAListenedButton("Choose destination directory", null, this);
		directoryBrowseButton.setActionCommand("directoryPathBrowse");

		dialog.addHorizontalLine(1);
		dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);

		dialog.addHorizontalLine(1);
		numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);
		otherOptionsField = dialog.addTextField("Other RAxML options:", otherOptions, 40);

		dialog.addHorizontalLine(1);
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"other", "MPI version", "PThreads version"}, threadingVersion);
		numProcessorsFiled = dialog.addIntegerField("Number of Processors", numProcessors, 8, 1, MesquiteInteger.infinite);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);

		dialog.addHorizontalLine(1);
		commandLabel = dialog.addLabel("");
		commandField = dialog.addSingleLineTextArea("", 2);
		dialog.addBlankLine();
		Button showCommand = dialog.addAListenedButton("Compose Command",null, this);
		showCommand.setActionCommand(composeProgramCommand);
		Button clearCommand = dialog.addAListenedButton("Clear",null, this);
		clearCommand.setActionCommand("clearCommand");

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			dnaModel = dnaModelField.getText();
			proteinModel = proteinModelField.getText();
			numRuns = numRunsField.getValue();
			randomIntSeed = seedField.getValue();
			bootstrapreps = bootStrapRepsField.getValue();
			otherOptions = otherOptionsField.getText();
			threadingVersion = threadingRadioButtons.getValue();
			MPIsetupCommand = MPISetupField.getText();
			numProcessors = numProcessorsFiled.getValue(); //
			baseFileName = baseFileNameField.getText();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	/**Performs actions based on buttons pressed in query dialog controlled by queryOptions method.*/
	public  void actionPerformed(ActionEvent e) {//over-rides RAxMLRunner's method
		if(e.getActionCommand().equalsIgnoreCase("directoryPathBrowse")){
			directoryPath = "";
			directoryPath = MesquiteFile.chooseDirectory("Where to save .phy and .config files?");
			if(StringUtil.blank(directoryPath)){
				if(!MesquiteThread.isScripting()){
					logln("Directory not accessable as entered (could not find /'" + directoryPath + "/'); files will be saved to " + MesquiteTrunk.suggestedDirectory + ".");
				}
				directoryPath = MesquiteTrunk.suggestedDirectory;
			}
			//Check to make sure directoryPath ends with file separator character.  If it doesn't, append it.
			String endOfPath = directoryPath.substring(directoryPath.length());
			if(!StringUtil.stringsEqual(endOfPath, MesquiteFile.fileSeparator)){
				directoryPath += MesquiteFile.fileSeparator;
			}
		}
		else
		if (e.getActionCommand().equalsIgnoreCase(composeProgramCommand)) {//TODO: other than displaying (in the dialog window) how the command would look, what does this actually do?
			String arguments = getArguments("fileName", proteinModelField.getText(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null,null);
			String command = getProgramCommand(threadingRadioButtons.getValue(), MPISetupField.getText(), numProcessorsFiled.getValue(), arguments, false);
			commandLabel.setText("This command will be used to run RAxML:");
			commandField.setText(command);

		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}
	/*.................................................................................................................*/
	/**Called to set the taxon name length and the module responsible for formatting taxon names.  Called by ZephyrUtil.saveExportFile.*/
	public void prepareExportFile(FileInterpreterI exporter) {//over-rides RAxMLRunner's method
		((InterpretPhylip)exporter).setTaxonNameLength(100);
		((InterpretPhylip)exporter).setTaxonNamer(namer);
		((InterpretPhylip)exporter).writeTaxaWithAllMissing=false;
		
	}
	/*.................................................................................................................*/
	/**Prepares the single-line command to be sent to RAxML.*/
	String getArguments(String fileName, String LOCproteinModel, String LOCdnaModel, String LOCotherOptions, int LOCbootstrapreps, int LOCbootstrapSeed, int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, String baseFileName){//over-rides RAxMLRunner's method
		String arguments = " -s " + fileName + " -n "+ baseFileName + ".out -m "; 

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
			arguments += " -q " + LOCMultipleModelFile;
		
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
				arguments += " -o " + outgroupSet.getStringList(",", namer, false);
		}

		return arguments;
	}
	/*.................................................................................................................*/
	private String[] getRateModels(CharactersGroup[] parts){//over-rides RAxMLRunner's method
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
	/*.................................................................................................................*/
	/**If there are partitions (or 'Groups' by Mesquiteese) in the selected matrix, will prepare the partition and return 
	 * a string that will ultimately be written to a partition text file called [baseFileName]_parts.txt.*/
	private String getMultipleModelFileString(CharacterData data, boolean partByCodPos){//over-rides RAxMLRunner's method
		boolean writeCodPosPartition = false;  // currently not used anywhere
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
		CharInclusionSet incl = null; //TODO: Never used?

		String codPosPart = ""; //TODO: Never used?
		boolean molecular = (data instanceof MolecularData); //TODO: Never used?
		boolean nucleotides = (data instanceof DNAData);
		boolean protein = (data instanceof ProteinData);
		String standardPart = "";//TODO: Never used?
		if (writeStandardPartition) {
			Listable[] partition = (Listable[])characterPartition.getProperties();
			partition = data.removeExcludedFromListable(partition);
			if (nucleotides) {
				String q;
				for (int i=0; i<parts.length; i++) {
					q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0));
					if (q != null) {
						if (nucleotides)
							sb.append("DNA, " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
				q = ListableVector.getListOfMatches(partition, null, CharacterStates.toExternal(0));
				if (q != null) {
					if (nucleotides)
						sb.append("DNA, unassigned = " +  q + "\n");
				}
			} else if (protein) {
				String[] rateModels = getRateModels(parts);
				if (rateModels!=null) {
					for (int i=0; i<parts.length; i++) {
						String q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0));
						if (q != null && i<rateModels.length) {
							sb.append(rateModels[i]+", " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(), true) + " = " +  q + "\n");
						}
					}
				}
			}
		} else if (writeCodPosPartition && partByCodPos) {//TODO: never accessed because in the only call of this method, partByCodPos is false.  //not anymore
			//codon positions if nucleotide
			int numberCharSets = 0; //TODO: Never used?
				CodonPositionsSet codSet = (CodonPositionsSet)data.getCurrentSpecsSet(CodonPositionsSet.class);
				boolean[] include = data.getBooleanArrayOfIncluded();
				for (int iw = 0; iw<4; iw++){
					String locs = codSet.getListOfMatches(iw,0,include);
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
	/**Called by RAxMLTrees.  In the current incarnation, method writes .phy and .config files to be run separately.*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble[] finalScore) {//over-rides RAxMLRunner's method
		if (matrix==null )
			return null;
		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return null;
		}

		MolecularData data = (MolecularData)CharacterData.getData(this, matrix, taxa);
		if (!(data instanceof MolecularData)){
			MesquiteMessage.discreetNotifyUser("Sorry, RAxMLTree works only if given a full MolecularData object");
			return null;
		}
		setRAxMLSeed(seed);

		isProtein = data instanceof ProteinData;

		suppressProjectPanelReset();

		data.incrementEditInhibition();
		if(directoryPath == null || baseFileName == null){
			return null;
		}
		
		String dataFile = baseFileName + ".phy";//will be used by getArguments method
		String dataFilePath = directoryPath + dataFile;
		String configFile = baseFileName + ".config";//TODO: necessary only for ZephyrUtil.saveExportFile call (see note in that class regarding unused variables)?
		String configFilePath = directoryPath + configFile;

		String multipleModelFileContents = getMultipleModelFileString(data, true);  //TODO: turned to true by Wayne
		String multipleModelFilePath = null;
		String multipleModelFileName = baseFileName + "_parts.txt";
		if (StringUtil.notEmpty(multipleModelFileContents)) {
			multipleModelFilePath = directoryPath + multipleModelFileName;
			MesquiteFile.putFileContents(multipleModelFilePath, multipleModelFileContents, true);
			if(!MesquiteThread.isScripting()){
				logln(directoryPath + multipleModelFileName);
			}
		}

		String arguments = getArguments(dataFile, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFileName, baseFileName);
		String configFileContents = "# replace <raxml_call> with appropriate command to call RAxML" + StringUtil.lineEnding();
		configFileContents += getProgramCommand(threadingVersion, MPIsetupCommand,numProcessors, arguments, true)+ StringUtil.lineEnding();
		

		/*Write the phylip file to disk, choosing the appropriate interpreter (DNA or protein).  The interpreter will shorten the name
		 * according to the format of the 'namer' object.  In this case, it is a TaxonNameShortener (see prepareExportFile method above).
		 * In RAxMLRunner, the namer is a SimpleTaxonNamer.*/
		
		FileInterpreterI exporter = null;
		if (data instanceof DNAData)
			exporter = getFileInterpreter(this,"#InterpretPhylipDNA");
		else if (data instanceof ProteinData)
			exporter = getFileInterpreter(this,"#InterpretPhylipProtein");
		if (exporter==null){
			desuppressProjectPanelReset();
			return null;
		}
		prepareExportFile(exporter);

		boolean fileSaved = false;
		//DANGER Debugg.println: Since the file interpreter here is the coordinator's, reentrancy could make a mess of things, including with writing hints
		if (data instanceof DNAData)
			fileSaved = ZephyrUtil.saveExportFile(this,exporter,  dataFilePath,  data, selectedTaxaOnly);
		else if (data instanceof ProteinData)
			fileSaved = ZephyrUtil.saveExportFile(this, exporter,  dataFilePath,  data, selectedTaxaOnly);

	
		MesquiteFile.putFileContents(configFilePath, configFileContents, true);//TODO: why ascii = true?
		
		desuppressProjectPanelReset();
		if (!fileSaved){
			if(!MesquiteThread.isScripting()){
				logln("Export problems encountered.  Files not written to disk.");
			}
			return null;
		}
		else {
			if(!MesquiteThread.isScripting()){
				logln("Files written to disk:");
				logln(dataFilePath);
				logln(configFilePath);
			}
		}

		//Considerable code from RAxMLRunner omitted here

		data.decrementEditInhibition();
		return null;
	}	

	/*.................................................................................................................*/
	/**Generates string that will be saved to .config file; will serve as the command to call RAxML.*/
	String getProgramCommand(int threadingVersion, String LOCMPIsetupCommand, int LOCnumProcessors, String arguments, boolean protect){
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

		command += "<raxml_call>" + fullArguments;

		return command;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}

	/*.................................................................................................................*/
	public String getName(){
		return "RAxML Exporter";
	}
	/*.................................................................................................................*/
	public String getExplanation(){
		return "Creates a data file (.phy) and configuration file (.config) for running RAxML on a remote cluster.  If the matrix selected for analysis contains partitions, it also creates a (_parts.txt) file with partition information.  Does *not* call RAxML to perform search; this must be done outside of Mesquite.";
	}
}
