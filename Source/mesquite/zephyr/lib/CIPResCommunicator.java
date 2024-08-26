package mesquite.zephyr.lib;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.externalCommunication.lib.*;

import org.dom4j.Document;
import org.dom4j.Element;

public class CIPResCommunicator extends RESTCommunicator implements UsernamePasswordKeeper, XMLPreferencesProcessor {

	public static final String CIPResRESTURL = "https://cipresrest.sdsc.edu/cipresrest/v1";

	double runLimit=0.5;

	static String CIPResPassword ="";
	protected String username = "";

	static boolean preferencesProcessed=false;

	int jobNumber = 3;
	public String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3"; //DAVIDCHECK: is this public?

	public CIPResCommunicator (MesquiteModule mb, String xmlPrefsString,String[] outputFilePaths) {
		super(mb, xmlPrefsString, outputFilePaths);
		if (xmlPrefsString != null)
			XMLUtil.readXMLPreferences(mb, this, xmlPrefsString);
		super.setUsernamePasswordKeeper(this);
	}
	

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String flavor, String content) {
		if ("userName".equalsIgnoreCase(tag))
			username = StringUtil.cleanXMLEscapeCharacters(content);
		 if ("jobNumber".equalsIgnoreCase(tag))
			jobNumber = MesquiteInteger.fromString(content);
		//super.processSingleXMLPreference(tag, flavor, content);
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("userName".equalsIgnoreCase(tag))
			username = StringUtil.cleanXMLEscapeCharacters(content);
		 if ("jobNumber".equalsIgnoreCase(tag))
			jobNumber = MesquiteInteger.fromString(content);
		//super.processSingleXMLPreference(tag, content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "jobNumber", jobNumber);  
		StringUtil.appendXMLTag(buffer, 2, "username", username);  
		return buffer.toString();
	}
	public double getRunLimit() {
		return runLimit;
	}
	public void setRunLimit(double runLimit) {
		this.runLimit = runLimit;
	}

	/*.................................................................................................................*/
	public String getBaseURL() {
		return "https://cipresrest.sdsc.edu";
	}
	/*.................................................................................................................*/
	public String getRESTURL() {
		return CIPResRESTURL;
	}
	/*.................................................................................................................*/
	public String getPassword(){
		if (useAPITestUser()) {
			return getAPITestPassword();
		} else 
			return CIPResPassword;
	}

	/*.................................................................................................................*/
	public void setPassword(String newPassword){
		if (!useAPITestUser()) {
			CIPResPassword = newPassword;
		}
	}
	/*.................................................................................................................*/
	public String getUsername(){
		if (useAPITestUser()) {
			return getAPITestUserName();
		} else 
			return username;
	}
	/*.................................................................................................................*/
	public void setUsername(String newName){
		if (!useAPITestUser()) 
			username=newName;
	}

	/*.................................................................................................................*
	public void setPasswordToCIPResPassword() {
		password = CIPResPassword;
	}

	/*.................................................................................................................*/
	public String getAPIURL() {
		return "";
	}
	/*.................................................................................................................*/
	public String getRegistrationHint() {
		return "Touch on the web link icon on the left to register for this service.";
	}
	/*.................................................................................................................*/
	public boolean showNeedToRegisterNote() {
		return true;
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

	public void reportError(String xmlFile, Document doc, String noteToUser, boolean resetPassword) {
		if (doc==null)
			return;
		String displayMessage = doc.getRootElement().elementText("displayMessage");
		String message = doc.getRootElement().elementText("message");
		if (StringUtil.notEmpty(message)) {
			if ("Authentication Error".equalsIgnoreCase(displayMessage)) {
				if (resetPassword)
					CIPResPassword = "";
				ownerModule.logln("\n\n******************");
				ownerModule.logln(noteToUser);
				ownerModule.logln(message);
				ownerModule.logln("******************\n");
			} else {
				ownerModule.logln("\n\n******************");
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
			if (MesquiteTrunk.debugMode) {
				ownerModule.logln(xmlFile);
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
						reportError(sb.toString(), errorDoc, "Error in communicating with CIPRes",  true);
				}
				EntityUtils.consume(response.getEntity());
				return cipresResponseDoc;
			} catch (IOException e) {
				ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.cipresQuery 1]\n************\n");
			}  
			catch (Exception e) {
				ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.cipresQuery 2]\n************\n");
			}
		} catch (Exception e) {
			ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.cipresQuery 3]\n************\n");
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
			ownerModule.logln("\n  Job URL: " + subelement.getText()+"\n");
			ownerModule.logln("  Job ID: " + reportedJobID);
			
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
			builder.addTextBody("vparam.runtime_", MesquiteDouble.toString(runLimit), ContentType.TEXT_PLAIN);
		}
	}


	/*.................................................................................................................*/
	/*.................................................................................................................*/
	/** The core method that initiates a job on CIPRes. */
	public boolean postJob(HttpClient httpclient, MultipartEntityBuilder builder, MesquiteString jobURL){
		if (builder==null)
			return false;
		String URL = getRESTURL() + "/job/" + StringUtil.encodeForURL(username);
		HttpPost httppost = new HttpPost(URL);
		httppost.addHeader("cipres-appkey", CIPRESkey); 
		
		//some of this from http://stackoverflow.com/questions/18964288/upload-a-file-through-an-http-form-via-multipartentitybuilder-with-a-progress
		HttpEntity cipresEntity = builder.build();	
		
		try {
			String entityFileName = rootDir + "cipresEntity.txt";
			File fstream = new File(entityFileName);
			FileOutputStream fos = new FileOutputStream(fstream);
			cipresEntity.writeTo(fos);
		} catch (Exception e) {
			// catch exceptions
		}

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
					ownerModule.logln(sb.toString());
				if (jobURL!=null)
					success = StringUtil.notEmpty(jobURL.getValue());
				else 
					success=true;
			} else {
				cipresResponseDoc = loadXMLFile(sb.toString());
				reportError(sb.toString(), cipresResponseDoc, "Error with CIPRes run", true);
			}
			EntityUtils.consume(response.getEntity());
			return success;
		} catch (IOException e) {
			ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.postJob]\n************\n");
		}
		return false;
	}



	static final String JOBCOMPLETED = "COMPLETED";
	static final String JOBSUBMITTED = "SUBMITTED";
	
	boolean queryFrequencyReported = false;

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
		
		element = cipresResponseDoc.getRootElement().element("minPollIntervalSeconds");
		if (element!=null) {
			minPollIntervalSeconds = MesquiteInteger.fromString(element.getText());
			if (!MesquiteInteger.isCombinable(minPollIntervalSeconds) || minPollIntervalSeconds<=0)
				minPollIntervalSeconds = getDefaultMinPollIntervalSeconds();
		}
		if (!queryFrequencyReported) {
			ownerModule.logln("   Mesquite will query "+getServiceName()+" about the status every " + minPollIntervalSeconds + " seconds\n");
			queryFrequencyReported = true;
		}



		if (JOBCOMPLETED.equalsIgnoreCase(status))
			return JOBCOMPLETED;
		return status;
	}


	/*.................................................................................................................*/
	/** Reports job status in the log. */
	public void reportJobStatus(Object location) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
			HttpClient httpclient = getHttpClient();
			Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
			if (cipresResponseDoc!=null) {
				ownerModule.logln("CIPRes Job Status: " + jobStatusFromResponse(cipresResponseDoc));
			}
		}
	}

	/*.................................................................................................................*/
	public String getJobStatus (HttpClient httpclient, Object location){
		String jobURL = (String)location;
		Document cipresResponseDoc = cipresQuery(httpclient, jobURL, "jobstatus");
		if (cipresResponseDoc!=null) {
			return jobStatusFromResponse(cipresResponseDoc);
		}
		return "Status not available";
	}


	/*.................................................................................................................*/
	public String getJobStatus(Object location, boolean warn) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
			HttpClient httpclient = getHttpClient();
			return getJobStatus(httpclient, jobURL);
		}
		if (warn)
			return "Status not available";
		return "";
	}
	/*.................................................................................................................*/
	public String getJobStatus(Object location) {
		return getJobStatus(location, true);

	}
	/*.................................................................................................................*/
	public String getServiceName() {
		return "CIPRes";
	}

	/*.................................................................................................................*/
	public boolean jobCompleted(Object location) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
			HttpClient httpclient = getHttpClient();
			return JOBCOMPLETED.equalsIgnoreCase(getJobStatus(httpclient, jobURL));
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean jobSubmitted(Object location) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
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
	/** This processes information about the files contained in either the results or working directory of a job. 
	 * */
	public RemoteJobFile[] processFilesDocument(Document cipresResponseDoc) {
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
		RemoteJobFile[] cipresJobFile = new RemoteJobFile[count];
		count=0;
		for (Iterator iter = tools.iterator(); iter.hasNext();) {
			Element nextJob = (Element) iter.next();
			if (nextJob!=null) {
				if (cipresJobFile[count]==null)
					cipresJobFile[count] = new RemoteJobFile();
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
			ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.cipresDownload]\n************\n");
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
				RemoteJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (fileName.equalsIgnoreCase(cipresJobFiles[job].getFileName()) && (!onlyNewOrModified || fileNewOrModified(previousRemoteJobFiles, cipresJobFiles, job)))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousRemoteJobFiles = cipresJobFiles.clone();
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
				RemoteJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					ownerModule.logln(cipresResponseDoc.toString());
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (!onlyNewOrModified || fileNewOrModified(previousRemoteJobFiles, cipresJobFiles, job))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousRemoteJobFiles = cipresJobFiles.clone();
				return true;

			}
		}
		return false;
	}
	/*.................................................................................................................*/
	public boolean downloadResults(Object location, String rootDir, boolean onlyNewOrModified) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
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
				RemoteJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}				
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (fileName.equalsIgnoreCase(cipresJobFiles[job].getFileName()) && (!onlyNewOrModified || fileNewOrModified(previousRemoteJobFiles, cipresJobFiles, job)))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousRemoteJobFiles = cipresJobFiles.clone();
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
				RemoteJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (cipresJobFiles==null || cipresJobFiles.length==0) {
					return false;
				}
				for (int job=0; job<cipresJobFiles.length; job++) {
					if (!onlyNewOrModified || fileNewOrModified(previousRemoteJobFiles, cipresJobFiles, job))
						cipresDownload(httpclient, cipresJobFiles[job].getDownloadURL(), rootDir + cipresJobFiles[job].getFileName());
				}
				previousRemoteJobFiles = cipresJobFiles.clone();
				return true;
			}
		}
		return false;
	}

	/*.................................................................................................................*/
	public boolean downloadWorkingResults(Object location, String rootDir, boolean onlyNewOrModified, boolean warn) {
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
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
				RemoteJobFile[] cipresJobFiles = processFilesDocument(cipresResponseDoc);
				if (MesquiteTrunk.debugMode)
					for (int job=0; job<cipresJobFiles.length; job++) {
						ownerModule.logln("fileName: " + cipresJobFiles[job].getFileName());
						ownerModule.logln("     downloadURL: " + cipresJobFiles[job].getDownloadURL());
						ownerModule.logln("     downloadTitle: " + cipresJobFiles[job].getDownloadTitle());
						ownerModule.logln("     length: " + cipresJobFiles[job].getLength());
					}
				previousRemoteJobFiles = cipresJobFiles.clone();

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
	public void processOutputFiles(Object location){
		if (rootDir!=null) {
			downloadWorkingResults(location, rootDir, true, false);  //TODO: only warn if few warnings
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
	public boolean monitorAndCleanUpShell2(Object location, ProgressIndicator progIndicator){
		boolean stillGoing = true;

		if (!checkUsernamePassword(true)) {
			return false;
		}
		lastModified=null;
		if (outputFilePaths==null)
			;
		if (outputFilePaths!=null) {
			lastModified = new long[outputFilePaths.length];
			LongArray.deassignArray(lastModified);
		}
		String status = "";
		MesquiteTimer timer = new MesquiteTimer();
		timer.start();
		int interval = 0;
		int pollInterval = minPollIntervalSeconds;
		boolean submittedReportedToUser = false;
		
		while (!jobCompleted(location) && stillGoing && !aborted){
			double loopTime = timer.timeSinceLastInSeconds();  // checking to see how long it has been since the last one
			if (loopTime>minPollIntervalSeconds) {
				pollInterval = minPollIntervalSeconds - ((int)loopTime-minPollIntervalSeconds);
				if (pollInterval<0) pollInterval=0;
			}
			else 
				pollInterval = minPollIntervalSeconds;
			if(!StringUtil.blank(status)) {
				if (!status.equalsIgnoreCase("SUBMITTED") || !submittedReportedToUser) 
					ownerModule.logln(getServiceName()+" Job Status: " + status + "  (" + StringUtil.getDateTime() + ")");
				if (status.equalsIgnoreCase("SUBMITTED"))
					submittedReportedToUser = true;
			}

			//	if (jobSubmitted(jobURL))
			//		processOutputFiles();
			try {
				for (int i=0; i<pollInterval; i++) {
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
			String newStatus = getJobStatus(location); 
			if (newStatus!=null && !newStatus.equalsIgnoreCase(status)) {
				ownerModule.logln(getServiceName()+" Job Status: " + newStatus + "  (" + StringUtil.getDateTime() + ")");
			} else
				ownerModule.log(".");
			status=newStatus;
			if (newStatus!=null && newStatus.equalsIgnoreCase("SUBMITTED")){  // job is running
				processOutputFiles(location);
			}
		}
		ownerModule.logln("\n"+getServiceName()+" job completed. (" + StringUtil.getDateTime() + " or earlier)");
		if (outputFileProcessor!=null) {
			if (rootDir!=null) {
				ownerModule.logln("About to download results from "+getServiceName()+" (this may take some time).");
				if (downloadResults(location, rootDir, false))
						outputFileProcessor.processCompletedOutputFiles(outputFilePaths);
				else
					return false;
			}
		}
		if (aborted)
			return false;
		return true;
	}

	/*.................................................................................................................*/
	/** This method simply lists the tools available */
	public void  listTools(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, getRESTURL() + "/tool", "tools");
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
		Document cipresResponseDoc = cipresQuery(httpclient, getRESTURL() + "/job/" + username, "joblist");
		if (cipresResponseDoc!=null) {
			String[] jobList = getJobURLs(cipresResponseDoc);
			return jobList;
		}
		return null;
	}
	/*.................................................................................................................*/
	/** Lists to the log the current jobs of the user */
	public void  listJobs(HttpClient httpclient){
		Document cipresResponseDoc = cipresQuery(httpclient, getRESTURL() + "/job/" + username, "joblist");
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
			ownerModule.logln("\n************\nError in attempting to communicate with CIPRes: " + e.getMessage() + "\n[CIPResCommunicator.deleteJob]\n************\n");
		}
	}

	/*.................................................................................................................*/
	public void deleteJob(Object location) {  // TODO:  check to see if the Zephyr run is ongoing, and warn user if so.
		if (checkUsernamePassword(false)) {
			String jobURL = (String)location;
			HttpClient httpclient = getHttpClient();
			deleteJob(httpclient, jobURL);
		}
	}
	/*.................................................................................................................*/
	public void deleteAllJobs() {
		if (checkUsernamePassword(false)) {   // TODO:  check to see if a Zephyr run is ongoing, and warn user if so.
			HttpClient httpclient = getHttpClient();
			String[] jobURLs = getJobURLs(httpclient);
			if (jobURLs!=null) {
				for (int job=0; job<jobURLs.length; job++){
					ownerModule.logln("deleting job "+ job + " of " + jobURLs.length);
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

	/*.................................................................................................................*/
	public String getRegistrationURL(){
		return "https://www.phylo.org/restusers/register.action";
	}

	/*.................................................................................................................*/
	public  String getSystemName() {
		return "CIPRes";

	}

}

