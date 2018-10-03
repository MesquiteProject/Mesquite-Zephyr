package mesquite.zephyr.SSHUtility;

import java.io.*;
import java.util.*;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.*;


public class SSHUtility extends UtilitiesAssistant {


	SimpleSSHCommunicator communicator;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {

		MesquiteSubmenuSpec mss = addSubmenu(null,"SSH Utility");

		addItemToSubmenu(null, mss, "List files", makeCommand("listFiles", this));
		communicator = new SimpleSSHCommunicator(this, null, null);
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "List Files", null, commandName, "listFiles")) {
			if (communicator.checkUsernamePassword(false)) {
				String[] commands = new String[]{"cd ..", "ls"};
				communicator.setWorkingDirectoryPath("/Users/david/Desktop");
				communicator.sendSSHCommands(commands);
			}
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}


	public String getName() {
		return "SSH Utility";
	}

}
