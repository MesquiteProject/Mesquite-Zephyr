package mesquite.zephyr.CIPResUtility;

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


public class CIPResUtility extends UtilitiesAssistant {


	CIPResCommunicator communicator;


	public boolean startJob(String arguments, Object condition, boolean hiredByName) {

		MesquiteSubmenuSpec mss = addSubmenu(null,"CIPRes Utility");
		
		addItemToSubmenu(null, mss, "List Jobs", makeCommand("listCIPResJobs", this));
		addItemToSubmenu(null, mss, "Job Status...", makeCommand("checkJob", this));
		addItemToSubmenu(null, mss,"Delete Job...", makeCommand("deleteJob", this));
		addItemToSubmenu(null, mss, "Delete All Jobs...", makeCommand("deleteAllJobs", this));
		addItemToSubmenu(null, mss, "Download Files from Job...", makeCommand("downloadFiles", this));
		addItemToSubmenu(null, mss, "CIPRes Tool List", makeCommand("listCIPResTools", this));
		communicator = new CIPResCommunicator(this, null, null);
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		 if (checker.compare(this.getClass(), "List Jobs", null, commandName, "listCIPResJobs")) {
			communicator.listCipresJobs();
		}
		else if (checker.compare(this.getClass(), "CIPRes Job Finished?", null, commandName, "checkJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Check Job Status", "Job URL", "");
			communicator.reportJobStatus(jobURL);
		}
		else if (checker.compare(this.getClass(), "Delete Job", null, commandName, "deleteJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Delete Job", "Job URL", "");
			communicator.deleteJob(jobURL);
		}
		else if (checker.compare(this.getClass(), "Delete All Jobs", null, commandName, "deleteAllJobs")) {
			if (AlertDialog.query(this, "Delete All CIPRes Jobs?", "Delete All CIPRes Jobs?"))
				communicator.deleteAllJobs();
		}
		else if (checker.compare(this.getClass(), "Download Files", null, commandName, "downloadFiles")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Download Job Files", "Job URL", "");
			communicator.downloadResults(jobURL, "/ciprestest/");
		}
		else if (checker.compare(this.getClass(), "List CIPRes Tools", null, commandName, "listCIPResTools")) {
			communicator.listCipresTools();
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}


	public String getName() {
		return "CIPRes Utility";
	}

}
