/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.TNTRunnerLocal;


import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;

import mesquite.categ.lib.*;
import mesquite.externalCommunication.AppHarvester.AppHarvester;
import mesquite.externalCommunication.lib.AppInformationFile;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.LocalScriptRunner.LocalScriptRunner;
import mesquite.zephyr.SSHRunner.SSHRunner;
import mesquite.zephyr.TNTTreesSSH.*;
import mesquite.zephyr.lib.*;
import mesquite.io.lib.*;


public class TNTRunnerLocal extends TNTRunner  {

	/*.................................................................................................................*/
	public String getExternalProcessRunnerModuleName(){
		return "#mesquite.zephyr.LocalScriptRunner.LocalScriptRunner";
	}
	/*.................................................................................................................*/
	public Class getExternalProcessRunnerClass(){
		return LocalScriptRunner.class;
	}
	/*.................................................................................................................*/
	public void setUpRunner() { 
		super.setUpRunner();
		hasApp = AppHarvester.builtinAppExists(this);
	}

	/*.................................................................................................................*/
	public AppInformationFile getAppInfoFile() {
		return AppHarvester.getAppInfoFileForProgram(this);
	}
	/*.................................................................................................................*/
	public boolean canUseLocalApp() {
		return true;
	}

	/*.................................................................................................................*/
	public String getAppOfficialName() {
		return "TNT";
	}

	/*.................................................................................................................*/

	public String getName() {
		return "TNT Trees [Local] Runner";
	}

	/*.................................................................................................................*/
	public boolean requiresLinuxTerminalCommands() {
		return true;
	}

	/*.................................................................................................................*

	public String getPrefixForProgramCommand() {
		return "nohup ";
	}
	/*.................................................................................................................*

	public String getSuffixForProgramCommand() {
		return " &";
	}
	/*.................................................................................................................*
	protected String getExecutableCommand() {
		String programCommand = externalProcRunner.getExecutableCommand();
		return programCommand + " bground";
	}
/**/




}
