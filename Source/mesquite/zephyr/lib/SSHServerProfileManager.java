/* Mesquite Chromaseq source code.  Copyright 2005-2011 David Maddison and Wayne Maddison.
Version 1.0   December 2011
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.zephyr.lib;


import mesquite.lib.*;
import mesquite.lib.duties.*;
import java.awt.*;

public abstract class SSHServerProfileManager extends MesquiteInit {
	public Choice choice;
	protected SSHServerProfile sshServerProfile=null;
	protected String sshServerProfileName="";	

	public Class getDutyClass(){
		return SSHServerProfileManager.class;
	}

	public abstract String[] getListOfProfiles();

	public abstract boolean manageSSHServerProfiles();

	public boolean queryOptions(){
		return true;
	}

	public boolean optionsSpecified(){
		return false;
	}
	public String getSshServerProfileName() {
		return sshServerProfileName;
	}

	public void setSshServerProfileName(String sshServerProfileName) {
		this.sshServerProfileName = sshServerProfileName;
	}

	/*.................................................................................................................*/
	public abstract SSHServerProfile getSSHServerProfile(int index);

	/*.................................................................................................................*/
	public abstract int findProfileIndex(String name);

	/*.................................................................................................................*/
	public abstract SSHServerProfile getSSHServerProfile(String name);


	public boolean hasOptions(){
		return false;
	}

	public void setChoice (Choice choice) {
		this.choice = choice;
	}
	public Choice getChoice() {
		return choice;
	}


}


