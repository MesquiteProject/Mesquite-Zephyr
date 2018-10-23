package mesquite.zephyr.lib;

import mesquite.lib.ExtensibleDialog;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.ProgressIndicator;
import mesquite.lib.ShellScriptRunner;
import mesquite.lib.ShellScriptUtil;
import mesquite.lib.StringUtil;

public abstract class ScriptRunner extends ExternalProcessRunner {

	protected String runningFilePath = "";
	protected ExternalProcessRequester processRequester;
	protected String localRootDir = null;  // local directory for storing files on local machine
	public boolean scriptBased = false;
	public boolean addExitCommand = true;
	protected boolean visibleTerminal = false;

	/*.................................................................................................................*/
	public String getShellScript(String programCommand, String args) {
		runningFilePath = localRootDir + "running";//+ MesquiteFile.massageStringToFilePathSafe(unique);
		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(isWindows(), localRootDir)+ StringUtil.lineEnding());
		if (StringUtil.notEmpty(additionalShellScriptCommands))
			shellScript.append(additionalShellScriptCommands + StringUtil.lineEnding());
		// 30 June 2017: added redirect of stderr
		//		shellScript.append(programCommand + " " + args+ " 2> " + ShellScriptRunner.stErrorFileName +  StringUtil.lineEnding());
		String suffix = "";
		if (isLinux()&&requiresLinuxTerminalCommands()) {
			shellScript.append(getLinuxBashScriptPreCommand());
			suffix="\"";
		}
		if (!processRequester.allowStdErrRedirect())
			shellScript.append(programCommand + " " + args + suffix+StringUtil.lineEnding());
		else {
			if (visibleTerminal && isMacOSX()) {
				shellScript.append(programCommand + " " + args+ " >/dev/tty   2> " + ShellScriptRunner.stErrorFileName +  suffix+StringUtil.lineEnding());
			}
			else
				shellScript.append(programCommand + " " + args+ " > " + ShellScriptRunner.stOutFileName+ " 2> " + ShellScriptRunner.stErrorFileName + suffix+ StringUtil.lineEnding());
		}
		if (isLinux()&&requiresLinuxTerminalCommands())
			shellScript.append(getLinuxBashScriptPostCommand());
		shellScript.append(ShellScriptUtil.getRemoveCommand(isWindows(), runningFilePath));
		if (scriptBased&&addExitCommand && ShellScriptUtil.exitCommandIsAvailableAndUseful())
			shellScript.append("\n" + ShellScriptUtil.getExitCommand() + "\n");
		return shellScript.toString();
	}

	public boolean requiresLinuxTerminalCommands(){
		return processRequester.requiresLinuxTerminalCommands();
	}
	/** Following section on how to invoke a linux terminal and have it not be asynchronous comes from
	 * https://askubuntu.com/questions/627019/blocking-start-of-terminal, courtesy of users Byte Commander and terdon.
	 * */
	
	String linuxTerminalCommand = "gnome-terminal -x bash -c \"echo \\$$>$pidfile; ";

	public String getLinuxTerminalCommand() {
		return linuxTerminalCommand;
	}
	public void setLinuxTerminalCommand(String linuxTerminalCommand) {
		this.linuxTerminalCommand = linuxTerminalCommand;
	}
	
	public String getLinuxBashScriptPreCommand () {
		  return "delay=0.1\n" + 
		  		"pidfile=$(mktemp)\n";
		}
	public String getLinuxBashScriptPostCommand () {
		  return "until [ -s $pidfile ] \n" + 
		  		"    do sleep $delay\n" + 
		  		"done\n" + 
		  		"terminalpid=$(cat \"$pidfile\")\n" + 
		  		"rm $pidfile\n" + 
		  		"while ps -p $terminalpid > /dev/null 2>&1\n" + 
		  		"    do sleep $delay\n" + 
		  		"done\n";
		}

	/*.................................................................................................................*/
	public String getExecutablePath(){
		return null;
	}
	/*.................................................................................................................*/
	public String getExecutableCommand(String executablePath){
		if (isWindows())
			return "call " + StringUtil.protectFilePathForWindows(executablePath);
		else if (isLinux()) {
			if (requiresLinuxTerminalCommands())
				return getLinuxTerminalCommand() + " " + StringUtil.protectFilePathForUnix(executablePath);
			else 
				return " \"" + executablePath+"\"";
		}
		else
			return StringUtil.protectFilePathForUnix(executablePath);
	}
	/*.................................................................................................................*/
	public String getExecutableCommand(){
		return getExecutableCommand(getExecutablePath());
	}

}
