package mesquite.zephyr.lib;

public class CipresJobFile {
	String downloadURL = null;
	String downloadTitle = null;
	String fileName = null;
	long length=0;
	
	public String getDownloadURL() {
		return downloadURL;
	}
	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
	public String getDownloadTitle() {
		return downloadTitle;
	}
	public void setDownloadTitle(String downloadTitle) {
		this.downloadTitle = downloadTitle;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}

}
