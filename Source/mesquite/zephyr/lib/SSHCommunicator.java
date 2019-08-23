package mesquite.zephyr.lib;

import mesquite.lib.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;
import mesquite.externalCommunication.lib.*;
import java.net.*;


import com.jcraft.jsch.*;


public  class SSHCommunicator extends RemoteCommunicator {

//TODO: implement kill process: https://stackoverflow.com/questions/22476506/kill-process-before-disconnecting

	//	protected String remoteWorkingDirectoryPath = "";
	protected String remoteWorkingDirectoryName = "";
	protected String remoteServerDirectoryPath = "";
	protected ProgressIndicator progressIndicator;
	protected String sshServerProfileName = "";
	protected SSHServerProfile sshServerProfile;
	String runDetailsForHelp = "";
	


	public SSHCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
		this.outputFilePaths = outputFilePaths;
		ownerModule = mb;
		//forgetPassword();
	}
	public void setProgressIndicator(ProgressIndicator progressIndicator) {
		this.progressIndicator= progressIndicator;
	}

	public Session createSession() {
		try {
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no"); //TODO: have options
			config.put( "PreferredAuthentications", "publickey,keyboard-interactive,password");
			JSch jsch = new JSch();
			Session session=jsch.getSession(sshServerProfile.getUsername(), host, 22);
			session.setPassword(sshServerProfile.getPassword());
			session.setConfig(config);
			if (MesquiteTrunk.debugMode)
				ownerModule.logln("Successfully created session to " + host);

			return session;
		} catch (Exception e) {
			ownerModule.logln("WARNING: could not create Session: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	public SSHServerProfile getSSHServerProfile() {
		return sshServerProfile;
	}
	public void setSSHServerProfile(SSHServerProfile sshServerProfile) {
		this.sshServerProfile = sshServerProfile;
		setUsernamePasswordKeeper(this.sshServerProfile);
	}

	public String getSshServerProfileName() {
		return sshServerProfileName;
	}
	public void setSshServerProfileName(String sshServerProfileName) {
		this.sshServerProfileName = sshServerProfileName;
	}

	public void setOutputFilePaths(String[] outputFilePaths) {
		this.outputFilePaths = outputFilePaths;
	}

	/*.................................................................................................................*/
	public String getRunDetailsForHelp() {
		return runDetailsForHelp;
	}

	/*.................................................................................................................*/
	public void setRunDetailsForHelp(String runDetailsForHelp) {
		this.runDetailsForHelp = runDetailsForHelp;
	}

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getUserName() {
		String s = sshServerProfile.getUsername();
		return sshServerProfile.getUsername();
	}
	public void setUserName(String username) {
		sshServerProfile.setUsername(username);
	}
	public String getRemoteWorkingDirectoryName() {
		return remoteWorkingDirectoryName;
	}

	
	
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the remote working directory path", "[file path]", commandName, "setRemoteDirectoryPath")) {
			Parser parser = new Parser();
			String path = parser.getFirstToken(arguments);
			parser.setString(path);
			String separator = sshServerProfile.getDirectorySeparator();
			String name = Parser.getLastItem(path, separator, null, true);
			String directory = Parser.getAllButLastItem(path, separator, null, true);
			if (!StringUtil.endsWithIgnoreCase(directory, separator))
				directory += separator;
			
			setRemoteWorkingDirectoryName(name);
			setRemoteServerDirectoryPath(directory);
			
			return sshServerProfile;
		} else if (checker.compare(this.getClass(), "Sets the username", "[username]", commandName, "setUserName")) {
			Parser parser = new Parser();
			String name = parser.getFirstToken(arguments);
			setUserName(name);
			return null;
		} 
		return null;
	}	

	

	public  boolean checkForUniqueRemoteWorkingDirectoryName (String executableName) {
		boolean connected = false;
		String proposedName="";
		try {
			Session session=createSession();
			session.connect();

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteWorkingDirectoryPath());		
			connected=true;
			boolean isDirectory = true;
			boolean isLink = false;

			int i = 1;
			while (isDirectory && !isLink) {
				proposedName = executableName +"-" + StringUtil.getDateDayOnly()+ "."+i;
				SftpATTRS sftpATTRS = channel.stat(proposedName);
				isDirectory = sftpATTRS.isDir();
				isLink = sftpATTRS.isLink();
				i++;
			}

			remoteWorkingDirectoryName = proposedName;

			
			channel.disconnect();
			session.disconnect();

		}  catch (Exception e) {
			if (ConnectionOrAuthorizationFailure(e)) {
				return false;
			} else if (connected) {
				remoteWorkingDirectoryName = proposedName;
			}
			else {
				ownerModule.logln("WARNING: could not communicate with SSH server to identify working directory folder: " + e.getMessage());
				return false;
			}
		}
		return true;
	}
/*
	public void checkForUniqueRemoteWorkingDirectoryName(String executableName) {
		int i = 1;
		String proposedName = executableName +"-" + StringUtil.getDateDayOnly()+ ".1";
		while (remoteDirectoryExists(proposedName,false)) {
			i++;
			proposedName = executableName +"-" + StringUtil.getDateDayOnly()+ "."+i;
		}
		remoteWorkingDirectoryName = proposedName;
	}
*/
	public void setRemoteWorkingDirectoryName(String workingDirectoryName) {
		this.remoteWorkingDirectoryName = workingDirectoryName;
	}
	public String getRemoteWorkingDirectoryPath() {
		return getRemoteServerDirectoryPath()+getRemoteWorkingDirectoryName();
	}
	/*public void setRemoteWorkingDirectoryPath(String workingDirectoryPath) {
		this.remoteWorkingDirectoryPath = workingDirectoryPath;
	}
	 */
	public String getRemoteServerDirectoryPath() {
		return remoteServerDirectoryPath;
	}
	public void setRemoteServerDirectoryPath(String remoteServerDirectoryPath) {
		this.remoteServerDirectoryPath = remoteServerDirectoryPath;
	}


	public  String lastModified (String remoteFileName) {
		try {
			Session session=createSession();
			session.connect();

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteWorkingDirectoryPath());

			SftpATTRS sftpATTRS = channel.stat(remoteFileName);

			channel.disconnect();
			session.disconnect();
			return sftpATTRS.getMtimeString();

		}  catch (Exception e) {
			ownerModule.logln("Could not determine last modified date of file on remote server: " + e.getMessage());
			e.printStackTrace();
			return "";
		}
	}
	public  boolean remoteDirectoryExists (String remoteDirectoryName, boolean warn) {
		try {
			Session session=createSession();
			session.connect();

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteWorkingDirectoryPath());

			SftpATTRS sftpATTRS = channel.stat(remoteDirectoryName);
			
			boolean isDirectory = sftpATTRS.isDir();
			boolean isLink = sftpATTRS.isLink();

			channel.disconnect();
			session.disconnect();
			return isDirectory && !isLink;

		}  catch (Exception e) {
			if (warn) {
				ownerModule.logln("Could not determine if directory exists on remote server.  Directory: " + remoteDirectoryName + ", Message: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}

	public  boolean connectionAvailable (boolean warn) {
		try {
			Session session=createSession();
			session.connect();
			session.disconnect();
			return true;
		}  catch (Exception e) {
			if (ConnectionFailure(e)) {
				return false;
			}
		}
		return true;
	}

	public  boolean remoteFileExists (String remoteFileName, boolean warn) {
		try {
			Session session=createSession();
			session.connect();
			setConnectionFailure(false);

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteWorkingDirectoryPath());

			SftpATTRS sftpATTRS = channel.stat(remoteFileName);
			
			channel.disconnect();
			session.disconnect();

			return !sftpATTRS.isDir() && !sftpATTRS.isLink();

		}  catch (Exception e) {
			if (!ConnectionOrAuthorizationFailure(e) && warn && !ShellScriptUtil.runningFileName.equalsIgnoreCase(remoteFileName)) {
				ownerModule.logln("Could not determine if file exists on remote server.  File: " + remoteFileName + ", Message: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}

	public  boolean remoteFileExists (ChannelSftp channel, String remoteFileName) {  // faster version if channel provided as doesn't need to establish a session
		try {
			if (channel==null)
				return false;
			if (!channel.isConnected())
				channel.connect();

			channel.cd(getRemoteWorkingDirectoryPath());

			SftpATTRS sftpATTRS = channel.stat(remoteFileName);

			return !sftpATTRS.isDir() && !sftpATTRS.isLink();

		}  catch (Exception e) {
			if (!ConnectionOrAuthorizationFailure(e) && !ShellScriptUtil.runningFileName.equalsIgnoreCase(remoteFileName)) {
				ownerModule.logln("Could not determine if file exists on remote server.  File: " + remoteFileName + ", Message: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}


	public static String remoteSSHErrorFileName = "CommunicationErrors.txt";


	public  boolean sendCommands (String[] commands, boolean waitForRunning, boolean cdIntoWorking, boolean captureErrorStream) {
		if (commands==null || commands.length==0)
			return false;
		try{
			if (cdIntoWorking)
				commands = StringArray.addToStart(commands, "cd " + getRemoteWorkingDirectoryPath());
			submittedReportedToUser = false;
			Session session=createSession();
			session.connect();
			ChannelExec channel=(ChannelExec)session.openChannel("exec");
	/*		boolean visibleTerminal = true;
			if (visibleTerminal) {
				channel.setPty(true);
				channel.setPtyType("bash");
			}
			*/
			String concatenated = "";
			for (int i=0; i<commands.length; i++)
				if (StringUtil.notEmpty(commands[i]))
					if (i==0)
						concatenated += commands[i];
					else
						concatenated += " && " + commands[i];
			if (verbose)
				ownerModule.logln("\n*** Command string: " + concatenated + "\n");

			if (captureErrorStream) {
				//channel.setCommand( "cd " + getRemoteWorkingDirectoryPath() + " && >"+remoteSSHErrorFileName);
				//channel.connect();
				//channel.disconnect();
				String filename = rootDir + "/"+ remoteSSHErrorFileName;
				File fstream = new File(filename);
				FileOutputStream fos = new FileOutputStream(fstream);
				PrintStream errorStream = new PrintStream(fos);
				channel.setErrStream(errorStream);
			}
			channel.setCommand(concatenated);
			InputStream in=channel.getInputStream();
			channel.connect();
			boolean success = false;

			byte[] tmp=new byte[1024];
			while(true){
				while(in.available()>0){
					int i=in.read(tmp, 0, 1024);
					if(i<0)break;
					ownerModule.logln(new String(tmp, 0, i));
				}

				if (channel.isClosed() && (!waitForRunning || !remoteFileExists(ShellScriptUtil.runningFileName, false))) {
					success=channel.getExitStatus()==0;
					if (!success || verbose)
						ownerModule.logln("exit-status: "+channel.getExitStatus());
					break;
				} else if (channel.isClosed()) {
					if (channel.getExitStatus()!=0 || verbose)
						ownerModule.logln("exit-status: "+channel.getExitStatus());

				}
				success=channel.getExitStatus()==0;
				monitorAndCleanUpShell(null,progressIndicator);

				try{Thread.sleep(1000);}catch(Exception ee){}
			}


			channel.disconnect();
			session.disconnect();
			return success;
		}catch(Exception e){
			ownerModule.logln("Could not successfully send commands to remote server: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}


	public  boolean monitorRun (boolean waitForRunning) {

		while(true){

			monitorAndCleanUpShell(null,progressIndicator);

			if ((!waitForRunning || !remoteFileExists(ShellScriptUtil.runningFileName, false))) {
				break;
			} 

			try{Thread.sleep(1000);}catch(Exception ee){}
		}

		return true;

	}

	public  boolean sendFilesToWorkingDirectory (String[] localFilePaths, String[] remoteFileNames) {
		if (localFilePaths==null || remoteFileNames==null)
			return false;
		try{
			Session session=createSession();
			if (session==null)
				return false;  // TODO: feedback
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			channel.cd(getRemoteWorkingDirectoryPath());

			for (int i=0; i<localFilePaths.length && i<remoteFileNames.length; i++)
				if (StringUtil.notEmpty(localFilePaths[i]) && StringUtil.notEmpty(remoteFileNames[i]))
					channel.put(localFilePaths[i], remoteFileNames[i]);

			channel.disconnect();
			session.disconnect();
			if (verbose)
				ownerModule.logln("Successfully sent files to working directory");
			return true;
		} catch(Exception e){
			ownerModule.logln("Could not SFTP files to working directory: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}

	public  boolean addEmptyFileToWorkingDirectory (String remoteFileName) {
		if (remoteFileName==null)
			return false;
		try{
			Session session=createSession();
			if (session==null)
				return false;  // TODO: feedback
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			channel.cd(getRemoteWorkingDirectoryPath());

			channel.put(new ByteArrayInputStream( "".getBytes() ), remoteFileName);

			channel.disconnect();
			session.disconnect();
			if (verbose)
				ownerModule.logln("Successfully sent empty file " + remoteFileName + " to working directory");
			return true;
		} catch(Exception e){
			ownerModule.logln("Could not send empty file " + remoteFileName + " to working directory: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

	}

	private boolean ConnectionFailure(Exception e) {
		if (e!=null && e instanceof JSchException && e.getMessage().contains("java.net.ConnectException")) {
			ownerModule.discreetAlert("\n*********\nConnection failure: " + e.getMessage()+ ". Please check to see you are appropriately connected to contact the server.\n*********");
			setConnectionFailure(true);
			return true;
		} else
			setConnectionFailure(false);
		return false;
	}

	private boolean AuthorizationFailure(Exception e) {
		if (e!=null && e instanceof JSchException && "Auth fail".equalsIgnoreCase(e.getMessage())) {
			ownerModule.discreetAlert("\n*********\nAuthentication failure.  Make sure you are using the correct username and password for the SSH server, and that you have appropriate access to the SSH server.\n*********");
			forgetPassword();
			setAuthorizationFailure(true);
			return true;
		}
		return false;
	}
	
	private boolean ConnectionOrAuthorizationFailure(Exception e) {
		if (e!=null && e instanceof JSchException) {
			if ("Auth fail".equalsIgnoreCase(e.getMessage())) {
				ownerModule.discreetAlert("\n*********\nAuthentication failure.  Make sure you are using the correct username and password for the SSH server, and that you have appropriate access to the SSH server.\n*********");
				forgetPassword();
				setAuthorizationFailure(true);
				return true;
			} 
			if (e.getMessage().contains("java.net.ConnectException")) {
				ownerModule.discreetAlert("\n*********\nConnection failure: " + e.getMessage()+ ". Please check to see you are appropriately connected to contact the server.\n*********");
				setConnectionFailure(true);
				return true;
			} else
				setConnectionFailure(false);
		}
		return false;
	}
	
	

	public  boolean createRemoteWorkingDirectory() {
		try {
			Session session=createSession();
			session.connect();

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteServerDirectoryPath());
			channel.mkdir(getRemoteWorkingDirectoryName());

			channel.disconnect();
			session.disconnect();
			return true;

		}  catch (Exception e) {
			if (ConnectionOrAuthorizationFailure(e)) {
				ownerModule.logln("\n*********\nERROR: Could not create remote working directory (\""+getRemoteWorkingDirectoryName()+"\"); Authorization Failure!\n*********");
			} else{
				ownerModule.logln("\n*********\nERROR: Could not create remote working directory (\""+getRemoteWorkingDirectoryName()+"\")\n*********");
				ownerModule.logln("Error message: "+e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}


	public boolean transferFilesToServer(String[] localFilePaths, String[] remoteFileNames) {
		return sendFilesToWorkingDirectory (localFilePaths, remoteFileNames);
	}
	public boolean transferFileToServer(String localFilePath, String remoteFileName) {
		return sendFilesToWorkingDirectory (new String[] {localFilePath}, new String[] {remoteFileName});
	}
	public boolean setRemoteFileToExecutable(String remoteFileName) {
		String[] commands = new String[] { "cd " + getRemoteWorkingDirectoryPath(), "chmod +x " + remoteFileName};
		if (sendCommands(commands,false, false, false))
			return true;
		else
			ownerModule.logln("Could not set remote file to be executable.");
		return false;
	}

	public  boolean jobCompleted (Object location) {
		return !remoteFileExists(ShellScriptUtil.runningFileName, false);
	}

	public String getJobStatus(Object location, boolean warn) {
		if (remoteFileExists(ShellScriptUtil.runningFileName, false)) 
			return submitted;
		if (warn)
			return "Job completed or not found.";
		return "";
	}

	public  boolean downloadFilesToLocalWorkingDirectory (boolean onlyNewOrModified, boolean warn) {
		try{
			Session session=createSession();
			if (session==null)
				return false;  // TODO: feedback
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			channel.cd(getRemoteWorkingDirectoryPath());
			Vector remoteFiles = channel.ls(getRemoteWorkingDirectoryPath());

			RemoteJobFile[] remoteJobFiles = new RemoteJobFile[remoteFiles.size()];  // now acquire the last modified dates
			for (int i=0; i<remoteFiles.size(); i++) {
				ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)remoteFiles.elementAt(i);
				String fileName = entry.getFilename();
				if (remoteFileExists(channel,fileName)) {
					remoteJobFiles[i] = new RemoteJobFile();
					remoteJobFiles[i].setLastModified(lastModified(fileName));
					remoteJobFiles[i].setFileName(fileName);
				}
			}
			for (int i=0; i<remoteFiles.size(); i++) {
				ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)remoteFiles.elementAt(i);
				String fileName = entry.getFilename();
				if (!ShellScriptUtil.runningFileName.equalsIgnoreCase(fileName) && remoteFileExists(channel,fileName)) {
					if (!onlyNewOrModified || fileNewOrModified(previousRemoteJobFiles, remoteJobFiles, i))
						channel.get(fileName, rootDir+fileName);
				}
			}
			previousRemoteJobFiles = remoteJobFiles.clone();

			channel.disconnect();
			session.disconnect();
			setAuthorizationFailure(false);
			setConnectionFailure(false);
			return true;
		} catch(Exception e){
			if (warn) {
				if (ConnectionOrAuthorizationFailure(e)) {
					ownerModule.logln("\n*********\nERROR: Could not download files from remote server. Authorization Failure!\n*********");
				} else
					ownerModule.logln("Could not download files from remote server: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}

	}

	public boolean downloadResults(Object location, String rootDir, boolean onlyNewOrModified) {
		return downloadFilesToLocalWorkingDirectory(onlyNewOrModified, false);
	}

	public boolean reAuthorize() {
		setAuthorizationFailure(false);
		return checkUsernamePassword(false, true);
	}

	/*.................................................................................................................*/
	public int getDefaultMinPollIntervalSeconds(){
		return 10;
	}

	/*.................................................................................................................*/
	public boolean downloadWorkingResults(Object location, String rootDir, boolean onlyNewOrModified,boolean warn) {
		if (checkUsernamePassword(false)) {
			return downloadFilesToLocalWorkingDirectory(onlyNewOrModified, warn);
		}
		return false;
	}

	public void deleteJob(Object location) {
	}
	/*.................................................................................................................*
	public void setPasswordToSSHProfilePassword(){
		if (sshServerProfile!=null)
			password=sshServerProfile.getPassword();
	}

	/*.................................................................................................................*/
	public void setPassword(String newPassword){
		if (!useAPITestUser()) {
			if (sshServerProfile!=null)
				sshServerProfile.setPassword(newPassword);
		}
	}

	/*.................................................................................................................*/
	public String getServiceName() {
		return "SSH Server";
	}

	/*.................................................................................................................*/
	public String getSystemTypeName() {
		return "";
	}
	public String getSystemName() {
		if (StringUtil.notEmpty(sshServerProfileName))
			return sshServerProfileName +" (via SSH)";
		return "Server (via SSH)";
	}
	@Override
	public String getBaseURL() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getAPIURL() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getRegistrationURL() {
		// TODO Auto-generated method stub
		return null;
	}


}
