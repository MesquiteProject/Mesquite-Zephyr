package mesquite.zephyr.lib;

import mesquite.lib.MesquiteCommand;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteModule;
import mesquite.lib.Taxa;
import mesquite.lib.Tree;
import mesquite.lib.TreeVector;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.zephyr.GarliTrees.GarliTrees;

public abstract class ZephyrRunner extends MesquiteModule implements ExternalProcessRequester{
	
	public abstract Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore);
	public abstract Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore);

	public abstract void initialize (ZephyrTreeSearcher ownerModule);
	public abstract boolean initializeTaxa (Taxa taxa);


	public abstract void reconnectToRequester(MesquiteCommand command);

	
	public abstract boolean bootstrap();

}
