/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.zephyr.lib;


import mesquite.externalCommunication.lib.AppInformationFile;
import mesquite.externalCommunication.lib.AppUser;
import mesquite.lib.MesquiteModule;

public interface ExternalProcessRequester {
	
	public void intializeAfterExternalProcessRunnerHired();

	public void runFilesAvailable(boolean[] filesAvailable);
	
	public String getLogFileName();
	
	public void runFailed(String message);
	
	public void runFinished(String message);
	
	public String getProgramName();
	
	public int getProgramNumber();
	
	public boolean getDirectProcessConnectionAllowed();
	
	public boolean requiresLinuxTerminalCommands();
	
	public String getRootNameForDirectory();
	
	public void setUserAborted(boolean userAborted);
	
	public String getExecutableName();
	
	public boolean getBuiltInExecutableAllowed();
	
	public AppInformationFile getAppInfoFile();
	
	public AppUser getAppUser();
	
	public void appChooserDialogBoxEntryChanged() ;

	public MesquiteModule getModule();
	
	public void prepareRunnerObject(Object obj);
	
	public String[] modifyOutputPaths(String[] outputFilePaths);
	
	public boolean localScriptRunsRequireTerminalWindow();

	public boolean errorsAreFatal();
	
	public boolean stdErrIsTrueError(String stdErr);

	
	public boolean allowStdErrRedirect();

	public String getPrefixForProgramCommand();
	
	public String getSuffixForProgramCommand();
	
	public boolean removeCommandSameCommandLineAsProgramCommand();

	public String getRunDetailsForHelp() ;
	
	


}
