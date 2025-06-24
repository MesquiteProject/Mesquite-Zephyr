package mesquite.zephyr.lib;

import mesquite.lib.*;



public abstract class RESTCommunicator extends RemoteCommunicator {

	public RESTCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
//		if (xmlPrefsString != null)
//			XMLUtil.readXMLPreferences(mb, this, xmlPrefsString);
		this.outputFilePaths = outputFilePaths;
		ownerModule = mb;
	}

	/*.................................................................................................................*/
	public abstract String getRESTURL();

	/*.................................................................................................................*/
	public String getSystemTypeName() {
		return " REST";
	}


}
