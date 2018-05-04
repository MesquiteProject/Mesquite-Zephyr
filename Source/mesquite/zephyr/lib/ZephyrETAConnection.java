/* Mesquite.zephyr source code.  Copyright 2007 and onwards D. Maddison and W. Maddison. 

Mesquite.zephyr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Zephry's web site is http://zephyr.mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.zephyr.lib;

import cgrb.eta.remote.api.*;

public class ZephyrETAConnection {
	ETAConnection etaConnection;

	public boolean establishConnection(String username, String password) {
		try {
			ETAConnection etaConnection = new ETAConnection(username, password);
		}
		catch (InValidUserNameException e) {
			return false;
		}
		return true;
	}
	
}
