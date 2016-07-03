/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.ScoreDiffParamBoot;

import java.awt.Checkbox;

import mesquite.lib.*;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.*;

public class ScoreDiffParamBoot extends NumberForMatrix {
	OneTreeSource constraintTreeSource;
	Tree constraintTree=null;
	MesquiteBoolean constrained = new MesquiteBoolean(false);
	

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		constraintTreeSource = (OneTreeSource)hireEmployee(OneTreeSource.class, "Source of Constraint Tree");
		if(constraintTreeSource==null){
			return sorry(getName() + " couldn't start because no constraint tree source was obtained.");
		}
		loadPreferences();
		if (!MesquiteThread.isScripting()) 
			if (!queryOptions()) 
				return false;
		return true;
	}
	public void initialize(MCharactersDistribution data) {
		// TODO Auto-generated method stub
		
	}

	/*.................................................................................................................*/
	public void processMorePreferences (String tag, String content) {
		if ("constrained".equalsIgnoreCase(tag))
			constrained.setFromTrueFalseString(content);
	}
	/*.................................................................................................................*/
	public String prepareMorePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "constrained", constrained);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("constrained "+ constrained.toOffOnString()); 
		return temp;
	}
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");

		Checkbox exactMatchCheck = dialog.addCheckBox("Topology constrained", constrained.getValue());

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			constrained.setValue(exactMatchCheck.getState());
			storePreferences();

		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	
	public void initialize(Tree tree, MCharactersDistribution matrix) {
	}

	/*.................................................................................................................*/

	
	public void calculateNumber(MCharactersDistribution matrix, MesquiteNumber result, MesquiteString resultString) {
		if (result==null || matrix == null)
			return;
		if (constrained.getValue() && constraintTree==null) {
			if (constraintTreeSource == null)
				return;
			Tree sourceTree = constraintTreeSource.getTree(matrix.getTaxa(), "This is the constraint tree");
			if (sourceTree==null)
				return;
			constraintTree = sourceTree.cloneTree();
			if(constraintTree==null || constraintTree.getTaxa()!=matrix.getTaxa())
				return;
		}
		
		if (resultString !=null)
			resultString.setValue("");
	   	clearResultAndLastResult(result);
	   	
	   	
	   	
	   	
	   	

		result.setValue(0);

		//(minimum)/(steps)
		saveLastResult(result);
		if (resultString != null)
			resultString.setValue("Difference between scores.: " + result);
		saveLastResultString(resultString);
	}

	public void employeeQuit(MesquiteModule m){
		iQuit();
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets whether or not exact matching should be used", "[on off]", commandName, "constrained")) {
			boolean oldValue = constrained.getValue();
			constrained.toggleValue(arguments);
			if (oldValue != constrained.getValue()) {
				parametersChanged();
			}
			return constrained;
		}
		else
			return  super.doCommand(commandName, arguments, checker);
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "";
	}
	/*.................................................................................................................*/
	public boolean loadModule() {
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Parametric Bootstrap Score Difference";
	}


}
