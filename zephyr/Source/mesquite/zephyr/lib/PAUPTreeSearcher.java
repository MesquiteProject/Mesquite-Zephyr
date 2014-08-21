/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

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
import mesquite.zephyr.PAUPRunner.*;

public abstract class PAUPTreeSearcher extends ExternalTreeSearcher implements ActionListener, PAUPCommander {
	PAUPRunner paupRunner;
	Taxa taxa;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	String PAUPPath;
	SingleLineTextField PAUPPathField =  null;
	boolean preferencesSet = false;
	
	boolean writeOnlySelectedTaxa = false;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");


		paupRunner = (PAUPRunner)hireNamedEmployee(PAUPRunner.class, "#mesquite.zephyr.PAUPRunner.PAUPRunner");
		if (paupRunner ==null)
			return false;
		paupRunner.setPAUPPath(PAUPPath);
		return true;
	}

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
		return "Please remember to cite the version of PAUP* you used.";
	}

	/*.................................................................................................................*/
	public Class getCharacterClass() {
		return null;
	}

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
		if (paupRunner !=null)
			paupRunner.setPAUPPath(PAUPPath);
		else return false;
		return true;
	}

	public boolean getPreferencesSet() {
		return preferencesSet;
	}
	public void setPreferencesSet(boolean b) {
		preferencesSet = b;
	}
	/*.................................................................................................................*/
	public void processMorePreferences (String tag, String content) {
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("PAUPPath".equalsIgnoreCase(tag)) 
			PAUPPath = StringUtil.cleanXMLEscapeCharacters(content);
		processMorePreferences(tag, content);
		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		return "";
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "PAUPPath", PAUPPath);  
		buffer.append(prepareMorePreferencesForXML());

		preferencesSet = true;
		return buffer.toString();
	}

	public void queryOptionsSetup(ExtensibleDialog dialog) {
	}
	/*.................................................................................................................*/
	public void queryOptionsProcess(ExtensibleDialog dialog) {
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options and Location");
		String helpString = "This module will prepare a matrix for PAUP, and ask PAUP do to an analysis.  A command-line version of PAUP must be installed. ";

		dialog.appendToHelpString(helpString);

		queryOptionsSetup(dialog);

		dialog.addHorizontalLine(1);
		PAUPPathField = dialog.addTextField("Path to PAUP:", PAUPPath, 40);
		Button PAUPBrowseButton = dialog.addAListenedButton("Browse...",null, this);
		PAUPBrowseButton.setActionCommand("PAUPBrowse");
		
		Checkbox selectedOnlyBox = dialog.addCheckBox("consider only selected taxa", writeOnlySelectedTaxa);

		//TextArea PAUPOptionsField = queryFilesDialog.addTextArea(PAUPOptions, 20);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			PAUPPath = PAUPPathField.getText();
			queryOptionsProcess(dialog);
			storePreferences();
			writeOnlySelectedTaxa = selectedOnlyBox.getState();


		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	public  void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("PAUPBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			PAUPPath = MesquiteFile.openFileDialog("Choose PAUP", directoryName, fileName);
			if (StringUtil.notEmpty(PAUPPath))
				PAUPPathField.setText(PAUPPath);
		}
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}
	/*.................................................................................................................*/
	private TreeVector getTrees(Taxa taxa) {
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick("PAUP Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());
		paupRunner.setPAUPPath(PAUPPath);

		MesquiteDouble finalScore = new MesquiteDouble();

		paupRunner.getTrees(trees, taxa, observedStates, rng.nextInt(), finalScore, getName(), this);

		return trees;
	}

	/*.................................................................................................................*/
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
		treeList.setName("Trees from PAUP search");
		treeList.setAnnotation ("Parameters: "  + getParameters(), false);
		if (trees!=null)
			treeList.addElements(trees, false);
		paupRunner=null;
	}



}
