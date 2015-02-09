package mesquite.zephyr.TestCIPRES;

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
import mesquite.zephyr.lib.*;

import org.dom4j.Document;
import org.dom4j.Element;


public class TestCIPRES extends UtilitiesAssistant {

	String baseURL = "https://www.phylo.org/cipresrest/v1";
	boolean verbose = true;

	static String username = "DavidMaddison";
	static String password = ""; 
	static int jobNumber = 3;
	String CIPRESkey = "Mesquite-7C63884588B8438CAE456E115C9643F3";
	
	CIPResCommunicator communicator;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		addMenuItem(null, "CIPRES:  send job", makeCommand("sendJobToCIPRES", this));
		addMenuItem(null, "CIPRES Job List", makeCommand("listCIPRESJobs", this));
		addMenuItem(null, "CIPRES Tool List", makeCommand("listCIPRESTools", this));
		addMenuItem(null, "CIPRES Job Status...", makeCommand("checkJob", this));
		addMenuItem(null, "CIPRES Delete Job...", makeCommand("deleteJob", this));
		addMenuItem(null, "CIPRES Download Files...", makeCommand("downloadFiles", this));
		communicator = new CIPResCommunicator(this, null, null);
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Send Job to CIPRES", null, commandName, "sendJobToCIPRES")) {
			
			communicator.sendJobToCipres(null, "RAXMLHPC2_TGB", null);
		}
		else if (checker.compare(this.getClass(), "CIPRES Job List", null, commandName, "listCIPRESJobs")) {
			communicator.listCipresJobs();
		}
		else if (checker.compare(this.getClass(), "CIPRES Tool List", null, commandName, "listCIPRESTools")) {
			communicator.listCipresTools();
		}
		else if (checker.compare(this.getClass(), "CIPRES Job Finished?", null, commandName, "checkJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Check Job Status", "Job URL", "");
			communicator.checkJobStatus(jobURL);
		}
		else if (checker.compare(this.getClass(), "Delete Job", null, commandName, "deleteJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Delete Job", "Job URL", "");
			communicator.deleteJob(jobURL);
		}
		else if (checker.compare(this.getClass(), "Download Files", null, commandName, "downloadFiles")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Download Job Files", "Job URL", "");
			communicator.downloadResults(jobURL, "/ciprestest/");
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}


	public String getName() {
		return "Test CIPRES connection";
	}

}
