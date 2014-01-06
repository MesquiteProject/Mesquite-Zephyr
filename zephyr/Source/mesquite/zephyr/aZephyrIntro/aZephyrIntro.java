/* Mesquite.zephyr source code.  Copyright 2007-2009 D. Maddison and W. Maddison. Version 0.9, November 2007.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.zephyr.aZephyrIntro;import mesquite.lib.duties.*;/* ======================================================================== */public class aZephyrIntro extends PackageIntro {	/*.................................................................................................................*/	public boolean startJob(String arguments, Object condition, boolean hiredByName) { 		return true;  	 }  	 public Class getDutyClass(){  	 	return aZephyrIntro.class;  	 } 	/*.................................................................................................................*/	 public String getExplanation() {	return "Zephyr is a package of Mesquite modules providing tools for connecting to other phylogenetic inference programs.";	 }   	/*.................................................................................................................*/    	 public String getName() {		return "Zephyr Package";   	 }	/*.................................................................................................................*/	/** Returns the name of the package of modules (e.g., "Basic Mesquite Package", "Rhetenor")*/ 	public String getPackageName(){ 		return "Zephyr Package"; 	}	/*.................................................................................................................*/	/** Returns citation for a package of modules*/ 	public String getPackageCitation(){ 		return "Maddison, D.R., & W.P. Maddison.  2013.  Zephyr: A Mesquite package for interacting with extermal phylogeny inference programs. Version 0.932."; 	}	/*.................................................................................................................*/	/** Returns whether there is a splash banner*/	public boolean hasSplash(){ 		return true; 	}	/*.................................................................................................................*/	/** Returns version for a package of modules*/	public String getPackageVersion(){		return "0.932";	}	/*.................................................................................................................*/	/** Returns version for a package of modules as an integer*/	public int getPackageVersionInt(){		return 932;	}	/*.................................................................................................................*/	/** returns the URL of the notices file for this module so that it can phone home and check for messages */	public String  getHomePhoneNumber(){ 		return "http://mesquiteproject.org/packages/zephyr/notices.xml";	}	public String getPackageDateReleased(){		return "1 December 2013";	}	/*.................................................................................................................*/	public int getVersionOfFirstRelease(){		return NEXTRELEASE;  	}}