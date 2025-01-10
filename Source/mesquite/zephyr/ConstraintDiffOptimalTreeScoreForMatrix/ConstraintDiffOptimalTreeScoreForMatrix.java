/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.ConstraintDiffOptimalTreeScoreForMatrix;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.lib.tree.TreeVector;
import mesquite.zephyr.lib.*;
import mesquite.zephyr.OptimalTreeScoreForMatrix.*;


public class ConstraintDiffOptimalTreeScoreForMatrix extends OptimalTreeScoreForMatrix  {
	
	/*.................................................................................................................*/

	public String getExplanation() {
		return "Calculates difference in optimal constrained and unconstrained tree score for a matrix";
	}
	public String getName() {
		return "Difference in Optimal Tree Score for Matrix";
	}
	public String getNameForMenuItem() {
		return "Difference in Optimal Tree Score for Matrix...";
	}
	/*.................................................................................................................*/
	public boolean loadModule(){
		return false;
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.................................................................................................................*/

	public void calculateNumber(MCharactersDistribution data, MesquiteNumber result, MesquiteString resultString) {
		if (taxa==null) 
			taxa=data.getTaxa();
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick(runner.getProgramName() + " Tree Search in progress " );

		Random rng = new Random(System.currentTimeMillis());

		double finalScore = 0.0;
		
		MesquiteDouble unconstrainedScore = new MesquiteDouble();
		MesquiteDouble constrainedScore = new MesquiteDouble();
		
		logln("_______________");
		logln("Calculating difference in constrained and unconstrained optimal tree scores");
		
		runner.setVerbose(false);
		
		runner.setConstainedSearchAllowed(true);
		runner.setConstrainedSearch(true);  
		runner.getTrees(trees, taxa, data, rng.nextInt(), constrainedScore);  // find score of constrained trees
		runner.setRunInProgress(false);
		
		
		runner.setConstainedSearchAllowed(false);
		runner.setConstrainedSearch(false);
		runner.getTrees(trees, taxa, data, rng.nextInt(), unconstrainedScore);   // find score of unconstrained trees
		runner.setRunInProgress(false);
		
		runner.setVerbose(true);

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
