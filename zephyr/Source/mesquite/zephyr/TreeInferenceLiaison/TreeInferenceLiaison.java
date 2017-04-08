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

import java.util.Vector;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;

/* ======================================================================== */
/* Manages an individual tree inference attempt */

public class TreeInferenceLiaison extends TreeInferenceHandler {

	TreeInferer inferenceTask;
	String taxaAssignedID = null;  
	FillerThread inferenceThread = null;
	Taxa taxa = null;
	static int maximumLogLines = 20;
	
	//MesquiteBoolean autoSave = new MesquiteBoolean(true);

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	public String getHTMLDescriptionOfStatus(int numLines){
		if (inferenceTask == null)
			return "No inference";
		String s = inferenceTask.getHTMLDescriptionOfStatus();
		s += "<hr>";
		if (inferenceLogger != null){
			s += "<p>" + inferenceLogger.getStrings(maximumLogLines);
			s += "<hr>";
		}
		return s;
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("setTreeSource ", inferenceTask);
		temp.addLine("reconnectToTreeSource " + StringUtil.tokenize(taxaAssignedID));
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
		else if (checker.compare(this.getClass(), "Restarts to unfinished tree block filling", "[name of tree block filler module]", commandName, "setTreeSource")) { 
			TreeInferer temp=  (TreeInferer)replaceEmployee(TreeInferer.class, arguments, "Source of trees", inferenceTask);
			if (temp!=null) {
				inferenceTask = temp;
			}
			return inferenceTask;
		}
		else if (checker.compare(this.getClass(), "Reconnects to unfinished tree block filling", "[name of tree block filler module]", commandName, "reconnectToTreeSource")) { 
			inferenceThread = new TreeBlockMonitorThread(this, parser.getFirstToken(arguments), inferenceTask);
			inferenceThread.start();
		}
		else if (checker.compare(this.getClass(), "Informs the tree inference handler that trees are ready", "[ID of tree block filler module]", commandName, "treesReady")) { 
			if (inferenceTask != null){
				String taxaID = parser.getFirstToken(arguments);
				taxa = null;
				if (taxaID !=null)
					taxa = getProject().getTaxa(taxaID);
				if (taxa == null)
					taxa = getProject().getTaxa(0);
				TreeVector trees = new TreeVector(taxa); 
				
			//	boolean okToSave = false;

				MesquiteThread.setHintToSuppressProgressIndicatorCurrentThread(true);
				inferenceTask.retrieveTreeBlock(trees, 100);
				if (trees.size() >0){
					trees.addToFile(getProject().getHomeFile(), getProject(), (TreesManager)findElementManager(TreeVector.class));
			//		okToSave = true;
					saveAndPresentTrees(inferenceTask, trees.getTaxa(), trees);
				}
				if (taxa != null)
					taxa.decrementEditInhibition();
				MesquiteThread.setHintToSuppressProgressIndicatorCurrentThread(false);
				fireTreeFiller();
			/*	if (okToSave && autoSave != null && autoSave.getValue()){
					FileCoordinator fCoord = getFileCoordinator();
					fCoord.writeFile(getProject().getHomeFile());
				}
				*/

				resetAllMenuBars();
				iQuit();
			}
			return null;
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}
	InferenceLogger inferenceLogger;
	Logger getLogger(){
		if (inferenceLogger == null)
			inferenceLogger= new InferenceLogger(this);
		return inferenceLogger;
	}
	void stringLogged(){
		parametersChanged();
	}
	void setTaxa(String id){
		String taxaID = parser.getFirstToken(id);
		taxa = null;
		if (taxaID !=null)
			taxa = getProject().getTaxa(taxaID);
		if (taxa == null)
			taxa = getProject().getTaxa(0);
	}
	/*public boolean storeLatestTree(){
	Tree latestTree = getLatestTree(null, null, null);
	MesquiteProject proj = getProject();
	TreesManager manager = (TreesManager)findElementManager(TreeVector.class);
	TreeVector trees = manager.makeNewTreeBlock(latestTree.getTaxa(), "latest tree", proj.getHomeFile());
	trees.addElement(latestTree, true);
	return false;
}
*/
	/*-----------------------------------------------------------------*/

	public boolean storeLatestTree(){
		if (inferenceTask==null)
			return false;
		Tree latestTree = inferenceTask.getLatestTree(null, null, null);
		if (latestTree==null) 
			return false;
		MesquiteProject proj = getProject();
		if (proj==null)
			return false;
		TreesManager manager = (TreesManager)findElementManager(TreeVector.class);
		TreeVector trees = manager.makeNewTreeBlock(latestTree.getTaxa(), inferenceTask.getTreeBlockName(false), proj.getHomeFile());
		trees.addElement(latestTree, true);
		if (trees.size() >0){
			trees.addToFile(getProject().getHomeFile(), getProject(), (TreesManager)findElementManager(TreeVector.class));
			saveAndPresentTrees(inferenceTask, latestTree.getTaxa(),  trees);
		}
		return true;
	}
	public boolean canStoreLatestTree(){
		if (inferenceTask!=null)
			return inferenceTask.canStoreLatestTree();
		else return false;
	}
	/*-----------------------------------------------------------------*/
	void startInference(){
		//arguments that should be accepted: (1) tree source, (2) which taxa, (3)  file id, (4) name of tree block, (5) how many trees  [number of taxa block] [identification number of file in which the tree block should be stored] [name of tree block] [how many trees to make]
		if (inferenceTask==null)  
			return;
		taxa=null;
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
	//	MesquiteBoolean autoSave = new MesquiteBoolean(true);
		inferenceThread = new TreeBlockThread(this, inferenceTask, trees, howManyTrees, file);
		inferenceThread.start();

	}
	public boolean stopInference(){
		if (inferenceThread!= null)
			inferenceThread.stopFilling();
		fireTreeFiller();

		iQuit();
		return true;
	}
	/*.................................................................................................................*/
	void saveAndPresentTrees(TreeInferer fillTask, Taxa taxa, TreeVector trees){
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
			commands += " tell It; setTaxa " + getProject().getTaxaReferenceInternal(taxa) + " ;  setTreeBlockByID " + trees.getID()  + "; endTell;  getWindow; tell It; setSize 400 300; " + extraWindowCommands + " endTell; showWindowForce; endTell; ";
			if (MesquiteTrunk.debugMode)
				logln(commands);
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
		resetUIOnMe = false;
		this.ownerModule = ownerModule;
		setSpontaneousIndicator(false);
	}
	public abstract void stopFilling();
}

/* ======================================================================== */
class InferenceLogger implements Logger {
	Vector strings = new Vector();
	int maxNumStrings=10;
	
	TreeInferenceLiaison ownerModule;
	public InferenceLogger(TreeInferenceLiaison owner){
		ownerModule = owner;
	}
	public void setMaxNumStrings(int num){
		maxNumStrings = num;
	} 
	public int getNumStrings(){
		return strings.size();
	}
	public String getStrings(int maxNumLines){
		int start = 0;
		if (maxNumLines!= maxNumStrings)
			maxNumStrings = maxNumLines;
		
		if (getNumStrings()> maxNumStrings)
			start = getNumStrings()-maxNumStrings;
		
		StringBuffer sb = new StringBuffer();
		for (int i =start; i<getNumStrings(); i++){
			String s = getString(i);
			if (s.startsWith("  "))
				s = "&nbsp;&nbsp;" + s;
			else if (s.startsWith(" "))
				s = "&nbsp;" + s;
			sb.append(s + "<br>");
		}
		return sb.toString();
	}
	public String getString(int i){
		if (i>=strings.size())
			return "";
		return (String)strings.elementAt(i);
	}
	public synchronized void logln(String s){
		if (strings.size()>= maxNumStrings){
			strings.removeElementAt(0);
		}
		strings.addElement(s);
		ownerModule.stringLogged();
	}
		public synchronized void log(String s){
		if (strings.size()== 0)
			strings.addElement(s);
		else {
			String last = (String)strings.elementAt(strings.size()-1);
			strings.removeElementAt(strings.size()-1);
			strings.addElement(last + s);
					
		}
		ownerModule.stringLogged();
	}

}
/* ======================================================================== */
class TreeBlockThread extends FillerThread {
	TreeInferer inferenceTask;
	TreeVector trees;
	MesquiteFile file;
	int howManyTrees;
	CommandRecord comRec = null;
	//MesquiteBoolean autoSave = null;
	boolean aborted = false;
	public TreeBlockThread (TreeInferenceLiaison ownerModule, TreeInferer fillTask, TreeVector trees, int howManyTrees, MesquiteFile file) {
		super(ownerModule);
		this.inferenceTask = fillTask;
		this.trees = trees;
		this.howManyTrees = howManyTrees;
		this.file = file;
//		this.autoSave = autoSave;
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
	
	public  void addItemsToDialogPanel(ExtensibleDialog dialog){
	}
	public  boolean optionsChosen(){
		return false;
	}

	/*.............................................*/
	public void run() {
		//MesquiteTrunk.mesquiteTrunk.incrementMenuResetSuppression();
		long s = System.currentTimeMillis();
		int before = trees.size();
			boolean okToSave = false;
		try {
			MesquiteThread.setLoggerCurrentThread(ownerModule.getLogger());
			MesquiteThread.setHintToSuppressProgressIndicatorCurrentThread(true);
			Taxa taxa = ownerModule.taxa;
			if (taxa != null)
				taxa.incrementEditInhibition();
			inferenceTask.fillTreeBlock(trees, howManyTrees);
			if (taxa != null)
				taxa.decrementEditInhibition();
			MesquiteThread.setHintToSuppressProgressIndicatorCurrentThread(false);

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
					ownerModule.saveAndPresentTrees(inferenceTask, trees.getTaxa(), trees);
				ownerModule.fireTreeFiller();
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
		FileCoordinator fCoord = ownerModule.getFileCoordinator();
		ownerModule.iQuit();
		if (okToSave) {  //WAYNECHECK: why is it saving to the Mesquite block a reference to a continuing tree inference task?
			//DAVID: because the snapshot above is written if THIS exists, i.e. it's the continued existence of the Liaison that triggers the memory, not of the inference task
			//Thus, we need to call this after this quits, but because things get disposed, need to get reference to file coordinator in advance of asking to quit
			if (inferenceTask.getAutoSave()){
				fCoord.writeFile(file);
			}
		}
		//MesquiteTrunk.mesquiteTrunk.decrementMenuResetSuppression();
		threadGoodbye();
	}
	public void stopFilling(){
		if (inferenceTask != null)
			inferenceTask.abortFilling();
		aborted = true;
		if (ownerModule.taxa != null)
			ownerModule.taxa.decrementEditInhibition();
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
		ownerModule.setTaxa(taxaIDString);
		Taxa taxa = ownerModule.taxa;
		if (taxa != null){
			taxa.incrementEditInhibition();
		}
		
		Reconnectable reconnectable = fillTask.getReconnectable();
		if (reconnectable != null){
			MesquiteThread.setLoggerCurrentThread(ownerModule.getLogger());
			MesquiteCommand command = new MesquiteCommand("treesReady", taxaIDString, ownerModule);
			command.setSupplementalLogger(ownerModule.getLogger());
			reconnectable.reconnectToRequester(command);
		}
		threadGoodbye();
	}
	public void stopFilling(){
		if (fillTask != null)
			fillTask.abortFilling();
		aborted = true;
		if (ownerModule.taxa != null)
			ownerModule.taxa.decrementEditInhibition();
	}
	/*.............................................*/
	public void dispose(){
		ownerModule = null;
		fillTask = null;
	}

}
