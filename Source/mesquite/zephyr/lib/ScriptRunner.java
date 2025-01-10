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
	public boolean addExitCommand = true;
	protected static final String scriptFileName = "Script.bat";
	protected String localScriptFilePath = "";

	/*.................................................................................................................*/
	public void setRunningFilePath() {
		runningFilePath = localRootDir + ShellScriptUtil.runningFileName;//+ MesquiteFile.massageStringToFilePathSafe(unique);

	}
	/*.................................................................................................................*/
	public ExternalProcessRequester getExternalProcessRequester() {
		return processRequester;

	}

	/*.................................................................................................................*/
	public String getShellScript(String programCommand, String workingDirectory, String args) {
		setRunningFilePath();
		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(isWindows(), workingDirectory)+ StringUtil.lineEnding(isWindows()));
		if (StringUtil.notEmpty(additionalShellScriptCommands))
			shellScript.append(additionalShellScriptCommands + StringUtil.lineEnding(isWindows()));
		// 30 June 2017: added redirect of stderr
		//		shellScript.append(programCommand + " " + args+ " 2> " + ShellScriptRunner.stErrorFileName +  StringUtil.lineEnding());
		
		programCommand= processRequester.getPrefixForProgramCommand()+ programCommand;
		
		String suffix = "";
		if (isLinux()&&requiresLinuxTerminalCommands()) {
			shellScript.append(getLinuxBashScriptPreCommand());
			suffix="\"";
		}
		suffix+= processRequester.getSuffixForProgramCommand();
			
		
		if (visibleTerminal && isMacOSX()) {
			 if (!processRequester.allowStdErrRedirect())
				 shellScript.append(programCommand + " " + args+ " >/dev/tty ");
			 else 
				 shellScript.append(programCommand + " " + args+ " >/dev/tty   2> " + ShellScriptRunner.stErrorFileName);
		}
		else if (!processRequester.allowStdErrRedirect())
			shellScript.append(programCommand + " " + args);
		else 
				shellScript.append(programCommand + " " + args+ " > " + ShellScriptRunner.stOutFileName+ " 2> " + ShellScriptRunner.stErrorFileName);
		
		if (!processRequester.removeCommandSameCommandLineAsProgramCommand()) {
			shellScript.append(suffix + StringUtil.lineEnding(isWindows()));
			if (isLinux()&&requiresLinuxTerminalCommands())
				shellScript.append(getLinuxBashScriptPostCommand());
		}
		else 
			shellScript.append(" && ");

		shellScript.append(ShellScriptUtil.getRemoveCommand(isWindows(), runningFilePath, false));
		if (processRequester.removeCommandSameCommandLineAsProgramCommand())
			shellScript.append(suffix);
		shellScript.append(StringUtil.lineEnding(isWindows()));
		if (scriptBased&&addExitCommand && ShellScriptUtil.exitCommandIsAvailableAndUseful(isWindows()))
			shellScript.append("\n" + ShellScriptUtil.getExitCommand(isMacOSX()) + "\n");
		return shellScript.toString();
	}

	public String getMessageIfUserAbortRequested () {
		if (scriptBased)
			return "\nIf you choose to stop, Mesquite will attempt to stop the analysis program. Normally this will work, but if there is an error, you may need to  "
					+ "use either the Task Manager (Windows) or the Activity Monitor (MacOS) or the equivalent to stop the other process.";
		return "";
	}
	public String getMessageIfCloseFileRequested () { 
		if (scriptBased)
			return "\nIf you allow the analysis to continue, it will stop on its own. Or, to force it to stop, you can open the file in Mesquite (if you save it), or use "
					+ "the Task Manager (Windows) or the Activity Monitor (MacOS) or the equivalent to stop it.";
		return "";
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
