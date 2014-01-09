package mesquite.zephyr.RAxMLTrees;

import java.util.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.GarliRunner.GarliRunner;
import mesquite.zephyr.RAxMLRunner.RAxMLRunner;
import mesquite.zephyr.lib.*;


public class RAxMLTrees extends ZephyrTreeSearcher {
	int rerootNode = 0;


	public String eachTreeCommands (){
		String commands="";
		if (rerootNode>0 && MesquiteInteger.isCombinable(rerootNode)) {
			commands += " rootAlongBranch " + rerootNode + "; ";
		}
		commands += " ladderize root; ";
		return commands;
	}

	
	/*.................................................................................................................*/
	public String getRunnerModuleName() {
		return "#mesquite.zephyr.RAxMLRunner.RAxMLRunner";
	}
	/*.................................................................................................................*/
	public Class getRunnerClass() {
		return RAxMLRunner.class;
	}

	/*.................................................................................................................*/
	public String getProgramName() {
		return "RAxML";
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

	public void newTreeAvailable(String path, TaxaSelectionSet outgroupTaxSet){
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();  		
		MesquiteThread.setCurrentCommandRecord(new CommandRecord(true));
		latestTree = null;

		String s = MesquiteFile.getFileLastDarkLine(path);
		SimpleTaxonNamer namer = new SimpleTaxonNamer();
		latestTree = ZephyrUtil.readPhylipTree(s,taxa,false,namer);

		if (latestTree!=null && latestTree.isValid()) {
			rerootNode = latestTree.nodeOfTaxonNumber(0);
		}

		MesquiteThread.setCurrentCommandRecord(cr);
		//Wayne: get tree here from file
		if (latestTree!=null && latestTree.isValid()) {
			newResultsAvailable(outgroupTaxSet);
		}

	}



}
