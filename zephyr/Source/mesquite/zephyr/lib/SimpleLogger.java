/** this should probably be replaecd by log4j, but I (MTH) did not want to introduce a third party dependency */
package mesquite.zephyr.lib;

import mesquite.lib.MesquiteModule;
import mesquite.lib.MesquiteTrunk;

public class SimpleLogger implements mesquite.lib.Logger {
	String name = null;
	int level;
	MesquiteModule module = null;
	
	public static final int DEBUG = 10;
	public static final int INFO = 20;
	public static final int WARN = 30;
	public static final int ERROR = 40;
	
	public static int getDefaultLevel() {
		/** @mth need to get property at runtime */
		return SimpleLogger.DEBUG;
	}
	public SimpleLogger(MesquiteModule mod) {
		this.module = mod;
		this.name = mod.getName();
		this.level = SimpleLogger.getDefaultLevel();
	}
	public void setLevel(int levelArg) {
		this.level = levelArg;
	}
	public void log(int msgLevel, Exception e) {
		String m = e.getMessage();
		this.log(msgLevel, "Exception: " + m);
		e.printStackTrace(System.err);
	}
		
	public void log(int msgLevel, String message) {
		if (msgLevel < this.level)
			return;
		this.writeLogMessage(this.getPrefix(msgLevel) + message);
	}
	public String getPrefix(int msgLevel) {
		if (this.name != null)
			return this.name + " " + this.getLevelName(msgLevel) + ": ";
		return this.getLevelName(msgLevel) + ": ";
	}
	public String getLevelName(int msgLevel) {
		if (msgLevel > SimpleLogger.INFO)
			return (msgLevel > SimpleLogger.WARN ? "ERROR" : "WARN"); 
		return (msgLevel > SimpleLogger.DEBUG ? "INFO" : "DEBUG");
	}
	boolean useSysOut(){
		return (!MesquiteTrunk.suppressSystemOutPrintln && ((!MesquiteTrunk.isMacOS() || (MesquiteTrunk.isMacOSX() && MesquiteTrunk.getJavaVersionAsDouble()>1.39))));
	}
	/*.................................................................................................................*/
	/** Places string in log AND in System.out.println.*/
	public void log(String s) {
		this.log(SimpleLogger.INFO, s);
	}
	/*.................................................................................................................*/
	/** Places string in log AND in System.out.println.*/
	public void info(String s) {
		this.log(SimpleLogger.INFO, s);
	}
	/*.................................................................................................................*/
	/** Places string in log AND in System.out.println.*/
	public void warn(String s) {
		this.log(SimpleLogger.WARN, s);
	}
	/*.................................................................................................................*/
	/** Places string in log AND in System.out.println.*/
	public void error(String s) {
		this.log(SimpleLogger.ERROR, s);
	}
	/*.................................................................................................................*/
	/** Places string in log AND in System.out.println.*/
	public void debug(String s) {
		this.log(SimpleLogger.DEBUG, s);
	}
	
	public void writeLogMessage (String s) {
		this.module.loglnNoEcho(s);
		if (useSysOut())
			System.out.println(s);
	}

	/*.................................................................................................................*/
	/** Places string and newline character in log AND in System.out.println.*/
	public void logln(String s) {
		this.log(SimpleLogger.INFO, s);
	}

}
