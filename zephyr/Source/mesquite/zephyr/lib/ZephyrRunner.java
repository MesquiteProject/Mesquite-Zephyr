package mesquite.zephyr.lib;

import java.awt.Checkbox;
import java.awt.Choice;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.MolecularData;
import mesquite.lib.ExtensibleDialog;
import mesquite.lib.MesquiteCommand;
import mesquite.lib.MesquiteDouble;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteTimer;
import mesquite.lib.ProgressIndicator;
import mesquite.lib.SpecsSetVector;
import mesquite.lib.Taxa;
import mesquite.lib.TaxaSelectionSet;
import mesquite.lib.Tree;
import mesquite.lib.TreeVector;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.zephyr.GarliTrees.GarliTrees;

public abstract class ZephyrRunner extends MesquiteModule implements ExternalProcessRequester{

	protected String[] logFileNames;
	protected ExternalProcessRunner externalProcRunner;
	protected ProgressIndicator progIndicator;
	protected CategoricalData data;
	protected MesquiteTimer timer = new MesquiteTimer();


	
	protected String outgroupTaxSetString = "";
	protected int outgroupTaxSetNumber = 0;

	public abstract Tree getTrees(TreeVector trees, Taxa taxa, MCharactersDistribution matrix, long seed, MesquiteDouble finalScore);
	public abstract Tree retrieveTreeBlock(TreeVector treeList, MesquiteDouble finalScore);
	public abstract void initialize (ZephyrTreeSearcher ownerModule);
	public abstract boolean initializeTaxa (Taxa taxa);
	public abstract boolean bootstrap();


	public abstract void reconnectToRequester(MesquiteCommand command);
	public abstract String getProgramName();
	
	
	public void setFilePaths () {
	}
	public void initializeMonitoring () {
	}

	
	

	/*.................................................................................................................*/
	public Tree continueMonitoring(MesquiteCommand callBackCommand) {
		logln("Monitoring " + getProgramName() + " run begun.");
		getProject().incrementProjectWindowSuppression();

		initializeMonitoring();
		setFilePaths();
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


}
