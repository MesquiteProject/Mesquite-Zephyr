package mesquite.zephyr.lib;

import mesquite.externalCommunication.lib.SSHCommunicator;
import mesquite.lib.Debugg;
import mesquite.lib.LongArray;
import mesquite.lib.MesquiteMessage;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteString;
import mesquite.lib.MesquiteTimer;
import mesquite.lib.ProgressIndicator;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.jcraft.jsch.*;

public class SimpleSSHCommunicator extends SSHCommunicator {


	public SimpleSSHCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
		super(mb, xmlPrefsString, outputFilePaths);
	}

	public  void sendCommand (Session session, String command) {
		Debugg.println("attempt to send: " + command);
		try{

			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec)channel).setErrStream(System.err);

			InputStream in=channel.getInputStream();
			channel.connect();

			byte[] tmp=new byte[1024];
			while(true){
				while(in.available()>0){
					int i=in.read(tmp, 0, 1024);
					if(i<0)break;
					Debugg.println(new String(tmp, 0, i));
				}
				if(channel.isClosed()){
					Debugg.println("exit-status: "+channel.getExitStatus());
					break;
				}
				try{Thread.sleep(1000);}catch(Exception ee){}
			}
			channel.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public  void sendCommand (Session session, String command, boolean waitForRunning) {
		Debugg.println("attempt to send: " + command);
		try{

			Channel channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec)channel).setErrStream(System.err);

			InputStream in=channel.getInputStream();
			channel.connect();

			while(true){
				// check for running file
				if(channel.isClosed()){
					Debugg.println("exit-status: "+channel.getExitStatus());
					break;
				}
				try{Thread.sleep(1000);}catch(Exception ee){}
			}
			channel.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	public  void execBinary (Session session, String command, boolean waitForRunning) {
		Debugg.println("attempt to send: " + command);
		try{

			ChannelExec channel=(ChannelExec)session.openChannel("exec");
			channel.setCommand(command);
			channel.setInputStream(null);
			channel.setErrStream(System.err);

			InputStream in=channel.getInputStream();
			channel.setCommand(command);
			channel.connect();
			//			channel.run();

			while(true){

				if(channel.isClosed()){
					Debugg.println("exit-status: "+channel.getExitStatus());
					break;
				}
				try{Thread.sleep(1000);}catch(Exception ee){}
			}
			channel.disconnect();


		}catch(Exception e){
			e.printStackTrace();
		}

	}




	public  void execBinary (String command) {
		try{
			Session session=createSession();
			session.connect();

			sendCommand(session, "cd " + getRemoteWorkingDirectoryPath());
			execBinary(session, command, true);

			ChannelSftp channelsftp=(ChannelSftp)session.openChannel("sftp");
			channelsftp.connect();
			Debugg.println("pwd after ChannelExec: "+channelsftp.pwd());
			channelsftp.disconnect();



			session.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	public  void execBatchFile (String command) {
		try{
			Session session=createSession();
			session.connect();

			sendCommand(session, "cd " +  getRemoteWorkingDirectoryPath());
			execBinary(session, command, true);

			session.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}


	public  void sendFileToWorkingDirectory (String localFilePath, String remoteFileName) {
		try{
			Session session=createSession();
			if (session==null)
				return;  // TODO: feedback
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			channel.cd( getRemoteWorkingDirectoryPath());
			channel.put(localFilePath, remoteFileName);

			channel.disconnect();
			session.disconnect();
		} catch(Exception e){
			ownerModule.logln("Could not SFTP files to working directory: " + e.getMessage());
			e.printStackTrace();
		}

	}


	/*.................................................................................................................*/
	public boolean sendJobToSSH(String[] commands) {
		if (checkUsernamePassword(true)) {
			ownerModule.storePreferences();
			sendCommands(commands,true,true,true);
		}
		return false;
	}

	

	public String getBaseURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAPIURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRegistrationURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSystemName() {
		return "SSH";
	}

}
