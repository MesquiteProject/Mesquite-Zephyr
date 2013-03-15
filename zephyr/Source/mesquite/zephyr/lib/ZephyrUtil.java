package mesquite.zephyr.lib;

import java.awt.Choice;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import cgrb.eta.remote.api.ETAConnection;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.DNAData;
import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.ProteinData;
import mesquite.io.InterpretTNT.InterpretTNT;
import mesquite.lib.*;
import mesquite.lib.characters.CharInclusionSet;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CharacterPartition;
import mesquite.lib.characters.CharacterStates;
import mesquite.lib.characters.CharactersGroup;
import mesquite.lib.characters.CodonPositionsSet;
import mesquite.lib.duties.FileCoordinator;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.duties.TreeSource;
import mesquite.lib.duties.TreesManager;



public class ZephyrUtil {



	/*.................................................................................................................*/
	public static void adjustTree(Tree t, TaxaSelectionSet taxonSet) {
		if (t instanceof AdjustableTree) {
			if (taxonSet==null) {
				int rerootNode = t.nodeOfTaxonNumber(0);  //TODO: but not if outgroups are defined!!!
				if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) 
					((AdjustableTree)t).reroot(rerootNode, t.getRoot(), false);
				((AdjustableTree)t).standardize(null,  false);
			} else 
				((AdjustableTree)t).standardize(taxonSet,  false);
		}
	}
	
	/*.................................................................................................................*/
	//TODO: Many unused variables in method call (taxa, directoryPath, fileName)?
	public static boolean saveExportFile(MesquiteModule module, String interpreterModuleName, Taxa taxa, String directoryPath, String fileName, String path, CategoricalData data) {
		if (data==null)
			return false;

		FileCoordinator coord = module.getFileCoordinator();
		if (coord == null) 
			return false;
		

		module.incrementMenuResetSuppression();

		FileInterpreterI exporter = (FileInterpreterI)coord.findEmployeeWithName(interpreterModuleName);
		MesquiteFile file = new MesquiteFile();
		file.writeTaxaWithAllMissing = false;
		if (exporter!=null) {
			if (module instanceof ZephyrFilePreparer)
				((ZephyrFilePreparer)module).prepareExportFile(exporter);
			StringBuffer sb = exporter.getDataAsFileText(file, data);
			if (sb!=null) {
				MesquiteFile.putFileContents(path, sb.toString(), true);
				module.decrementMenuResetSuppression();

				return true;
			}
		}

		module.decrementMenuResetSuppression();
		return false;
	}	

	/*.................................................................................................................*/
	public static Tree readPhylipTree (String line, Taxa taxa, boolean permitTaxaBlockEnlarge, SimpleTaxonNamer namer) {
		if (StringUtil.blank(line))
			return null;
		MesquiteTree t = new MesquiteTree(taxa);
		t.setPermitTaxaBlockEnlargement(permitTaxaBlockEnlarge);
		t.readTree(line, namer);
		return t;
	}
	/*.................................................................................................................*/
	public  static Tree readTNTTrees(MesquiteModule module, TreeVector trees, String contents, String treeName, int firstTreeNumber, Taxa taxa, boolean firstTree) {
		FileCoordinator coord = module.getFileCoordinator();
		if (coord == null) 
			return  null;

		MesquiteModule.incrementMenuResetSuppression();

		InterpretTNT exporter = (InterpretTNT)coord.findEmployeeWithName("#InterpretTNT");
		Parser parser = new Parser();
		parser.setString(contents);
		parser.setLineEndString("*");
		String line = parser.getRawNextDarkLine();
		Tree returnTree = null;
		int count = firstTreeNumber;
		boolean foundTree = false;
		exporter.resetTreeNumber();

		if (exporter!=null) {
			while (StringUtil.notEmpty(line)) {
				MesquiteTree t = (MesquiteTree)exporter.readTREAD(null, taxa, line, firstTree, null);
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
				line = parser.getRawNextDarkLine();
			}
			MesquiteModule.decrementMenuResetSuppression();
			return returnTree;
		}
		MesquiteModule.decrementMenuResetSuppression();
		return  null;
	}	
	/*.................................................................................................................*/
	public static Tree readTNTTreeFile(MesquiteModule module, TreeVector trees, Taxa taxa, String treeFilePath, String treeName, int firstTreeNumber, MesquiteBoolean success, boolean firstTree) {
		Tree t =null;
		String contents = MesquiteFile.getFileContentsAsString(treeFilePath, -1);
		t = readTNTTrees(module, trees,contents,treeName, firstTreeNumber, taxa,firstTree);

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
	}
	/*.................................................................................................................*/
	public static int getPartitionSubset(CharacterData data, String chosen){
		if (data==null)
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
		}
		return -1;
	}
	/*.................................................................................................................*/
	public static String getStandardPartitionNEXUSCommands(CharacterData data){
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
		for (int i=0; i<parts.length; i++) {
			String q = ListableVector.getListOfMatches((Listable[])characterPartition.getProperties(), parts[i], CharacterStates.toExternal(0));
			if (q != null) {
				standardPartition +=  "\n\tcharset " + StringUtil.simplifyIfNeededForOutput(parts[i].getName(),true) + " = " + q + ";";
				numCharSets++;
				if (!StringUtil.blank(partitionCommand))
					partitionCommand += ", ";
				partitionCommand += " part"+i + ":" + StringUtil.simplifyIfNeededForOutput(parts[i].getName(),true) ;
			}
		}
		if (!StringUtil.blank(standardPartition)) {
			standardPartitionSection +=standardPartition;
			standardPartitionSection += "\n\tcharpartition * currentPartition = " + partitionCommand + ";\n";
		}	
		return standardPartitionSection;
	}
	/*.................................................................................................................*/
	public static String getCodPosPartitionNEXUSCommands(CharacterData data){
		//codon positions if nucleotide
		String codPosPartitionSection = "";
		int numberCharSets = 0;
		String charSetCommands = "";
		String partitionCommand = "";
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
				charSetCommands += "\n\tcharset " + charSetName + " = " +  locs + ";";
				if (!StringUtil.blank(partitionCommand))
					partitionCommand += ", ";
				partitionCommand += " part"+iw + ":" + charSetName;
			}
		}
		//	String codPos = ((DNAData)data).getCodonsAsNexusCharSets(numberCharSets, charSetList); // equivalent to list
		if (!StringUtil.blank(charSetCommands)) {
			codPosPartitionSection +=charSetCommands;
			codPosPartitionSection += "\n\tcharpartition * currentPartition = " + partitionCommand + ";\n";
		}	
		return codPosPartitionSection;
	}

	/*.................................................................................................................*/

	public static  String getNEXUSSetsBlock(CategoricalData data, boolean useCodPosIfAvailable){
		StringBuffer sb = new StringBuffer();
		
		String partitions = "";
		if (useCodPosIfAvailable && data instanceof DNAData && ((DNAData)data).someCoding())
			partitions = getCodPosPartitionNEXUSCommands(data);
		else
			partitions = getStandardPartitionNEXUSCommands(data);
		
		if (StringUtil.notEmpty(partitions)) {
			sb.append("begin sets;\n");
			sb.append(partitions);
			sb.append("end;\n");
		}
		return sb.toString();
	}

	/*.................................................................................................................*/
	public static void writeNEXUSFile(Taxa taxa, String dir, String fileName, String path, CategoricalData data, boolean useStandardizedTaxonNames, boolean writeOnlySelectedTaxa, boolean writeSetsBlock, boolean useCodPosIfAvailable) {
		if (path != null) {
			MesquiteFile f = MesquiteFile.newFile(dir, fileName);
			f.openWriting(true);
			f.interleaveAllowed=false;
			f.useSimplifiedNexus=true;
			f.useDataBlocks=true;
			f.useStandardizedTaxonNames=useStandardizedTaxonNames;
			f.writeExcludedCharacters=false;
			f.writeTaxaWithAllMissing = false;
			f.writeOnlySelectedTaxa = writeOnlySelectedTaxa;
			f.writeLine("#NEXUS" + StringUtil.lineEnding());
			data.getMatrixManager().writeCharactersBlock(data, null, f, null);
			String setsBlock = getNEXUSSetsBlock(data,useCodPosIfAvailable);
			if (StringUtil.notEmpty(setsBlock))
				f.writeLine(setsBlock + StringUtil.lineEnding());

			//data.getMatrixManager().writeCharactersBlock(data, cB, file, progIndicator)
			f.closeWriting();
		}
	}

	/*.................................................................................................................*/
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

		String logFilePath = ownerModule.getProject().getHomeDirectoryName();
		if (!StringUtil.blank(logFilePath))
			logFilePath += MesquiteFile.getAvailableFileName(logFilePath, programName,".log");
		else
			logFilePath = MesquiteFile.saveFileAsDialog("Save copy of "+programName+" log to file");
		if (!StringUtil.blank(logFilePath)) {
			MesquiteFile.copyFileFromPaths(originalLogFilePath, logFilePath, true);
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

	/*.................................................................................................................*/
	public static String createDirectoryForFiles(MesquiteModule module, boolean askForLocation, String name) {
		MesquiteBoolean directoryCreated = new MesquiteBoolean(false);
		String rootDir = null;
		if (!askForLocation)
			rootDir = module.createEmptySupportDirectory(directoryCreated) + MesquiteFile.fileSeparator;  //replace this with current directory of file
		if (!directoryCreated.getValue()) {
			rootDir = MesquiteFile.chooseDirectory("Choose folder for storing "+name+" files");
			if (rootDir==null) {
				MesquiteMessage.discreetNotifyUser("Sorry, directory for storing "+name+" files could not be created.");
				return null;
			} else
				rootDir += MesquiteFile.fileSeparator;
		}
		return rootDir;
	}


}
