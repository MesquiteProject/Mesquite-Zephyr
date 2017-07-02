/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://mesquitezephyr.wikispaces.com

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.zephyr.lib;

import mesquite.lib.*;

public interface ExternalProcessRequester {
	
	public void intializeAfterExternalProcessRunnerHired();

	public void runFilesAvailable(boolean[] filesAvailable);
	
	public void runFailed(String message);
	
	public void runFinished(String message);
	
	public String getProgramName();
	
	public String getRootNameForDirectory();
	
	public void setUserAborted(boolean userAborted);
	
	public String getExecutableName();
	
	public void prepareRunnerObject(Object obj);
	
	public String[] modifyOutputPaths(String[] outputFilePaths);
	
	public boolean localMacRunsRequireTerminalWindow();

	public boolean errorsAreFatal();
	
	public boolean allowStdErrRedirect();

	
}
