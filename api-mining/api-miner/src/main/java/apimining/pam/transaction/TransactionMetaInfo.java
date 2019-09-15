package apimining.pam.transaction;

public class TransactionMetaInfo {
	private String ProjectName;
	private String FileName;
	private String CallerSignature;
	
	public TransactionMetaInfo(String line) {
		String[] infos = line.split("','");
//		this.ProjectName = infos[0].split("\\.")[0] + "." + infos[0].split("\\.")[1];
//		twitter4j
//		if (infos[1].split("\\.").length < 2) {
//			this.ProjectName = infos[1].split("\\.")[0];
//		}
//		else {
//			this.ProjectName = infos[0].split("_____")[0] + "." + infos[0].split("_____")[1];
		if (infos[1].contains("\\.")) {
			this.ProjectName = infos[1].split("\\.")[0] + "." + infos[1].split("\\.")[1];
		}
		else {
			this.ProjectName = infos[1];
		}
//		}
		this.FileName = infos[0].replaceAll("'", "");
		this.CallerSignature = infos[2];
	}
	
	public String getProjectName() {
		return this.ProjectName;
	}
	
	public String getFileName() {
		return this.FileName;
	}
	
	public String getCallerSignature() {
		return this.CallerSignature;
	}
}
