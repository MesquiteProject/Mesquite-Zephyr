package mesquite.zephyr.lib;

import mesquite.lib.*;
import mesquite.lib.ui.ProgressIndicator;
import mesquite.externalCommunication.lib.*;

import java.io.File;

/*
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

*/

public abstract class RemoteCommunicator  {
	protected int minPollIntervalSeconds =getDefaultMinPollIntervalSeconds();
	protected int pollInterval = minPollIntervalSeconds;

	protected String host="";
	protected String xmlPrefsString = null;
	protected String[] outputFilePaths; //local copies of files
	protected MesquiteModule ownerModule;
	protected boolean verbose = MesquiteTrunk.debugMode;
	protected boolean aborted = false;
	protected String rootDir;
	protected long[] lastModified;
	protected final static String submitted="SUBMITTED";
	protected RemoteJobFile[] previousRemoteJobFiles;

	protected OutputFileProcessor outputFileProcessor; // for reconnection
	protected ProcessWatcher watcher; // for reconnection
	protected UsernamePasswordKeeper usernamePasswordKeeper;
	protected boolean hasBeenReconnected = false;
	protected boolean authorizationFailure = false;
	protected boolean connectionFailure = false;


	public RemoteCommunicator () {
	}

	public RemoteCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
	//	if (xmlPrefsString != null)
	//		XMLUtil.readXMLPreferences(mb, this, xmlPrefsString);
		this.outputFilePaths = outputFilePaths;
		ownerModule = mb;
	}
	
	/*.................................................................................................................*/
	public abstract String getBaseURL();
	public abstract String getAPIURL();
	public abstract String getRegistrationURL();
	public abstract String getSystemName();

	/*.................................................................................................................*/
	public void setUsernamePasswordKeeper(UsernamePasswordKeeper usernamePasswordKeeper) {
		this.usernamePasswordKeeper = usernamePasswordKeeper;
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	public int getDefaultMinPollIntervalSeconds(){
		return 30;
	}
	public int getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(int pollInterval) {
		this.pollInterval = pollInterval;
	}

	public boolean hasBeenReconnected() {
		return hasBeenReconnected;
	}

	public void setHasBeenReconnected(boolean hasBeenReconnected) {
		this.hasBeenReconnected = hasBeenReconnected;
	}

	/*.................................................................................................................*/
	public String getAPITestUserName(){
		return "";
	}
	public String getAPITestPassword(){
		return "";
	}
	public boolean useAPITestUser() {
		return false;
	}
	public boolean isAborted() {
		return aborted;
	}

	public void setAborted(boolean aborted) {
		this.aborted = aborted;
	}
	/*.................................................................................................................*/
	public String getRootDir() {
		return rootDir;
	}
	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}
	public boolean reAuthorize() {
		return true;
	}
	public boolean isAuthorizationFailure() {
		return authorizationFailure;
	}
	public void setAuthorizationFailure(boolean authorizationFailure) {
		this.authorizationFailure = authorizationFailure;
	}

	public boolean isConnectionFailure() {
		return connectionFailure;
	}
	public void setConnectionFailure(boolean connectionFailure) {
		this.connectionFailure = connectionFailure;
	}


	/*.................................................................................................................*/
	public void setOutputProcessor(OutputFileProcessor outputFileProcessor){
		this.outputFileProcessor = outputFileProcessor;
	}
	public void setWatcher(ProcessWatcher watcher){
		this.watcher = watcher;
	}

	/*.................................................................................................................*/
	public String getUserName(){
		if (useAPITestUser()) {
			return getAPITestUserName();
		} else 
			return usernamePasswordKeeper.getUsername();
	}
	/*.................................................................................................................*/
	public void setUserName(String newName){
		if (!useAPITestUser()) 
			usernamePasswordKeeper.setUsername(newName);;
	}
	/*.................................................................................................................*/
	public String getPassword(){
		if (useAPITestUser()) {
			return getAPITestPassword();
		} else if (usernamePasswordKeeper==null)
			return null;
		else
			return usernamePasswordKeeper.getPassword();
	}
	/*.................................................................................................................*/
	public void setPassword(String newPassword){
		if (!useAPITestUser()) 
			usernamePasswordKeeper.setPassword(newPassword);
	}

	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		//temp.addLine("setUsername " + ParseUtil.tokenize(usernamePasswordKeeper.getUsername()));
		return temp;
	}
	Parser parser = new Parser();
	/*.................................................................................................................*
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the username", "[username]", commandName, "setUsername")) {
			username = parser.getFirstToken(arguments);
		}
		return null;
	}	
	/*.................................................................................................................*

	public void processSingleXMLPreference (String tag, String content) {
		processSingleXMLPreference(tag, null, content);

	}

	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String flavor, String content) {
		if ("userName".equalsIgnoreCase(tag))
			username = StringUtil.cleanXMLEscapeCharacters(content);
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "username", username);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public void forgetPassword() {
		if (usernamePasswordKeeper!=null)
			usernamePasswordKeeper.setPassword("");
	}


	/*.................................................................................................................*/
	public String getSystemTypeName() {
		return "";
	}
	/*.................................................................................................................*/
	public String getRegistrationHint() {
		return "";
	}
	/*.................................................................................................................*/
	public String getRunDetailsForHelp() {
		return "";
	}
	/*.................................................................................................................*/
	public boolean showNeedToRegisterNote() {
		return false;
	}
	/*.................................................................................................................*/
	public boolean checkUsernamePassword(boolean tellUserAboutSystem){
		return checkUsernamePassword(tellUserAboutSystem, false);

	}
	/*.................................................................................................................*/
	public boolean checkUsernamePassword(boolean tellUserAboutSystem, boolean giveWarningAboutPreviousFailure){
		MesquiteBoolean answer = new MesquiteBoolean(false);
		if (StringUtil.blank(getUserName()) || StringUtil.blank(getPassword())){
			MesquiteString usernameString = new MesquiteString();
			if (getUserName()!=null)
				usernameString.setValue(getUserName());
			MesquiteString passwordString = new MesquiteString();
			if (getPassword()!=null)
				passwordString.setValue(getPassword());
			String help = "";
			if (showNeedToRegisterNote())
				help = "You need an account on the "+getSystemName()+getSystemTypeName() + " system to use this service.  To register, go to " + getRegistrationURL();
			else {
				help = getRunDetailsForHelp();
			}
			if (giveWarningAboutPreviousFailure)
				new UserNamePasswordDialog(ownerModule.containerOfModule(), "Try again to sign in to "+getSystemName(), help, getRegistrationURL(), getRegistrationHint(), "Username", "Password", answer, usernameString, passwordString);
			else
				new UserNamePasswordDialog(ownerModule.containerOfModule(), "Sign in to "+getSystemName(), help, getRegistrationURL(), getRegistrationHint(), "Username", "Password", answer, usernameString, passwordString);
			if (answer.getValue()){
				setUserName(usernameString.getValue());
				setPassword(passwordString.getValue());
			}
			ownerModule.storePreferences();
		}
		boolean success = StringUtil.notEmpty(getUserName()) && StringUtil.notEmpty(getPassword());
		if (!success && tellUserAboutSystem && answer.getValue()) {
			String URL = getRegistrationURL();
			if (StringUtil.notEmpty(URL))
				MesquiteMessage.discreetNotifyUser("Use of the "+getSystemName()+" service requires an account with the service, and for you to sign in successfully.  Go to "+ getRegistrationURL()+" to register for an account");
			else 
				MesquiteMessage.discreetNotifyUser("Use of the "+getSystemName()+" service requires an account with the service, and for you to sign in successfully.");
		}
		return success;

	}

	/*.................................................................................................................*
	public HttpClient getHttpClient(){
		// from http://www.artima.com/forums/flat.jsp?forum=121&thread=357685
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getUserName(), getPassword());
		provider.setCredentials(AuthScope.ANY, credentials);
		return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
	}
	/*.................................................................................................................*/
	public boolean fileNewOrModified (RemoteJobFile[] previousJobFiles, RemoteJobFile[] jobFiles, int fileNumber) {
		if (previousJobFiles!=null && jobFiles!=null && fileNumber<jobFiles.length) {
			String fileName = jobFiles[fileNumber].getFileName();
			if (StringUtil.notEmpty(fileName)){
				for (int i=0; i<previousJobFiles.length; i++) {
					if (previousJobFiles[i]!=null && fileName.equalsIgnoreCase(previousJobFiles[i].getFileName())) {  // we've found the file
						String lastMod = jobFiles[fileNumber].getLastModified();
						if (StringUtil.notEmpty(lastMod))
							return !lastMod.equals(previousJobFiles[i].getLastModified());  // return true if the strings don't match
						else
							return true;
					}
				}
			}
		}
		return true;
	}
	int consecutiveDownloadFailures = 0;
	static int maxDownloadFailuresToWarnAbout = 3;
	/*.................................................................................................................*/
	public void processOutputFiles(Object location){
		if (rootDir!=null && !isAuthorizationFailure()) {
			boolean downloaded = downloadWorkingResults(location, rootDir, true, consecutiveDownloadFailures>maxDownloadFailuresToWarnAbout);
			if (!downloaded) {
				consecutiveDownloadFailures++;
				if (consecutiveDownloadFailures>maxDownloadFailuresToWarnAbout)
					ownerModule.logln("\n*********\nWARNING: There appears to be a problem communicating with the server. You may wish to check your Internet connection.\n*********");
			}
			else
				consecutiveDownloadFailures=0;  // have success, reset
			if (outputFileProcessor!=null && outputFilePaths!=null && lastModified !=null && !isAuthorizationFailure()) {
				String[] paths = outputFileProcessor.modifyOutputPaths(outputFilePaths);
				for (int i=0; i<paths.length && i<lastModified.length; i++) {
					File file = new File(paths[i]);
					long lastMod = file.lastModified();
					if (!MesquiteLong.isCombinable(lastModified[i])|| lastMod>lastModified[i]){
						outputFileProcessor.processOutputFile(paths, i);
						lastModified[i] = lastMod;
					}
				}
			}
		}
	}

	/*.................................................................................................................*/
	// TODO: turn these to abstract once version 2.5 or Zephyr is released?
	public boolean jobCompleted(Object location) { return true;}
	/*.................................................................................................................*/
	public String getJobStatus(Object location) {
		return getJobStatus(location, true);

	}
	public String getJobStatus(Object location, boolean warn) { return "";}
	public boolean downloadWorkingResults(Object location, String rootDir, boolean onlyNewOrModified, boolean warn) { return true;}
	public boolean downloadResults(Object location, String rootDir, boolean onlyNewOrModified) { return true;}
	public void stopJob(Object location) {}
	public void deleteJob(Object location) {}
	public String getServiceName() { return "";}
	protected boolean submittedReportedToUser = false;
	public  boolean connectionAvailable (boolean warn) {return true;}

	/*.................................................................................................................*/
	public boolean monitorAndCleanUpShell(Object location, ProgressIndicator progIndicator){
		boolean stillGoing = true;

		if (!checkUsernamePassword(true)) {
			return false;
		}
		lastModified=null;
		if (outputFilePaths!=null) {
			lastModified = new long[outputFilePaths.length];
			LongArray.deassignArray(lastModified);
		}
		String status = "";
		MesquiteTimer timer = new MesquiteTimer();
		timer.start();
		int interval = 0;
		minPollIntervalSeconds = 5;
		int sleepTimeForThisLoop = getPollInterval();
		boolean onceThrough = false;
		
		while (((!jobCompleted(location) || !onceThrough) && stillGoing && !aborted)|| isAuthorizationFailure() || isConnectionFailure()){
			if (isAuthorizationFailure())
				if (!reAuthorize()) {
					setAborted(true);
					if (hasBeenReconnected())
						MesquiteMessage.notifyUser("Attempts to reconnect to the server have failed.  If you wish to try again, you will need to close the file WITHOUT SAVING, and reopen it.");
					return false;
				}
			double loopTime = timer.timeSinceLastInSeconds();  // checking to see how long it has been since the last one
			if (loopTime>getPollInterval()) {  //  loop time is longer than the required, 
					//which means we can decrease the amount to sleep by the excess
				sleepTimeForThisLoop = getPollInterval() - ((int)loopTime-getPollInterval());  //
				if (sleepTimeForThisLoop<0) sleepTimeForThisLoop=0;
			}
			else 
				sleepTimeForThisLoop = getPollInterval();
			if(!StringUtil.blank(status)) {
				if (!status.equalsIgnoreCase(submitted) || !submittedReportedToUser) 
					if (hasBeenReconnected())
						ownerModule.logln(getServiceName()+" job status: " + status);
					else
						MesquiteMessage.logCurrentTime(getServiceName()+" job status: " + status + ": ");
				if (status.equalsIgnoreCase(submitted))
					submittedReportedToUser = true;
			}

			//processOutputFiles(location);
			try {
				for (int i=0; i<sleepTimeForThisLoop; i++) {
					if (progIndicator!=null)
						progIndicator.spin();
					Thread.sleep(1000);
				}
			}
			catch (InterruptedException e){
				MesquiteMessage.notifyProgrammer("InterruptedException in "+getServiceName()+" monitoring");
				return false;
			}

			stillGoing = watcher == null || watcher.continueProcess(null);
			String newStatus = getJobStatus(location, onceThrough && submittedReportedToUser); 
			if (StringUtil.notEmpty(newStatus) && !newStatus.equalsIgnoreCase(status) && !submittedReportedToUser) {
				if (hasBeenReconnected() && newStatus.equalsIgnoreCase(submitted))
					ownerModule.logln(getServiceName()+" job status: " + newStatus);
				else
					MesquiteMessage.logCurrentTime(getServiceName()+" job status: " + newStatus + ": ");
			} else
				ownerModule.log(".");
			status=newStatus;
			if (status.equalsIgnoreCase(submitted))
				submittedReportedToUser = true;
			if (!isConnectionFailure())
				if (submittedReportedToUser || hasBeenReconnected){  // job is running
					processOutputFiles(location);
				}
			onceThrough = true;
		}
		if (!isAuthorizationFailure() && !isConnectionFailure()) {
			boolean done = jobCompleted(location);
			if (done && (submittedReportedToUser || hasBeenReconnected))
				ownerModule.logln(getServiceName()+" job completed. (" + StringUtil.getDateTime() + " or earlier)");
			if (outputFileProcessor!=null) {
				if (rootDir!=null) {
					if (done && (submittedReportedToUser || hasBeenReconnected))
						ownerModule.logln("About to download results from "+getServiceName()+" (this may take some time).");
					if (downloadResults(location, rootDir, false))
						outputFileProcessor.processCompletedOutputFiles(outputFilePaths);
					else
						return false;
				}
			}
		}
		if (aborted || isAuthorizationFailure())
			return false;
		return true;
	}
	public boolean warnIfError() {
		return true;
	}

}
