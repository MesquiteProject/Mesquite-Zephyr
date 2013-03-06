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
