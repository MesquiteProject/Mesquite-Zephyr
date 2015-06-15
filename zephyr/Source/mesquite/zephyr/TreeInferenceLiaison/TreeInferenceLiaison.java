/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 

 
 Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
 The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
 Perhaps with your help we can be more than a few, and make Mesquite better.

 Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
 Mesquite's web site is http://mesquiteproject.org

 This source code and its compiled class files are free and modifiable under the terms of 
 GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.zephyr.TreeInferenceLiaison;
/*~~  */

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

/* ======================================================================== */
/* Manages an individual tree inference attempt */

public class TreeInferenceLiaison extends TreeInferenceHandler {
	
	TreeInferer inferenceTask;
	String taxaAssignedID = null;  

	
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("restartTreeSource ", inferenceTask);
		temp.addLine("reconnectTreeSource " + StringUtil.tokenize(taxaAssignedID));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Hires a tree inferer and infers trees", null, commandName, "startInference")) {
			if (getProject().getNumberTaxas()==0){
				discreetAlert("A taxa block must be created first before doing tree inference");
				return null;
			}
			inferenceTask = (TreeInferer)replaceEmployee(TreeInferer.class, arguments, "Tree inference", inferenceTask);
			startInference();
			return inferenceTask;
		}
		else if (checker.compare(this.getClass(), "Restarts to unfinished tree block filling", "[name of tree block filler module]", commandName, "restartTreeSource")) { 
			TreeInferer temp=  (TreeInferer)replaceEmployee(TreeInferer.class, arguments, "Source of trees", inferenceTask);
			if (temp!=null) {
				inferenceTask = temp;
			}
			return inferenceTask;
		}
		else if (checker.compare(this.getClass(), "Reconnects to unfinished tree block filling", "[name of tree block filler module]", commandName, "reconnectTreeSource")) { 
			TreeBlockMonitorThread thread = new TreeBlockMonitorThread(this, parser.getFirstToken(arguments), inferenceTask);
			thread.start();
		}
		else if (checker.compare(this.getClass(), "Informs the tree inference handler that trees are ready", "[ID of tree block filler module]", commandName, "treesReady")) { 
			if (inferenceTask != null){
				String taxaID = parser.getFirstToken(arguments);
				Taxa taxa = null;
				if (taxaID !=null)
					taxa = getProject().getTaxa(taxaID);
				if (taxa == null)
					taxa = getProject().getTaxa(0);
				TreeVector trees = new TreeVector(taxa); 
				inferenceTask.retrieveTreeBlock(trees, 100);
				trees.addToFile(getProject().getHomeFile(), getProject(), (TreesManager)findElementManager(TreeVector.class));
				doneQuery(inferenceTask, trees.getTaxa(), trees);
				fireTreeFiller();
				resetAllMenuBars();
				iQuit();
			}
			return null;
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*-----------------------------------------------------------------*/
	void startInference(){
		//arguments that should be accepted: (1) tree source, (2) which taxa, (3)  file id, (4) name of tree block, (5) how many trees  [number of taxa block] [identification number of file in which the tree block should be stored] [name of tree block] [how many trees to make]
		if (inferenceTask==null)  
			return;
		Taxa taxa=null;
		MesquiteFile file=getProject().getHomeFile();

		if (getProject().getNumberTaxas()==1) 
			taxa = getProject().getTaxa(0);
		else {
			ListableVector taxas = getProject().getTaxas();
			taxa = (Taxa)ListDialog.queryList(containerOfModule(), "Select taxa", "Select taxa (for tree inference)",MesquiteString.helpString, taxas, 0);
		}

 
		if (taxa==null || file == null)
			return;

		taxaAssignedID = getProject().getTaxaReferenceExternal(taxa);  
		TreeVector trees = new TreeVector(taxa);
		if (trees == null)
			return;
		int howManyTrees = 100;
		if (!inferenceTask.hasLimitedTrees(trees.getTaxa())){  //leave here for when we get Bayesian going.
			howManyTrees = MesquiteInteger.queryInteger(containerOfModule(), "How many trees?", "How many trees?", 100, 1, 100000000);
			if (!MesquiteInteger.isCombinable(howManyTrees)) {
				return;
			}
		}
		//DW: put the burden of the autosave query onto the inferenceTask, and add a method to TreeInferer to ask it if autosave
			MesquiteBoolean autoSave = new MesquiteBoolean(false);
			TreeBlockThread tLT = new TreeBlockThread(this, inferenceTask, trees, howManyTrees, autoSave, file);
			tLT.start();

	}
	/*.................................................................................................................*/
	void doneQuery(TreeInferer fillTask, Taxa taxa, TreeVector trees){
		MesquiteModule fCoord = getFileCoordinator();
		MesquiteModule treeWindowCoord = null;
		if (fCoord!=null)
			treeWindowCoord = fCoord.findEmployeeWithName("Tree Window Coordinator");
		if (treeWindowCoord==null && fCoord!=null)
			treeWindowCoord = fCoord.findEmployeeWithName("#BasicTreeWindowCoord");

		if (treeWindowCoord!=null){
			//send script to tree window coord to makeTreeWindow with set of taxa and then set to stored trees and this tree vector
			TreesManager manager = (TreesManager)findElementManager(TreeVector.class);
			int whichTreeBlock = manager.getTreeBlockNumber(taxa, trees);
			String extraWindowCommands = fillTask.getExtraTreeWindowCommands();
			if (StringUtil.blank(extraWindowCommands))
				extraWindowCommands="";
			String commands = "makeTreeWindow " + getProject().getTaxaReferenceInternal(taxa) + "  #BasicTreeWindowMaker; tell It; setTreeSource  #StoredTrees;";
			commands += " tell It; setTaxa " + getProject().getTaxaReferenceInternal(taxa) + " ;  setTreeBlock " + TreeVector.toExternal(whichTreeBlock)  + "; endTell;  getWindow; tell It; setSize 400 300; " + extraWindowCommands + " endTell; showWindowForce; endTell; ";
			MesquiteInteger pos = new MesquiteInteger(0);
			Puppeteer p = new Puppeteer(this);
			CommandRecord prev = MesquiteThread.getCurrentCommandRecord();
			CommandRecord cRec = new CommandRecord(true);
			MesquiteThread.setCurrentCommandRecord(cRec);
			p.execute(treeWindowCoord, commands, pos, null, false, null, null);
			MesquiteThread.setCurrentCommandRecord(prev);
		}
	}
	/*.................................................................................................................*/
	void fireTreeFiller(){
		fireEmployee(inferenceTask);  
		inferenceTask = null;
		taxaAssignedID = null;
}

	/*.................................................................................................................*/
	public boolean isPrerelease() { 
		return true;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Tree Inference Liaison";
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return 304;  
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Manages a tree inference." ;  
	}

}

/* ======================================================================== */
abstract class FillerThread extends MesquiteThread {
	TreeInferenceLiaison ownerModule;
	public FillerThread (TreeInferenceLiaison ownerModule) {
		super();
		this.ownerModule = ownerModule;
	}
	public abstract void stopFilling();
}

/* ======================================================================== */
/* ======================================================================== */
class TreeBlockThread extends FillerThread {
	TreeInferer inferenceTask;
	TreeVector trees;
	MesquiteFile file;
	int howManyTrees;
	CommandRecord comRec = null;
	MesquiteBoolean autoSave = null;
	boolean aborted = false;
	public TreeBlockThread (TreeInferenceLiaison ownerModule, TreeInferer fillTask, TreeVector trees, int howManyTrees, MesquiteBoolean autoSave, MesquiteFile file) {
		super(ownerModule);
		this.inferenceTask = fillTask;
		this.trees = trees;
		this.howManyTrees = howManyTrees;
		this.file = file;
		this.autoSave = autoSave;
		setCurrent(1);
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();
		boolean sc;
		if (cr == null)
			sc = false;
		else
			sc = cr.recordIsScripting();
		comRec = new CommandRecord(sc);
		setCommandRecord(comRec);

	}

	public String getCurrentCommandName(){
		return "Making trees";
	}
	public String getCurrentCommandExplanation(){
		return null;
	}
	/*.............................................*/
	public void run() {
		long s = System.currentTimeMillis();
		int before = trees.size();
		try {
			inferenceTask.fillTreeBlock(trees, howManyTrees);

			boolean okToSave = false;
			if (!ownerModule.isDoomed()){
				if (!aborted){
					if (trees.size()==before) {
						ownerModule.alert("Sorry, no trees were returned by " + inferenceTask.getName());
						ownerModule.fireTreeFiller();

					}
					else {
						trees.addToFile(file, ownerModule.getProject(), 		(TreesManager)ownerModule.findElementManager(TreeVector.class));
						okToSave = true;
					}
				}
				if (trees.size()!=before)
					ownerModule.doneQuery(inferenceTask, trees.getTaxa(), trees);
				ownerModule.fireTreeFiller();
				if (okToSave && autoSave != null && autoSave.getValue()){
					FileCoordinator fCoord = ownerModule.getFileCoordinator();
					fCoord.writeFile(file);
				}
			}
			ownerModule.resetAllMenuBars();
		}
		catch (Exception e){
			MesquiteFile.throwableToLog(this, e);
			ownerModule.alert("Sorry, there was a problem in making the tree block.  An Exception was thrown (class " + e.getClass() +"). For more details see Mesquite log file.");
		}
		catch (Error e){
			MesquiteFile.throwableToLog(this, e);
			ownerModule.alert("Sorry, there was a problem in making the tree block.  An Error was thrown (class " + e.getClass() +"). For more details see Mesquite log file.");
			throw e;
		}
		ownerModule.iQuit();
		threadGoodbye();
	}
	public void stopFilling(){
		if (inferenceTask != null)
			inferenceTask.abortFilling();
		aborted = true;
	}
	/*.............................................*/
	public void dispose(){
		ownerModule = null;
		inferenceTask = null;
		trees = null;
		file = null;
	}

}
/* ======================================================================== */

class TreeBlockMonitorThread extends FillerThread {
	TreeInferer fillTask;
	CommandRecord comRec = null;
	boolean aborted = true;
	String taxaIDString = null;
	
	public TreeBlockMonitorThread (TreeInferenceLiaison ownerModule, String taxaID, TreeInferer fillTask) {
		super(ownerModule);
		this.fillTask = fillTask;
		taxaIDString = taxaID;
		setCurrent(1);
		CommandRecord cr = MesquiteThread.getCurrentCommandRecord();
		boolean sc;
		if (cr == null)
			sc = false;
		else
			sc = cr.recordIsScripting();
		comRec = new CommandRecord(sc);
		setCommandRecord(comRec);

	}

	public String getCurrentCommandName(){
		return "Making trees";
	}
	public String getCurrentCommandExplanation(){
		return null;
	}
	/*.............................................*/
	public void run() {
		Reconnectable reconnectable = fillTask.getReconnectable();
		if (reconnectable != null){
			reconnectable.reconnectToRequester(new MesquiteCommand("treesReady", taxaIDString, ownerModule));
		}
		threadGoodbye();
	}
	public void stopFilling(){
		if (fillTask != null)
			fillTask.abortFilling();
		aborted = true;
	}
	/*.............................................*/
	public void dispose(){
		ownerModule = null;
		fillTask = null;
	}

}
