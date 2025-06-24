package mesquite.zephyr.SSHServerProfileForZephyr;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import mesquite.lib.MesquiteInteger;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.MesquiteWindow;
import mesquite.zephyr.lib.SSHServerProfile;


public class SSHServerProfileDialog extends ExtensibleDialog {
	
	private SSHServerProfileForZephyr sshServerProfileManager;
	private SSHServerProfile sshServerProfile;
	private Choice sshServerProfileChoice;	
	private String sshServerProfileName="";	
	
	public SSHServerProfileDialog(MesquiteWindow parent, String title, MesquiteInteger buttonPressed,
			SSHServerProfileForZephyr sshServerProfileManager, String defaultRuleName) {
		super(parent, title, buttonPressed);
		this.sshServerProfileManager = sshServerProfileManager;
		this.sshServerProfileName = defaultRuleName;
		addNameParsingComponents();
	}

	
	protected void addNameParsingComponents() {
		addLabel(getTitle());
		
		sshServerProfileChoice = addPopUpMenu("Server Profiles", sshServerProfileManager.sshServerProfileVector.getElementArray(), 0);
		sshServerProfileManager.setChoice(sshServerProfileChoice);
		sshServerProfileChoice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getItemSelectable() == sshServerProfileChoice){
					getNameRuleFromChoice();
				}				
			}
		});
		if (sshServerProfileChoice!=null) {
			boolean noChoiceItems = (sshServerProfileChoice.getItemCount()<=0);
			int sL = sshServerProfileManager.sshServerProfileVector.indexOfByName(sshServerProfileName);
			if (sL <0) {
				sL = 0;
			}		
			if (!noChoiceItems) {
				sshServerProfileChoice.select(sL); 
				sshServerProfile = (SSHServerProfile)(sshServerProfileManager.sshServerProfileVector.elementAt(sL));
			}
		}	
		suppressNewPanel();
		GridBagConstraints gridConstraints;
		gridConstraints = getGridBagConstraints();
		gridConstraints.fill = GridBagConstraints.NONE;
		setGridBagConstraints(gridConstraints);
		Panel panel = addNewDialogPanel(gridConstraints);
		String editNameParserButtonString = "Edit Specifications...";
		Button editNameParsersButton = addAButton(editNameParserButtonString, panel);
		editNameParsersButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (sshServerProfileManager!=null) {
					sshServerProfile = sshServerProfileManager.chooseServerProfile(sshServerProfile);
				}				
			}
		});		
	}
	
	public void getNameRuleFromChoice() {
		sshServerProfile=null;
		if (sshServerProfileChoice!=null) {
			sshServerProfileName = sshServerProfileChoice.getSelectedItem();
			boolean noChoiceItems = (sshServerProfileChoice.getItemCount()<=0);
			int sL = sshServerProfileChoice.getSelectedIndex();
			if (sL <0) {
				sL = 0;
			}		
			if (!noChoiceItems) {
				sshServerProfile = (SSHServerProfile)(sshServerProfileManager.sshServerProfileVector.elementAt(sL));
			}
		}
		 	if (sshServerProfile==null){ 
				sshServerProfile = new SSHServerProfile();  //make default one	}
		 	}
	}

	public SSHServerProfile getNameParsingRule() {
		return sshServerProfile;
	}	
}
