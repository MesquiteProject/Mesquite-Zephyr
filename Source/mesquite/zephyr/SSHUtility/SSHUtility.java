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

		addItemToSubmenu(null, mss, "Send Commands", makeCommand("sendCommands", this));
		addItemToSubmenu(null, mss, "Start Run", makeCommand("startRun", this));
		addItemToSubmenu(null, mss, "Start Batch File", makeCommand("startBatch", this));
		addItemToSubmenu(null, mss, "List files", makeCommand("listFiles", this));
		addItemToSubmenu(null, mss, "Send file", makeCommand("sendFile", this));
		communicator = new SimpleSSHCommunicator(this, null, null);
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		String tempHostOld1 = "192.168.0.102";
		String tempHostOld2 = "10.0.0.7";
		String tempHost = "10.0.0.7";
		if (checker.compare(this.getClass(), "send commands", null, commandName, "sendCommands")) {
			communicator.setHost(tempHost);
			if (communicator.checkUsernamePassword(false)) {
				String programCommand = "/usr/local/bin/raxmlHPC-PTHREADS8210-AVX2  -s data.phy -n file.out  -m GTRGAMMAI -q multipleModelFile.txt -p 1083962335 -# 100 -b 1083962335 -T 2  >StandardOutputFile   2> StandardErrorFile";
				String[] commands = new String[]{"> running", programCommand, "rm -f running", "ls -la"};
				communicator.setRemoteServerDirectoryPath("/Users/david/Desktop/");
				communicator.setRemoteWorkingDirectoryName("sendCommands");
				communicator.sendCommands(commands, true, true, true);
			}
		} else if (checker.compare(this.getClass(), "Send file", null, commandName, "sendFile")) {
			communicator.setHost(tempHost);
			if (communicator.checkUsernamePassword(false)) {
				communicator.setRemoteServerDirectoryPath("/Users/david/Desktop/");
				communicator.setRemoteWorkingDirectoryName("destination");
				MesquiteString directoryName = new MesquiteString();
				MesquiteString fileName = new MesquiteString();
				String filePath = MesquiteFile.openFileDialog("Choose file to transfer", directoryName, fileName);
				communicator.sendFileToWorkingDirectory(filePath, fileName.getValue());
			}

		} else if (checker.compare(this.getClass(), "Start run", null, commandName, "startRun")) {
			communicator.setHost(tempHost);
			if (communicator.checkUsernamePassword(false)) {
				communicator.setRemoteServerDirectoryPath("/Users/david/Desktop/");
				communicator.setRemoteWorkingDirectoryName("runTest");

				String programCommand = "/usr/local/bin/raxmlHPC-PTHREADS8210-AVX2  -s data.phy -n file.out  -m GTRGAMMAI -q multipleModelFile.txt -p 1083962335 -# 100 -b 1083962335 -T 2  >/dev/tty   2> StandardErrorFile";


				String[] commands = new String[]{">running", programCommand, "rm -f running"};
				communicator.execBinary(programCommand);
			}

		} else if (checker.compare(this.getClass(), "Start batch", null, commandName, "startBatch")) {
			communicator.setHost(tempHost);
			if (communicator.checkUsernamePassword(false)) {
				communicator.setRemoteServerDirectoryPath("/Users/david/Desktop/");
				communicator.setRemoteWorkingDirectoryName("batchTest");
				String programCommand = "./Script.bat";
				communicator.execBinary(programCommand);
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
