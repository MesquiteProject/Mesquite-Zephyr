package mesquite.zephyr.SSHUtility;

import mesquite.lib.CommandChecker;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.duties.UtilitiesAssistant;
import mesquite.lib.ui.MesquiteSubmenuSpec;
import mesquite.zephyr.lib.SSHServerProfileManager;


public class SSHUtility extends UtilitiesAssistant {

	MesquiteString xmlPrefs= new MesquiteString();
	String xmlPrefsString = null;
	StringBuffer extraPreferences;
	SSHServerProfileManager sshServerProfileManager;  //ZQ static issue


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences(xmlPrefs);
		if (sshServerProfileManager == null)
			sshServerProfileManager= (SSHServerProfileManager)MesquiteTrunk.mesquiteTrunk.findEmployeeWithDuty(SSHServerProfileManager.class);
		if (sshServerProfileManager == null) {
			return false;
		} 
		xmlPrefsString = xmlPrefs.getValue();

		MesquiteSubmenuSpec mss = addSubmenu(null,"SSH Server Utility");

		addItemToSubmenu(null, mss, "Manage SSH Server Profiles...", makeCommand("manageServers", this));
	//	addLineToSubmenu(null, mss);
		return true;
	}

	/*.................................................................................................................*/
	public  void manageServers() {
		sshServerProfileManager.manageSSHServerProfiles();
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Manage SSH Servers", null, commandName, "manageServers")) {
			manageServers();
		} 
		/*else if (checker.compare(this.getClass(), "send commands", null, commandName, "sendCommands")) {
			communicator = new SimpleSSHCommunicator(this, null, null);
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
		}*/
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}


	public String getName() {
		return "SSH Utility";
	}

}
