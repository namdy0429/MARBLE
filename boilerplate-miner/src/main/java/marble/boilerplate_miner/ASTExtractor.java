package marble.boilerplate_miner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import at.unisalzburg.dbresearch.apted.costmodel.PerEditOperationStringNodeDataCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;
import at.unisalzburg.dbresearch.apted.parser.InputParser;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.io.TreeIoUtils.TreeSerializer;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;

public class ASTExtractor {
	
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "Log file with usage patterns and the clicnet code list", required = true)
//		String logFile = "/Users/dayen/eclipse-workspace/api-mining-master/output/all/javax_xml_transform/pam/PAM_logs.log";
		String logFile = "";
		
		/** dayen: add source file directory to print the method implementation of patterns */
		@Parameter(names = { "-sd", "--source" }, description = "Source code directory", required = true)
//		String sourceDir = "/Users/dayen/eclipse-workspace/api-mining-master/datasets/source/javax_xml_transform/";
		String sourceDir = "";

		@Parameter(names = { "-od", "--outDIr" }, description = "Output directory", required = true)
		/** dayen: when printing files containing the patterns, input folder dir instead of the file name */
//		String outDir = "/Users/dayen/eclipse-workspace/api-mining-master/output/all/javax_xml_transform/pam/diff/";
		String outDir = "";

		@Parameter(names = { "-ps", "--patternStart" }, description = "Top n patterns to evaluate")
		int patternStart = 0;

		@Parameter(names = { "-pl", "--patternLimit" }, description = "Top n patterns to evaluate")
		int patternLimit = -1;
		
		@Parameter(names = {"-p", "--processor"}, description = "Number of Threads")
		int numThread = 1;
		
		@Parameter(names = {"-ms", "--max_statement"}, description = "Max number of AST method invocation nodes a single-call subtree can have")
		int max_api_calls = 20;
		
	}
	
	public static void main(final String[] args) throws Exception{
		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);
		
		long tStart = System.currentTimeMillis();

		/** dayen: limit the number of threads */
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(params.numThread));
		
		try {
			jc.parse(args);

			System.out.println("Processing " + FilenameUtils.getBaseName(params.logFile) + "...");
			compareASTs(params.logFile, params.sourceDir, params.outDir, params.patternStart, params.patternLimit, params.numThread, params.max_api_calls);

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}
		long tEnd = System.currentTimeMillis();
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		System.out.println(elapsedSeconds);
	}
	
	private static void compareASTs(final String logFile, final String sourceDir, final String outDir, 
			final int patternStart, int patternLimit, final int numThread, final int max_api_calls) throws Exception {
		HashMap<Integer, APIPattern> dictionary = new HashMap<Integer, APIPattern>();
		readLogFile(logFile, dictionary);
		
		Run.initGenerators();

		if (patternLimit == -1) {
			patternLimit = dictionary.size();
		}
		for (final Entry<Integer, APIPattern> entry : dictionary.entrySet()) {
			if (entry.getKey() < patternStart) {
				continue;
			}
			if (entry.getKey() > patternLimit) {
				break;
			}
			File outFolder = new File(outDir);
			if (! outFolder.exists()){
				outFolder.mkdir();
			}
			outFolder = new File(outDir + "/AST");
			if (! outFolder.exists()){
				outFolder.mkdir();
			}

//			final File sf = new File(outDir + "Similarity_" + Integer.toString(entry.getKey()) + ".txt");
//			final PrintWriter sOut = new PrintWriter(sf);
			final File ef = new File(outDir + "embedding_" + Integer.toString(entry.getKey()) + ".txt");
			final PrintWriter eOut = new PrintWriter(ef);
			
//			final File el = new File(outDir + "pattern_" + Integer.toString(entry.getKey()) + ".edgelist");
//			final PrintWriter eOut = new PrintWriter(el);

			final APIPattern apiPattern = entry.getValue();
			final ArrayList<ClientMethod> client_methods = new ArrayList<ClientMethod>();
//			sOut.println(apiPattern.getPatternInString());
//			sOut.println();
			
			System.out.println();
			apiPattern.printPattern();
			final ArrayList<String> curPattern = apiPattern.getPattern();
			
			HashSet<String> fileSet = new HashSet<String>();
			
			for (int i=0; i<apiPattern.getClientMethod().size(); i++) {
				String cur_file = apiPattern.getClientMethod().get(i).getFileName();
				if (fileSet.contains(cur_file)) {
					continue;
				}
				else {
					fileSet.add(cur_file);
					client_methods.add(apiPattern.getClientMethod().get(i));
				}
			}

			
//			HashMap<String, Pair<ITree, TreeContext>> AST_map = new HashMap<String, Pair<ITree, TreeContext>>();
			ArrayList<Pair<ITree, TreeContext>> tree_list = new ArrayList<Pair<ITree, TreeContext>>();
			for (ClientMethod method: client_methods) {
				
				Pair <ITree, TreeContext> tree_pair = getTree(sourceDir,  outDir, method.getFileName(), method.getCallerName());
//				AST_map.put(method.getFileName(), tree_pair);
				tree_list.add(tree_pair);
			}
			ArrayList<String> embeddings = new ArrayList<String>();
			for (Pair<ITree, TreeContext> tp :tree_list) {
				embeddings.add(formTransaction(tp.second, tp.first, "", 0, true));
//				embeddings.add(getSubtreeEmbeddings(tp.first, tp.second, curPattern, max_api_calls));
			}
			for (String s : embeddings) {
				eOut.println(s);
			}

//			sOut.println("File Index Table");
//			sOut.println();
//			for (int i=0; i<client_methods.size(); i++) {
//				sOut.print(String.format("%d \t %s %n", i, client_methods.get(i).getFileName()));
//			}
			eOut.close();
//			sOut.close();
//			eOut.close();
		}
	}
	
	private static int getNumMethodCalls(ITree tree) {
		int num_calls = 0;
		for (ITree node: tree.getDescendants()) {
			if (node.getType() == 32 || node.getType() == 14) {
				num_calls += 1;
			}
		}
		return num_calls;
	}
	
	
	private static Pair<ITree, TreeContext> getTree(final String sourceDir, final String outDir, final String srcFileName, final String srcCallerName) {
		Pair<ITree, TreeContext> result;
		try {
			result = _getSubTree(sourceDir, outDir, srcFileName, srcCallerName);
		} catch (IOException e) {
			result = null;
			e.printStackTrace();
		}
		return result;
	}
	
	private static Pair<ITree, TreeContext> _getSubTree(final String sourceDir, final String outDir, final String srcFileName, final String srcCallerName) throws IOException {
		String srcFile = sourceDir + srcFileName + ".java";
		
		File srcAST = new File(outDir + "AST/" + srcFileName + ".ast");
		TreeContext tc1;
		ITree tree1;
		Boolean isSrcASTExist = srcAST.exists();
		
		if (isSrcASTExist) {
			try {
				TreeGenerator g = TreeIoUtils.fromXml();
				tc1 = g.generateFromFile(srcAST);
				tree1 = tc1.getRoot();
			} catch(NullPointerException e) {
				tc1 = Generators.getInstance().getTree(srcFile); 
				tree1 = tc1.getRoot();
				System.out.println("Could not read AST file: " + srcFileName + ", " + srcCallerName);
			}
		}
		else {
			tc1 = Generators.getInstance().getTree(srcFile); 
			tree1 = tc1.getRoot();
			
			TreeSerializer g = TreeIoUtils.toXml(tc1);
			try {
				g.writeTo(srcAST);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		Boolean foundMethod = false;
//		for (final ITree it: tree1.getChild(tree1.getChildren().size()-1).getChildren()) {
//			for (final ITree cit: it.getChildren()) {
//				if (tc1.getTypeLabel(cit).equals("SimpleName") && cit.getLabel().equals(srcCallerName)) {
//					foundMethod = true;
////					tree1 = it.getChild(it.getChildren().size()-1);
//					tree1 = it;
//					break;
//				}
//			}
//		}
//		if (!foundMethod) {
//			for (int c_idx = tree1.getChildren().size()-2; c_idx>=0; c_idx--) {
//				if(!foundMethod) {
//					for (final ITree it: tree1.getChild(c_idx).getChildren()) {
//						if(!foundMethod) {
//							for (final ITree cit: it.getChildren()) {
//								if (tc1.getTypeLabel(cit).equals("SimpleName") && cit.getLabel().equals(srcCallerName)) {
//									foundMethod = true;
//									tree1 = tree1.getChild(c_idx);
//									break;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		if (!foundMethod) {
////			System.out.println("Cannot find caller method from AST, use the whole AST");
////			System.out.println(srcFileName + ", " + srcCallerName);
////			throw new TreeTraversalException("Cannot find caller method from AST: " + srcFileName + ", " + srcCallerName);
//		}
		return new Pair<ITree, TreeContext> (tree1, tc1);
	}
	
//	private static ArrayList<Pair<String, String>> getSubtreeEmbeddings(final ITree tree1, final TreeContext tc1, final ArrayList<String> curPattern, final int max_api_calls) {
	private static ArrayList<String> getSubtreeEmbeddings(final ITree tree1, final TreeContext tc1, final ArrayList<String> curPattern, final int max_api_calls) {
		
//		ArrayList<Pair<String, String>> subtreeEmbeddings = new ArrayList<Pair<String, String>>();
		ArrayList<String> treeEmbeddings = new ArrayList<String>();
		LinkedHashMap<String, ArrayList<ITree>> matchMap1 = new LinkedHashMap<String, ArrayList<ITree>>();
		for (final String call: curPattern) {
			matchMap1.put(getSimpleName(call), new ArrayList<ITree>());
		}
//		for (final ITree it: tree1.preOrder()) {
//			if (tc1.getTypeLabel(it.getType()).equals("SimpleName")) {					
//				ArrayList<ITree> curList = matchMap1.get(it.getLabel());
//				if (curList != null) {
//					curList.add(it);
//				}
//			}
//		}
//		ArrayList<ITree> subtrees1 = null; 
//		if (curPattern.size() == 1) {		
//			subtrees1 = getSingleAPISubtree(matchMap1, curPattern, tree1, tc1, max_api_calls);
//		}
//		else {
//			subtrees1 = getAPTEDSubtree(matchMap1, curPattern, tree1, max_api_calls);
//		}
//		if (subtrees1.size() == 0) {
//			return subtreeEmbeddings;
//		}

//		for (ITree it1 : subtrees1) {
			String trans1 = formTransaction(tc1, tree1, "", 0, true);
//			int num_nodes = StringUtils.countMatches(trans1, "{"); 
//			if (num_nodes <= 3) {
//				continue;
//			}
			treeEmbeddings.add(trans1);
//			String positions = String.format("%d, %d, %d", it1.getPos(), it1.getEndPos(), num_nodes); 
//			subtreeEmbeddings.add(new Pair<String, String>(trans1, positions));
//		}
//		return subtreeEmbeddings;
			return treeEmbeddings;
		
	}	

	private static Pair<Double, String> fasterASTEDClientFiles(final ArrayList<Pair<String, String>> subtrees1, 
			final ArrayList<Pair<String,String>> subtrees2) throws IOException, TreeTraversalException {	
			// Parse the input and transform to Node objects storing node information in MyNodeData.
			InputParser<StringNodeData> parser = new BracketStringInputParser();
			APTED<PerEditOperationStringNodeDataCostModel, StringNodeData> apted = new APTED<PerEditOperationStringNodeDataCostModel, StringNodeData>(new PerEditOperationStringNodeDataCostModel(1, 1, 1));
			
			if (subtrees1.size() == 0 || subtrees2.size() == 0) {
				return new Pair<Double, String>(-1.0, "");
			}
			
			
			double minDistance = Double.MAX_VALUE;
			String srcPositions = "";
			String dstPositions = "";
			
			for (Pair<String, String> it1 : subtrees1) {
				for (Pair<String, String> it2 : subtrees2) {
					String trans1 = it1.first;
					String trans2 = it2.first;
//					System.out.println(trans1);
//					System.out.println(trans2);
					
					Node<StringNodeData> t1 = parser.fromString(trans1);
					Node<StringNodeData> t2 = parser.fromString(trans2);
					// Initialise APTED.
//					APTED<PerEditOperationStringNodeDataCostModel, StringNodeData> apted = new APTED<PerEditOperationStringNodeDataCostModel, StringNodeData>(new PerEditOperationStringNodeDataCostModel(1, 1, 1));
					// Execute APTED.
					double result = apted.computeEditDistance(t1, t2);
					
					apted.init(t1, t2);
					
					if (minDistance > result) {
						minDistance = result;
						srcPositions = it1.second;
						dstPositions = it2.second;
					}
				}
			}
			String positions = String.format("%s, %s%n", srcPositions, dstPositions);
			
			double similarity = 0;
			if (minDistance != 0) {
				similarity = (double)1 / java.lang.Math.exp((double)minDistance*0.1);
			}
			else {
				return new Pair<Double, String>(1.0, positions);
				
			}
			
			return new Pair<Double, String>(similarity, positions);
	
	}
	
	
	private static String formTransaction(TreeContext tc, ITree t, String p_tag, Integer child_num, Boolean is_root) {
		String result = "";
		String tag = tc.getTypeLabel(t.getType());
		
		if (tag.equals("SimpleName")) {
//			if ((p_tag.equals("MethodInvocation") && child_num < 2) ||
			if ((p_tag.equals("MethodInvocation") && child_num == 1) ||
				(p_tag.equals("ClassInstanceCreation") && child_num == 0) ){
			result = result + "{" + t.getLabel() + "}";
			return result;
//			result = result + " " + t.getLabel();
			}
		}
		ArrayList<String> interestingTags = new ArrayList<String>();
		
		interestingTags.add("TryStatement");
		interestingTags.add("CastExpression");
		interestingTags.add("CatchClause");
		interestingTags.add("ConditionalExpression");
		interestingTags.add("DoStatement");
		interestingTags.add("ForStatement");
		interestingTags.add("EnhancedForStatement");
		interestingTags.add("IfStatement");
		interestingTags.add("SwitchCase");
		interestingTags.add("SwitchStatement");
		interestingTags.add("ThrowStatement");
		interestingTags.add("WhileStatement");
//		interestingTags.add("MethodInvocation");
//		interestingTags.add("ClassInstanceCreation");
//		interestingTags.add("InfixExpression");
//		interestingTags.add("NullLiteral");
//		interestingTags.add("StringLiteral");
		
		ArrayList<String> interestingParentTags = new ArrayList<String>();
		interestingParentTags.add("CatchClause");
		interestingParentTags.add("IfStatement");
		interestingParentTags.add("SwitchCase");
		
		
		ArrayList<String> loopTags = new ArrayList<String>();
		loopTags.add("ForStatement");
		loopTags.add("WhileStatement");
		
		
		if (interestingTags.contains(tag)) {
			if (tag.equals("DoStatement") || tag.equals("EnhancedForStatement") || loopTags.contains(tag))
				result = result + "{Loop";
			else
				result = result + "{" + tag;
		}
		// TODO: try-with, try-catch-finally ... 
//		else if (!interestingTags.contains(tag) && p_tag.equals("TryStatement")) {
//			if (t.getParent().getChildren().size() > 0){
//				if (child_num == 0)
//					result = result + "{with";
//				else if (child_num == t.getParent().getChildren().size()-1) 
//					result = result + "}{Execute";
//			}
//			else {
//				
//			}
//			
//		}
		else if (!interestingTags.contains(tag) && interestingParentTags.contains(p_tag)) {
			if (child_num == 0) {
				result = result + "{Condition";
			}
			else {
				result = result + "{Execute";
			}
			
		}
		else if (!interestingTags.contains(tag) && loopTags.contains(p_tag)) {
			if (child_num == 0) {
				result = result + "{Condition";
			}
			else if (child_num == t.getParent().getChildren().size()-1 && child_num != 0) {
				result = result + "{Execute";
			}
		}
		else if (!interestingTags.contains(tag) && p_tag.equals("EnhancedForStatement")) {
			if (child_num == 0) {
				result = result + "{Condition";
			}
			else if (child_num == 2) {
				result = result + "{Execute";
			}
		}
		else if (!interestingTags.contains(tag) && p_tag.equals("DoStatement")) {
			if (child_num == 0) {
				result = result + "{Execute";
			}
			else if (child_num == 1) {
				result = result + "{Condition";
			}
		}
		


		int child_count = 0;
		//TODO: cannot extract the API call only, but need to add params / calling objs
		if (tag.equals("MethodInvocation")) {
			if (t.getChildren().size() > 2) {
				for (int child_idx=2; child_idx < t.getChildren().size(); child_idx++) {
					result = result + formTransaction(tc, t.getChild(child_idx), tag, child_idx, false);
				}
				result = result + formTransaction(tc, t.getChild(0), tag, 0, false);
				result = result + formTransaction(tc, t.getChild(1), tag, 1, false);
			}
			else if (t.getChildren().size() == 2 ) {
				result = result + formTransaction(tc, t.getChild(0), tag, 0, false);
				result = result + formTransaction(tc, t.getChild(1), tag, 1, false);
			}
			else if (t.getChildren().size() == 1) {
				// If we use both [0] and [1] in comparison, the similarity score will fluctuate when client code use different variable names 
				// ignore [0] -> sacrifice some API calls in this case: [api_call](params)
				// but since all client code will contain this api_call, wouldn't make a lot of difference
//				result = result + formTransaction(tc, t.getChild(0), tag, 0, false); 
				result = result + formTransaction(tc, t.getChild(0), tag, 1, false);
			}
			else {
				System.out.println("ERROR: Method Invocation has wrong format!");
			}
			
		}
		else if (tag.equals("ClassInstanceCreation")) {
			if (t.getChildren().size() > 1) {
				for (int child_idx=1; child_idx < t.getChildren().size(); child_idx++) {
					result = result + formTransaction(tc, t.getChild(child_idx), tag, child_idx, false);
				}
			}
			result = result + "{Class" + formTransaction(tc, t.getChild(0), tag, 0, false) + "}";
		}
		else {
			for (ITree ch: t.getChildren()) {
				if (tag.equals("SimpleType") && p_tag.equals("ClassInstanceCreation")) {
					result = result + formTransaction(tc, ch, p_tag, child_count, false);
				}
				else {
					result = result + formTransaction(tc, ch, tag, child_count, false);
				}
				child_count = child_count + 1;
			}
		}
		
		//TODO: separate the case baed on the child num
		if (interestingTags.contains(tag)) {
			result = result + "}";
		}
		else if (interestingParentTags.contains(p_tag)){
			result = result + "}";
		}
		else if (loopTags.contains(p_tag) && ((child_num == 0) || (child_num == t.getParent().getChildren().size()-1) )){
			result = result + "}";
		}
		else if (p_tag.equals("EnhancedForStatement") && ((child_num == 1) || (child_num == t.getParent().getChildren().size()-1) )){
			result = result + "}";
		}
		else if (p_tag.equals("DoStatement") && ((child_num == 0) || (child_num == t.getParent().getChildren().size()-1) )){
			result = result + "}";
		}
		if (is_root) {
			result = "{Root" + result + "}";
		}
		return result;
	}
	
	private static ITree getEncompassingSubtreeRange(ITree t, int start_idx, int end_idx) {
		ITree st = t;
		boolean isContinue = true;
		
		while (isContinue) {
			List<ITree> curChildren = st.getChildren();
			isContinue = false;
			for (int i=1; i<curChildren.size(); i++) {
				if (curChildren.get(i-1).getId() < start_idx && curChildren.get(i).getId() > end_idx) {
					st = curChildren.get(i);
					isContinue = true;
					break;
				}
			}
		}
		
		return st;
				
	}
	
	private static String getSimpleName(String fqname) {
		String[] fullName = fqname.split("\\.");
		if (fullName[fullName.length - 1].equals("<init>")) {
			return fullName[fullName.length - 2];
		}
		else {
			return fullName[fullName.length - 1];
		}
	}
	
	private static ITree getPrevSibling(ITree curNode) {
		if (curNode.positionInParent() > 0) {
			return curNode.getParent().getChild(curNode.positionInParent()-1);
		}
		else if (curNode.positionInParent() == -1) {
			return curNode;
		}
		else if (curNode.positionInParent() == 0) { 
			return getPrevSibling(curNode.getParent()); 
		}
		else {
			return null;
		}
	}

	private static void readArffFile(final String arffFile, HashMap<Integer, APIPattern> dictionary) throws IOException {
		
		final LineIterator it = FileUtils.lineIterator(new File(arffFile));
		boolean found = false;
		ArrayList<String> method_list = new ArrayList<String>();
		
		while (it.hasNext()) {
			final String line = it.nextLine();
			
			if (line.contains("@data")) {
				found = true;
			}
			if (found) {
				method_list.add(line.split("','")[0].replaceAll("'", ""));
			}
		}
		
	}
	
	private static void readLogFile(final String logFile, HashMap<Integer, APIPattern> dictionary) throws IOException {

		boolean found = false;
		final LineIterator it = FileUtils.lineIterator(new File(logFile));
		
		/** dayen: save meta_data of each transaction */
		int pattern_idx = -1;
//		boolean isOdd = true;
		boolean readPattern = false;
		
		while (it.hasNext()) {
			final String line = it.nextLine();

			if (found) {
				if (line.contains("pattern ") && !line.contains("biased ")) {
					pattern_idx = Integer.parseInt(line.split("pattern ")[1].replaceAll("\\s","")); 
					readPattern = true;
				}
				/** dayen: add pattern index and patterns into dictionary */
				else if (line.contains("[") && line.contains("]") && readPattern) {
					dictionary.put(pattern_idx, new APIPattern(pattern_idx, line));
					readPattern = false;
				}
				
				else if (line.contains("file:")) {
					dictionary.get(pattern_idx).addClientMethod(new ClientMethod(line));
				}
				else if (line.contains("prob: ")) {
					dictionary.get(pattern_idx).setProbInt(line);
				}
				else if (line.contains("support: ")) {
					dictionary.get(pattern_idx).setSupNumprj(line);
				}
				else if (line.equals("\n")) {
					continue;
				}
				else if (line.contains("biased")) {
					continue;
				}
				else if (line.contains("Filtered SEQUENCES")) {
					break;
				}
				else {
//					System.out.println(line);
				}
		
			}

			if (line.contains("INTERESTING SEQUENCES"))
				found = true;

		}
		it.close();
	}
}
