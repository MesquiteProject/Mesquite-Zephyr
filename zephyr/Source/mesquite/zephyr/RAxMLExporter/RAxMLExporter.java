package mesquite.zephyr.RAxMLExporter;

import java.awt.*;
import java.awt.event.ActionEvent;
import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.zephyr.RAxMLRunner.*;
import mesquite.zephyr.lib.*;

/*TODO: Eventually will become a module to run RAxML on a remote cluster.
 * To accomplish this, RAxMLRunner should become an abstract superclass for two modules,
 * RAxMLLocal, which will function as RAxMLRunner does now, and RAxMLRemote, which will
 * replace this module.*/

public class RAxMLExporter extends RAxMLRunner {
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
//	boolean showIntermediateTrees = true;
		
	int numRuns = 5;
//	int numRunsCompleted = 0;
//	int run = 0;
	Taxa taxa;
	String outgroupTaxSetString = "";
//	int outgroupTaxSetNumber = 0;
//	boolean preferencesSet = false;
	boolean isProtein = false;

	int bootstrapreps = 0;
	int bootstrapSeed = Math.abs((int)System.currentTimeMillis());
	static String dnaModel = "GTRGAMMAI";
	static String proteinModel = "PROTGAMMAJTT";
	static String otherOptions = "";

	
	SingleLineTextField raxMLPathField, dnaModelField, proteinModelField, otherOptionsField, MPISetupField;
	IntegerField seedField;
	javax.swing.JLabel commandLabel;
	SingleLineTextArea commandField;
	IntegerField numProcessorsFiled, numRunsField, bootStrapRepsField;
//	Checkbox onlyBestBox, retainFilescheckBox;
	RadioButtons threadingRadioButtons;

	//TODO: add functionality to commandField
	
	/*.................................................................................................................*/
	/**A dialog for various RAxML options and file information, called by startJob.*/
	public boolean queryOptions() { //over-riding RAxMLRunner's method 
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "RAxML Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("RAxML - Options");
		String helpString = "This module will prepare a matrix and command file for RAxML.  RAxML must be run separately. "
			+ "You can ask it to do multiple searches for optimal trees, OR to do a bootstrap analysis (but not both). ";

		dialog.appendToHelpString(helpString);

		//PopUpPanelOfCards cardPanel = dialog.addPopUpPanelOfCards("");
		//Panel[] cardPanels = new Panel[3];

		//dialog.setAddPanel(cardPanel);

		//cardPanels[0] = cardPanel.addNewCard("RAxML Location");
		//dialog.setAddPanel(cardPanels[0]); 

		SingleLineTextField baseFileNameField;
		baseFileNameField = dialog.addTextField("Base name for files:", baseFileName, 30);

//		SingleLineTextField directoryField = dialog.addTextField("Directory to save files:",directoryPath,50);
		dialog.addBlankLine();
		Button directoryBrowseButton = dialog.addAListenedButton("Choose destination directory", null, this);
		directoryBrowseButton.setActionCommand("directoryPathBrowse");

		dialog.addHorizontalLine(1);

		//if (!isProtein)
			dnaModelField = dialog.addTextField("DNA Model:", dnaModel, 20);
		//else
			proteinModelField = dialog.addTextField("Protein Model:", proteinModel, 20);
		dialog.addHorizontalLine(1);

		//cardPanels[1] = cardPanel.addNewCard("Search Replicates & Bootstrap");
		//dialog.setAddPanel(cardPanels[1]);
		numRunsField = dialog.addIntegerField("Number of Search Replicates", numRuns, 8, 1, MesquiteInteger.infinite);
//		onlyBestBox = dialog.addCheckBox("save only best tree", onlyBest);
		bootStrapRepsField = dialog.addIntegerField("Bootstrap Replicates", bootstrapreps, 8, 0, MesquiteInteger.infinite);
		seedField = dialog.addIntegerField("Random number seed: ", randomIntSeed, 20);

		dialog.addHorizontalLine(1);
		//cardPanels[2] = cardPanel.addNewCard("Character Models");
		//dialog.setAddPanel(cardPanels[2]);
		//dialog.addHorizontalLine(1);
		otherOptionsField = dialog.addTextField("Other RAxML options:", otherOptions, 40);

//		dialog.addHorizontalLine(1);
//		raxMLPathField = dialog.addTextField("Path to RAxML:", raxmlPath, 40);
//		Button RAxMLBrowseButton = dialog.addAListenedButton("Browse...",null, this);
//		RAxMLBrowseButton.setActionCommand("RAxMLBrowse");
		dialog.addHorizontalLine(1);
		threadingRadioButtons= dialog.addRadioButtons(new String[] {"other", "MPI version", "PThreads version"}, threadingVersion);
		numProcessorsFiled = dialog.addIntegerField("Number of Processors", numProcessors, 8, 1, MesquiteInteger.infinite);
		MPISetupField = dialog.addTextField("MPI setup command: ", MPIsetupCommand, 20);
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
//		retainFilescheckBox = dialog.addCheckBox("Retain Files", retainFiles);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
//			raxmlPath = raxMLPathField.getText();
			dnaModel = dnaModelField.getText();
			proteinModel = proteinModelField.getText();
			numRuns = numRunsField.getValue();
			randomIntSeed = seedField.getValue();
			bootstrapreps = bootStrapRepsField.getValue();
//			onlyBest = onlyBestBox.getState();
			otherOptions = otherOptionsField.getText();
			threadingVersion = threadingRadioButtons.getValue();
			MPIsetupCommand = MPISetupField.getText();
			numProcessors = numProcessorsFiled.getValue(); //
//			retainFiles = retainFilescheckBox.getState();
			baseFileName = baseFileNameField.getText();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
//		return (buttonPressed.getValue()==0) && !StringUtil.blank(raxmlPath);
	}
	/*.................................................................................................................*/
	/**Performs actions based on buttons pressed in query dialog controlled by queryOptions method.*/
	public  void actionPerformed(ActionEvent e) {//over-rides RAxMLRunner's method
/*		if (e.getActionCommand().equalsIgnoreCase("RAxMLBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			raxmlPath = MesquiteFile.openFileDialog("Choose RAxML", directoryName, fileName);
			if (StringUtil.notEmpty(raxmlPath))
				raxMLPathField.setText(raxmlPath);
		}*/
		if(e.getActionCommand().equalsIgnoreCase("directoryPathBrowse")){
			directoryPath = "";
			directoryPath = MesquiteFile.chooseDirectory("Where to save .phy and .config files?");
			if(StringUtil.blank(directoryPath)){
				if(!MesquiteThread.isScripting()){
					logln("Directory not accessable as entered (could not find /'" + directoryPath + "/'); files will be saved to " + MesquiteTrunk.suggestedDirectory + ".");
				}
				directoryPath = MesquiteTrunk.suggestedDirectory;
			}
			//TODO: if field is re-implemented, then the check for correct file separator should happen elsewhere
			//Check to make sure directoryPath ends with file separator character.  If it doesn't append it.
			String endOfPath = directoryPath.substring(directoryPath.length());
			if(!StringUtil.stringsEqual(endOfPath, MesquiteFile.fileSeparator)){
				directoryPath += MesquiteFile.fileSeparator;
			}
		}
		else
		if (e.getActionCommand().equalsIgnoreCase("composeRAxMLCommand")) {
			
/*			String multipleModelFileContents = getMultipleModelFileString(data, false);
			String multipleModelFilePath = null;
			if (StringUtil.notEmpty(multipleModelFileContents)) {
				multipleModelFilePath = rootDir + "multipleModelFile.txt";
				MesquiteFile.putFileContents(multipleModelFilePath, multipleModelFileContents, true);
			}
*/
			String arguments = getArguments("fileName", proteinModelField.getText(), dnaModelField.getText(), otherOptionsField.getText(), bootStrapRepsField.getValue(), bootstrapSeed, numRunsField.getValue(), outgroupTaxSetString, null,null);
//			String command = getProgramCommand(threadingRadioButtons.getValue(), MPISetupField.getText(), numProcessorsFiled.getValue(), raxMLPathField.getText(), arguments, false);
			String command = getProgramCommand(threadingRadioButtons.getValue(), MPISetupField.getText(), numProcessorsFiled.getValue(), arguments, false);
			commandLabel.setText("This command will be used to run RAxML:");
			commandField.setText(command);//TODO: not currently implemented?

		}
		else	if (e.getActionCommand().equalsIgnoreCase("clearCommand")) {
			commandField.setText("");
			commandLabel.setText("");
		}
	}
	/*.................................................................................................................*/
	String getArguments(String fileName, String LOCproteinModel, String LOCdnaModel, String LOCotherOptions, int LOCbootstrapreps, int LOCbootstrapSeed, int LOCnumRuns, String LOCoutgroupTaxSetString, String LOCMultipleModelFile, String baseFileName){
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
				arguments += " -o " + outgroupSet.getStringList(",", true);
		}

		return arguments;
	}
	/*.................................................................................................................*/
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
	/*.................................................................................................................*/
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
	/**Called by RAxMLTrees.  In the current incarnation, method writes .phy and .config files to be run separately.*/
	public Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble[] finalScore) {//over-rides RAxMLRunner's method
		if (matrix==null )
			return null;
		if (!(matrix.getParentData() != null && matrix.getParentData() instanceof MolecularData)){
			MesquiteMessage.discreetNotifyUser("Sorry, RAxMLTree works only if given a full MolecularData object");
			return null;
		}
		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return null;
		}

		setRAxMLSeed(seed);

		MolecularData data = (MolecularData)matrix.getParentData();
		isProtein = data instanceof ProteinData;

		getProject().incrementProjectWindowSuppression();

		data.setEditorInhibition(true);
//		String unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
//		String rootDir = ZephyrUtil.createDirectoryForFiles(this, retainFiles, "RAxML");
//		if (rootDir==null)
//			return null;
		if(directoryPath == null || baseFileName == null){
			return null;
		}
		
//		String fileName = "tempData" + MesquiteFile.massageStringToFilePathSafe(unique) + ".phy";   //replace this with actual file name?
//		String filePath = rootDir +  fileName;
		String dataFile = baseFileName + ".phy";//will be used by getArguments method
		String dataFilePath = directoryPath + dataFile;
		String configFile = baseFileName + ".config";//TODO: necessary only for ZephyrUtil.saveExportFile call (see note in that class regarding unused variables)?
		String configFilePath = directoryPath + configFile;

		String multipleModelFileContents = getMultipleModelFileString(data, false);
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
//		String arguments = getArguments(fileName, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFilePath);
		String configFileContents = "# replace <raxml_call> with appropriate command to call RAxML" + StringUtil.lineEnding();
		configFileContents += getProgramCommand(threadingVersion, MPIsetupCommand,numProcessors, arguments, true)+ StringUtil.lineEnding();
//		String configFileContents = getProgramCommand(threadingVersion, MPIsetupCommand,numProcessors, raxmlPath, arguments, true)+ StringUtil.lineEnding();
		
		boolean fileSaved = false;

		//Write the phylip file to disk, choosing the appropriate interpreter (DNA or protein)
		if (data instanceof DNAData){
//			fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipDNA", taxa, rootDir,  fileName,  filePath,  data);
			fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipDNA", taxa, directoryPath,  dataFile,  dataFilePath,  data);

		} else if (data instanceof ProteinData){
//			 fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipProtein", taxa, rootDir,  fileName,  filePath,  data);
			 fileSaved = ZephyrUtil.saveExportFile(this,"#InterpretPhylipProtein", taxa, directoryPath,  dataFile,  dataFilePath,  data);
		}
		MesquiteFile.putFileContents(configFilePath, configFileContents, true);//TODO: why ascii = true?
		
		if (!fileSaved){
			return null;
		}
		else {
			if(!MesquiteThread.isScripting()){
				logln("Files written to disk:");
				logln(dataFilePath);
				logln(configFilePath);
			}
		}

/*
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

		String multipleModelFileContents = getMultipleModelFileString(data, false);
		String multipleModelFilePath = null;
		if (StringUtil.notEmpty(multipleModelFileContents)) {
//			multipleModelFilePath = rootDir + "multipleModelFile.txt";
			multipleModelFilePath = directoryPath + baseFileName + "_parts.txt";
			MesquiteFile.putFileContents(multipleModelFilePath, multipleModelFileContents, true);
			if(!MesquiteThread.isScripting()){
				logln(directoryPath + baseFileName + "_parts.txt");
			}
		}


		String arguments = getArguments(fileName, proteinModel, dnaModel, otherOptions, bootstrapreps, bootstrapSeed, numRuns, outgroupTaxSetString, multipleModelFilePath);
*/
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
/*		TaxaSelectionSet outgroupSet =null;
		if (!StringUtil.blank(outgroupTaxSetString)) {
			outgroupSet = (TaxaSelectionSet) taxa.getSpecsSet(outgroupTaxSetString,TaxaSelectionSet.class);
		//	if (outgroupSet!=null) 
		//		arguments += " -o " + outgroupSet.getStringList(",", true);
		}
*/		

/*		
		
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
		logln("Total time: " + timer.timeSinceVeryStartInSeconds() + " seconds");
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
*/
/*
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
					if (MesquiteDouble.isCombinable(bestScore))
						logln("Best score: " + bestScore);

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
		} */
/*		deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();*/
		data.setEditorInhibition(false);
		return null;
	}	

	/*.................................................................................................................*/
	/**Generates string that will be saved to .config file; will serve as the command to call RAxML.*/
	String getProgramCommand(int threadingVersion, String LOCMPIsetupCommand, int LOCnumProcessors, String arguments, boolean protect){
//	String getProgramCommand(int threadingVersion, String LOCMPIsetupCommand, int LOCnumProcessors, String LOCraxmlPath, String arguments, boolean protect){
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

/*		
		if (!protect)
			command += LOCraxmlPath + fullArguments;
		else if (MesquiteTrunk.isWindows())
			command += StringUtil.protectForWindows(LOCraxmlPath)+ fullArguments;
		else
			command += StringUtil.protectForUnix(LOCraxmlPath )+ fullArguments;
*/
		
		return command;
	}
	/*.................................................................................................................*/
	public String getName(){
		return "RAxML Exporter";
	}
	/*.................................................................................................................*/
	public String getExplanation(){
		return "Creates a data file (.phy) and configuration file (.config) for running RAxML on a remote cluster.  Does *not* call RAxML to perform search; this must be done outside of Mesquite.";
	}
}
