/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.zephyr.SOWHTest;
/*~~  */

import java.awt.*;
import java.util.Random;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;
import mesquite.categ.lib.CategoricalData;
import mesquite.diverse.lib.*;
import mesquite.io.lib.IOUtil;


// see SimMatricesOnTrees for CharacterSimulator bookkeeping

public class SOWHTest extends TreeWindowAssistantA      {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(NumForCharAndTreeDivers.class, getName() + "  needs a method to calculate diversification statistics.",
				"You can choose the diversification calculation initially or under the Diversification Measure submenu.");
		e.setPriority(2);
		EmployeeNeed ew = registerEmployeeNeed(CharSourceCoordObed.class, getName() + "  needs a source of characters.",
				"The source of characters is arranged initially");
		EmployeeNeed e2 = registerEmployeeNeed(ZephyrRunner.class, getName() + "  needs a module to search for trees.","");
		EmployeeNeed e3 = registerEmployeeNeed(MatrixSourceCoord.class, getName() + "  needs a module to provide a matrix.","");
		EmployeeNeed e4 = registerEmployeeNeed(CharacterSimulator.class, getName() + "  needs a module to simulate matrices.","");
		EmployeeNeed e5 = registerEmployeeNeed(DataAlterer.class, getName() + "  needs a module to alter matrices.","");
	}
	/*.................................................................................................................*/
	int current = 0;

	protected ZephyrRunner runner;
	protected ConstrainedSearcher constrainedSearcher;
	CharacterSimulator charSimulatorTask;
	DataAlterer altererTask;

	Tree hypothesisTree;
	Tree constraintTree;
	MatrixSourceCoord matrixSourceTask;
	Taxa taxa;
	Class stateClass;
	MesquiteWindow containingWindow;
	SOWHPanel panel;
	MesquiteString numberTaskName;
	MesquiteCommand ntC;
	protected MCharactersDistribution observedStates;
	long originalSeed=System.currentTimeMillis(); //0L;
	Random rng;
	boolean userAborted = false;
	
	int totalReps = 100;
	double observedDelta = MesquiteDouble.unassigned;
	boolean calculateObservedDelta = true;
	boolean alterData = false;



	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");
		rng = new Random(originalSeed);

		charSimulatorTask= (CharacterSimulator)hireEmployee(CharacterSimulator.class, "Character Simulator");
		if (charSimulatorTask == null) {
			return sorry("Simulated Matrices on Trees can't start because not appropiate character simulator module was obtained");
		}

		runner = (ZephyrRunner)hireEmployee(ConstrainedSearcherTreeScoreProvider.class, "External tree searcher");
		
		if (runner ==null || !(runner instanceof ZephyrRunner))
			return false;
		runner.initialize(this);
		runner.setBootstrapAllowed(false);
		constrainedSearcher = (ConstrainedSearcher)runner;


		final MesquiteWindow f = containerOfModule();
		if (f instanceof MesquiteWindow){
			containingWindow = (MesquiteWindow)f;
			containingWindow.addSidePanel(panel = new SOWHPanel(), 250);
		}
		MesquiteMenuSpec mms = makeMenu("SOWH Test");
		addMenuItem( "Close SOWH Test", makeCommand("close",  this));

		return true;
	}
	
	
	private boolean initializeObservedStates(Taxa taxa) {
		if (matrixSourceTask!=null) {
			if (observedStates ==null) {
				observedStates = matrixSourceTask.getCurrentMatrix(taxa);
				if (observedStates==null)
					return false;
			}
		}
		else return false;
		return true;
	}

	public boolean initialize(Taxa taxa) {
		this.taxa = taxa;
		if (matrixSourceTask!=null) {
			matrixSourceTask.initialize(taxa);
		} else
			return false;
		if (!initializeObservedStates(taxa))
			return false;
		if (runner ==null) {
			//TODO:	runner = (ZephyrRunner)hireNamedEmployee(ZephyrRunner.class, getRunnerModuleName());
		}
		if (runner !=null){
			charSimulatorTask.initialize(taxa);
			runner.initializeTaxa(taxa);
		}
		else
			return false;
		return true;
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("totalReps".equalsIgnoreCase(tag)) {
			totalReps = MesquiteInteger.fromString(content);
		}
/*		else if ("alignmentMethodText".equalsIgnoreCase(tag)) {
			alignmentMethodText = StringUtil.cleanXMLEscapeCharacters(content);
		}
		else if ("useMaxCores".equalsIgnoreCase(tag))
			useMaxCores = MesquiteBoolean.fromTrueFalseString(content);
*/
		super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "totalReps", totalReps);  

		return super.preparePreferencesForXML()+buffer.toString();
	}


	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}
	/*.................................................................................................................*/
	public Class getCharacterClass() {
		return null;
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	public void windowGoAway(MesquiteWindow whichWindow) {
		if (whichWindow == null)
			return;
		whichWindow.hide();
		whichWindow.dispose();
		iQuit();
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		return null;   // returning null ensures it won't be saved in file.
		
		/*final Snapshot temp = new Snapshot();
		temp.addLine("setCalculator ", numberTask); 
		temp.addLine("setMatrixSource ", matrixSourceTask); 
		temp.addLine("setCharacter " + CharacterStates.toExternal(current)); 
		temp.addLine("doCounts");

		return temp;*/
	}
	
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "SOWH Test Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Options for SOWH Test");
		StringBuffer sb = new StringBuffer();
		sb.append("You can either enter the value you previously calculated for delta, the test statistic, or you can ask Mesquite to calculate it now. ");
		sb.append("The advantage of calculating it in advance is that you then have more careful control over how you calculate it. ");
		sb.append("<br><br> The test statistic, delta, is the tree score for the optimal constrained tree minus the tree score for the optimal unconstrained tree. ");
		dialog.appendToHelpString(sb.toString());

		int radioValue = 1;
		if (calculateObservedDelta)
			radioValue=0;
		RadioButtons calculateObservedDeltaRadios = dialog.addRadioButtons(new String[] {"calculate observed value of test statistic (delta)","use pre-calculated observed value"}, radioValue);
		DoubleField observedDeltaField = dialog.addDoubleField("pre-calculated observed value:", observedDelta, 8);
		dialog.addHorizontalLine(1);

		IntegerField totalRepsField = dialog.addIntegerField("Number of simulated matrices to examine:",  totalReps,5,1,MesquiteInteger.infinite);
		Checkbox alterDataCheckbox = dialog.addCheckBox("Alter data after each simulation, before tree inference", alterData);
		if (runner !=null) {
			dialog.addHorizontalLine(1);
			runner.addItemsToSOWHDialogPanel(dialog);
		}
		
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			radioValue=calculateObservedDeltaRadios.getValue();
			if (radioValue!=0) {
				observedDelta = observedDeltaField.getValue();
				if (!MesquiteDouble.isCombinable(observedDelta)){
					if (AlertDialog.query(containerOfModule(), "Warning", "Pre-calculated observed value was chosen, but no value was entered. Do you wish to ask that it be calculated, or cancel the SOWH test?", "Calculate", "Cancel"))
						calculateObservedDelta = true;
					else
						return false;
				} else
					calculateObservedDelta = (radioValue==0);
			}
			totalReps = totalRepsField.getValue();
			alterData =alterDataCheckbox.getState();
			if (runner !=null) 
				runner.SOWHoptionsChosen();  // TODO: what if false?
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	void hireDataAltererIfNeeded () {
		if (alterData && altererTask==null)
			altererTask = (DataAlterer)hireEmployee(DataAlterer.class, "Alterer of data");
	}

	/*.................................................................................................................*/
	MesquiteInteger pos = new MesquiteInteger();
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {

		if (checker.compare(this.getClass(), "Provokes Calculation", null, commandName, "doCounts")) {
			doSOWHTest();
		}
		else if (checker.compare(this.getClass(), "Quits", null, commandName, "close")) {
			if (panel != null && containingWindow != null)
				containingWindow.removeSidePanel(panel);
			iQuit();
		}
		else if (checker.compare(this.getClass(), "Quits", null, commandName, "close")) {
			if (panel != null && containingWindow != null)
				containingWindow.removeSidePanel(panel);
			iQuit();
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}
	long oldTreeID = -1;
	long oldTreeVersion = 0;
	/*.................................................................................................................*/
	public   void setTree(Tree tree) {
		if (tree==null)
			return;
		this.hypothesisTree=tree;
		taxa = tree.getTaxa();
		if ((tree.getID() != oldTreeID || tree.getVersionNumber() != oldTreeVersion) && !MesquiteThread.isScripting()) {
			doSOWHTest();  //only do counts if tree has changed
		}
		oldTreeID = tree.getID();
		oldTreeVersion = tree.getVersionNumber();
	}
	/*.................................................................................................................*
	public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {
		if (numberTask!=null && !MesquiteThread.isScripting())
			doCounts();
	}
	/*.................................................................................................................*/
	String blankIfNull(String s){
		if (s == null)
			return "";
		return s;
	}

	public Object getWritableResults(){
		return lastResult;
	}
	public Object getResultsHeading(){
		return lastResult;
	}


	/*.................................................................................................................*/
	private MCharactersDistribution getSimulatedMatrix(Taxa taxa, int matrixNumber){
		if (hypothesisTree == null) {
			return null;
		}
		else if (charSimulatorTask==null) {
			System.out.println("Simulator task null");
			return null;
		}
		else if (taxa==null){
			System.out.println("taxa null");
			return null;
		}
		int numChars = observedStates.getNumChars();
		MAdjustableDistribution matrix = null;
		Class c = charSimulatorTask.getStateClass();//getDataClass from simulator and get it to make matrix
		if (c==null)
			return null;
		try {
			CharacterState s = (CharacterState)c.newInstance();
			if (s!=null) {
				matrix = s.makeMCharactersDistribution(taxa, numChars, taxa.getNumTaxa());
				if (matrix == null)
					return null;
			}
		}
		catch (IllegalAccessException e){alert("SOWH iae getM"); return null; }
		catch (InstantiationException e){alert("SOWH ie getM");  return null;}


		/*rng.setSeed(originalSeed);
		long rnd = originalSeed;
		for (int it = 0; it<=currentDataSet; it++)
			rnd =  rng.nextInt();
		rng.setSeed(rnd+1);
		seed.setValue(rnd + 1);//v. 1. 1, October 05: changed so as to avoid two adjacent matrices differing merely by a frameshift of random numbers
		 */

		//if (tree instanceof MesquiteTree)
		//	((MesquiteTree)tree).setName("Tree # " + MesquiteTree.toExternal(currentDataSet)  + " from " + treeTask.getName());
		CharacterDistribution states = null;
		MesquiteLong seed = new MesquiteLong(rng.nextInt());

		for (int ic = 0; ic<numChars; ic++) {
			states = charSimulatorTask.getSimulatedCharacter(states, hypothesisTree, seed, ic); 
			matrix.transferFrom(ic, states);
		}
		matrix.setName("Matrix " + matrixNumber + " simulated by " + charSimulatorTask.getName());
		matrix.setAnnotation(accumulateParameters(" "), false);
		matrix.setBasisTree(hypothesisTree);

		return matrix;
	}

	/*.................................................................................................................*/
	/** This calculates the value of the test statistic, delta.  Delta is the tree score of the optimal tree that is constrained to match the
	 * constraint tree minus the tree score of the optimal unconstrained tree.  This method invokes the tree searcher, whatever it might be.
	 * If rep >= 0, then this is the replicate number.
	 * If rep is < 0, this indicates calculating the observed matrix value.
	 */
	public double calculateDelta(MCharactersDistribution matrix, int rep, int totalReps) {
		if (taxa==null) 
			taxa=matrix.getTaxa();
		TreeVector trees = new TreeVector(taxa);


		Random rng = new Random(System.currentTimeMillis());

		double finalScore = 0.0;

		MesquiteDouble unconstrainedScore = new MesquiteDouble();
		MesquiteDouble constrainedScore = new MesquiteDouble();

		if (rep>=0) {  // one of the simulation replicates
			CommandRecord.tick(runner.getProgramName() + " SOWH test in progress, replicate " + (rep+1) + " of " + totalReps );
			logln("_______________");
			logln("Replicate " + (rep+1) + " of " + totalReps);
			runner.setExtraQueryOptionsTitle("Simulated Matrix");
		} else {
			CommandRecord.tick(runner.getProgramName() + " SOWH test in progress, calculating observed value" );
			logln("Calculating observed value of delta");
			runner.setExtraQueryOptionsTitle("Observed Matrix");
		}

		runner.setVerbose(rep<0);

		//First, do the constrained search
		runner.setConstainedSearchAllowed(false);
		runner.setConstrainedSearch(true);  
		runner.resetSOWHOptionsConstrained();
		if (runner.getTrees(trees, taxa, matrix, rng.nextInt(), constrainedScore)==null)  // find score of constrained tree
			return MesquiteDouble.unassigned;  
		if (runner.getUserAborted())
			userAborted = true;
		runner.setRunInProgress(false);
		if (rep<0){
			observedRunDetails = runner.getSOWHDetailsObserved();
		}

		if (!userAborted) {
			//Now, do the unconstrained search
			//runner.setConstainedSearchAllowed(false);
			runner.setConstrainedSearch(false);
			runner.resetSOWHOptionsUnconstrained();
			if (runner.getTrees(trees, taxa, matrix, rng.nextInt(), unconstrainedScore)==null)
					return MesquiteDouble.unassigned;   // find score of unconstrained trees
			if (runner.getUserAborted())
				userAborted = true;
			runner.setRunInProgress(false);
		}


		runner.setVerbose(true);
		trees.dispose();
		trees = null;

		if (!userAborted) {
			if (unconstrainedScore.isCombinable() && constrainedScore.isCombinable()){
				if (runner.smallerIsBetter()) {
					finalScore = constrainedScore.getValue() - unconstrainedScore.getValue();
					logln("\ndelta = "+finalScore + "  (=" + constrainedScore.getValue()+"-"+unconstrainedScore.getValue()+")");
				}
				else {
					finalScore = unconstrainedScore.getValue() - constrainedScore.getValue();
					logln("\ndelta = "+finalScore + "  (=" + (-constrainedScore.getValue())+"-"+(-unconstrainedScore.getValue())+")");
				}
				return finalScore;
			}
		} else {
			logln("\nSOWH Test aborted by user.");
			panel.setAborted(true);
		}
		return MesquiteDouble.unassigned;

	}

	/*.................................................................................................................*/
	/** Calculates the pValue by seeing how many of the simulated values are greater than or equal to the observed value.
	 * Also calculates the fraction of the values that are less than 0. */
	double calculatePValue(double observed, double[] simulated, MesquiteDouble fractionNegative) {
		DoubleArray.sort(simulated);
		int count = 0;
		int asExtreme = 0;
		int negative = 0;
		for (int i=0; i<simulated.length; i++) {
			if (MesquiteDouble.isCombinable(simulated[i])) {
				count++;
				if (simulated[i]<0)
					negative++;
				if (observed<=simulated[i])
					asExtreme++;
			} else {
				break;
			}
		}
		if (count>0) {
			if (fractionNegative!=null)
				fractionNegative.setValue(negative*1.0/count);
			return (asExtreme*1.0)/count;
		}

		return MesquiteDouble.unassigned;
	}
	/*.................................................................................................................*/
	String reportFilePath=null;
	/*.................................................................................................................*/
	public void saveResults(String results) {
		if (StringUtil.blank(reportFilePath))
			return;

		MesquiteFile.putFileContents(reportFilePath, results, false);

	}
	
	private String getInitialText(CharacterData data){
		StringBuffer initialText = new StringBuffer();
		initialText.append("\nTesting a phylogenetic hypothesis with\n the Swofford-Olsen-Waddell-Hillis test\n");
		initialText.append("\nHypothesis Tree: " + hypothesisTree.getName() );
		if (StringUtil.notEmpty(constrainedSearcher.getConstraintTreeName()))
			initialText.append("\nConstraint Tree: " + constrainedSearcher.getConstraintTreeName());
		initialText.append("\nObserved Matrix: " + data.getName() +  "\n");
		initialText.append("\n\nValue of the test statistic for\n   observed matrix:  "  + observedDelta);

		initialText.append("\n\nValues of the test statistic for\n   simulated matrices\n");
		return initialText.toString();

	}
	
	String observedRunDetails = "";
	/** This method provides text for the start of the report file */
	private String getStartOfReportFileText(){
		StringBuffer logBuffer = new StringBuffer();
		logBuffer.append("SOWH Test\n");
		logBuffer.append(StringUtil.getDateTime()+"\n");
		logBuffer.append("Mesquite version: " + MesquiteTrunk.mesquiteTrunk.getVersion()+", build number " + MesquiteTrunk.mesquiteTrunk.getBuildNumber()+"\n");
		logBuffer.append("Zephyr version: " + getVersion()+"\n");
		logBuffer.append("Phylogeny inferences conducted by " + runner.getName()+"\n\n");
		if (StringUtil.notEmpty(observedRunDetails))
			logBuffer.append(observedRunDetails+"\n");
		logBuffer.append(runner.getSOWHDetailsSimulated()+"\n");
		return logBuffer.toString();

	}
	/*.................................................................................................................*/
	public void appendToReportFile(String s) {
		if (StringUtil.blank(reportFilePath))
			return;
		MesquiteFile.appendFileContents(reportFilePath, s, false);
	}
	/*.................................................................................................................*/
	public String getListHeading(boolean extraValuesForFile) {
		String s= "\nrep\tdelta\tp-value";
		if (extraValuesForFile)
			s+="\tfraction-negative";
		s+="\n-------------------------------"; 
		return s;
	}
	/*.................................................................................................................*/
	public String getReplicateLine(int rep, double delta, double pValue) {
		return "\n" + (rep+1)+"\t"+ MesquiteDouble.toStringDigitsSpecified(delta, 8) + "\t"+MesquiteDouble.toStringDigitsSpecified(pValue, 4);
	}
	double totalTime=0;
	/*.................................................................................................................*/
	/** This method does the core calculations for the SOWH test. */
	public void doSOWHTest() {
		if (taxa == null || panel == null) {
			iQuit();
			return;
		}
		if (observedStates==null)
			initialize(taxa);
		
		if (!MesquiteThread.isScripting())
			if (!queryOptions()) {
				iQuit();
				return;
			}

		reportFilePath = MesquiteFile.saveFileAsDialog("Save SOWH Test report file", new StringBuffer("SOWH Test"));
		if (reportFilePath==null) {
			iQuit();
			return;
		}

		double[] simulatedDeltas = new double[totalReps];
		for (int rep = 0; rep<totalReps; rep++) {
			simulatedDeltas[rep] = MesquiteDouble.unassigned;
		}

		MesquiteString rs = new MesquiteString();
		
		if (observedStates == null ) {
			rs.setValue("Sorry, no matrix was not obtained.  The SOWH analysis could not be completed.");
			iQuit();
		}

		CategoricalData data = (CategoricalData)observedStates.getParentData();
		stateClass = observedStates.getStateClass();
		
		panel.setCalculating(true);
		panel.repaint();
		if (calculateObservedDelta) {
			panel.setCalculatingObserved(true);
			observedDelta = calculateDelta(observedStates, -1, -1);
			if (!MesquiteDouble.isCombinable(observedDelta)){
				MesquiteMessage.discreetNotifyUser("The observed value of the test statistic could not be calculated.  The SOWH analysis could not be completed." );
				iQuit();
				return;
			}
			if (userAborted)
				return;
		}
		panel.setCalculatingObserved(false);

		StringBuffer initialText = new StringBuffer();
		initialText.append(getInitialText(data));
		panel.appendToInitialPanelText(initialText.toString());
		panel.setText("");
		
		if (!MesquiteThread.isScripting() && calculateObservedDelta)
			if (!runner.queryOptions()){  // prompt again so that searches can be less thorough
				return;
		}

		hireDataAltererIfNeeded();
		
		MesquiteDouble fractionNegative = new MesquiteDouble(0.0);
		StringBuffer repReport = new StringBuffer();
		int uncombinableSimulatedDelta=0;
		MesquiteTimer timer = new MesquiteTimer();
		timer.start();
		totalTime = 0;
		
		for (int rep = 0; rep<totalReps; rep++) {
			panel.setReplicate(rep+1);
			MCharactersDistribution simulatedStates = getSimulatedMatrix(taxa,(rep+1));
			boolean createdNewDataObject = simulatedStates.getParentData()==null;
			CharacterData simulatedData = (CategoricalData)CharacterData.getData(this,  simulatedStates, taxa);
			if (simulatedData!=null) {
				((MAdjustableDistribution)simulatedStates).setParentData(simulatedData);
				IOUtil.copyCurrentSpecSets(data,simulatedData);  // WAYNECHECK: is this ok?
				if (altererTask!=null && alterData)
					altererTask.alterData(simulatedData, null, null);

			}
			double simulatedDelta = calculateDelta(simulatedStates, rep, totalReps);
			if (userAborted) {
				return;
			} 
			if (createdNewDataObject && simulatedData!=null) {
				simulatedData.dispose();
				simulatedData=null;
			}
			if (!MesquiteDouble.isCombinable(simulatedDelta)) {  
				logln("WARNING: replicate " + (rep+1) + " of the SOWH test failed to yield a valid value; replicate being repeated");
				uncombinableSimulatedDelta++;
				rep--;
				continue;
				//MesquiteMessage.discreetNotifyUser("There was a problem with the SOWH test and it was terminated.");
				//return;
			}
			simulatedDeltas[rep] = simulatedDelta;
			double pValue = calculatePValue(observedDelta,simulatedDeltas,fractionNegative);
			if (rep==0) {
				initialText.setLength(0);
				initialText.append(getInitialText(data));
				panel.setInitialText(initialText.toString());
				saveResults(getStartOfReportFileText() +"\n"+ initialText.toString());
				appendToReportFile("\n"+getListHeading(true));
			}

			repReport.insert(0,getReplicateLine(rep, simulatedDelta, pValue));
			StringBuffer panelText = new StringBuffer();
			panelText.append("\nReplicates completed: "+ (rep+1) + " of " +totalReps + "\n");
			panelText.append("\nFraction of delta values <0: "+ fractionNegative.toString(4)+"\n");
			panel.setText(panelText.toString()+ getListHeading(false) + repReport.toString());
			appendToReportFile(getReplicateLine(rep, simulatedDelta, pValue)+"\t"+fractionNegative.toString(4));
			panel.setPValue(pValue);
			panel.repaint();
			totalTime += timer.timeSinceLastInSeconds();
			if (rep==totalReps-1) {
				if (AlertDialog.query(containerOfModule(), "More replicates?", "Do you want to do more replicates?" , "More Replicates", "No")){
					MesquiteInteger moreReps= new MesquiteInteger(100);
					if (QueryDialogs.queryInteger(containerOfModule(), "Number of additional replicates", "Number of additional replicates", true, moreReps)){
						if (moreReps.isCombinable()) {
							totalReps+=moreReps.getValue();
							simulatedDeltas = DoubleArray.copyIntoDifferentSize(simulatedDeltas, totalReps, MesquiteDouble.unassigned);
						}
					}
				}
			}
			timer.timeSinceLast();  // do this to reset the timer so that we don't get penalized for the alert dialog asking if more replicates.
			double totalTimeInSeconds = timer.timeSinceVeryStartInSeconds();
			double timePerReplicate = totalTime/(rep+1);
			String timeRemaining = StringUtil.secondsToHHMMSS((int)(timePerReplicate*(totalReps-rep-1)));
			logln("\nSOWH Test running time: "+ StringUtil.secondsToHHMMSS((int)totalTimeInSeconds) + ".  Estimated time remaining: " + timeRemaining);

		}
		
		if (isPrerelease() || MesquiteTrunk.debugMode) {
			logln("Number of replicates with uncombinable delta values: " + uncombinableSimulatedDelta);
		}

		panel.setCalculating(false);
		panel.repaint();
		
		logln("SOWH Test completed.");
		//		window.append("\n\n  " + rs);
	}
	/*.................................................................................................................*/
	public String getName() {
		return "SOWH Test";
	}

	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "SOWH Test...";
	}

	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Conducts a Swofford-Olsen-Waddell-Hillis test for phylogenetic structure." ;
	}
	public void endJob() {
		if (panel != null && containingWindow != null)
			containingWindow.removeSidePanel(panel);
		super.endJob();
	}
}

class SOWHPanel extends MousePanel{
	int numRowsInTitle = 3;
	int titleHeight = 56;
	int pValueHeight = 30;
	TextArea text;
	StringBuffer initialText;
	Font df = new Font("Dialog", Font.BOLD, 14);
	boolean calculating = false;
	int topOfText = titleHeight+pValueHeight + 2;
	double pValue = MesquiteDouble.unassigned;
	boolean calculatingObserved = true;
	boolean hasCalculated = false;
	boolean aborted = false;
	int rep = 0;
	
	public SOWHPanel(){
		super();
		text = new TextArea(" ", 50, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
		setLayout(null);
		add(text);
		text.setLocation(0,topOfText);
		text.setVisible(true);
		text.setEditable(false);
		initialText = new StringBuffer();
		setBackground(Color.darkGray);
		text.setBackground(Color.white);
	}
	public void setCalculating(boolean calculating){
		this.calculating = calculating;
		if (calculating)
			hasCalculated = true;
	}
	public void setInitialText(String t){
		initialText.setLength(0);
		initialText.append(t);
	}
	public void appendToInitialPanelText(String t){
		initialText.append(t);
	}
	public void setText(String t){
		text.setText(initialText.toString() + t);
	}
	public void setPValue(double pValue){
		this.pValue = pValue;
	}
	public void setReplicate(int rep){
		this.rep = rep;
	}
	
	public void drawPValue(Graphics g){
		g.setColor(ColorDistribution.burlyWood);
		g.fillRect(0,titleHeight, getBounds().width, pValueHeight);
		if (MesquiteDouble.isCombinable(pValue)) {
			g.setColor(Color.blue);
			g.drawString("p value: " + MesquiteDouble.toStringDigitsSpecified(pValue, 4), 8, titleHeight+pValueHeight-8);
		}
	}
	public void setCalculatingObserved(boolean calculatingObserved) {
		this.calculatingObserved = calculatingObserved;
		repaint();
	}
	public void setAborted(boolean aborted) {
		this.aborted = aborted;
		repaint();
	}

	public void append(String t){
		text.append(t);
	}
	public void setSize(int w, int h){
		super.setSize(w, h);
		text.setSize(w, h-topOfText);
	}
	public void setBounds(int x, int y, int w, int h){
		super.setBounds(x, y, w, h);
		text.setSize(w, h-topOfText);
	}
	public void paint(Graphics g){
		g.setFont(df);

		if (!calculating){
			g.setColor(Color.white);
			g.drawString("SOWH Test", 8, 20);
			if (hasCalculated)
				g.drawString("Calculations complete", 8, 44);
		}
		else if (aborted){
			g.setColor(Color.white);
			g.drawString("SOWH Test", 8, 20);
			g.drawString("Calculations aborted", 8, 44);
		}
		else{
			g.setColor(Color.black);
			g.fillRect(0,0, getBounds().width, titleHeight);
			g.setColor(Color.red);
			g.drawString("SOWH Test", 8, 20);
			 if (calculatingObserved)
				g.drawString("Calculating Observed Value...", 8, 44);
			else {
				g.drawString("Simulation Replicate " + rep, 8, 44);
				//g.drawString("" + rep, 14, 62);
			}
		}
		drawPValue(g);
	}
	

}

