package mesquite.zephyr.SumTreesNumbersForNodes;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.SumTreesRunner.SumTreesRunner;

public class SumTreesNumbersForNodes extends NumbersForNodes {
    /* ................................................................................................................. */
	SumTreesRunner sumTreesRunner;
    public boolean startJob(String arguments, Object condition, boolean hiredByName) {

		
		sumTreesRunner = (SumTreesRunner)hireNamedEmployee(SumTreesRunner.class, "#mesquite.bosque.SumTreesRunner.SumTreesRunner");
		if (sumTreesRunner == null)
			return false;
		return true;
    }

	/*.................................................................................................................*/
  	 public boolean isPrerelease(){
  	 	return false;
  	 }

    /* ................................................................................................................. */
	public void calculateNumbers(Tree tree, NumberArray result, MesquiteString resultString) {
		this.initialize(tree);
		if (result == null || tree == null)
			return;
		clearResultAndLastResult(result);
		try {
			sumTreesRunner.findSupportForSplitsInTree(tree);
		} catch (Exception e) {
			; // pass
		}
		saveLastResult(result);
		saveLastResultString(resultString);
	}

  	/*.................................................................................................................*/
   	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
   	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
   	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
      	public int getVersionOfFirstRelease(){
      		return NEXTRELEASE;  
      	}
  /* ................................................................................................................. */
    /** Explains what the module does. */

    public String getExplanation() {
        return "Adds support values to nodes based on the number of times a split occurs in a set of trees";
    }

    /* ................................................................................................................. */
    /** Name of module */
    public String getName() {
        return "SumTrees support values";
    }

	@Override
	public void initialize(Tree tree) {
		sumTreesRunner.initialize(this, tree.getTaxa());
	}


}
