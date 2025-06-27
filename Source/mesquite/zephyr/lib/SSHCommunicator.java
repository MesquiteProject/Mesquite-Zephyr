package mesquite.zephyr.lib;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

import mesquite.externalCommunication.lib.RemoteJobFile;
import mesquite.lib.CommandChecker;
import mesquite.lib.Commandable;
import mesquite.lib.MesquiteFile;
import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteTimer;
import mesquite.lib.MesquiteTrunk;
import mesquite.lib.ParseUtil;
import mesquite.lib.Parser;
import mesquite.lib.ShellScriptUtil;
import mesquite.lib.Snapshot;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;
import mesquite.lib.ui.ProgressIndicator;


public  class SSHCommunicator extends RemoteCommunicator implements Commandable {

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
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		String s = "setDoNotDownload ";
		for (int i=0; i<filesToNotDownload.length; i++)
			s+=ParseUtil.tokenize(filesToNotDownload[i])+ " ";
		temp.addLine(s);
		return temp;
	}

	int sessionsCreated =0;
	MesquiteTimer sshTimer;;
	
	public Session createSession(String methodName) {
		if (sshTimer==null) {
			sshTimer = new MesquiteTimer();
			sshTimer.start();
		}

		try {
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no"); //TODO: have options
			config.put( "PreferredAuthentications", "publickey,keyboard-interactive,password");
			JSch jsch = new JSch();
			Session session=jsch.getSession(sshServerProfile.getUsername(), host, 22);
			session.setPassword(sshServerProfile.getPassword());
			session.setConfig(config);
			sessionsCreated++;
			if (MesquiteTrunk.debugMode) {
				ownerModule.logln("Successfully created session to " + host);
				ownerModule.logln("    "+sessionsCreated +  " sessions created [" + methodName+"]");
			}
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
		if (sshServerProfile==null)
			return null;
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

		} else if (checker.compare(this.getClass(), "Sets the files to not download", "[list of files]", commandName, "setDoNotDownload")) {
			Parser parser = new Parser(arguments);
			int numFiles= parser.getNumberOfTokens();
			filesToNotDownload = new String[numFiles];
			String name =parser.getFirstToken();
			int count=0;
			while (StringUtil.notEmpty(name)) {
				if (count<filesToNotDownload.length)
					filesToNotDownload[count] = name;
				count++;
				name = parser.getNextToken();
			}
			return filesToNotDownload;
		} 
		return null;
	}	


	public  boolean checkForUniqueRemoteWorkingDirectoryName (String executableName) {
		boolean connected = false;
		String proposedName="";
		verbose = true;
		try {
			Session session=createSession("checkForUniqueRemoteWorkingDirectoryName");
			
			session.connect();

			ChannelSftp channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			String remoteDir = getRemoteWorkingDirectoryPath();
			ownerModule.logln("Checking for remote working directory: " + remoteDir);
			channel.cd(remoteDir);		
			ownerModule.logln("[Directory found]\n");
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
				ownerModule.logln("\nWARNING: could not communicate with SSH server to identify working directory folder: " + e.getMessage());
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
		Session session=null;
		ChannelSftp channel = null;
		try {
			session=createSession("lastModified");
			session.connect();

			channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			channel.cd(getRemoteWorkingDirectoryPath());

			SftpATTRS sftpATTRS = channel.stat(remoteFileName);

			channel.disconnect();
			session.disconnect();
			return sftpATTRS.getMtimeString();

		}  catch (Exception e) {
			ownerModule.logln("\nCould not determine last modified date of file \"" + remoteFileName +"\" on remote server: " + e.getMessage());
			e.printStackTrace();
			if (channel !=null && channel.isConnected())
				channel.disconnect();
			if (session !=null && session.isConnected())
				session.disconnect();
			return "";
		}
	}
	public  boolean remoteDirectoryExists (String remoteDirectoryName, boolean warn) {
		try {
			Session session=createSession("remoteDirectoryExists");
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
			Session session=createSession("connectionAvailable");
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

	public  boolean remoteFileExists (String remoteFileName, boolean warn, boolean alwaysReturnTrueIfConnectionException) {
		Session session=null;
		ChannelSftp channel = null;
		try {
			session=createSession("remoteFileExists");
			session.connect();
			setConnectionFailure(false);

			channel=(ChannelSftp)session.openChannel("sftp");
			channel.connect();
			String remotePath = getRemoteWorkingDirectoryPath();
			if (verbose) {
				ownerModule.logln("\nremote working directory path: " + remotePath);
				ownerModule.logln("remote file name: " + remoteFileName);
			}
			channel.cd(remotePath);

			SftpATTRS sftpATTRS = channel.stat(remoteFileName);

			channel.disconnect();
			session.disconnect();

			return !sftpATTRS.isDir() && !sftpATTRS.isLink();

		}  catch (Exception e) {
			if (!ConnectionOrAuthorizationFailure(e) && warn && !ShellScriptUtil.runningFileName.equalsIgnoreCase(remoteFileName)) {
				ownerModule.logln("Could not determine if file exists on remote server.  File: " + remoteFileName + ", Message: " + e.getMessage());
				e.printStackTrace();
			}
			if (alwaysReturnTrueIfConnectionException && ConnectionOrAuthorizationFailure(e))
				return true;
			if (channel !=null && channel.isConnected())
				channel.disconnect();
			if (session !=null && session.isConnected())
				session.disconnect();
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
			Session session=createSession("sendCommands");
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

				if (channel.isClosed() && (!waitForRunning || !remoteFileExists(ShellScriptUtil.runningFileName, false, true))) {
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

			if ((!waitForRunning || !remoteFileExists(ShellScriptUtil.runningFileName, false, true))) {
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
			String serverName =getSshServerProfileName();
			if (StringUtil.notEmpty(serverName))
				ownerModule.logln("About to send files to remote server " + serverName);
			else 
				ownerModule.logln("About to send files to remote server");
			//if (verbose) {
			ownerModule.logln("Files to send:");
			for (int i=0; i<remoteFileNames.length; i++) {
				if (StringUtil.notEmpty(localFilePaths[i]) && StringUtil.notEmpty(remoteFileNames[i]))
					ownerModule.logln("   "+remoteFileNames[i]);
			}
			//}
			MesquiteTimer timer = new MesquiteTimer();
			timer.start();
			Session session=createSession("sendFilesToWorkingDirectory");
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
			if (StringUtil.notEmpty(serverName))
				ownerModule.logln("Successfully sent files to remote server " + serverName + " (" + timer.timeSinceLastInSeconds()+" seconds)");
			else 
				ownerModule.logln("Successfully sent files to remote server ("+ timer.timeSinceLastInSeconds()+" seconds)");
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
			Session session=createSession("addEmptyFileToWorkingDirectory");
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
			if (e.getMessage().contains("Auth fail")) {
				ownerModule.discreetAlert("\n*********\nAuthentication failure.  Make sure you are using the correct username and password for the SSH server, and that you have appropriate access to the SSH server.\n*********");
				forgetPassword();
				setAuthorizationFailure(true);
				return true;
			} 
			if (e.getMessage().contains("java.net.ConnectException") || e.getMessage().contains("java.net.SocketException")) {
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
			Session session=createSession("createRemoteWorkingDirectory");
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

	String[] filesToNotDownload;

	public void setFilesToNotDownload(String[] remoteFileNames) {
		filesToNotDownload = new String[remoteFileNames.length];
		for (int i=0; i<filesToNotDownload.length; i++)
			filesToNotDownload[i] = remoteFileNames[i];
	}
	
	public void addToFilesToNotDownload(String remoteFileName) {
		filesToNotDownload = StringArray.concatenate(filesToNotDownload, new String[] {remoteFileName});
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
		return !remoteFileExists(ShellScriptUtil.runningFileName, false, true);
	}

	public String getJobStatus(Object location, boolean warn) {
		if (remoteFileExists(ShellScriptUtil.runningFileName, false, true)) 
			return submitted;
		if (warn)
			return "Job completed or not found.";
		return "";
	}

	public  boolean fileOnDownloadProhibitedList (String fileName) {
		if (filesToNotDownload==null || fileName==null)
			return false;
		for (int i=0;i<filesToNotDownload.length; i++)
			if (fileName.equalsIgnoreCase(filesToNotDownload[i]))
				return true;
		return false;
	}
	
	public  boolean downloadFilesToLocalWorkingDirectory (boolean onlyNewOrModified, boolean warn) {
		String fileName="";
		Session session=null;
		ChannelSftp channel = null;
		try{
			session=createSession("downloadFilesToLocalWorkingDirectory");
			if (session==null)
				return false;  // TODO: feedback
			session.connect();
			channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			channel.cd(getRemoteWorkingDirectoryPath());
			Vector remoteFiles = channel.ls(getRemoteWorkingDirectoryPath());

			RemoteJobFile[] remoteJobFiles = new RemoteJobFile[remoteFiles.size()];  // now acquire the last modified dates
			for (int i=0; i<remoteFiles.size(); i++) {
				ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)remoteFiles.elementAt(i);
				fileName = entry.getFilename();
				if (remoteFileExists(channel,fileName) && !fileOnDownloadProhibitedList(fileName)) {
					remoteJobFiles[i] = new RemoteJobFile();
					remoteJobFiles[i].setLastModified(lastModified(fileName));
					remoteJobFiles[i].setFileName(fileName);
				}
			}
			for (int i=0; i<remoteFiles.size(); i++) {
				ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry)remoteFiles.elementAt(i);
				fileName = entry.getFilename();
				if (!fileOnDownloadProhibitedList(fileName))
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
					ownerModule.logln("Could not download file (\"" + fileName + "\") at directory location \"" + rootDir + "\" from remote server: " + e.getMessage() + ".");
				e.printStackTrace();
			}
			if (channel !=null && channel.isConnected())
				channel.disconnect();
			if (session !=null && session.isConnected())
				session.disconnect();
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

	/*.................................................................................................................*/
	public void stopJob(Object location) {
		// NOT YET IMPLEMENTED
	}
	/*.................................................................................................................*/
	public void deleteJob(Object location) {
		// NOT YET IMPLEMENTED
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
