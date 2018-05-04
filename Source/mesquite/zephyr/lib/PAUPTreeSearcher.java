/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

public abstract class PAUPTreeSearcher extends ZephyrTreeSearcher   {
//	Taxa taxa;
//	private MatrixSourceCoord matrixSourceTask;
//	protected MCharactersDistribution observedStates;
	
	boolean writeOnlySelectedTaxa = false;

	/*.................................................................................................................*

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		runner = (PAUPRunner)hireNamedEmployee(getRunnerClass(), getRunnerModuleName());
		if (runner ==null)
			return false;
		return true;
	}
	/*.................................................................................................................*



	public String getExtraTreeWindowCommands (){

		String commands = "setSize 400 500; getTreeDrawCoordinator #mesquite.trees.BasicTreeDrawCoordinator.BasicTreeDrawCoordinator;\ntell It; ";
		commands += "setTreeDrawer  #mesquite.trees.SquareTree.SquareTree; tell It; orientRight; ";
		commands += "setNodeLocs #mesquite.trees.NodeLocsStandard.NodeLocsStandard;";
		commands += " tell It; branchLengthsToggle on; endTell; ";
		commands += " setEdgeWidth 3; endTell; endTell;";
		return commands;
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return true;
	}
	/*.................................................................................................................*/
	public String getCitation()  {
		return "Please remember to cite the version of PAUP you used.";
	}

	/*.................................................................................................................*/
	public Class getCharacterClass() {
		return null;
	}
	/*.................................................................................................................*

	public boolean initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
			if (observedStates ==null)
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
			if (observedStates ==null)
				return false;
		} else
			return false;
		return true;
	}


	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}
	
	/*.................................................................................................................*/
	public String getTreeBlockName(boolean completedRun){
		return "";
	}

	/*.................................................................................................................*/
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick("PAUP Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());

		MesquiteDouble finalScore = new MesquiteDouble();
		
//		((PAUPRunner)runner).setPaupCommander(runner);


		runner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScore);
		runner.setRunInProgress(false);
		trees.setName(getTreeBlockName(true));  //Debugg.println  no other tree searchers do this; probably shouldn't be done here

		return trees;
	}


	/*.................................................................................................................*
	public void fillTreeBlock(TreeVector treeList){
		if (treeList==null || paupRunner==null)
			return;
		taxa = treeList.getTaxa();
		if (!initialize(taxa)) 
			return;


		if (!queryOptions())
			return;
		
		if (paupRunner!=null)
			paupRunner.setWriteOnlySelectedTaxa(writeOnlySelectedTaxa);
		boolean pleaseStorePref = false;
		if (!preferencesSet)
			pleaseStorePref = true;
		if (StringUtil.blank(PAUPPath)) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			PAUPPath = MesquiteFile.openFileDialog("Choose PAUP", directoryName, fileName);
			if (StringUtil.blank(PAUPPath))
				return;
			PAUPPath= directoryName.getValue();
			if (!PAUPPath.endsWith(MesquiteFile.fileSeparator))
				PAUPPath+=MesquiteFile.fileSeparator;
			PAUPPath+=MesquiteFile.fileSeparator+fileName.getValue();
			pleaseStorePref = true;
		}
		if (pleaseStorePref)
			storePreferences();

		TreeVector trees = getTrees(taxa);
		treeList.setName(getTreeBlockName());
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		paupRunner=null;
	}
*/


}
