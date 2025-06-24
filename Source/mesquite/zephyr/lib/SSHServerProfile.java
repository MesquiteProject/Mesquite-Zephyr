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

import java.awt.*;
import java.util.regex.*;

import org.dom4j.*;

import mesquite.lib.*;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.SingleLineTextField;
import mesquite.externalCommunication.lib.*;

public class SSHServerProfile implements Listable, Explainable, UsernamePasswordKeeper {

	public String name = "SSH Server";
	public boolean isDefault = false; //if true, then can't rename or edit;
	public String host = "";   
	static String macOS = "MacOS X";
	static String linuxOS = "Linux";
	static String windowsOS = "Windows";
	public String OSType = macOS;  
	public String description = "";  
	public String username = "";  
	protected String tempFileDirectory = "";  
	public int pollingInterval = 30;
	public int maxCores = 2;

	public static int numProgramsSupported = 6;
	public static int IQTREE = 0;
	public static int RAxML = 1;
	public static int RAxMLNG = 2;
	public static int GARLI = 3;
	public static int PAUP = 4;
	public static int TNT = 5;
	public String[] programNames = new String[]{"IQ-TREE", "RAxML", "RAxML-NG", "GARLI", "PAUP*", "TNT"};
	public String[] programPaths;
	public String RAxMLpath = "";
	
	private String password = "";  // for temporary storage
	
	public String path;

	public String explanation;

	public SSHServerProfile() {
		initializeProgramPaths();
	}

	public SSHServerProfile(SSHServerProfile spec) {
		initializeProgramPaths();
		if (spec!=null) {
			name = spec.name;
			description = spec.description;
			host = spec.host;
			OSType = spec.OSType;
			tempFileDirectory = spec.tempFileDirectory;
			username = spec.username;
			for (int i=0; i<numProgramsSupported; i++)
				programPaths[i] = spec.programPaths[i];

			if (MesquiteInteger.isCombinable(spec.pollingInterval))
				pollingInterval = spec.pollingInterval;
			if (MesquiteInteger.isCombinable(spec.maxCores))
				maxCores = spec.maxCores;
		}
	}

	void initializeProgramPaths() {
		programPaths= new String[numProgramsSupported];
		for (int i=0; i<numProgramsSupported; i++)
			programPaths[i] = "";
	}

	public void setOSType(String OSType){
		this.OSType = OSType;
	}
	public String getOSType(){
		return OSType;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isMacOSX(){
		return macOS.equalsIgnoreCase(OSType);
	}
	public boolean isLinux(){
		return linuxOS.equalsIgnoreCase(OSType);
	}
	public boolean isWindows(){
		return windowsOS.equalsIgnoreCase(OSType);
	}
	public void setPath(String path){
		this.path = path;
	}
	public boolean validProgramPath(int program){
		return StringUtil.notEmpty(programPaths[program]);
	}
	public String getProgramPath(int program){
		return programPaths[program];
	}
	public void setName(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
	public void setUsername(String username){
		this.username = username;
	}
	public String getUsername(){
		return username;
	}
	/*.................................................................................................................*/
	public String getDirectorySeparator() {
		if (isWindows())
			return "\\";
		return "/";
	}

	public void adjustTempFileDirectory(){
		tempFileDirectory = StringUtil.stripTrailingWhitespace(tempFileDirectory);
		if (StringUtil.blank(tempFileDirectory))
				return;

		String lastCharacter=tempFileDirectory.substring(tempFileDirectory.length()-1, tempFileDirectory.length());
		if (!getDirectorySeparator().equalsIgnoreCase(lastCharacter))  // make sure it ends in a separator
			tempFileDirectory = tempFileDirectory+getDirectorySeparator();
	}
	public String getTempFileDirectory(){
		adjustTempFileDirectory();
		return tempFileDirectory;
	}
	public int getPollingInterval(){
		return pollingInterval;
	}
	public int getMaxCores(){
		return maxCores;
	}
	public String getDescription(){
		return description;
	}

	public String getHost(){
		return host;
	}
	public String getExplanation(){
		return explanation;
	}
	String getProcessedTokenForWrite(String s) {
		if (" ".equals(s))
			return "\\ ";
		else if (StringUtil.blank(s))
			return " ";
		else
			return s;
	}
	public String getXML(){
		Element mesquiteElement = DocumentHelper.createElement("mesquite");
		Document doc = DocumentHelper.createDocument(mesquiteElement);
		Element sequenceProfileElement = DocumentHelper.createElement("sshServerProfile");
		mesquiteElement.add(sequenceProfileElement);
		XMLUtil.addFilledElement(sequenceProfileElement, "version","1");
		Element boundedByTokensElement = DocumentHelper.createElement("boundedByTokens");
		sequenceProfileElement.add(boundedByTokensElement);
		XMLUtil.addFilledElement(boundedByTokensElement, "name",name);
		XMLUtil.addFilledElement(boundedByTokensElement, "host",DocumentHelper.createCDATA(host));
		XMLUtil.addFilledElement(boundedByTokensElement, "username",DocumentHelper.createCDATA(username));
		XMLUtil.addFilledElement(boundedByTokensElement, "OSType",DocumentHelper.createCDATA(OSType));
		XMLUtil.addFilledElement(boundedByTokensElement, "description",DocumentHelper.createCDATA(description));
		XMLUtil.addFilledElement(boundedByTokensElement, "tempFileDirectory",DocumentHelper.createCDATA(tempFileDirectory));
		XMLUtil.addFilledElement(boundedByTokensElement, "pollingInterval",DocumentHelper.createCDATA(""+pollingInterval));
		XMLUtil.addFilledElement(boundedByTokensElement, "maxCores",DocumentHelper.createCDATA(""+maxCores));
		for (int i=0; i<numProgramsSupported; i++)
			XMLUtil.addFilledElement(boundedByTokensElement, StringUtil.cleanseStringOfFancyChars(programNames[i]+"_path", false, true),DocumentHelper.createCDATA(programPaths[i]));

		return XMLUtil.getDocumentAsXMLString(doc);
	}
	public void save(String path, String name){
		this.name = name;
		this.path = path;
		MesquiteFile.putFileContents(path, getXML(), true); 	
	}

	public void save(){
		if (path!=null)
			MesquiteFile.putFileContents(path, getXML(), true); 	
	}

	/*.................................................................................................................*/
	public boolean readXML(String contents) {
		Element root = XMLUtil.getRootXMLElementFromString("mesquite", contents);
		if (root==null)
			return false;

		Element sequenceProfileElement = root.element("sshServerProfile");
		if (sequenceProfileElement != null) {
			Element versionElement = sequenceProfileElement.element("version");
			if (versionElement == null || !versionElement.getText().equals("1")) {
				return false;
			}
			Element boundedByTokens = sequenceProfileElement.element("boundedByTokens");
			if (boundedByTokens == null) {
				return false;
			}
			name = boundedByTokens.elementText("name");
			host = boundedByTokens.elementText("host");
			username = boundedByTokens.elementText("username");
			OSType = boundedByTokens.elementText("OSType");
			description = boundedByTokens.elementText("description");
			tempFileDirectory = boundedByTokens.elementText("tempFileDirectory");
			pollingInterval = MesquiteInteger.fromString(boundedByTokens.elementText("pollingInterval"));
			int tempMaxCores = MesquiteInteger.fromString(boundedByTokens.elementText("maxCores"));
			if (MesquiteInteger.isCombinable(tempMaxCores))
				maxCores = tempMaxCores;
			for (int i=0; i<numProgramsSupported; i++)
				programPaths[i] = boundedByTokens.elementText(StringUtil.cleanseStringOfFancyChars(programNames[i]+"_path", false, true));

		} else {
			return false;
		}
		return true;
	}

	/*.................................................................................................................*
	public String processTokenAfterRead(String s) {
		if ("\\ ".equals(s))
			return " ";
		else if (StringUtil.blank(s))
			return "";
		else
			return s;
	}
	/*.................................................................................................................*/
	public String[] OSStrings() {   // from http://www.insdc.org/controlled-vocabulary-moltype-qualifier
		return new String[] {
				linuxOS, 
				macOS, 
				windowsOS, 
		};
	}

	Choice OSTypeChoice;
	/*.................................................................................................................*/
	public boolean queryOptions(String name) {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(MesquiteTrunk.mesquiteTrunk.containerOfModule(), "SSH Server Profile",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		String s = "This allows you to create a profile for each server on which you can analyze data using Zephyr. You must have an account on the server, and the ability to SSH into the server. The file paths to be entered here are file paths on the remote server.\n";
		dialog.appendToHelpString(s);

		if (!StringUtil.blank(name))
			dialog.addLabel("SSH Server Profile ("+name+")");
		else
			dialog.addLabel("SSH Server Profile");

		SingleLineTextField nameField = dialog.addTextField("Name of Server:", name,60, true);
		SingleLineTextField descriptionField = dialog.addTextField("Description:", description,60, true);
		SingleLineTextField hostField = dialog.addTextField("Server domain name or IP address:", host,50, true);
		int item = StringArray.indexOfIgnoreCase(OSStrings(), OSType);
		if (item<0) item=0;
		OSTypeChoice = dialog.addPopUpMenu("Operating System", OSStrings(), 	item);
		SingleLineTextField usernameField = dialog.addTextField("Default username:", username,50, true);
		IntegerField pollingIntervalField = dialog.addIntegerField("Interval (in seconds) between server checks", pollingInterval, 10, 1, Integer.MAX_VALUE);
		IntegerField maxCoresField = dialog.addIntegerField("Maximum number of processor to use on server", maxCores, 10, 1, Integer.MAX_VALUE);
		SingleLineTextField tempFileDirectoryField = dialog.addTextField("Path to temporary files directory:", tempFileDirectory, 60, true);

		SingleLineTextField[] pathsField = new SingleLineTextField[numProgramsSupported];
		for (int i=0; i<numProgramsSupported; i++)
			pathsField[i] = dialog.addTextField("Path to "+programNames[i]+": ", programPaths[i],60, true);



		dialog.completeAndShowDialog(true);

		if (buttonPressed.getValue()==0)  {
			description = descriptionField.getText();
			host = hostField.getText();
			username = usernameField.getText();
			OSType = OSTypeChoice.getSelectedItem();
			tempFileDirectory = tempFileDirectoryField.getText();
			pollingInterval = pollingIntervalField.getValue();
			maxCores = maxCoresField.getValue();
			name = nameField.getText();
			for (int i=0; i<numProgramsSupported; i++)
				programPaths[i] = pathsField[i].getText();
		}
		//storePreferences();  // do this here even if Cancel pressed as the File Locations subdialog box might have been used
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}



}
