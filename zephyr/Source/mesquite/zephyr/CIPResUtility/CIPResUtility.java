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
		
		addItemToSubmenu(null, mss, "CIPRes Job List", makeCommand("listCIPResJobs", this));
		addItemToSubmenu(null, mss, "CIPRes Tool List", makeCommand("listCIPResTools", this));
		addItemToSubmenu(null, mss, "CIPRes Job Status...", makeCommand("checkJob", this));
		addItemToSubmenu(null, mss,"CIPRes Delete Job...", makeCommand("deleteJob", this));
		addItemToSubmenu(null, mss, "CIPRes Delete All Jobs...", makeCommand("deleteAllJobs", this));
		addItemToSubmenu(null, mss, "CIPRes Download Files...", makeCommand("downloadFiles", this));
		communicator = new CIPResCommunicator(this, null, null);
		return true;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		 if (checker.compare(this.getClass(), "CIPRes Job List", null, commandName, "listCIPResJobs")) {
			communicator.listCipresJobs();
		}
		else if (checker.compare(this.getClass(), "CIPRes Tool List", null, commandName, "listCIPResTools")) {
			communicator.listCipresTools();
		}
		else if (checker.compare(this.getClass(), "CIPRes Job Finished?", null, commandName, "checkJob")) {
			String jobURL = MesquiteString.queryShortString(containerOfModule(), "Check Job Status", "Job URL", "");
			communicator.checkJobStatus(jobURL);
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
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}


	public String getName() {
		return "CIPRes Utility";
	}

}
