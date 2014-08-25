/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.ImportRAxMLTrees;

import mesquite.io.lib.*;
import mesquite.lib.*;
import mesquite.lib.duties.TaxaManager;
import mesquite.lib.duties.TreesManager;

public class ImportRAxMLTrees extends InterpretPhylipTrees {
	
	String infoFileName = "RAxML_info.output";
	/*.................................................................................................................*/
	public boolean initializeTreeImport(MesquiteFile file, Taxa taxa) {  
		 String translationFile = null;
		 String directoryPath = file.getDirectoryName();
		 String translationTablePath = directoryPath+IOUtil.translationTableFileName;
		 translationFile = MesquiteFile.getFileContentsAsString(translationTablePath);
		 if (StringUtil.notEmpty(translationFile)){
			 taxonNamer = new SimpleTaxonNamer();
			 ((SimpleTaxonNamer)taxonNamer).loadTranslationTable(taxa, translationFile);
		 }
		 else 
			 taxonNamer = null;
		 return true;
	}
	/*.................................................................................................................*/
	public boolean importExtraFiles(MesquiteFile file, Taxa taxa, TreeVector trees) {  
		String directoryPath = file.getDirectoryName();
		MesquiteString directoryName= new MesquiteString(directoryPath);
		MesquiteString fileName= new MesquiteString("infoFileName");
		String filePath = MesquiteFile.openFileDialog("Choose RAxML Info File...", directoryName, fileName);
		String summary = MesquiteFile.getFileContentsAsString(filePath);
		if (StringUtil.notEmpty(summary)) {
			DoubleArray finalValues = new DoubleArray(trees.size());
			DoubleArray optimizedValues = new DoubleArray(trees.size());
			IOUtil.readRAxMLInfoFile(this, summary, true, trees, finalValues, optimizedValues);
		}
		return true;
	}
	/*.................................................................................................................*/
	public void readTreeFile(MesquiteProject mf, MesquiteFile file, String arguments) {
		boolean enlargeTaxaBlock = false;
		Taxa taxa = getProject().chooseTaxa(containerOfModule(), "From what taxa are these trees composed?");
		if (taxa== null) {
			TaxaManager taxaTask = (TaxaManager)findElementManager(Taxa.class);
			taxa = taxaTask.makeNewTaxa("Taxa", 0, false);
			taxa.addToFile(file, getProject(), taxaTask);
			enlargeTaxaBlock = true;
		}
		incrementMenuResetSuppression();
		initializeTreeImport(file, taxa);
		String fileNameBase = StringUtil.getAllButLastItem(file.getName(), ".");
		int count=0;
		MesquiteFile treeFile=null;
		TreeVector trees = new TreeVector(taxa);

		while (MesquiteFile.fileExists(file.getDirectoryName(), fileNameBase+"."+count))  {
			treeFile = MesquiteFile.open(file.getDirectoryName(), fileNameBase+"."+count);
			if (treeFile.openReading()) {

				MesquiteMessage.println("Reading file " + treeFile.getName());
				TreeVector newTrees = IOUtil.readPhylipTrees(this,mf, treeFile, null, null, taxa, enlargeTaxaBlock, taxonNamer,getTreeNameBase(), false);
				trees.addElements(newTrees, false);
				if (trees != null && count==0){
					trees.setName("RAxML Trees");
					trees.addToFile(taxa.getFile(),mf,(TreesManager)this.findElementManager(TreeVector.class));
				}

				if (treeFile!=null) 
					treeFile.closeReading();
			}
			count++;

		}

		importExtraFiles(file,taxa, trees);
		finishImport(null, file, false );

		decrementMenuResetSuppression();
	}


	public String getTreeNameBase () {
		return "RAxML tree ";
	}

	/*.................................................................................................................*/
	public String getName() {
		return "RAxML Results Import";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Imports RAxML trees and associated data." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -100;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
	}
	
	//TODO: read in translation table file if it exists.

}
