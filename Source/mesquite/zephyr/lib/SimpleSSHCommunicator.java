package mesquite.zephyr.lib;

import mesquite.externalCommunication.lib.SSHCommunicator;
import mesquite.lib.Debugg;
import mesquite.lib.MesquiteModule;
import mesquite.lib.StringArray;
import mesquite.lib.StringUtil;

import java.io.InputStream;

import com.jcraft.jsch.*;

public class SimpleSSHCommunicator extends SSHCommunicator {
	
	protected String workingDirectoryPath = "";

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
	public  void sendSSHCommands (String[] commands) {
		if (commands==null || commands.length==0)
			return;
	    String host="192.168.0.102";
	    try{
	    	commands = StringArray.addToStart(commands, "cd " + getWorkingDirectoryPath());
	    	java.util.Properties config = new java.util.Properties(); 
	    	config.put("StrictHostKeyChecking", "no");
	    	JSch jsch = new JSch();
	    	Session session=jsch.getSession(username, host, 22);
	    	session.setPassword(password);
	    	session.setConfig(config);
	    	session.connect();
	    	System.out.println("Connected");
	    	
	    	for (int i=0; i<commands.length; i++)
	    		if (StringUtil.notEmpty(commands[i]))
	    				sendCommand(session, commands[i]);
	    	
	        session.disconnect();
	        System.out.println("DONE");
	    }catch(Exception e){
	    	e.printStackTrace();
	    }

	}
	
	public String getWorkingDirectoryPath() {
		return workingDirectoryPath;
	}

	public void setWorkingDirectoryPath(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
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
