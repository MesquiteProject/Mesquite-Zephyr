package mesquite.zephyr.SSHTester;

/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

import mesquite.lib.*;

import mesquite.lib.duties.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.jcraft.jsch.*;



/* 
 * 
 * doesn't listen properly to changes
 * 
 * */

/*======================================================================== */
public class SSHTester extends FileAssistantA {
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
	//	https://github.com/mwiede/jsch/releases
			
			MesquiteBoolean answer = new MesquiteBoolean(true);
		MesquiteString userName = new MesquiteString();
		MesquiteString pwd = new MesquiteString();
	new UserNamePasswordDialog (containerOfModule(),  "user/pwd", null, null, null, "User", "Password",  answer,  userName, pwd);
		String user = userName.getValue();
        String password = pwd.getValue();
        String host = "192.168.87.120";
        int port = 22;
        String remoteFile = "/home/" + user + "/Desktop/sshtest.txt";

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            System.out.println("Establishing Connection...");
            session.connect();
            System.out.println("Connection established.");
            System.out.println("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("SFTP Channel created.");

            InputStream inputStream = sftpChannel.get(remoteFile);

            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
		return true;
	}
	

	/*.................................................................................................................*/
	public String getName() {
		return "Test SSH";
	}
	/*.................................................................................................................*/
	public boolean isSubstantive() {
		return true;
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return false;
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Summarizes reconstructions of state changes of a character over a series of trees.";
	}
	
}

