package marble.boilerplate_miner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class APIPattern {
	private int id;
	private ArrayList<String> APICalls;
	private ArrayList<ClientMethod> ClientMethods;
	private double Probability;
	private double Interestingness;
	private double Support;
	private double NumProjects;
	
	public APIPattern(int id, String line) {
		this.id = id;
		this.APICalls = new ArrayList<String>(Arrays.asList(line.replaceAll("\\s", "").replaceAll("\\[", "").replaceAll("\\]", "").split(",")));
		this.ClientMethods = new ArrayList<ClientMethod>();
	}
	
	public void setProbInt(String line) {
		this.Probability = Double.parseDouble(line.split("\tint: ")[0].replaceAll("prob: ", "").replaceAll("\\s",""));
		this.Interestingness = Double.parseDouble(line.split("\tint: ")[1].split("\tcount: ")[0].replaceAll("\\s",""));
	}
	
	public void setSupNumprj(String line) {
		this.Support = Integer.parseInt(line.split("\tnum of projects: ")[0].replaceAll("support: ", "").replaceAll("\\s",""));
		this.NumProjects = Integer.parseInt(line.split("\tnum of projects: ")[1].replaceAll("\\s",""));
	}
	
	public void addClientMethod(ClientMethod cm) {
		this.ClientMethods.add(cm);
	}
	
	public boolean isSingleCall() {
		if (this.APICalls.size() == 1) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean containsDuplicateCalls() {
		if (this.APICalls.size() != new HashSet<String>(this.APICalls).size()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int getID() {
		return this.id;
	}
	
	public ArrayList<String> getPattern() {
		return this.APICalls;
	}
	
	public ArrayList<ClientMethod> getClientMethod() {
		return this.ClientMethods;
	}
	
	public void printPattern() {
		System.out.println("pattern " + Integer.toString(this.id));
		String prefix = "";
		for (String p : this.APICalls) {
			System.out.print(prefix + p);
			prefix = ", ";
		}
		System.out.println();
	}
	
	public String getPatternInString() {
		String result = "[";
		String prefix = "";
		for (String p : this.APICalls) {
			result = result + prefix + p;
			prefix = ", ";
		}
		result = result + "]";
		return result;
	}
}
