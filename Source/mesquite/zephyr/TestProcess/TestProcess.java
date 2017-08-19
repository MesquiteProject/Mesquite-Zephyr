
package mesquite.zephyr.TestProcess; 


import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.List;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;
import mesquite.molec.lib.*;
import org.dom4j.*;

/* ======================================================================== */
public class TestProcess extends UtilitiesAssistant  { 

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		addMenuItem(null, "Test Process...", makeCommand("testProcess", this));
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}

	/*.................................................................................................................*/
	public synchronized void testProcessPAUP(){ 
		String programPath = "/Users/david/Desktop/paup";
		String arguments = "/Users/david/Desktop/paupRun/PAUPCommands.txt";
		
		Process proc = ShellScriptUtil.startProcess("/Users/david/Desktop/paupRun/", "/Users/david/Desktop/paupRun/StOutLaDeDa3", "/Users/david/Desktop/paupRun/StErrLaDeDa3", programPath, arguments); 
		
		Debugg.println("after proc created");
		
		//proc.destroy();
		
		//Debugg.println("process exit value: " + proc.exitValue());

	}
	/*.................................................................................................................*/
	public synchronized void testProcess(){ 
		String programPath = "/usr/local/bin/muscle3.8.31_i86darwin64";
		MesquiteInteger errorCode = new MesquiteInteger(ShellScriptUtil.NOERROR);
		//String programPath = "/test/mafft";
		
		Process proc = ShellScriptUtil.startProcess(errorCode, "/test/", "/test/stOutLa2", "/test/stErrLa2", ExternalProcessManager.getStringArray(programPath, "-in", "inFile.fas")); 

		//Process proc = ShellScriptUtil.startProcess("/test/", "/test/stOutLa", "/test/stErrLa", ExternalProcessManager.getStringArray(programPath, "--thread", "-1", "--localpair", "--maxiterate", "1000", "inFile.fas")); 
		
		Debugg.println("after proc created");
		
		//proc.destroy();
		
		//Debugg.println("process exit value: " + proc.exitValue());

	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Starts a separate process.", null, commandName, "testProcess")) {
			testProcess();
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) {
		Snapshot temp = new Snapshot();
		return temp;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Test Processes";
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Test Process spawning.";
	}
}





