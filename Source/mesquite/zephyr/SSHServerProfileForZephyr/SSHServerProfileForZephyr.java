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

package mesquite.zephyr.SSHServerProfileForZephyr;

import java.awt.event.WindowEvent;
import java.io.File;

import mesquite.lib.Listable;
import mesquite.lib.ListableVector;
import mesquite.lib.MesquiteBoolean;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteInteger;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.ExtensibleListDialog;
import mesquite.zephyr.lib.SSHServerProfile;
import mesquite.zephyr.lib.SSHServerProfileManager;


public class SSHServerProfileForZephyr extends SSHServerProfileManager {
	public ListableVector sshServerProfileVector;
	public String prefDirectoryName = "SSHServerProfilesForZephyr";
	ManageServerProfileDLOG chooseSSHServerProfileDialog;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		checkForProfileDirectory();
		sshServerProfileVector = new ListableVector();
		loadServerProfiles();
		if (getNumRules()<=0) {
			SSHServerProfile defaultRule = new SSHServerProfile();  
			sshServerProfileVector.addElement(defaultRule, false); 
			defaultRule.name ="Make New Server Profile";
			defaultRule.isDefault = true;
		}
		int ruleNumber = sshServerProfileVector.indexOfByNameIgnoreCase(sshServerProfileName);
		if (ruleNumber>=0)
			sshServerProfile = (SSHServerProfile)(sshServerProfileVector.elementAt(ruleNumber));
		return true;
	}
	/*.................................................................................................................*/
	
	public boolean optionsSpecified(){
		boolean db = StringUtil.notEmpty(sshServerProfileName);
		db = sshServerProfile!=null;
		int ruleNumber = sshServerProfileVector.indexOfByNameIgnoreCase(sshServerProfileName);
		
		return (StringUtil.notEmpty(sshServerProfileName) && sshServerProfile!=null) ;
	}
	public boolean hasOptions(){
		return true;
	}

	public  String getServerModifiers(int serverType) {
		return "";
	}
	/*.................................................................................................................*/
	public String[] getListOfProfiles(){
		if (sshServerProfileVector==null || sshServerProfileVector.size()<1)
			return null;
		return sshServerProfileVector.getStringArrayList();
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(ExtensibleDialog.defaultCANCEL);
		SSHServerProfileDialog dialog = new SSHServerProfileDialog(MesquiteTrunk.mesquiteTrunk.containerOfModule(), "Choose the server profile", buttonPressed, this, sshServerProfileName);		


		String s = "In preparing servers for GenBank submission, Mesquite saves data about the server (e.g., gene name, genetic code, etc.). ";
		dialog.appendToHelpString(s);

		dialog.completeAndShowDialog(true);
		boolean success=(buttonPressed.getValue()== dialog.defaultOK);
		if (success)  {
			sshServerProfile = dialog.getNameParsingRule();
			sshServerProfileName = sshServerProfile.getName();
		}
		storePreferences();  // do this here even if Cancel pressed as the File Locations subdialog box might have been used
		dialog.dispose();
		return success;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer();
		StringUtil.appendXMLTag(buffer, 2, "serverProfileName", sshServerProfileName);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("serverProfileName".equalsIgnoreCase(tag)) {
			sshServerProfileName = StringUtil.cleanXMLEscapeCharacters(content);
			
		}
	}
	/*.................................................................................................................*/
	public String getParameters () {
		if (StringUtil.blank(sshServerProfileName))
			return "Server profile: not chosen.";
		return "Server profile: " + sshServerProfileName;
	}

	/*.................................................................................................................*/
	public SSHServerProfile loadServerProfileFile(String cPath, String fileName, boolean requiresEnding,  boolean userDef) {
		File cFile = new File(cPath);
		if (cFile.exists() && !cFile.isDirectory() && (!requiresEnding || fileName.endsWith("xml"))) {
			String contents = MesquiteFile.getFileContentsAsString(cPath);
			if (!StringUtil.blank(contents)) {
				SSHServerProfile localServerProfile = new SSHServerProfile();
				localServerProfile.path = cPath;
				if  (localServerProfile.readXML(contents)){
					sshServerProfileVector.addElement(localServerProfile, false);
					return localServerProfile;
				}
				return null;
			}
		}
		return null;
	}


	public int getNumRules() {
		return sshServerProfileVector.getNumberOfParts();
	}

	/*.................................................................................................................*/
	private void loadServerProfiles(String path, File storageDir, boolean userDef){
		if (storageDir.exists() && storageDir.isDirectory()) {
			String[] fileNames = storageDir.list();
			StringArray.sort(fileNames);
			for (int i=0; i<fileNames.length; i++) {
				if (fileNames[i]==null )
					;
				else {
					String cPath = path + MesquiteFile.fileSeparator + fileNames[i];
					loadServerProfileFile(cPath, fileNames[i], true, userDef);
				}
			}
		}
	}
	private void loadServerProfiles(){
		String path = MesquiteModule.prefsDirectory+ MesquiteFile.fileSeparator + prefDirectoryName;
		File storageDir = new File(path);
		loadServerProfiles(path, storageDir, true);
	}

	public SSHServerProfile chooseServerProfile (SSHServerProfile spec) {

		SSHServerProfile serverProfile = spec;
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		chooseSSHServerProfileDialog = new ManageServerProfileDLOG(this, sshServerProfileName, buttonPressed);
		boolean ok = (buttonPressed.getValue()==0);

		if (ok && choice !=null) {
			sshServerProfileName = choice.getSelectedItem();
			int sL = sshServerProfileVector.indexOfByName(sshServerProfileName);
			if (sL >=0 && sL < sshServerProfileVector.size()) {
				serverProfile = (SSHServerProfile)sshServerProfileVector.elementAt(sL);
			}
			storePreferences();
		}
		chooseSSHServerProfileDialog.dispose();
		chooseSSHServerProfileDialog = null;
		return serverProfile;
	}
	/*.................................................................................................................*/
	public boolean manageSSHServerProfiles() {

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		chooseSSHServerProfileDialog = new ManageServerProfileDLOG(this, sshServerProfileName, buttonPressed);

		if (choice !=null) {
			sshServerProfileName = choice.getSelectedItem();
			int sL = sshServerProfileVector.indexOfByName(sshServerProfileName);
			if (sL >=0 && sL < sshServerProfileVector.size()) {
				sshServerProfile = (SSHServerProfile)sshServerProfileVector.elementAt(sL);
			}
			storePreferences();
		}
		chooseSSHServerProfileDialog.dispose();
		chooseSSHServerProfileDialog = null;
		return true;
	}


	/*.................................................................................................................*/
	public int numProfiles(){
		return sshServerProfileVector.size();
	}
	/*.................................................................................................................*/
	public MesquiteString getProfile(String name){
		int i = sshServerProfileVector.indexOfByName(name);
		if (i<0)
			return null;
		Listable listable = sshServerProfileVector.elementAt(i);
		if (listable!=null)
			return new MesquiteString(listable.getName());	
		else 
			return null;
	}
	/*.................................................................................................................*/
	public int findProfileIndex(String name){
		return sshServerProfileVector.indexOfByName(name);
	}
	/*.................................................................................................................*/
	public SSHServerProfile getSSHServerProfile(int index){
		return (SSHServerProfile)(sshServerProfileVector.elementAt(index));

	}
	/*.................................................................................................................*/
	public SSHServerProfile getSSHServerProfile(String name){
		int index = sshServerProfileVector.indexOfByName(name);
		return (SSHServerProfile)(sshServerProfileVector.elementAt(index));
	}
	/*.................................................................................................................*/
	public MesquiteString getProfile(int i){
		if (i<0 || i>= sshServerProfileVector.size())
			return null;
		Listable listable = sshServerProfileVector.elementAt(i);
		if (listable!=null)
			return new MesquiteString(listable.getName());	
		else 
			return null;
	}
	/*.................................................................................................................*/
	private void checkForProfileDirectory(){
		String base = MesquiteModule.prefsDirectory+ MesquiteFile.fileSeparator + prefDirectoryName;
		if (!MesquiteFile.fileExists(base)) {
			File f = new File(base);
			f.mkdir();
		}
	}

	/*.................................................................................................................*/
	private String newProfilePath(String name){
		checkForProfileDirectory();
		String base = MesquiteModule.prefsDirectory+ MesquiteFile.fileSeparator + prefDirectoryName;
		String candidate = base + MesquiteFile.fileSeparator + StringUtil.punctuationToUnderline(name)+ ".xml";
		if (!MesquiteFile.fileExists(candidate))
			return candidate;
		candidate = base + MesquiteFile.fileSeparator  + "specification1.xml";
		int count = 2;
		while (MesquiteFile.fileExists(candidate)){
			candidate = base + MesquiteFile.fileSeparator  + "specification" + (count++) + ".xml";
		}
		return candidate;
	}
	/*.................................................................................................................*/
	public void addProfile(SSHServerProfile serverProfile, String name){
		serverProfile.save(newProfilePath(name), name);
		sshServerProfileVector.addElement(serverProfile, false);	
		if (choice!=null)
			choice.add(name);
		sshServerProfileName = name;
		

		//	return s;
	}
	/*.................................................................................................................*/
	public SSHServerProfile duplicate(SSHServerProfile serverProfile, String name){
		SSHServerProfile specification = new SSHServerProfile(serverProfile);
		specification.setName(name);
		specification.setPath(newProfilePath(name));
		specification.save();
		sshServerProfileVector.addElement(specification, false);	
		if (choice!=null)
			choice.add(name);
		sshServerProfileName = name;
		return specification;
		//	return s;
	}
	/*.................................................................................................................*/
	void renameProfile(int i, String name){
		SSHServerProfile specification = (SSHServerProfile)sshServerProfileVector.elementAt(i);
		specification.setName(name);
		specification.save();
		if (choice!=null) {
			choice.remove(i);
			choice.insert(name,i);
		}
		sshServerProfileName=name;
	}
	/*.................................................................................................................*/
	void deleteProfile(int i){
		SSHServerProfile specification = (SSHServerProfile)sshServerProfileVector.elementAt(i);
		if (specification!=null) {
			String oldTemplateName = specification.getName();
			File f = new File(specification.path);
			f.delete();		
			//MesquiteString s = getNameRule(i);
			//if (s !=null)
			sshServerProfileVector.removeElement(specification, false);  //deletes it from the vector
			if (choice!=null)
				choice.remove(i);
			if (sshServerProfileVector.size() == 0){
				SSHServerProfile defaultRule = new SSHServerProfile();  
				sshServerProfileVector.addElement(defaultRule, false); 
				defaultRule.name ="Make New Server Profile";
				defaultRule.isDefault = true;
		}
		}
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Server Profile for GenBank";
	}

}	



/*=======================================================================*/
class ManageServerProfileDLOG extends ExtensibleListDialog {
	SSHServerProfileForZephyr ownerModule;
	boolean editLastItem = false;

	public ManageServerProfileDLOG (SSHServerProfileForZephyr ownerModule, String nameParsingRulesName, MesquiteInteger buttonPressed){
		super(ownerModule.containerOfModule(), "Server Profile Manager", "Server Profile", buttonPressed, ownerModule.sshServerProfileVector);
		this.ownerModule = ownerModule;
		completeAndShowDialog("Done", null, true, null);

	}
	/*.................................................................................................................*/
	public void windowOpened(WindowEvent e){
		if (editLastItem)
			editNumberedElement(getLastItem());
		editLastItem = false;
		super.windowOpened(e);
	}
	/*.................................................................................................................*/
	/** this is the name of the class of objects */
	public  String objectName(){
		return "SSH Server Profile";
	}
	/*.................................................................................................................*/
	/** this is the name of the class of objects */
	public  String pluralObjectName(){
		return "SSH Server Profiles";
	}

	/*.................................................................................................................*/
	public Listable createNewElement(String name, MesquiteBoolean success){
		hide();
		SSHServerProfile serverProfile = new SSHServerProfile();
		if (serverProfile.queryOptions(name)) {
			addNewElement(serverProfile,name);  //add name to list
			ownerModule.addProfile(serverProfile, name);
			if (success!=null) success.setValue(true);
			if (ownerModule.sshServerProfileVector.size()>1){
				SSHServerProfile firstProfile = (SSHServerProfile)ownerModule.sshServerProfileVector.elementAt(0);
				if (firstProfile.isDefault) {
					ownerModule.sshServerProfileVector.removeElement(firstProfile, false);  //deletes it from the vector
					if (ownerModule.choice!=null) {
						ownerModule.choice.remove(0);
					}
				}
					
			}
		
			setVisible(true);
			return serverProfile;

		}
		else  {
			if (success!=null) success.setValue(false);
			setVisible(true);
			return null;
		}
	}
	
	protected boolean okToDeleteElement(Listable element){
		if (((SSHServerProfile)element).isDefault){
			ownerModule.alert("This server profile is just a placeholder. You can't delete it, although it will go away after you create a new one and you reopen the dialog.");
			return false;
		}
		return true;
	}
	/*.................................................................................................................*/
	public void deleteElement(int item, int newSelectedItem){
		hide();
		ownerModule.deleteProfile(item);
		setVisible(true);
	}
	protected boolean okToRenameElement(Listable element){
		if (((SSHServerProfile)element).isDefault){
			ownerModule.alert("This server profile is just a placeholder. You can't rename it. To create your server profile, hit the New button");
			return false;
		}
		return true;
	}
	/*.................................................................................................................*/
	public void renameElement(int item, Listable element, String newName){
		ownerModule.renameProfile(item,newName);
	}
	protected boolean okToDuplicateElement(Listable element){
		if (((SSHServerProfile)element).isDefault){
			ownerModule.alert("This server profile is just a placeholder with nothing in it. You can't duplicate it. To create a new server profile, hit the New button");
				
			return false;
		}
		return true;
	}
	/*.................................................................................................................*/
	public Listable duplicateElement(String name){
		SSHServerProfile serverProfile = ownerModule.duplicate((SSHServerProfile)currentElement, name);
		return serverProfile;
	}
	/*.................................................................................................................*/
	public boolean getEditable(int item){
		return true;
	}
	/*.................................................................................................................*/
	public void editElement(int item){
		//hide();
		SSHServerProfile serverProfile = ((SSHServerProfile)ownerModule.sshServerProfileVector.elementAt(item));
		if (serverProfile.isDefault){
			ownerModule.alert("This server profile is just a placeholder. You can't edit it. To create a new server profile, hit the New button");
			return;
		}
		if (serverProfile.queryOptions(serverProfile.getName()))
			serverProfile.save();
		setVisible(true);
	}




}

