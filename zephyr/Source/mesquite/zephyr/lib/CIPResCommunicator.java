package mesquite.zephyr.lib;

import java.io.*;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.zephyr.lib.CipresJobFile;

import org.dom4j.Document;
import org.dom4j.Element;

public class CIPResCommunicator implements XMLPreferencesProcessor {
	public int sleepTime = 60000;


	String[] outputFilePaths; //local copies of files
	String rootDir;
	long[] lastModified;
	CipresJobFile[] previousCipresJobFiles;

	String xmlPrefsString = null;
	static boolean preferencesProcessed=false;
	MesquiteModule ownerModule;

	String baseURL = "https://www.phylo.org/cipresrest/v1";
	boolean verbose = MesquiteTrunk.debugMode;

	static int jobNumber = 3;
	static String username = "";
	static String password = ""; 
	String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";

	OutputFileProcessor outputFileProcessor; // for reconnection
	ShellScriptWatcher watcher; // for reconnection

	public CIPResCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
		XMLUtil.readXMLPreferences(mb, this, xmlPrefsString);
		this.outputFilePaths = outputFilePaths;
		ownerModule = mb;
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("setUsername " + ParseUtil.tokenize(username));
		return temp;
	}
	Parser parser = new Parser();
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the username", "[username]", commandName, "setUsername")) {
			username = parser.getFirstToken(arguments);
		}
		return null;
	}	
	public void processSingleXMLPreference (String tag, String content) {
		processSingleXMLPreference(tag, null, content);

	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String flavor, String content) {
		if ("userName".equalsIgnoreCase(tag))
			username = StringUtil.cleanXMLEscapeCharacters(content);
		else if ("jobNumber".equalsIgnoreCase(tag))
			jobNumber = MesquiteInteger.fromString(content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "username", username);  
		StringUtil.appendXMLTag(buffer, 2, "jobNumber", jobNumber);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public String getRootDir() {
		return rootDir;
	}
	public void setRootDir(String rootDir) {
		this.rootDir = rootDir;
	}
	/*.................................................................................................................*/
	public void setOutputProcessor(OutputFileProcessor outputFileProcessor){
		this.outputFileProcessor = outputFileProcessor;
	}
	public void setWatcher(ShellScriptWatcher watcher){
		this.watcher = watcher;
	}

	/*.................................................................................................................*/
	public String getBaseURL() {
		return baseURL;
	}
	/*.................................................................................................................*/
	boolean checkUsernamePassword(boolean tellUserAboutCipres){
		if (StringUtil.blank(username) || StringUtil.blank(password)){
			MesquiteBoolean answer = new MesquiteBoolean(false);
			MesquiteString usernameString = new MesquiteString();
			if (username!=null)
				usernameString.setValue(username);
			MesquiteString passwordString = new MesquiteString();
			if (password!=null)
				passwordString.setValue(password);
			String help = "You will need an account on the CIPRes REST system to use this service.  To register, go to https://www.phylo.org/restusers/register.action";
			new UserNamePasswordDialog(ownerModule.containerOfModule(), "Sign in to CIPRes", help, "", "Username", "Password", answer, usernameString, passwordString);
			if (answer.getValue()){
				username=usernameString.getValue();
				password=passwordString.getValue();
			}
			ownerModule.storePreferences();
		}
		boolean success = StringUtil.notEmpty(username) && StringUtil.notEmpty(password);
		if (!success && tellUserAboutCipres) {
			MesquiteMessage.discreetNotifyUser("Use of the CIPRes service requires an account with CIPRes's REST service.  Go to https://www.phylo.org/restusers/register.action to register for an account");
		}
		return success;

	}

	/*.................................................................................................................*/
	/** This method returns a Document from the contents of the XML file as contained in the String xmlFile.  If rootTag is not empty AND
	 * the root element of the XML file does NOT equal the value of rootTag, then this will return a null Document. */
	public Document loadXMLFile(String rootTag, String xmlFile) {
		if (!StringUtil.blank(xmlFile)) {
			Document CipresDoc = XMLUtil.getDocumentFromString(rootTag, xmlFile);
			return CipresDoc;
		}
		return null;
	}
	/*.................................................................................................................*/
	/** This method returns a Document from the contents of the XML file as contained in the String xmlFile, with no restriction as to the root element. */
	public Document loadXMLFile(String xmlFile) {
		if (!StringUtil.blank(xmlFile)) {
			Document CipresDoc = XMLUtil.getDocumentFromString(xmlFile);
			return CipresDoc;
		}
		return null;
	}

	/*.................................................................................................................*/
	public HttpClient getHttpClient(){
		// from http://www.artima.com/forums/flat.jsp?forum=121&thread=357685
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(AuthScope.ANY, credentials);
		return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
	}
	/*.................................................................................................................*/

	public void reportError(Document doc, String noteToUser, boolean resetPassword) {
		if (doc==null)
			return;
		String displayMessage = doc.getRootElement().elementText("displayMessage");
		String message = doc.getRootElement().elementText("message");
		if (StringUtil.notEmpty(message)) {
			if ("Authentication Error".equalsIgnoreCase(displayMessage)) {
				if (resetPassword)
					password = "";
			} else {
				ownerModule.logln("\n******************");
				ownerModule.logln(noteToUser);
				ownerModule.logln(displayMessage);
				ownerModule.logln(message);
				List paramErrors = doc.getRootElement().elements("paramError");
				if (paramErrors!=null)
					for (Iterator iter = paramErrors.iterator(); iter.hasNext();) {
						Element nextEntry = (Element) iter.next();
						String param = nextEntry.elementText("param");
						String error = nextEntry.elementText("error");
						ownerModule.logln("  " + param + ": " + error);
					}

				ownerModule.logln("\n******************\n");
			}
		}
	}

	/*.................................................................................................................*/
	/** this is the primary method that sends a query to the CIPRes REST service.  
	 * It expects to receive an XML file, which it returns in Document if the root tag matc hes xmlRootTag */
	public Document  cipresQuery(HttpClient httpclient, String URL, String xmlRootTag){
		if (StringUtil.blank(URL))
			return null;
		try {
			HttpGet httpget = new HttpGet(URL); 
			httpget.addHeader("cipres-appkey", CIPRESkey);
			try {
				HttpResponse response = httpclient.execute(httpget);
				HttpEntity responseEntity = response.getEntity();
				InputStream instream = responseEntity.getContent();
				BufferedReader br = new BufferedReader(new InputStreamReader(instream));
				String line = "";
				StringBuffer sb = new StringBuffer();
				while((line = br.readLine()) != null) {
					sb.append(line+StringUtil.lineEnding());
				}
				Document cipresResponseDoc = loadXMLFile(xmlRootTag, sb.toString());
				if (cipresResponseDoc!=null && verbose) {
					ownerModule.logln(sb.toString());
				}
				if (cipresResponseDoc==null) {
					Document errorDoc = loadXMLFile(sb.toString());
					if (errorDoc!=null)
						reportError(errorDoc, "Error in communicating with CIPRes",  true);
				}
				EntityUtils.consume(response.getEntity());
				return cipresResponseDoc;
			} catch (IOException e) {
				Debugg.printStackTrace(e);
			}  
			catch (Exception e) {
				Debugg.printStackTrace(e);
			}
		} catch (Exception e) {
			Debugg.printStackTrace(e);
		}
		return null;
	}




	/*.................................................................................................................*/

	public void processJobSubmissionResponse(Document cipresResponseDoc, MesquiteString jobURL) {

		Element element = cipresResponseDoc.getRootElement().element("selfUri");
		Element subelement = null;
		if (element!=null)
			subelement=element.element("url");

		if ("false".equals(cipresResponseDoc.getRootElement().elementText("failed"))) {

			element = cipresResponseDoc.getRootElement().element("metadata");
			List entries = element.elements("entry");
			String reportedJobID = "";
			for (Iterator iter = entries.iterator(); iter.hasNext();) {
				Element nextEntry = (Element) iter.next();
				if ("clientJobId".equals(nextEntry.elementText("key")))
					reportedJobID= nextEntry.elementText("value");
			}

			ownerModule.logln("\nJob successfully submitted to CIPRes.");
			ownerModule.logln("  Job URL: " + subelement.getText());
			ownerModule.logln("  Job ID: " + reportedJobID+"\n");
			if (jobURL!=null)
				jobURL.setValue(subelement.getText());
		}


	}

	/*.................................................................................................................*/
	public void prepareBuilder(MultipartEntityBuilder builder, String cipresTool, String jobID){
		if (builder!=null) {
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addTextBody("tool", cipresTool);
			if (StringUtil.notEmpty(jobID))
				builder.addTextBody("metadata.clientJobId", jobID);
			builder.addTextBody("metadata.statusEmail", "true");
			builder.addTextBody("vparam.runtime_", "0.50", ContentType.TEXT_PLAIN);
		}
	}


	/*.................................................................................................................*/
	/** The core method that initiates a job on CIPRes. */
	public boolean postJob(HttpClient httpclient, MultipartEntityBuilder builder, MesquiteString jobURL){
		if (builder==null)
			return false;
		String URL = baseURL + "/job/" + username;
		HttpPost httppost = new HttpPost(URL);
		httppost.addHeader("cipres-appkey", CIPRESkey); 

		//some of this from http://stackoverflow.com/questions/18964288/upload-a-file-through-an-http-form-via-multipartentitybuilder-with-a-progress
		HttpEntity cipresEntity = builder.build();

		httppost.setEntity(cipresEntity);

		try {
			HttpResponse response = httpclient.execute(httppost);

			HttpEntity responseEntity = response.getEntity();
			InputStream instream = responseEntity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(instream));
			StringBuffer sb = new StringBuffer();
			String line = "";
			while((line = br.readLine()) != null) {
				sb.append(line+StringUtil.lineEnding());
			}

			Document cipresResponseDoc = loadXMLFile("jobstatus", sb.toString());  // let's see how it went
			boolean success = false;
			if (cipresResponseDoc!=null) {  
				processJobSubmissionResponse(cipresResponseDoc, jobURL);
				if (verbose)
					Debugg.println(sb.toString());
				if (jobURL!=null)
					success = StringUtil.notEmpty(jobURL.getValue());
				else 
					success=true;
			} else {
				cipresResponseDoc = loadXMLFile(sb.toString());
				reportError(cipresResponseDoc, "Error with CIPRes run", false);
			}
			EntityUtils.consume(response.getEntity());
			return success;
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
		return false;
	}



	static final String JOBCOMPLETED = "COMPLETED";
	static final String JOBSUBMITTED = "SUBMITTED";

	/*.................................................................................................................*/
	public String jobStatusFromResponse(Document cipresResponseDoc) {
		String status = "Status not available";

		Element element = cipresResponseDoc.getRootElement().element("terminalStage");
		if (element!=null) {
			status = element.getText();
			if ("true".equalsIgnoreCase(status))
				return JOBCOMPLETED;
		}
		element = cipresResponseDoc.getRootElement().element("messages");
		if (element==null)
			return status;

		List entries = element.elements("message");
		String reportedJobID = "";
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Element nextEntry = (Element) iter.next();
			if (nextEntry!=null)
				status= nextEntry.elementText("stage");
		}

		if (JOBCOMPLETED.equalsIgnoreCase(status))
			return JOBCOMPLETED;
		return status;
	}


	/*.................................................................................................................*/
	/** Reports job status in the log. */
	public void reportJobStatus(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
			if (cipresResponseDoc!=null) {
				ownerModule.logln("CIPRes Job Status: " + jobStatusFromResponse(cipresResponseDoc));
			}
		}
	}

	/*.................................................................................................................*/
	public String getJobStatus (HttpClient httpclient, String jobURL){
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		if (cipresResponseDoc!=null) {
			return jobStatusFromResponse(cipresResponseDoc);
		}
		return "Status not available";
	}


	/*.................................................................................................................*/
	public String getJobStatus(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return getJobStatus(httpclient, jobURL);
		}
		return "Status not available";

	}
	/*.................................................................................................................*/
	public boolean jobCompleted(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return JOBCOMPLETED.equalsIgnoreCase(getJobStatus(httpclient, jobURL));
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean jobSubmitted(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return JOBSUBMITTED.equalsIgnoreCase(getJobStatus(httpclient, jobURL));
		}
		return false;
	}

	/*.................................................................................................................*/
	public String getDirectoryFromJobStatusResponse(Document cipresResponseDoc, String elementTag) {
		String wd = "Directory not available";
		Element element = cipresResponseDoc.getRootElement().element(elementTag);
		if (element!=null) {
			wd = element.elementText("url");
		}
		return wd;
	}

	/*.................................................................................................................*/
	public String getResultsDirectory (HttpClient httpclient, String jobURL){
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		if (cipresResponseDoc!=null) {
			return getDirectoryFromJobStatusResponse(cipresResponseDoc, "resultsUri");
		}
		return "Results directory not available";
	}
	/*.................................................................................................................*/
	public String getResultsDirectory(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return getResultsDirectory(httpclient, jobURL);
		}
		return "Working directory not available";
	}

	/*.................................................................................................................*/
	public String getWorkingDirectory (HttpClient httpclient, String jobURL){
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		if (cipresResponseDoc!=null) {
			return getDirectoryFromJobStatusResponse(cipresResponseDoc,"workingDirUri");
		}
		return "Working directory not available";
	}
	/*.................................................................................................................*/
	public String getWorkingDirectory(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return getWorkingDirectory(httpclient, jobURL);
		}
		return "Working directory not available";
	}
	/*.................................................................................................................*/
	boolean fileNewOrModified (CipresJobFile[] previousJobFiles, CipresJobFile[] jobFiles, int fileNumber) {
		if (previousJobFiles!=null && jobFiles!=null && fileNumber<jobFiles.length) {
			String fileName = jobFiles[fileNumber].getFileName();
			if (StringUtil.notEmpty(fileName)){
				for (int i=0; i<previousJobFiles.length; i++) {
					if (fileName.equalsIgnoreCase(previousJobFiles[i].getFileName())) {  // we've found the file
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

	/*.................................................................................................................*/
	/** This processes information about the files contained in either the results or working directory of a job. 
	 * */
	public CipresJobFile[] processFilesDocument(Document cipresResponseDoc) {
		Element jobfiles = cipresResponseDoc.getRootElement().element("jobfiles");
		if (jobfiles==null)
			return null;
		List tools = jobfiles.elements("jobfile");
		int count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextTool = (Element) iter.next();
			count++;
		}
		if (count==0) return null;
		CipresJobFile[] cipresJobFile = new CipresJobFile[count];
		count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextJob = (Element) iter.next();
			if (nextJob!=null) {
				if (cipresJobFile[count]==null)
					cipresJobFile[count] = new CipresJobFile();
				Element jobFileElement= nextJob.element("downloadUri");
				String fileInfo = null;
				if (jobFileElement!=null) {
					fileInfo = jobFileElement.elementText("url");
					if (!StringUtil.blank(fileInfo)&& count<cipresJobFile.length) {
						cipresJobFile[count].setDownloadURL(fileInfo);
					}
					fileInfo = jobFileElement.elementText("title");
					if (!StringUtil.blank(fileInfo)&& count<cipresJobFile.length) {
						cipresJobFile[count].setDownloadTitle(fileInfo);
					}
				}
				fileInfo= nextJob.elementText("dateModified");
				if (!StringUtil.blank(fileInfo)&& count<cipresJobFile.length) {
					cipresJobFile[count].setLastModified(fileInfo);
				}
				fileInfo= nextJob.elementText("filename");
				if (!StringUtil.blank(fileInfo)&& count<cipresJobFile.length) {
					cipresJobFile[count].setFileName(fileInfo);
				}
				fileInfo= nextJob.elementText("length");
				if (!StringUtil.blank(fileInfo)&& count<cipresJobFile.length) {
					cipresJobFile[count].setLength(MesquiteLong.fromString(fileInfo));
				}

				count++;
			}
		}
		return cipresJobFile;

	}

	/*.................................................................................................................*/
	public void  cipresDownload(HttpClient httpclient, String URL, String filePath){
		HttpGet httpget = new HttpGet(URL); 
		httpget.addHeader("cipres-appkey", CIPRESkey);
		try {
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity responseEntity = response.getEntity();
			BufferedInputStream bis = new BufferedInputStream(responseEntity.getContent());
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
			int inByte;
			while((inByte = bis.read()) != -1) bos.write(inByte);
			bis.close();
			bos.close();
			EntityUtils.consume(response.getEntity());
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
	}

	/*.................................................................................................................*/
	public boolean downloadResults (HttpClient httpclient, String jobURL, String rootDir, String fileName, boolean onlyNewOrModified){
		if (StringUtil.blank(fileName))
			return false;
		String resultsUri = getResultsDirectory(jobURL);
		if (StringUtil.notEmpty(resultsUri)) {
			Document cipresResponseDoc = cipresQuery(httpclient, resultsUri, "results");
			if (cipresResponseDoc!=null) {
				CipresJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (fileName.equalsIgnoreCase(cipresJobFiles[job].getFileName()) && (!onlyNewOrModified || fileNewOrModified(previousCipresJobFiles, cipresJobFiles, job)))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousCipresJobFiles = cipresJobFiles.clone();
				return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean downloadResults (HttpClient httpclient, String jobURL, String rootDir, boolean onlyNewOrModified){

		String resultsUri = getResultsDirectory(jobURL);
		if (StringUtil.notEmpty(resultsUri)) {
			Document cipresResponseDoc = cipresQuery(httpclient, resultsUri, "results");
			if (cipresResponseDoc!=null) {
				CipresJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					ownerModule.logln(cipresResponseDoc.toString());
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (!onlyNewOrModified || fileNewOrModified(previousCipresJobFiles, cipresJobFiles, job))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousCipresJobFiles = cipresJobFiles.clone();
				return true;

			}
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean downloadResults(String jobURL, String rootDir, boolean onlyNewOrModified) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return downloadResults(httpclient, jobURL, rootDir, onlyNewOrModified);
		}
		return false;
	}

	/*.................................................................................................................*/
	public boolean downloadWorkingResults (HttpClient httpclient, String jobURL, String rootDir, String fileName, boolean onlyNewOrModified){
		if (StringUtil.blank(fileName))
			return false;
		String workingUri = getWorkingDirectory(jobURL);
		if (StringUtil.notEmpty(workingUri)) {
			Document cipresResponseDoc = cipresQuery(httpclient, workingUri, "workingdir");
			if (cipresResponseDoc!=null) {
				CipresJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}				
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (fileName.equalsIgnoreCase(cipresJobFiles[job].getFileName()) && (!onlyNewOrModified || fileNewOrModified(previousCipresJobFiles, cipresJobFiles, job)))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousCipresJobFiles = cipresJobFiles.clone();
				return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean downloadWorkingResults (HttpClient httpclient, String jobURL, String rootDir, boolean onlyNewOrModified){
		String resultsUri = getWorkingDirectory(jobURL);
		if (StringUtil.notEmpty(resultsUri)) {
			Document cipresResponseDoc = cipresQuery(httpclient, resultsUri, "workingdir");
			if (cipresResponseDoc!=null) {
				CipresJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (!onlyNewOrModified || fileNewOrModified(previousCipresJobFiles, cipresJobFiles, job))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousCipresJobFiles = cipresJobFiles.clone();
				return true;
			}
		}
		return false;
	}

	/*.................................................................................................................*/
	public boolean downloadWorkingResults(String jobURL, String rootDir, boolean onlyNewOrModified) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			return downloadWorkingResults(httpclient, jobURL, rootDir, onlyNewOrModified);
		}
		return false;
	}

	/*.................................................................................................................*/
	public void getResults (HttpClient httpclient, String jobURL){

		String resultsUri = getResultsDirectory(jobURL);
		if (StringUtil.notEmpty(resultsUri)) {
			Document cipresResponseDoc = cipresQuery(httpclient, resultsUri, "results");
			if (cipresResponseDoc!=null) {
				CipresJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				for (int job=0; job<cipresJobFiles.length; job++) {
					Debugg.println("fileName: " + cipresJobFiles[job].getFileName());
					Debugg.println("     downloadURL: " + cipresJobFiles[job].getDownloadURL());
					Debugg.println("     downloadTitle: " + cipresJobFiles[job].getDownloadTitle());
					Debugg.println("     length: " + cipresJobFiles[job].getLength());
				}
				previousCipresJobFiles = cipresJobFiles.clone();

			}
		}
	}

	/*.................................................................................................................*/
	public void getResults(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			getResults(httpclient, jobURL);
		}
	}


	/*.................................................................................................................*/
	public boolean sendJobToCipres(MultipartEntityBuilder builder, String tool, MesquiteString jobURL) {
		if (checkUsernamePassword(true)) {
			HttpClient httpclient = getHttpClient();

			if (builder==null)
				builder =MultipartEntityBuilder.create() ;
			prepareBuilder(builder, tool, "ZEPHYR.GI."+username+"."+jobNumber);
			jobNumber++;
			ownerModule.storePreferences();

			return postJob(httpclient, builder, jobURL);
		}
		return false;
	}
	/*.................................................................................................................*/
	public void processOutputFiles(String jobURL){
		if (rootDir!=null) {
			downloadWorkingResults(jobURL, rootDir, true);
			if (outputFileProcessor!=null && outputFilePaths!=null && lastModified !=null) {
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
	public boolean monitorAndCleanUpShell(String jobURL){
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
		while (!jobCompleted(jobURL) && stillGoing){
			if(StringUtil.blank(status))

				ownerModule.logln("CIPRes Job Status: " + getJobStatus(jobURL) + "  (" + StringUtil.getDateTime() + ")");

			//	if (jobSubmitted(jobURL))
			//		processOutputFiles();
			try {
				Thread.sleep(sleepTime);
			}
			catch (InterruptedException e){
				MesquiteMessage.notifyProgrammer("InterruptedException in CIPRes monitoring");
				return false;
			}

			stillGoing = watcher == null || watcher.continueShellProcess(null);
			String newStatus = getJobStatus(jobURL);
			if (newStatus!=null && !newStatus.equalsIgnoreCase(status)) {
				ownerModule.logln("CIPRes Job Status: " + newStatus + "  (" + StringUtil.getDateTime() + ")");
				status=newStatus;
			} else
				ownerModule.log(".");
			if (newStatus!=null && newStatus.equalsIgnoreCase("SUBMITTED")){  // job is running
				processOutputFiles(jobURL);
			}
		}
		ownerModule.logln("CIPRes job completed.");
		if (outputFileProcessor!=null) {
			if (rootDir!=null) {
				ownerModule.logln("About to download results from CIPRes.");
				if (downloadResults(jobURL, rootDir, false))
						outputFileProcessor.processCompletedOutputFiles(outputFilePaths);
				else
					return false;
			}
		}

		return true;
	}

	/*.................................................................................................................*/
	/** This method simply lists the tools available */
	public void  listTools(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, baseURL + "/tool", "tools");
		if (cipresResponseDoc!=null) {
			String elementName = "tool";
			List tools = cipresResponseDoc.getRootElement().elements(elementName);
			int count=0;
			for (Iterator iter = tools.iterator(); iter.hasNext();) {  // let's get a count as to how many tools there are.
				Element nextTool = (Element) iter.next();
				count++;
			}
			String[] toolName = new String[count];
			count=0;
			for (Iterator iter = tools.iterator(); iter.hasNext();) {
				Element nextTool = (Element) iter.next();
				String name = nextTool.elementText("toolId");
				if (!StringUtil.blank(name)&& count<toolName.length) {
					ownerModule.logln(name);
				}
				name = nextTool.elementText("toolName");
				if (!StringUtil.blank(name)&& count<toolName.length) {
					ownerModule.logln("   " + name);
				}
				count++;
			}
		}
	}
	/*.................................................................................................................*/

	public String[] getJobURLs(Document cipresResponseDoc) {
		String elementName = "jobstatus";
		Element jobs = cipresResponseDoc.getRootElement().element("jobs");
		if (jobs==null)
			return null;
		List tools = jobs.elements("jobstatus");
		int count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextTool = (Element) iter.next();
			count++;
		}
		String[] url = new String[count];
		count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextJob = (Element) iter.next();
			if (nextJob!=null) {
				Element selfUriElement= nextJob.element("selfUri");
				if (selfUriElement!=null) {
					String jobURL = selfUriElement.elementText("url");
					if (!StringUtil.blank(jobURL)&& count<url.length) {
						url[count] = jobURL;
					}
				}
				count++;
			}
		}
		return url;

	}
	/*.................................................................................................................*/
	/** This method returns an array of Strings containing the URLs for the jobs for the current user */
	public String[] getJobURLs(HttpClient httpclient) {
		Document cipresResponseDoc = cipresQuery(httpclient, baseURL + "/job/" + username, "joblist");
		if (cipresResponseDoc!=null) {
			String[] jobList = getJobURLs(cipresResponseDoc);
			return jobList;
		}
		return null;
	}
	/*.................................................................................................................*/
	/** Lists to the log the current jobs of the user */
	public void  listJobs(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, baseURL + "/job/" + username, "joblist");
		if (cipresResponseDoc!=null) {
			String[] jobList = getJobURLs(cipresResponseDoc);
			if (jobList!=null)
				for (int job=0; job<jobList.length; job++){
					ownerModule.logln("\njob " + job + ": " + jobList[job]);
					String status = getJobStatus(jobList[job]);
					ownerModule.logln("   " + status);
					String wd = getWorkingDirectory(jobList[job]);
					ownerModule.logln("   " + wd);
				}
		}
	}

	/*.................................................................................................................*/
	public void listCipresJobs() {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			listJobs(httpclient);
		}
	}


	/*.................................................................................................................*/
	public void  deleteJob (HttpClient httpclient, String URL){
		HttpDelete httpdelete = new HttpDelete(URL); 
		httpdelete.addHeader("cipres-appkey", CIPRESkey);
		try {
			HttpResponse response = httpclient.execute(httpdelete);
		} catch (IOException e) {
			Debugg.printStackTrace(e);
		}
	}

	/*.................................................................................................................*/
	public void deleteJob(String jobURL) {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			deleteJob(httpclient, jobURL);
		}
	}
	/*.................................................................................................................*/
	public void deleteAllJobs() {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			String[] jobURLs = getJobURLs(httpclient);
			if (jobURLs!=null) {
				for (int job=0; job<jobURLs.length; job++){
					deleteJob(httpclient, jobURLs[job]);
				}
				if (jobURLs.length==0)
					ownerModule.logln("No CIPRes jobs to delete.");
				else
					ownerModule.logln("All " + jobURLs.length + " CIPRes jobs deleted.");
			}
		}
	}
	/*.................................................................................................................*/
	public void listCipresTools() {
		if (checkUsernamePassword(false)) {
			HttpClient httpclient = getHttpClient();
			listTools(httpclient);
		}
	}


}

