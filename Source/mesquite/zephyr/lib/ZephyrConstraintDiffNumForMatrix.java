/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;

import java.util.Random;

import mesquite.lib.CommandRecord;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteNumber;
import mesquite.lib.MesquiteString;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.tree.TreeVector;


public abstract class ZephyrConstraintDiffNumForMatrix extends ZephyrNumberForMatrix  {
	
	/*.................................................................................................................*/

	public String getExplanation() {
		return "If "+ getProgramName() + " is installed, will script "+ getProgramName() + " to conduct a search for the optimal tree constrained by a specified constraints, and the optimal unconstrained tree, and calculate the difference in their scores.";
	}
	public String getName() {
		return getProgramName() + " Difference between Constrained and Unconstrained Tree Score";
	}
	public String getNameForMenuItem() {
		return getProgramName() + " Difference between Constrained and Unconstrained Tree Score...";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/

	public void calculateNumber(MCharactersDistribution data, MesquiteNumber result, MesquiteString resultString) {
		if (taxa==null) 
			taxa=data.getTaxa();
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick(getProgramName() + " Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());

		double finalScore = 0.0;
		
		MesquiteDouble unconstrainedScore = new MesquiteDouble();
		MesquiteDouble constrainedScore = new MesquiteDouble();
		
		logln("_______________");
		logln("Calculating difference in constrained and unconstrained optimal tree scores");
		
		runner.setVerbose(false);
		
		runner.setConstrainedSearch(true);  
		runner.getTrees(trees, taxa, data, rng.nextInt(), constrainedScore, null);  // find score of constrained trees
		runner.setRunInProgress(false);
		
		
		runner.setConstrainedSearch(false);
		runner.getTrees(trees, taxa, data, rng.nextInt(), unconstrainedScore, null);   // find score of unconstrained trees
		runner.setRunInProgress(false);
		
		if (unconstrainedScore.isCombinable() && constrainedScore.isCombinable())
			finalScore = constrainedScore.getValue() - unconstrainedScore.getValue();
		
//		if (outputBuffer.length()==0)
//			outputBuffer.append("constrained\tunconstrained\tdifference");
		logln("\nResults from run:");
		logln("constrained\tunconstrained\tdifference");
		logln(""+constrainedScore.getValue()+"\t"+unconstrainedScore.getValue()+"\t"+finalScore);
		
		trees.dispose();
		trees = null;

		if (result!=null)
			result.setValue(finalScore);

		if (resultString!=null)
			resultString.setValue(""+finalScore);
	}


	/*.................................................................................................................*/


}
