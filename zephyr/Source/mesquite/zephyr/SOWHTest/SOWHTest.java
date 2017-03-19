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
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;
import mesquite.zephyr.lib.ConstrainedSearcher;
import mesquite.zephyr.lib.ZephyrRunner;
import mesquite.categ.lib.CategoricalData;
import mesquite.diverse.lib.*;


// see SimMatricesOnTrees for CharacterSimulator bookkeeping

public class SOWHTest extends TreeWindowAssistantA    {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(NumForCharAndTreeDivers.class, getName() + "  needs a method to calculate diversification statistics.",
				"You can choose the diversification calculation initially or under the Diversification Measure submenu.");
		e.setPriority(2);
		EmployeeNeed ew = registerEmployeeNeed(CharSourceCoordObed.class, getName() + "  needs a source of characters.",
				"The source of characters is arranged initially");
		EmployeeNeed e2 = registerEmployeeNeed(ZephyrRunner.class, getName() + "  needs a module to run an external process.","");
		EmployeeNeed e3 = registerEmployeeNeed(MatrixSourceCoord.class, getName() + "  needs a module to provide a matrix.","");
		EmployeeNeed e4 = registerEmployeeNeed(CharacterSimulator.class, getName() + "  needs a module to simulate matrices.","");
	}
	/*.................................................................................................................*/
	int current = 0;

	protected ZephyrRunner runner;
	protected ConstrainedSearcher constrainedSearcher;
	CharacterSimulator charSimulatorTask;

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
	int totalReps = 100;



	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();

		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, getCharacterClass(), "Source of matrix (for " + getName() + ")");
		if (matrixSourceTask == null)
			return sorry(getName() + " couldn't start because no source of matrix (for " + getName() + ") was obtained");
		rng = new Random(originalSeed);

		charSimulatorTask= (CharacterSimulator)hireEmployee(CharacterSimulator.class, "Character simulator");
		if (charSimulatorTask == null) {
			return sorry("Simulated Matrices on Trees can't start because not appropiate character simulator module was obtained");
		}

		runner = (ZephyrRunner)hireEmployee(ConstrainedSearcher.class, "External tree searcher");
		
		if (runner ==null)
			return false;
		runner.initialize(this);
		runner.setBootstrapAllowed(false);
		constrainedSearcher = (ConstrainedSearcher)runner;


		final MesquiteWindow f = containerOfModule();
		if (f instanceof MesquiteWindow){
			containingWindow = (MesquiteWindow)f;
			containingWindow.addSidePanel(panel = new SOWHPanel(), 250);
		}

		//	addMenuItem( "Choose Character...", makeCommand("chooseCharacter",  this));
		//	addMenuItem( "Close Character-Associated Diversification Analysis", makeCommand("close",  this));
		addMenuSeparator();

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
	public boolean satisfiesSnapshotMode(){
		return false;
	}

	/*.................................................................................................................*
	public Snapshot getSnapshot(MesquiteFile file) { 
		final Snapshot temp = new Snapshot();
		temp.addLine("setCalculator ", numberTask); 
		temp.addLine("setMatrixSource ", matrixSourceTask); 
		temp.addLine("setCharacter " + CharacterStates.toExternal(current)); 
		temp.addLine("doCounts");

		return temp;
	}
		/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "SOWH Test Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Options for SOWH Test");

		IntegerField totalRepsField = dialog.addIntegerField("Number of simulated matricess to examine:",  totalReps,5,1,MesquiteInteger.infinite);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			totalReps = totalRepsField.getValue();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	MesquiteInteger pos = new MesquiteInteger();
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {


		if (checker.compare(this.getClass(), "Provokes Calculation", null, commandName, "doCounts")) {
			doCounts();
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
			doCounts();  //only do counts if tree has changed
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
			states = charSimulatorTask.getSimulatedCharacter(states, hypothesisTree, seed); 
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
	 */
	public double calculateDelta(MCharactersDistribution data, int rep, int totalReps) {
		if (taxa==null) 
			taxa=data.getTaxa();
		TreeVector trees = new TreeVector(taxa);

		CommandRecord.tick(runner.getProgramName() + " SOWH test in progress, replicate " + (rep+1) + " of " + totalReps );

		Random rng = new Random(System.currentTimeMillis());

		double finalScore = 0.0;

		MesquiteDouble unconstrainedScore = new MesquiteDouble();
		MesquiteDouble constrainedScore = new MesquiteDouble();

		if (rep>=0) {
			logln("_______________");
			logln("Replicate " + (rep+1) + " of " + totalReps);
		} else
			logln("Calculating observed value of delta");

		runner.setVerbose(rep<0);

		//First, do the constrained search
		runner.setConstainedSearchAllowed(true);
		runner.setConstrainedSearch(true);  
		runner.getTrees(trees, taxa, data, rng.nextInt(), constrainedScore);  // find score of constrained trees
		runner.setRunInProgress(false);

		//Now, do the unconstrained search
		runner.setConstainedSearchAllowed(false);
		runner.setConstrainedSearch(false);
		runner.getTrees(trees, taxa, data, rng.nextInt(), unconstrainedScore);   // find score of unconstrained trees
		runner.setRunInProgress(false);

		runner.setVerbose(true);

		if (unconstrainedScore.isCombinable() && constrainedScore.isCombinable())
			finalScore = constrainedScore.getValue() - unconstrainedScore.getValue();

		logln("\ndelta = "+finalScore + "  (=" + constrainedScore.getValue()+"-"+unconstrainedScore.getValue()+")");

		trees.dispose();
		trees = null;

		return finalScore;
	}

	/*.................................................................................................................*/
	double calculatePValue(double observed, double[] simulated) {
		DoubleArray.sort(simulated);
		int count = 0;
		int asExtreme = 0;
		for (int i=0; i<simulated.length; i++) {
			if (MesquiteDouble.isCombinable(simulated[i])) {
				count++;
				if (observed<=simulated[i])
					asExtreme++;
			} else {
				break;
			}
		}
		if (count>0)
			return (asExtreme*1.0)/count;

		return MesquiteDouble.unassigned;
	}
	/*.................................................................................................................*/
	String reportFilePath=null;
	/*.................................................................................................................*/
	public void saveResults(StringBuffer results) {
		if (StringUtil.blank(reportFilePath))
			return;

		MesquiteFile.putFileContents(reportFilePath, results.toString(), false);

	}
	/*.................................................................................................................*/
	public void appendToReportFile(String s) {
		if (StringUtil.blank(reportFilePath))
			return;
		MesquiteFile.appendFileContents(reportFilePath, s, false);
	}


	/** This method does the core calculations for the SOWH test. */
	public void doCounts() {
		if (taxa == null || panel == null)
			return;
		if (observedStates==null)
			initialize(taxa);
		
		if (!MesquiteThread.isScripting())
			if (!queryOptions())
				return;

		reportFilePath = MesquiteFile.saveFileAsDialog("Save SOWH Test report file", new StringBuffer("SOWH Test"));
		if (reportFilePath==null)
			return;

		double[] simulatedDeltas = new double[totalReps];
		for (int rep = 0; rep<totalReps; rep++) {
			simulatedDeltas[rep] = MesquiteDouble.unassigned;
		}

		MesquiteString rs = new MesquiteString();

		CategoricalData data = (CategoricalData)observedStates.getParentData();
		if (observedStates == null )
			rs.setValue("Sorry, no matrix was not obtained.  The SOWH analysis could not be completed.");
		stateClass = observedStates.getStateClass();
		//		window.setText("");
		clearLastResult();
		panel.setStatus(true);
		panel.repaint();
		panel.setCalculatingObserved(true);
		double observedDelta = calculateDelta(observedStates, -1, -1);
		panel.setCalculatingObserved(false);

		StringBuffer initialText = new StringBuffer();
		initialText.append("\nTesting a phylogenetic hypothesis with the Swofford-Olsen-Waddell-Hillis test\n");
		initialText.append("\nHypothesis Tree: " + hypothesisTree.getName() );
		initialText.append("\nConstraint Tree: " + constrainedSearcher.getConstraintTreeName());
		initialText.append("\nObserved Matrix: " + data.getName() +  "\n");
		initialText.append("\n\nValue of the test statistic for observed matrix\n  "  + observedDelta);

		initialText.append("\n\nValues of the test statistic for simulated matrices");
		panel.appendToInitialPanelText(initialText.toString());
		panel.setText("");
		initialText.append("\ndelta\tp-value");
		saveResults(initialText);
		
		if (!MesquiteThread.isScripting())
			if (!runner.queryOptions()){  // prompt again so that searches can be less thorough
				return;
		}

		StringBuffer repReport = new StringBuffer();
		for (int rep = 0; rep<totalReps; rep++) {
			MCharactersDistribution simulatedStates = getSimulatedMatrix(taxa,(rep+1));
			double simulatedDelta = calculateDelta(simulatedStates, rep, totalReps);
			simulatedDeltas[rep] = simulatedDelta;
			double pValue = calculatePValue(observedDelta,simulatedDeltas);
			repReport.append("\n  "  + simulatedDelta+ "\t"+MesquiteDouble.toStringDigitsSpecified(pValue, 4));
			panel.setText("\nReplicates completed: "+ (rep+1) + "\n\n" + repReport.toString());
			appendToReportFile("\n"  + simulatedDelta + "\t"+MesquiteDouble.toStringDigitsSpecified(pValue, 4));
			panel.setPValue(pValue);
			panel.repaint();

		}

		panel.setStatus(false);
		panel.repaint();
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
		return "Conducts a SOWH Test." ;
	}
	public void endJob() {
		if (panel != null && containingWindow != null)
			containingWindow.removeSidePanel(panel);
		super.endJob();
	}
}

class SOWHPanel extends MousePanel{
	int titleHeight = 50;
	int pValueHeight = 30;
	TextArea text;
	StringBuffer initialText;
	Font df = new Font("Dialog", Font.BOLD, 14);
	boolean calculating = false;
	int topOfText = titleHeight+pValueHeight + 2;
	double pValue = MesquiteDouble.unassigned;
	boolean calculatingObserved = true;
	
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
	public void setStatus(boolean calculating){
		this.calculating = calculating;
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
	
	public void drawPValue(Graphics g){
		g.setColor(ColorDistribution.burlyWood);
		g.fillRect(0,titleHeight, getBounds().width, pValueHeight);
		if (MesquiteDouble.isCombinable(pValue)) {
			g.setColor(Color.blue);
			g.drawString("p value: " + MesquiteDouble.toStringDigitsSpecified(pValue, 4), 8, titleHeight+pValueHeight-6);
		}
	}
	public boolean isCalculatingObserved() {
		return calculatingObserved;
	}
	public void setCalculatingObserved(boolean calculatingObserved) {
		this.calculatingObserved = calculatingObserved;
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
		}
		else{
			g.setColor(Color.black);
			g.fillRect(0,0, getBounds().width, titleHeight);
			g.setColor(Color.red);
			g.drawString("SOWH Test", 8, 20);
			if (calculatingObserved)
				g.drawString("Calculating Observed Value...", 8, 46);
			else
				g.drawString("Calculating Values under Hypothesis...", 8, 46);
		}
		drawPValue(g);
	}
}

