package mesquite.zephyr.ScoreDiffParamBoot;

import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteTree;
import mesquite.lib.Tree;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.NumberForMatrixAndTree;

public class ScoreDiffParamBoot extends NumberForMatrixAndTree {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return false;
	}

	public void initialize(Tree tree, MCharactersDistribution matrix) {
	}

	public void calculateNumber(Tree tree, MCharactersDistribution matrix, MesquiteNumber result, MesquiteString resultString) {
		if (result==null || tree == null || matrix == null)
			return;
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
	public String getName() {
		return "Parametric Bootstrap Score Difference";
	}

}
