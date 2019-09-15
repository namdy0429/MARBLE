package marble.boilerplate_miner;

import java.util.ArrayList;
import java.util.Arrays;

public class ClientMethod {
	private String ProjectName;
	private String FileName;
	private int FileId = -1;
	private String CallerName;
	private ArrayList<String> Parameters;
	
	public ClientMethod(String line) {
		String[] infos = line.split("\tcaller: ");
		this.ProjectName = "";
//		this.ProjectName = infos[0].replaceAll("file: ", "").split("_____")[0] + "_____" + infos[0].replaceAll("file: ", "").split("_____")[1];
//		this.ProjectName = infos[0].replaceAll("file: ", "").split("\\.")[0] + infos[0].replaceAll("file: ", "").split("\\.")[1];
		this.FileName = infos[0].replaceAll("file: ", "").replaceAll("\\s","");
		String[] CallerNameSplits = infos[1].split("\\(")[0].split("\\."); 
		this.CallerName = CallerNameSplits[CallerNameSplits.length-1];
		this.Parameters = new ArrayList<String>(Arrays.asList(
				infos[1].split("\\(")[1].replaceAll("\\)", "").replaceAll("\\s","").split(",")));
	}
	
	public void setFileId(int file_id) {
		this.FileId = file_id;
	}
	
	public int getFileId() {
		return this.FileId;
	}
	
	public String getProjectName() {
		return this.ProjectName;
	}
	
	public String getFileName() {
		return this.FileName;
	}
	
	public String getCallerName() {
		return this.CallerName;
	}
	
	public ArrayList<String> getParameters() {
		return this.Parameters;
	}
}
