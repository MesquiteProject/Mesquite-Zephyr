package mesquite.zephyr.LocalScriptRunner;

import java.util.Random;

import mesquite.lib.*;
import mesquite.zephyr.lib.*;

public class LocalScriptRunner extends ExternalProcessRunner {
	ShellScriptRunner scriptRunner;
	Random rng;
	String rootDir;
	String garliPath;

	public String getName() {
		return "Local Script Runner";
	}
	public String getExplanation() {
		return "Runs local scripts.";
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean requestPrimaryChoice(){
		return true;
	}
	/*.================================================================..*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		rng = new Random(System.currentTimeMillis());
		loadPreferences();
		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("garliPath".equalsIgnoreCase(tag)) 
			garliPath = StringUtil.cleanXMLEscapeCharacters(content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "garliPath", garliPath);  
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (scriptRunner != null){
			temp.addLine("reviveScriptRunner ");
			temp.addLine("tell It");
			temp.incorporate(scriptRunner.getSnapshot(file), true);
			temp.addLine("endTell");
			//	temp.addLine("startMonitoring ");  this happens via reconnectToRequester so that it happens on the separate thread
		}
		if (rootDir != null)
			temp.addLine("setRootDir " +  ParseUtil.tokenize(rootDir));
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the running file path", "[file path]", commandName, "reviveScriptRunner")) {
			Debugg.println("ReviveScriptRunner");
			scriptRunner = new ShellScriptRunner();
			//scriptRunner.setOutputProcessor(this);
			//scriptRunner.setWatcher(this);
			return scriptRunner;
		}
		else if (checker.compare(this.getClass(), "Sets root directory", null, commandName, "setRootDir")) {
			rootDir = parser.getFirstToken(arguments);
		}
		return null;
	}	

	// setting the requester, to whom this runner will communicate about the run
	public  void setProcessRequester(ExternalProcessRequester processRequester){
	}

	// given the opportunity to fill in options for user
	public  void fillPanelInDialog(ExtensibleDialog dialog, MesquitePanel panel){
	}
	public boolean optionsChosen(ExtensibleDialog dialog, MesquitePanel panel, MesquiteBoolean okChosen){
		return false;
	}

	// the actual data & scripts.  
	public void setInputFiles(String script, String[] files){  //assumes for now that all input files are in the same directory
	}

	public void setOutputFilePathsToWatch(String[] filePaths){
	}

	public String getOutputFile(String filePath){
		return null;
	}


	// starting the run
	public boolean beginExecution(){  //do we assume these are disconnectable?
		return false;
	}

	public String checkStatus(){
		return null;
	}
	public boolean stopExecution(){
		return false;
	}

	//results can be harvested by getOutputFile	
}
