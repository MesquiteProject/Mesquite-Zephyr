/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.TNTTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.TNTRunner.TNTRunner;
import mesquite.zephyr.lib.*;


public class TNTTrees extends ZephyrTreeSearcher {
	TNTRunner tntRunner;
	TreeSource treeRecoveryTask;
	Taxa taxa;
	private MatrixSourceCoord matrixSourceTask;
	protected MCharactersDistribution observedStates;
	int rerootNode = 0;


	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.TNTRunner.TNTRunner";
	}
	/*.................................................................................................................*/
	public String getProgramName() {
		return "TNT";
	}

	/*.................................................................................................................*/
	 public String getProgramURL() {
		 return "http://www.lillo.org.ar/phylogeny/tnt/";
	 }

	 public Class getRunnerClass(){
		 return TNTRunner.class;
	 }
	 /*.................................................................................................................*/
	 public String getCitation() {
		 return "Maddison DR and Maddison KW. 2014.  TNT Tree Searcher, in " + getPackageIntroModule().getPackageCitation();
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
	public boolean canGiveIntermediateResults(){
		return true;
	}
	/*.................................................................................................................*/

	public String eachTreeCommands (){
		String commands="";
		if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";

		return commands;
	}

	/*.................................................................................................................*/
	Tree latestTree = null;
	/*.................................................................................................................*/

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastContents(path);


		latestTree = ZephyrUtil.readTNTTrees(this, null,s,"TNTTree", 0, taxa,true, false, null);

		if (latestTree!=null && latestTree.isValid()) {
			rerootNode = latestTree.nodeOfTaxonNumber(1);
			if (outgroupTaxSet!=null) {
				int firstOutgroup = outgroupTaxSet.firstBitOn();
				if (MesquiteInteger.isCombinable(firstOutgroup) && firstOutgroup>=0)
					rerootNode = latestTree.nodeOfTaxonNumber(firstOutgroup+1);
			}
			//logln(latestTree.getName());
		}

		MesquiteThread.setCurrentCommandRecord(cr);
		//Wayne: get tree here from file
		if (latestTree!=null && latestTree.isValid())
			newResultsAvailable(outgroupTaxSet);

	}
	/*.................................................................................................................*/


}
