/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.Choice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.DNAData;
import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.ProteinData;
import mesquite.charMatrices.ManageCharInclusion.ManageCharInclusion;
import mesquite.charMatrices.ManageCharWeights.ManageCharWeights;
import mesquite.io.InterpretTNT.InterpretTNT;
import mesquite.lib.*;
import mesquite.lib.characters.CharInclusionSet;
import mesquite.lib.characters.CharSpecsSet;
import mesquite.lib.characters.CharWeightSet;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CharacterModel;
import mesquite.lib.characters.CharacterPartition;
import mesquite.lib.characters.CharacterStates;
import mesquite.lib.characters.CharactersGroup;
import mesquite.lib.characters.CodonPositionsSet;
import mesquite.lib.characters.ModelSet;
import mesquite.lib.duties.FileCoordinator;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.duties.TreeSource;
import mesquite.lib.duties.TreesManager;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.taxa.TaxaSelectionSet;
import mesquite.lib.taxa.Taxon;
import mesquite.lib.taxa.TaxonNamer;
import mesquite.lib.tree.AdjustableTree;
import mesquite.lib.tree.MesquiteTree;
import mesquite.lib.tree.Tree;
import mesquite.lib.tree.TreeUtil;
import mesquite.lib.tree.TreeVector;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.SingleLineTextField;
import mesquite.parsimony.ManageTypesets.ManageTypesets;
import mesquite.parsimony.lib.ParsimonyModelSet;



public class ZephyrUtil {
	public static final String VERSION_FILE = "fileToDetermineVersion";


	/*.................................................................................................................*/
	public static void adjustTree(Tree t, TaxaSelectionSet outgroupSet) {
		if (t instanceof AdjustableTree) {
			if (outgroupSet==null) {
				int rerootNode = t.nodeOfTaxonNumber(0);  
				if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) 
					((AdjustableTree)t).reroot(rerootNode, t.getRoot(), false);
				((AdjustableTree)t).standardize(null,  false);
			} else 
				((AdjustableTree)t).standardize(outgroupSet,  false);
		}
	}

	/*.................................................................................................................*/
	//TODO: Many unused variables in method call (taxa, directoryPath, fileName)?
	public static boolean saveExportFile(MesquiteModule module, FileInterpreterI exporter, String path, CategoricalData data, boolean selectedTaxaOnly) {
		if (data==null)
			return false;

		module.incrementMenuResetSuppression();
		boolean success = false;
		if (exporter!=null) {
			/* oldStyle 
			MesquiteFile file = new MesquiteFile();
			file.writeTaxaWithAllMissing = false;
			file.writeExcludedCharacters = false;
			file.writeCharactersWithNoData=false;
			file.writeCharLabelInfo = false;
			file.setPath(path);
				exporter.writeOnlySelectedTaxa = selectedTaxaOnly;
			if (module instanceof ZephyrFilePreparer)
				((ZephyrFilePreparer)module).prepareExportFile(exporter);
			MesquiteStringBuffer msb = exporter.getDataAsFileText(file, data);  //Debugg.println use writeMatrixToFile instead, thoug this will require other rearrangements
			if (msb!=null) {
				MesquiteFile.putFileContents(path, msb, true);
				module.decrementMenuResetSuppression();

				return true;
			}
		*/
			exporter.writeOnlySelectedTaxa = selectedTaxaOnly;
			exporter.writeTaxaWithAllMissing = false;
			exporter.writeExcludedCharacters = false;
			exporter.writeCharactersWithNoData=false;
			exporter.writeCharLabels = false;  
			
			if (module instanceof ZephyrFilePreparer)
				((ZephyrFilePreparer)module).prepareExportFile(exporter);
			
			 success = exporter.writeMatrixToFile(data, path);
			
		}

		module.decrementMenuResetSuppression();
		return success;
	}	
	
	/*.................................................................................................................*/
	/* QZ: MOVE this and next two to MesquiteTree alongside read methods there, or to PhylipUtils in mesquite.io */
	public static boolean validNewickEnds(String line){  // check to see if tree is valid; used only here
		Parser parser = new Parser(line);
		String s = parser.getFirstToken();
		if (!s.startsWith("(")){
			return false;
		}
		s = parser.getLastToken();
		if (!";".equalsIgnoreCase(s)){
			return false;
		}
		return true;
	}

	/*.................................................................................................................*/
	public static Tree readPhylipTree (String line, Taxa taxa, boolean permitTaxaBlockEnlarge, TaxonNamer namer) {
		return readPhylipTree(line, taxa, permitTaxaBlockEnlarge, false, namer);
	}
	/*.................................................................................................................*/
	public static Tree readPhylipTree (String line, Taxa taxa, boolean permitTaxaBlockEnlarge, boolean permitSpaceUnderscoreEquivalent, TaxonNamer namer) {
		if (StringUtil.blank(line))
			return null;
		if (!validNewickEnds(line))
			return null;
		MesquiteTree t = new MesquiteTree(taxa);
		t.setPermitTaxaBlockEnlargement(permitTaxaBlockEnlarge);
		t.readTree(line, namer, null, "():;,[]\'"); //tree reading adjusted to use Newick punctuation rather than NEXUS
		return t;
	}
	
	/*.................................................................................................................*/

	public static final String RAXMLSCORENAME = "RAxMLScore";
	public static final String IQTREESCORENAME = "IQTREEScore";
	public static final String RAXMLFINALSCORENAME = "RAxMLScore (Final Gamma-based)";
	/*.................................................................................................................*/

	public static String[] getRAxMLRateModels(MesquiteModule mb, String[] partNames, String defaultModel){
		if (partNames==null || partNames.length==0 || partNames.length>20)
			return null;
		String[] rateModels = new String[partNames.length+1];
		for (int i=0; i<rateModels.length; i++)
			rateModels[i] = defaultModel;

		if (!MesquiteThread.isScripting()) {
			MesquiteInteger buttonPressed = new MesquiteInteger(1);
			ExtensibleDialog dialog = new ExtensibleDialog(mb.containerOfModule(), "Character Models",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
			dialog.addLabel("Character Rate Models");

			SingleLineTextField[] modelFields = new SingleLineTextField[rateModels.length];
			for (int i=0; i<rateModels.length; i++)
				if (i<partNames.length)
					modelFields[i] = dialog.addTextField(partNames[i]+":", rateModels[i], 20);
				else
					modelFields[i] = dialog.addTextField("unassigned:", rateModels[i], 20);

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
		return rateModels;
	}

	/*.................................................................................................................*/

	public static String getMultipleModelRAxMLString(MesquiteModule mb, CharacterData data, boolean partByCodPos){
		return getMultipleModelRAxMLString(mb,data,partByCodPos, "JTT", "DNA", false, true);
	}
	/*.................................................................................................................*/

	public static String getMultipleModelRAxMLString(MesquiteModule mb, CharacterData data, boolean partByCodPos, String proteinModel, String dnaModel, boolean isRAxMLNG, boolean specifyPartByPartModels){
		boolean writeCodPosPartition = false;
		boolean writeStandardPartition = false;
		String localDNAModel = "DNA";
		if (StringUtil.notEmpty(dnaModel) && isRAxMLNG)
			localDNAModel=dnaModel;
		CharactersGroup[] parts =null;
		if (data instanceof DNAData)
			writeCodPosPartition = ((DNAData)data).someCoding();
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition == null && !writeCodPosPartition) {
			return null;
		} 
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
			writeStandardPartition = parts!=null;
		}

		if (!writeStandardPartition && !writeCodPosPartition) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		CharInclusionSet incl = (CharInclusionSet)data.getCurrentSpecsSet(CharInclusionSet.class);

		String codPosPart = "";
		boolean molecular = (data instanceof MolecularData);
		boolean nucleotides = (data instanceof DNAData);
		boolean protein = (data instanceof ProteinData);
		String defaultModel ="";
		if (protein) {
			defaultModel=proteinModel;
		}
		else 
			defaultModel=localDNAModel;

		String standardPart = "";
		if (writeStandardPartition && !partByCodPos) {
			String[] partNames= new String[parts.length];
			for (int i=0; i<parts.length; i++) {
				partNames[i]=parts[i].getName();
			}
			Listable[] partition = (Listable[])characterPartition.getProperties();
			partition = data.removeExcludedFromListable(partition);
			partition = data.removeEmptyFromListable(partition, false);
			if ((nucleotides && !isRAxMLNG) || !specifyPartByPartModels) {
				String q;
				for (int i=0; i<parts.length; i++) {
					q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0), true, ",");
					if (q != null) {
							sb.append(defaultModel + ", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_"+parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
				q = ListableVector.getListOfMatches(partition, null, CharacterStates.toExternal(0), true, ",");
				if (q != null) {
					sb.append(defaultModel + ", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_unassigned", true) + " = " +  q + "\n");
				}
			} else if (protein || isRAxMLNG) {
				String[] rateModels = getRAxMLRateModels(mb, partNames, defaultModel);
				String q;
				if (rateModels!=null) {
					for (int i=0; i<parts.length; i++) {
						q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0), true, ",");
						if (q != null && i<rateModels.length) {
							sb.append(rateModels[i]+", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_"+parts[i].getName(), true) + " = " +  q + "\n");
						}
					}
				}
				q = ListableVector.getListOfMatches(partition, null, CharacterStates.toExternal(0), true, ",");
				if (q != null) {
					sb.append(rateModels[rateModels.length-1]+", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_unassigned", true) + " = " +  q + "\n");
				}
			} else {  // non molecular
				String q;
				for (int i=0; i<parts.length; i++) {
					q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0), true, ",");
					if (q != null) {
						if (nucleotides)
							sb.append("MULTI, " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_"+parts[i].getName(), true) + " = " +  q + "\n");
					}
				}
				q = ListableVector.getListOfMatches(partition, null, CharacterStates.toExternal(0), true, ",");
				if (q != null) {
					sb.append("MULTI, " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_unassigned", true) + " = " +  q + "\n");
				}
			} 
		} else if (writeCodPosPartition && partByCodPos) {   //by codon position
			//codon positions if nucleotide
			CodonPositionsSet codSet = (CodonPositionsSet)data.getCurrentSpecsSet(CodonPositionsSet.class);
			
			int numberCharSets = 0;
			boolean[] include = data.getBooleanArrayOfIncludedAndNotEmpty();
			if (specifyPartByPartModels && isRAxMLNG) {
				String[] partNames= new String[4];
				partNames[0] = "non-coding";
				partNames[1] = "first positions";
				partNames[2] = "second positions";
				partNames[3] = "third positions";
				String[] rateModels = getRAxMLRateModels(mb, partNames, defaultModel);
				String q;
				for (int iw = 0; iw<4; iw++){
					String locs = codSet.getListOfMatches(iw,0, include, true);
					if (!StringUtil.blank(locs)) {
						String charSetName = "";
						if (iw==0) 
							charSetName = StringUtil.tokenize("nonCoding");
						else 
							charSetName = StringUtil.tokenize("codonPos" + iw);			
						numberCharSets++;
						sb.append(rateModels[iw] + ", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_"+charSetName,true) + " = " +  locs + "\n");
					}
				}
			} else {
				for (int iw = 0; iw<4; iw++){
					String locs = codSet.getListOfMatches(iw,0, include, true);
					if (!StringUtil.blank(locs)) {
						String charSetName = "";
						if (iw==0) 
							charSetName = StringUtil.tokenize("nonCoding");
						else 
							charSetName = StringUtil.tokenize("codonPos" + iw);			
						numberCharSets++;
						sb.append( localDNAModel + ", " + StringUtil.simplifyIfNeededForOutput(data.getName()+"_"+charSetName,true) + " = " +  locs + "\n");
					}
				}
			}
			//	String codPos = ((DNAData)data).getCodonsAsNexusCharSets(numberCharSets, charSetList); // equivalent to list
		}

		return sb.toString();
	}

	/*.................................................................................................................*/

	public static void readRAxMLInfoFile(MesquiteModule mb, String fileContents, boolean verbose, TreeVector trees, DoubleArray finalValues, DoubleArray optimizedValues) {
		if (finalValues==null) return;
		Parser parser = new Parser(fileContents);
		parser.setAllowComments(false);
		parser.allowComments = false;
		int count =0;

		String line = parser.getRawNextDarkLine();
		if (verbose)
			mb.logln("\nSummary of RAxML Search");

		while (!StringUtil.blank(line) && count < finalValues.getSize()) {
			if (line.startsWith("Inference[")) {
				Parser subParser = new Parser();
				subParser.setString(line);
				String token = subParser.getFirstToken();
				while (!StringUtil.blank(token) && ! subParser.atEnd()){
					if (token.indexOf("likelihood")>=0) {
						token = subParser.getNextToken();
						finalValues.setValue(count,-MesquiteDouble.fromString(token));
						//	finalScore[count].setValue(finalValues[count]);
						mb.logln("RAxML Run " + (count+1) + " ln L = -" + finalValues.getValue(count));
					}
					token = subParser.getNextToken();
				}
				count++;
			}
			parser.setAllowComments(false);
			line = parser.getRawNextDarkLine();
		}
		
		count =0;

		if (optimizedValues!=null) {
			while (!StringUtil.blank(line) && count < optimizedValues.getSize()) {
				if (line.startsWith("Inference[")) {
					Parser subParser = new Parser();
					subParser.setString(line);
					String token = subParser.getFirstToken();   // should be "Inference"
					while (!StringUtil.blank(token) && ! subParser.atEnd()){
						if (token.indexOf("Likelihood")>=0) {
							token = subParser.getNextToken(); // :
							token = subParser.getNextToken(); // -
							optimizedValues.setValue(count,-MesquiteDouble.fromString(token));
						}
						token = subParser.getNextToken();
					}
					count++;
				}
				parser.setAllowComments(false);
				line = parser.getRawNextDarkLine();
			}
		}


		double bestScore =MesquiteDouble.unassigned;
		int bestRun = MesquiteInteger.unassigned;
		for (int i=0; i<trees.getNumberOfTrees(); i++) {
			Tree newTree = trees.getTree(i);
			if (MesquiteDouble.isCombinable(finalValues.getValue(i))){
				MesquiteDouble s = new MesquiteDouble(-finalValues.getValue(i));
				s.setName(RAXMLSCORENAME);
				((Attachable)newTree).attachIfUniqueName(s);
			}
			if (MesquiteDouble.isCombinable(optimizedValues.getValue(i))){
				MesquiteDouble s = new MesquiteDouble(-optimizedValues.getValue(i));
				s.setName(RAXMLFINALSCORENAME);
				((Attachable)newTree).attachIfUniqueName(s);
			}

			if (MesquiteDouble.isCombinable(finalValues.getValue(i)))
				if (MesquiteDouble.isUnassigned(bestScore)) {
					bestScore = finalValues.getValue(i);
					bestRun = i;
				}
				else if (bestScore>finalValues.getValue(i)) {
					bestScore = finalValues.getValue(i);
					bestRun = i;
				}
		}
		if (MesquiteInteger.isCombinable(bestRun)) {
			Tree t = trees.getTree(bestRun);

			String newName = t.getName() + " BEST";
			if (t instanceof AdjustableTree )
				((AdjustableTree)t).setName(newName);
		}

	}
	
	/*.................................................................................................................*/
	public  static Tree readTNTTrees(MesquiteModule module, TreeVector trees, String path, String contents, String treeName, int firstTreeNumber, Taxa taxa, boolean firstTree, boolean onlyLastTree, NameReference valuesAtNodes, TaxonNamer namer) {
		FileCoordinator coord = module.getFileCoordinator();
		if (coord == null) 
			return  null;

		MesquiteModule.incrementMenuResetSuppression();

		String translationFile = null;
		String translationTablePath = MesquiteFile.getDirectoryPathFromFilePath(path)+ TreeUtil.translationTableFileName;
		translationFile = MesquiteFile.getFileContentsAsString(translationTablePath);
		if (StringUtil.notEmpty(translationFile)){
			if (namer==null)
				namer = new SimpleNamesTaxonNamer();
			((SimpleNamesTaxonNamer)namer).loadTranslationTable(taxa, translationFile);
		}
		else 
			namer = null;


		InterpretTNT exporter = (InterpretTNT)coord.findEmployeeWithName("#InterpretTNT");
		Parser parser = new Parser();
		parser.setString(contents);
		parser.setLineEndString("*");
		String line = parser.getRawNextDarkLine();
		String previousLine = "";
		Tree returnTree = null;
		int count = firstTreeNumber;
		boolean foundTree = false;
		exporter.resetTreeNumber();

		if (exporter!=null) {
			if (namer!=null) {
			}
			while (StringUtil.notEmpty(line)) {
				if (!onlyLastTree) {
					MesquiteTree t = (MesquiteTree)exporter.readTREAD(null, taxa, line, firstTree, null, valuesAtNodes, namer);
					if (t!=null) {
						if (!foundTree)
							returnTree = t;
						foundTree = true;
						if (trees!=null) {
							if (firstTree)
								t.setName(treeName);
							else
								t.setName(treeName+count);
							trees.addElement(t, false);
						}
						if (firstTree)
							break;
						count++;
					}
				}
				previousLine=line;
				line = parser.getRawNextDarkLine();
			}
			if (onlyLastTree && StringUtil.notEmpty(previousLine)) {
				MesquiteTree t = (MesquiteTree)exporter.readTREAD(null, taxa, previousLine, false, null, valuesAtNodes, namer);
				if (t!=null) {
						returnTree = t;
					if (trees!=null) {
						t.setName("Strict consensus tree");
						trees.addElement(t, false);
					}
				}
			}
			MesquiteModule.decrementMenuResetSuppression();
			return returnTree;
		}
		MesquiteModule.decrementMenuResetSuppression();
		return  null;
	}	
	/*.................................................................................................................*/
	public static Tree readTNTTreeFile(MesquiteModule module, TreeVector trees, Taxa taxa, String treeFilePath, String treeName, int firstTreeNumber, MesquiteBoolean success, boolean firstTree, boolean onlyLastTree, NameReference valuesAtNodes, TaxonNamer namer) {
		Tree t =null;
		String contents = MesquiteFile.getFileContentsAsString(treeFilePath, -1);
		t = readTNTTrees(module, trees,treeFilePath, contents,treeName, firstTreeNumber, taxa,firstTree, onlyLastTree, valuesAtNodes, namer);

		if (t!=null) {
			if (success!=null)
				success.setValue(true);
			if (t instanceof AdjustableTree )
				((AdjustableTree)t).setName(treeName);
		}
		return t;

	}
	/*.................................................................................................................*/
	public String getExclusionCommand(CharacterData data) {
		return null;
	}

	/*.................................................................................................................*/
	public static boolean arePartitions(CharacterData data) {
		if (data instanceof DNAData && ((DNAData)data).someCoding()) {
			return true;
		}

		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition!=null) {
			return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	public static void setPartitionChoice(CharacterData data, Choice partitionChoice){
		if (data==null)
			return;
		CharactersGroup[] parts =null;
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
		}
		if (parts==null)
			return;
		for (int i=0; i<parts.length; i++) {
			partitionChoice.addItem(parts[i].getName());
		}
		if (characterPartition.getAnyCurrentlyUnassigned())
			partitionChoice.addItem("unassigned");

	}
	/*.................................................................................................................*/
	public static int getPartitionSubset(CharacterData data, String chosen){
		if (data==null || StringUtil.blank(chosen))
			return -1;
		CharactersGroup[] parts =null;
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
		}
		if (parts!=null){
			for (int i=0; i<parts.length; i++) {
				if (parts[i].getName().equalsIgnoreCase(chosen))
					return i;
			}
			if (chosen.equalsIgnoreCase("unassigned"))
				return parts.length;
		}
		return -1;
	}
	/*.................................................................................................................*/
	public static String getStandardPartitionNEXUSCommands(CharacterData data, boolean writeExcludedCharacters, boolean includeAsterisk, MesquiteInteger numParts){
		String standardPartitionSection = "";
		String standardPartition = "";
		String partitionCommand = "";
		CharactersGroup[] parts =null;
		CharacterPartition characterPartition = (CharacterPartition)data.getCurrentSpecsSet(CharacterPartition.class);
		if (characterPartition!=null) {
			parts = characterPartition.getGroups();
		}
		if (parts==null)
			return null;
		int numCharSets = 0;
		Listable[] partition = (Listable[])characterPartition.getProperties();
		if (!writeExcludedCharacters) 
			partition = data.removeExcludedFromListable(partition);
		String q;
		for (int i=0; i<parts.length; i++) {
			q = ListableVector.getListOfMatches(partition, parts[i], CharacterStates.toExternal(0));
			if (q != null) {
				standardPartition +=  "\n\tcharset " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(),true) + " = " + q + ";";
				numCharSets++;
				if (!StringUtil.blank(partitionCommand))
					partitionCommand += ", ";
				partitionCommand += " part"+i + ":" + StringUtil.simplifyIfNeededForOutput(parts[i].getName(),true) ;
			}
		}
		q = ListableVector.getListOfMatches(partition, null, CharacterStates.toExternal(0));
		if (q != null) {
			standardPartition +=  "\n\tcharset unassigned = " + q + ";";
			numCharSets++;
			if (!StringUtil.blank(partitionCommand))
				partitionCommand += ", ";
			partitionCommand += " part" + parts.length + ": unassigned" ;
		}
		if (numParts!=null) 
			numParts.setValue(numCharSets);
		if (!StringUtil.blank(standardPartition)) {
			standardPartitionSection +=standardPartition;
			standardPartitionSection += "\n\tcharpartition ";
			if (includeAsterisk)
			standardPartitionSection += "* ";
			standardPartitionSection += "currentPartition = " + partitionCommand + ";\n";
		}	
		return standardPartitionSection;
	}
	/*.................................................................................................................*/
	public static String getCodPosPartitionNEXUSCommands(CharacterData data, boolean writeExcludedCharacters, boolean includeAsterisk, MesquiteInteger numParts){
		//codon positions if nucleotide
		String codPosPartitionSection = "";
		int numCharSets = 0;
		String charSetCommands = "";
		String partitionCommand = "";
		CodonPositionsSet codSet = (CodonPositionsSet)data.getCurrentSpecsSet(CodonPositionsSet.class);
		boolean[] include = null;
		if (!writeExcludedCharacters)
			include = data.getBooleanArrayOfIncluded();
		for (int iw = 0; iw<4; iw++){
			String locs = codSet.getListOfMatches(iw,0,include);
			if (!StringUtil.blank(locs)) {
				String charSetName = "";
				if (iw==0) 
					charSetName = StringUtil.tokenize("nonCoding");
				else 
					charSetName = StringUtil.tokenize("codonPos" + iw);			
				numCharSets++;
				charSetCommands += "\n\tcharset " + charSetName + " = " +  locs + ";";
				if (!StringUtil.blank(partitionCommand))
					partitionCommand += ", ";
				partitionCommand += " part"+iw + ":" + charSetName;
			}
		}
		//	String codPos = ((DNAData)data).getCodonsAsNexusCharSets(numberCharSets, charSetList); // equivalent to list
		if (numParts!=null) 
			numParts.setValue(numCharSets);
		if (!StringUtil.blank(charSetCommands)) {
			codPosPartitionSection +=charSetCommands;
			codPosPartitionSection += "\n\tcharpartition ";
			if (includeAsterisk)
				codPosPartitionSection += "* ";
			codPosPartitionSection += "currentPartition = " + partitionCommand + ";\n";
		}	
		return codPosPartitionSection;
	}
	
	/*.................................................................................................................*/

	public static String nexusStringForInclusionSet(CharSpecsSet specsSet, CharacterData data){
		if (specsSet ==null || !(specsSet instanceof CharInclusionSet))
			return null;
		CharInclusionSet inclusionSet = (CharInclusionSet)specsSet;
		String s= "";
		if (inclusionSet !=null) {
			String sT = ManageCharInclusion.nexusCoreStringForSpecsSet(specsSet, data);
			if (!StringUtil.blank(sT)) {
				s+= "\tEXSET " ;
				s += "* ";
				s+= StringUtil.tokenize(inclusionSet.getName()) + " ";
				s+= "  = "+  sT + ";" + StringUtil.lineEnding();
			}
		}
		return s;
	}
	/*.................................................................................................................*/

	public static String nexusStringForTypeSet(CharSpecsSet specsSet, CharacterData data){
		if (specsSet ==null || !(specsSet instanceof ParsimonyModelSet))
			return null;
		ModelSet modelSet = (ModelSet)specsSet;
		String s= "";
		if (modelSet !=null) {
			String sT = ManageTypesets.nexusCoreStringForSpecsSet(specsSet, data);
			if (!StringUtil.blank(sT)) {
				s+= "\tTYPESET " ;
				s += "* ";
				s+= StringUtil.tokenize(modelSet.getName()) + " ";
				s+= "  = "+  sT + ";" + StringUtil.lineEnding();
			}
		}
		return s;
	}
	/*.................................................................................................................*/

	public static String nexusStringForWeightSet(CharSpecsSet specsSet, CharacterData data){
		if (specsSet ==null || !(specsSet instanceof CharWeightSet))
			return null;
		CharWeightSet modelSet = (CharWeightSet)specsSet;
		String s= "";
		if (modelSet !=null) {
			String sT = ManageCharWeights.nexusCoreStringForSpecsSet(specsSet, data);
			if (!StringUtil.blank(sT)) {
				s+= "\tWTSET " ;
				s += "* ";
				s+= StringUtil.tokenize(modelSet.getName()) + " ";
				s+= "  = "+  sT + ";" + StringUtil.lineEnding();
			}
		}
		return s;
	}

	/*.................................................................................................................*/

	public static  String getNEXUSAssumptionBlock(CategoricalData data){
		StringBuffer sb = new StringBuffer();

		ParsimonyModelSet typeSet = (ParsimonyModelSet)data.getCurrentSpecsSet(ParsimonyModelSet.class);
		String typeSetString = nexusStringForTypeSet(typeSet,data);
		CharWeightSet wtSet = (CharWeightSet)data.getCurrentSpecsSet(CharWeightSet.class);
		String wtSetString = nexusStringForWeightSet(wtSet,data);
		CharInclusionSet inclusionSet = (CharInclusionSet)data.getCurrentSpecsSet(CharInclusionSet.class);
		String inclusionSetString = nexusStringForInclusionSet(inclusionSet,data);

		if (StringUtil.notEmpty(typeSetString) || StringUtil.notEmpty(wtSetString)|| StringUtil.notEmpty(inclusionSetString)) {
			sb.append("\nBEGIN assumptions;\n");
			if (StringUtil.notEmpty(typeSetString)){
				Vector modelsUsed = new Vector();
				for (int ic = 0; ic<typeSet.getNumChars(); ic++){ //checking to see if stepmatrices etc. are being used, and if so, include them
					CharacterModel cm = typeSet.getModel(ic);
					if (!cm.isBuiltIn() && modelsUsed.indexOf(cm)<0){
						modelsUsed.addElement(cm);
						String s = "\t"+ cm.getNEXUSCommand() + " ";  
						s += StringUtil.tokenize(cm.getName()) + " (" ;  
						s += StringUtil.tokenize(cm.getNEXUSClassName()) + ") = " + StringUtil.lineEnding();
						s += "\t\t"+ cm.getNexusSpecification()+";" + StringUtil.lineEnding(); 
						sb.append(s);
					}
				}
				sb.append(typeSetString);
			}
			if (StringUtil.notEmpty(wtSetString))
				sb.append(wtSetString);
			if (StringUtil.notEmpty(inclusionSetString))
				sb.append(inclusionSetString);
			sb.append("END;\n");
		}
		return sb.toString();
	}
	/*.................................................................................................................*/

	public static  String getNEXUSSetsBlock(CategoricalData data, boolean useCodPosIfAvailable, boolean writeExcludedCharacters, boolean includeAsterisk, MesquiteInteger numParts){
		StringBuffer sb = new StringBuffer();

		String partitions = "";
		if (useCodPosIfAvailable && data instanceof DNAData && ((DNAData)data).someCoding())
			partitions = getCodPosPartitionNEXUSCommands(data, writeExcludedCharacters, includeAsterisk, numParts);
		else
			partitions = getStandardPartitionNEXUSCommands(data, writeExcludedCharacters, includeAsterisk, numParts);

		if (StringUtil.notEmpty(partitions)) {
			sb.append("\nBEGIN sets;\n");
			sb.append(partitions);
			sb.append("END;\n");
		}
		return sb.toString();
	}

	/*.................................................................................................................*/
	public static boolean writeNEXUSFile(Taxa taxa, String dir, String fileName, String path, CategoricalData data, boolean writeSimplifiedNEXUS, boolean useStandardizedTaxonNames, boolean writeOnlySelectedTaxa, boolean writeSetsBlock, boolean writeAssumptionsBlock, boolean useCodPosIfAvailable, boolean writeExcludedCharacters) {
		if (path != null) {
			MesquiteFile f = MesquiteFile.newFile(dir, fileName);
			f.openWriting(true);
			f.interleaveAllowed=false;
			f.useSimplifiedNexus=writeSimplifiedNEXUS;
			f.useDataBlocks=true;
			f.useStandardizedTaxonNames=useStandardizedTaxonNames;
			f.writeExcludedCharacters=writeExcludedCharacters;
			f.writeTaxaWithAllMissing = false;
			if (taxa.anySelected())
				f.writeOnlySelectedTaxa = writeOnlySelectedTaxa;
			f.writeLine("#NEXUS" + StringUtil.lineEnding());
			boolean nexusIDs = NexusBlock.suppressNEXUSIDS;
			NexusBlock.suppressNEXUSIDS = true;
			data.getMatrixManager().writeCharactersBlock(data, null, f, null);
			if (writeSetsBlock) {
				String setsBlock = getNEXUSSetsBlock(data,useCodPosIfAvailable, writeExcludedCharacters, true, null);
				if (StringUtil.notEmpty(setsBlock))
					f.writeLine(setsBlock + StringUtil.lineEnding());
			}
			if (writeAssumptionsBlock) {
				String assumptionsBlock = getNEXUSAssumptionBlock(data);
				if (StringUtil.notEmpty(assumptionsBlock))
					f.writeLine(assumptionsBlock + StringUtil.lineEnding());
			}
			NexusBlock.suppressNEXUSIDS = nexusIDs;

			//data.getMatrixManager().writeCharactersBlock(data, cB, file, progIndicator)
			f.closeWriting();
			return true;
		}
		return false;
	}

	/*.................................................................................................................*/
	public static boolean writeNEXUSSetsBlock(Taxa taxa, String dir, String fileName, String path, CategoricalData data, boolean useCodPosIfAvailable, boolean writeExcludedCharacters, boolean includeAsterisk, MesquiteInteger numParts) {
		if (path != null) {
			MesquiteFile f = MesquiteFile.newFile(dir, fileName);
			f.openWriting(true);
			f.writeLine("#NEXUS" + StringUtil.lineEnding());
			String setsBlock = getNEXUSSetsBlock(data,useCodPosIfAvailable, writeExcludedCharacters, includeAsterisk, numParts);
			if (StringUtil.notEmpty(setsBlock))
				f.writeLine(setsBlock + StringUtil.lineEnding());

			f.closeWriting();
			return true;
		}
		return false;
	}

	/*.................................................................................................................*
	public static void writeNEXUSFile(Taxa taxa, String dir, String fileName, String path, CategoricalData data, boolean useStandardizedTaxonNames,boolean writeSetsBlock, boolean useCodPosIfAvailable) {
		writeNEXUSFile(taxa, dir, fileName, path, data, useStandardizedTaxonNames, false, writeSetsBlock, useCodPosIfAvailable);
	}
	/*.................................................................................................................*/
	public static void copyOutputText(MesquiteModule ownerModule, String originalOutputFilePath, String text) {

		String outputFilePath = ownerModule.getProject().getHomeDirectoryName();
		if (!StringUtil.blank(outputFilePath)) {
			String fileName = StringUtil.getLastItem(originalOutputFilePath,MesquiteFile.fileSeparator);
			String extension = "";
			if (StringUtil.notEmpty(fileName) && fileName.indexOf('.')>=0){
				extension = "."+StringUtil.getLastItem(fileName, ".");
				fileName = StringUtil.getAllButLastItem(fileName, ".");
			}

			outputFilePath += MesquiteFile.getAvailableFileName(outputFilePath, fileName, extension);
		}
		else
			outputFilePath = MesquiteFile.saveFileAsDialog("Save copy of output to file");
		if (!StringUtil.blank(outputFilePath)) {
			MesquiteFile.putFileContents(outputFilePath, text, true);
		}
	}
	/*.................................................................................................................*/
	public static void copyLogFile(MesquiteModule ownerModule, String programName, String originalLogFilePath) {
		try {
		String logFilePath = ownerModule.getProject().getHomeDirectoryName();
		if (!StringUtil.blank(logFilePath))
			logFilePath += MesquiteFile.getAvailableFileName(logFilePath, programName,".log");
		else
			logFilePath = MesquiteFile.saveFileAsDialog("Save copy of "+programName+" log to file");
		if (!StringUtil.blank(logFilePath)) {
			MesquiteFile.copyFileFromPaths(originalLogFilePath, logFilePath, true);
		}
		}
		catch (NullPointerException e){
			// used to avoid error if user quite mesquite mid-calculation
		}
	}
	/*.................................................................................................................*/

	/// Copied from ManageTrees.getTreeBlock and refactored to remove project-wide dependencies
	public static void writeTaxaBlock(Writer w, Taxa taxa, boolean suppressTitle, boolean useStandardizedTaxonNames) throws Exception, IOException {
		if (taxa == null || taxa.getNumTaxa() == 0)
			return;
		if (taxa.hasDuplicateNames() != null)
			throw new Exception("Duplicate taxa names in taxa block");
		String end = StringUtil.lineEnding();
		w.write(end);
		w.write("BEGIN TAXA;");
		w.write(end);

		if (!suppressTitle)
			w.write("\tTITLE " + StringUtil.tokenize(taxa.getName()) + ";" + end);
		if (taxa.getAnnotation()!=null) 
			w.write("[!" + taxa.getAnnotation() + "]" + StringUtil.lineEnding());

		w.write("\tDIMENSIONS NTAX=" + taxa.getNumTaxa() + ";" + end + "\tTAXLABELS" + end + "\t\t");
		String taxonName = "";
		for (int it=0; it<taxa.getNumTaxa(); it++) {
			taxonName = taxa.getTaxon(it).getName();
			if (taxonName != null) {
				if (useStandardizedTaxonNames)
					w.write(taxa.getStandardizedTaxonName(it));
				else
					w.write(StringUtil.simplifyIfNeededForOutput(taxonName, false) + " ");
			}
			else
				w.write(StringUtil.tokenize(" "));
		}
		w.write(end + "\t;" + end);
		w.write("END;" + end + end);
	}


	/// Copied from ManageTrees.getTreeBlock and refactored to remove project-wide dependencies
	public static void writeTreesBlock(Writer w, TreeVector trees, boolean suppressTitle, boolean suppressLink) throws IOException{
		if (trees == null || trees.size() == 0)
			return;
		String endLine = ";" + StringUtil.lineEnding();
		Taxa taxa = trees.getTaxa();
		w.write("BEGIN TREES");
		w.write(endLine);
		if (!suppressTitle) {
			w.write("\tTitle " + StringUtil.tokenize(trees.getName()));
			w.write(endLine);
		}
		if (taxa!=null && !suppressLink) {
			w.write("\tLINK Taxa = " + StringUtil.tokenize(taxa.getName()));
			w.write(endLine);
		}
		if (trees.getAnnotation()!=null) {
			w.write("[!" + trees.getAnnotation() + "]");
			w.write(StringUtil.lineEnding());
		}
		w.write("\tTRANSLATE" + StringUtil.lineEnding());
		String tt = trees.getTranslationTable();
		int writeMode = Tree.BY_TABLE;
		if (tt == null) {
			tt = "";
			if (taxa!=null)
				for(int i = 0; i < taxa.getNumTaxa(); i++) {
					if (i > 0)
						tt += ","+ StringUtil.lineEnding();
					tt += "\t\t" + Taxon.toExternal(i) + "\t" + StringUtil.tokenize(taxa.getTaxonName(i)) ;
				}
			writeMode = Tree.BY_NUMBERS;
		}
		w.write(tt);
		w.write(endLine);

		Enumeration e = trees.elements();
		while (e.hasMoreElements()) {
			Object obj = e.nextElement();
			Tree t = (Tree)obj;

			w.write("\tTREE ");
			if (t instanceof MesquiteTree && !StringUtil.blank(((MesquiteTree)t).getAnnotation())) {
				String s = ((MesquiteTree)t).getAnnotation();
				s= StringUtil.replace(s, '\n', ' ');
				s=StringUtil.replace(s, '\r', ' ');
				w.write(" [!" + s + "] ");
			}

			w.write(StringUtil.tokenize(t.getName()) + " = ");
			Object weightObject = ((Attachable)t).getAttachment(TreesManager.WEIGHT);
			if (trees.getWriteWeights()&& weightObject!=null && weightObject instanceof MesquiteString)
				w.write("[&W " + ((MesquiteString)weightObject).getValue() + "] ");
			w.write(t.writeTree(writeMode) + StringUtil.lineEnding());

		}
		w.write("END;" + StringUtil.lineEnding()+ StringUtil.lineEnding());
	}

	public static void writeNEXUSTreeFile(Taxa taxa, TreeVector tv, String dir, String fileName) throws Exception, IOException {
		File f = new File(dir, fileName);
		FileWriter w = new FileWriter(f);
		w.write("#NEXUS" + StringUtil.lineEnding());
		ZephyrUtil.writeTaxaBlock(w, taxa, true, false);
		ZephyrUtil.writeTreesBlock(w, tv, true, true);
		w.close();
	}

	public static void writeNEXUSTreeFile(Taxa taxa, TreeSource treeSource, String dir, String fileName) throws Exception, IOException {
		if (dir != null && fileName != null ) {
			int nt = treeSource.getNumberOfTrees(taxa);
			if (nt == MesquiteInteger.infinite) 
				throw new Exception("Cannot create a tree file from and neverending set of trees.");
			TreeVector tv = new TreeVector(taxa);
			for (int i = 0; i < nt ; ++i) {
				Tree iTree = treeSource.getTree(taxa, i);
				tv.addElement(iTree, false);
			}
			ZephyrUtil.writeNEXUSTreeFile(taxa, tv, dir, fileName);
		}
	}

	public static void writeNEXUSTreeFile(Taxa taxa, Tree tree, String dir, String fileName) throws Exception, IOException {
		if (dir != null && fileName != null ) {
			TreeVector tv = new TreeVector(taxa);
			tv.addElement(tree, false);
			ZephyrUtil.writeNEXUSTreeFile(taxa, tv, dir, fileName);
		}
	}
	
	
	public static String getStandardExtraTreeWindowCommands (boolean doMajRule, boolean isBootstrap, long treeBlockID, boolean branchLengthsProportional){
		return getStandardExtraTreeWindowCommands(doMajRule, isBootstrap, false, null, treeBlockID, branchLengthsProportional);
	}

	public static String getStandardExtraTreeWindowCommands (boolean doMajRule, boolean isBootstrap, boolean nodeValuesAsText, String nodeValueNameRef, long treeBlockID, boolean branchLengthsProportional){
		String commands = "";//"setSize 400 600;  ";
		if (doMajRule){  //DAVIDCHECK:  Temporary tree window can't handle this doMajRule, so an error is given when file reread.
			commands += "getOwnerModule; tell It; setTreeSource  #mesquite.consensus.ConsensusTree.ConsensusTree; tell It; setTreeSource  #mesquite.trees.StoredTrees.StoredTrees; tell It;  ";  
			commands += " setTreeBlockByID " + treeBlockID + ";";
			commands += " toggleUseWeights off; endTell; setConsenser  #mesquite.consensus.MajRuleTree.MajRuleTree; endTell; endTell; setTreeNumber 1;";
		}
	
		commands += "getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareLineTree.SquareLineTree; tell It; showEdgeLines off; ";
		
		
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;  tell It; orientRight; ";
		if (!isBootstrap && branchLengthsProportional)
			commands += " branchLengthsToggle on; ";
		commands += " endTell; setEdgeWidth 3; endTell; ";  // endTell is for SquareLineTree
		if (isBootstrap){
			commands += "labelBranchLengths off;";
		}
		commands += "getEmployee #mesquite.trees.BasicDrawTaxonNames.BasicDrawTaxonNames; tell It; setTaxonNameStyler  #mesquite.trees.ColorTaxonByPartition.ColorTaxonByPartition; setFontSize 10; endTell; ";		

		commands += " endTell; resetTitle;"; //endTell for BasicTreeDrawCoordinator
		commands += "getOwnerModule; tell It; getEmployee #mesquite.ornamental.ColorTreeByPartition.ColorTreeByPartition; tell It; colorByPartition on; endTell; endTell; ";
//QZ Debugg.println shift to new system; 
		if (isBootstrap){
			commands += "getOwnerModule; tell It; getEmployee #NodePropertyDisplayControl; tell It; showAssociate consensusFrequency; endTell; endTell; ";
		}		
		if (nodeValuesAsText){
			commands += "getOwnerModule; tell It; getEmployee #NodePropertyDisplayControl; tell It; showAssociate  " + nodeValueNameRef+ "; endTell; endTell; ";
		
		}		

		return commands;
	}

	


}
