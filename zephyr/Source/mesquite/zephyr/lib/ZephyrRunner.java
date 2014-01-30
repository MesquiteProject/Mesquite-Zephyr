package mesquite.zephyr.lib;

import java.awt.Checkbox;
import java.awt.Choice;
import java.util.Random;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.MolecularData;
import mesquite.lib.ExtensibleDialog;
import mesquite.lib.MesquiteCommand;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteThread;
import mesquite.lib.MesquiteTimer;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.ProgressIndicator;
import mesquite.lib.SpecsSetVector;
import mesquite.lib.StringUtil;
import mesquite.lib.Taxa;
import mesquite.lib.TaxaSelectionSet;
import mesquite.lib.Tree;
import mesquite.lib.TreeVector;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.zephyr.GarliTrees.GarliTrees;
import mesquite.zephyr.RAxMLTrees.RAxMLTrees;

public abstract class ZephyrRunner extends MesquiteModule implements ExternalProcessRequester{

	String[] logFileNames;
	protected ExternalProcessRunner externalProcRunner;
	protected ProgressIndicator progIndicator;
	protected CategoricalData data;
	protected MesquiteTimer timer = new MesquiteTimer();
	protected Taxa taxa;
	protected String unique;
	protected Random rng;
	protected ZephyrTreeSearcher ownerModule;

	
	protected String outgroupTaxSetString = "";
	protected int outgroupTaxSetNumber = 0;

	public abstract Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore);
	public abstract Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore);
	public abstract boolean bootstrap();
	public abstract void reconnectToRequester(MesquiteCommand command);
	public abstract String getProgramName();
	public abstract boolean queryOptions();
	
	public abstract String[] getLogFileNames();

	
	public void initialize (ZephyrTreeSearcher ownerModule) {
		this.ownerModule= ownerModule;
	}
	/*.................................................................................................................*/
	public boolean initializeTaxa (Taxa taxa) {
		Taxa currentTaxa = this.taxa;
		this.taxa = taxa;
		if (taxa!=currentTaxa && taxa!=null) {
			if (!MesquiteThread.isScripting() && !queryTaxaOptions(taxa))
				return false;
		}
		return true;
	}

	public void setFileNames () {
	}
	
	
	public void initializeMonitoring () {
	}
	

	/*.................................................................................................................*/
	/** Override this to provide any subclass-specific initialization code needed before QueryOptions is called. */
	public boolean initializeJustBeforeQueryOptions(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean initializeGetTrees(Class requiredClassOfData, MCharactersDistribution matrix) {
		if (matrix==null )
			return false;
		if (!(matrix.getParentData() != null && requiredClassOfData.isInstance(matrix.getParentData()))){
			MesquiteMessage.discreetNotifyUser("Sorry, " + getProgramName() + " works only if given a full "+requiredClassOfData.getName()+" object");
			return false;
		}

		if (this.taxa != taxa) {
			if (!initializeTaxa(taxa))
				return false;
		}
		data = (CategoricalData)matrix.getParentData();
		if (!initializeJustBeforeQueryOptions())
			return false;
		
		if (!MesquiteThread.isScripting() && !queryOptions()){
			return false;
		}
		initializeMonitoring();
		data.setEditorInhibition(true);
		rng = new Random(System.currentTimeMillis());
		unique = MesquiteTrunk.getUniqueIDBase() + Math.abs(rng.nextInt());
		getProject().incrementProjectWindowSuppression();
		logln(getProgramName() + " analysis using data matrix " + data.getName());
		return true;
	}

	/*.................................................................................................................*/
	public boolean runProgramOnExternalProcess (String programCommand, String[] fileContents, String[] fileNames, String progTitle) {

		/*  ============ SETTING UP THE RUN ============  */
		boolean success = externalProcRunner.setInputFiles(programCommand,fileContents, fileNames);
		if (!success){
			// give message about failure
			return false;
		}
		logFileNames = getLogFileNames();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		progIndicator = new ProgressIndicator(getProject(),progTitle, getProgramName() + " Search", 0, true);
		if (progIndicator!=null){
			progIndicator.start();
		}

		MesquiteMessage.logCurrentTime("\nStart of "+getProgramName()+" analysis: ");
		timer.start();

		// starting the process
		success = externalProcRunner.startExecution();
		
		// the process runs
		if (success)
			success = externalProcRunner.monitorExecution();
		else
			alert("The "+getProgramName()+" run encountered problems. ");  // better error message!

		// the process completed
		logln("\n"+getProgramName()+" analysis completed at " + getDateAndTime());
		double totalTime= timer.timeSinceVeryStartInSeconds();
		if (totalTime>120.0)
			logln("Total time: " + StringUtil.secondsToHHMMSS((int)totalTime));
		else
			logln("Total time: " + totalTime  + " seconds");
		if (progIndicator!=null)
			progIndicator.goAway();
		if (!success)
			logln("Execution of "+getProgramName()+" unsuccessful [1]");
		return success;
	}
	

	/*.................................................................................................................*/
	public Tree continueMonitoring(MesquiteCommand callBackCommand) {
		logln("Monitoring " + getProgramName() + " run begun.");
		getProject().incrementProjectWindowSuppression();

		initializeMonitoring();
		setFileNames();
		logFileNames = getLogFileNames();
		externalProcRunner.setOutputFileNamesToWatch(logFileNames);

		/*	MesquiteModule inferer = findEmployerWithDuty(TreeInferer.class);
		if (inferer != null)
			((TreeInferer)inferer).bringIntermediatesWindowToFront();*/
		boolean success = externalProcRunner.monitorExecution();

		if (progIndicator!=null)
			progIndicator.goAway();

		getProject().decrementProjectWindowSuppression();
		if (data != null)
			data.setEditorInhibition(false);
		if (callBackCommand != null)
			callBackCommand.doItMainThread(null,  null,  this);
		return null;
	}	

	/*.................................................................................................................*/
	public boolean queryTaxaOptions(Taxa taxa) {
		if (taxa==null)
			return true;
		SpecsSetVector ssv  = taxa.getSpecSetsVector(TaxaSelectionSet.class);
		if (ssv==null || ssv.size()<=0)
			return true;

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getProgramName()+" Outgroup Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getProgramName()+" Outgroup Options");

		boolean specifyOutgroup = false;

		Choice taxonSetChoice = null;
		Checkbox specifyOutgroupBox = dialog.addCheckBox("specify outgroup", specifyOutgroup);
		taxonSetChoice = dialog.addPopUpMenu ("Outgroups: ", ssv, 0);


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			specifyOutgroup = specifyOutgroupBox.getState();
			outgroupTaxSetString = taxonSetChoice.getSelectedItem();
			if (!specifyOutgroup)
				outgroupTaxSetString="";
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	public void runFilesAvailable(int fileNum) {
	}

	/*.................................................................................................................*/

	public void runFilesAvailable(boolean[] filesAvailable) {
		if ((progIndicator!=null && progIndicator.isAborted()))
			return;
		String filePath = null;
		int fileNum=-1;
		String[] outputFilePaths = new String[filesAvailable.length];
		for (int i=0; i<outputFilePaths.length; i++)
			if (filesAvailable[i]){
				fileNum= i;
				break;
			}
		if (fileNum<0) return;
		runFilesAvailable(fileNum);
	}
	/*.................................................................................................................*/
	public void runFilesAvailable(){   // this should really only do the ones needed, not all of them.
		if (logFileNames==null)
			logFileNames = getLogFileNames();
		if (logFileNames==null)
			return;
		for (int i = 0; i<logFileNames.length; i++){
			runFilesAvailable(i);
		}
	}

}
