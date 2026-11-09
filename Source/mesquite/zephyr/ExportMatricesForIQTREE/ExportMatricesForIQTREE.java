/* Mesquite Chromaseq source code.  Copyright 2005-2011 David Maddison and Wayne Maddison.
Version 1.0   December 2011
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.ExportMatricesForIQTREE;
/*~~  */

import java.awt.Checkbox;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.MolecularState;
import mesquite.lib.Arguments;
import mesquite.lib.ExporterDialog;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteProject;
import mesquite.lib.MesquiteStringBuffer;
import mesquite.lib.MesquiteThread;
import mesquite.lib.NEXUSFileUtil;
import mesquite.lib.Parser;
import mesquite.lib.StringUtil;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.FileInterpreterI;
import mesquite.lib.duties.OneTreeSource;
import mesquite.lib.misc.VoucherInfoFromOTUIDDB;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.tree.Tree;
import mesquite.lib.ui.RadioButtons;
import mesquite.lib.ui.SingleLineTextField;


/* ============  a file interpreter for DNA/RNA  Fasta files ============*/

public class ExportMatricesForIQTREE extends FileInterpreterI {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		//	EmployeeNeed e = registerEmployeeNeed(VoucherInfoCoord.class, "Voucher information is needed for FASTA export for GenBank submissions.",
		//			"This is activated automatically when you choose this exporter.");
	}
	//VoucherInfoCoord voucherInfoTask;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		//voucherInfoTask = (VoucherInfoCoord)hireEmployee(VoucherInfoCoord.class, null);
		return true;
	}
	public void readFile(MesquiteProject mf, MesquiteFile mNF, String arguments) {

	}

	/*.................................................................................................................*/
	public boolean requestPrimaryChoice() {  
		return true;  //
	}
	/*.................................................................................................................*/
	public boolean canImport() {  
		return false;  //
	}
	public boolean canImport(String arguments){
		return false;
	}
	/** returns whether module is able ever to export.*/
	public boolean canExportEver(){
		return true;
	}
	/** returns whether module has something it can export in the project.  Should be overridden*/
	public boolean canExportProject(MesquiteProject project){
		return project.getNumberCharMatrices(MolecularState.class) > 0;  //
	}

	/** returns whether module can export a character data matrix of the given type.  Should be overridden*/
	public boolean canExportData(Class dataClass){
		if (dataClass==null) return false;
		return ((MolecularState.class).isAssignableFrom(dataClass)); 
	}

	protected int taxonNameLengthLimit() {
		return 50;
	}

	/* ============================  exporting ============================*/
	/*.................................................................................................................*/
	boolean convertAmbiguities = false;
	boolean useData = true;
	//	String addendum = "";
	//	String fileName = "untitled.nex";
	//	boolean permitMixed = false;
	//	boolean generateMBBlock = true;
	//	boolean simplifyNames = false;
	//	boolean removeExcluded = false;


	//	protected boolean buildFileName = false;
	//	protected boolean includeTaxonName = true;
	//	protected String voucherPrefix = "DNA";
	//	String voucherSuffix = "";
	//	protected boolean includeGeneNameInFastaHeader = false;

	boolean useGuideTree = false;
	Tree guideTree=null;


	public boolean getExportOptions(boolean dataSelected, boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExporterDialog exportDialog = new ExporterDialog(this,containerOfModule(), "Export Multiple Matrices for IQTREE", buttonPressed);
		exportDialog.setSuppressLineEndQuery(true);
		exportDialog.setDefaultButton(null);
		exportDialog.addLabel("Saving matrices as separate files");

		Checkbox useGuideTreeBox= exportDialog.addCheckBox("use guide tree", useGuideTree);

		exportDialog.completeAndShowDialog(dataSelected, taxaSelected);

		boolean ok = (exportDialog.query(dataSelected, taxaSelected)==0);

		//		convertAmbiguities = convertToMissing.getState();
		if (ok) {
			useGuideTree = useGuideTreeBox.getState();
		}

		exportDialog.dispose();
		return ok;
	}	
	/*.................................................................................................................*
	public String getFileName(Taxa taxa, int it, CharacterData data, int index, String voucherID, String identifierString) {
		String fileName = "";
		boolean prefixWithID = buildFileName && StringUtil.notEmpty(voucherID);
		if (prefixWithID)
			fileName=StringUtil.cleanseStringOfFancyChars(voucherPrefix+voucherID,false,true);
		else 
			fileName=StringUtil.cleanseStringOfFancyChars(taxa.getName(it),false,true);

		String s = ChromaseqUtil.getFragmentName(data, index);
		if (StringUtil.notEmpty(s)) 
			fileName += "_"+StringUtil.cleanseStringOfFancyChars(s,false,true);
		else
			fileName += "_"+StringUtil.cleanseStringOfFancyChars(data.getName(),false,true);

		if (buildFileName) {
			if (includeTaxonName && prefixWithID)
				fileName+="_"+StringUtil.cleanseStringOfFancyChars(taxa.getName(it),false,true);
			if (StringUtil.notEmpty(voucherSuffix))
				fileName+="_"+StringUtil.cleanseStringOfFancyChars(voucherSuffix,false,true);
		}

		fileName += ".fas";

		return fileName;
	}

	/*.................................................................................................................*
	public String getTitleLineForTabbedFile() {
		return "";
	}

	/*.................................................................................................................*
	public String getLineForTabbedFile(Taxa taxa, int it, CharacterData data, int index, String voucherID, String identifierString) {
		return "";
	}

	/*.................................................................................................................*

	private void putFastaAsFile(Taxa taxa, int it, CharacterData data, int index, String directory, String fasta, String voucherID, String identifierString) {
		String filePath = directory;
		filePath = directory+getFileName(taxa, it, data, index, voucherID, identifierString);
		if (filePath == null)
			return;
		MesquiteFile.putFileContents(filePath, fasta, true);
	}
	/*.................................................................................................................*
	public String getSequenceName(Taxa taxa, int it, CharacterData data, String voucherID) {
		String s = taxa.getTaxonName(it);
		if (includeGeneNameInFastaHeader)
			s=data.getName()+": " + s;
		return s;
	}
	/*.................................................................................................................*
	public String getIdentifierString() {
		return "";
	}


	/*.................................................................................................................*/
	protected OneTreeSource guideTreeTask = null;
	/*.................................................................................................................*/
	protected OneTreeSource getGuideTreeSource(){
		if (guideTreeTask == null){
			guideTreeTask = (OneTreeSource)hireEmployee(OneTreeSource.class, "Source of guide tree");
		}
		return guideTreeTask;
	}
	/*.................................................................................................................*/
	public synchronized boolean exportFile(MesquiteFile file, String arguments) { //if file is null, consider whole project open to export
		Arguments args = new Arguments(new Parser(arguments), true);
		//		boolean usePrevious = args.parameterExists("usePrevious");

		if (!MesquiteThread.isScripting())
			if (!getExportOptions(false, true))
				return false;

		String directory = MesquiteFile.chooseDirectory("Choose directory into which files will be saved:");
		if (StringUtil.blank(directory))
			return false;
		if (!directory.endsWith(MesquiteFile.fileSeparator))
			directory+=MesquiteFile.fileSeparator;


		StringBuffer listOfFilesBuffer = new StringBuffer(0);
		StringBuffer commandBuffer = new StringBuffer(0);
		int count = 0;
		
		String guideTreeDescription ="";
		boolean guideTreeAcceptable = useGuideTree;

		Taxa taxa = getProject().chooseTaxa(containerOfModule(), "What block of taxa?");
		if (useGuideTree){
			if (guideTree==null) { // we don't have one
				getGuideTreeSource();
				if (guideTreeTask != null){
					guideTree = guideTreeTask.getTree(taxa, "This will be the guide tree");
				}
			}
			if (guideTree == null){
				discreetAlert("Guide tree is not available.");
				guideTreeAcceptable = false;
			}
			else if (guideTree.hasBranchLengths()){
				guideTreeDescription = guideTree.writeTreeByT0Names(true) + ";";
			} else {
				discreetAlert("Guide tree must have branch lengths specified.");
				guideTreeAcceptable = false;
			}

				
		}

		int numMatrices = getProject().getNumberCharMatrices(null, taxa, MolecularState.class, true);
		for (int iM = 0; iM < numMatrices; iM++){
			CharacterData data = getProject().getCharacterMatrixVisible(taxa, iM, MolecularState.class);
			if (data != null && data instanceof CategoricalData) {
				String dataFileRoot = StringUtil.cleanseStringOfFancyChars(data.getName(), false, true);
				String dataFileName = StringUtil.cleanseStringOfFancyChars(data.getName(), false, true) + ".nex";
				listOfFilesBuffer.append(dataFileName + "\n");
				if (guideTreeAcceptable)
					commandBuffer.append("iqtree -s " + dataFileName +" -ft guideTree.txt" + " -m MFP -pre " + dataFileRoot + "\n");
				else 
					commandBuffer.append("iqtree -s " + dataFileName + " -m MFP =pre " + dataFileRoot + "\n");
				String dataFilePath = directory + dataFileName;
				NEXUSFileUtil.writeNEXUSFile(taxa, directory, dataFileName, dataFilePath, (CategoricalData)data, true, true, writeOnlySelectedTaxa, false, false, false, false);
				count++;
			}
		}


		if (StringUtil.notEmpty(guideTreeDescription))
			MesquiteFile.putFileContents(directory+"guideTree.txt", guideTreeDescription, true);

		
			
		if (listOfFilesBuffer.length()>0)
			MesquiteFile.putFileContents(directory+"fileList.txt", listOfFilesBuffer.toString(), true);
		if (commandBuffer.length()>0)
			MesquiteFile.putFileContents(directory+"commands.txt", commandBuffer.toString(), true);

		logln(""+ count + " NEXUS files saved");

		return true;
	}


	/*.................................................................................................................*/
	public String getName() {
		return "NEXUS Files of Multiple Matrices for IQTREE";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports one NEXUS file for each matrix, as well as various associated IQTREE files." ;
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}


}
