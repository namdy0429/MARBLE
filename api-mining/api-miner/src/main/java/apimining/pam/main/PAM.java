package apimining.pam.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.eclipse.jdt.core.dom.CompilationUnit;

import apimining.pam.main.InferenceAlgorithms.InferGreedy;
import apimining.pam.main.InferenceAlgorithms.InferenceAlgorithm;
import apimining.pam.sequence.Sequence;
import apimining.pam.util.Logging;
import apimining.pam.transaction.TransactionMetaInfo;
import apimining.java.ASTVisitors;
import apimining.java.ASTVisitors.MethodPrintVisitor;
/** dayen: import EMStep to use getTransactionsOfSequences */
import apimining.pam.main.PAMCore;

public class PAM extends PAMCore {

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "ARFF file with call sequences", required = true)
//		String arffFile = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/calls/hadoop.arff";
		String arffFile = "";

		/** dayen: add source file directory to print the method implementation of patterns */
		@Parameter(names = { "-sd", "--source" }, description = "Source code directory", required = true)
//		String sourceDir = "/Users/dayen/eclipse-workspace/api-mining-master/datasets/source/";
		String sourceDir = "";

		@Parameter(names = { "-od", "--outDir" }, description = "Output directory", required = true)
//		String outDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/";
		String outDir = "";

		@Parameter(names = { "-s", "--maxSteps" }, description = "Max structure steps")
		int maxStructureSteps = 100_000;

		@Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
		int maxEMIterations = 10_000;

		@Parameter(names = { "-l", "--log-level" }, description = "Log level", converter = LogLevelConverter.class)
		Level logLevel = Level.INFO;

		@Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
		long maxRunTime = 72 * 60; // 12hrs

		@Parameter(names = { "-t", "--timestamp" }, description = "Timestamp Logfile", arity = 1)
		boolean timestampLog = true;

		@Parameter(names = { "-v", "--verbose" }, description = "Log to console instead of logfile")
		boolean verbose = false;

		@Parameter(names = { "-p", "--process" }, description = "Number of Processes")
		String numThread = "8";
	}

	public static void main(final String[] args) throws Exception {

		// Main fixed parameters
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);
		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}
		/** dayen: limit the number of threads */
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", params.numThread);

		// Set loglevel, runtime, timestamp and log file
		LOG_LEVEL = params.logLevel;
		MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
		String[] apiNameSplit = params.arffFile.split(".arff")[0].split("/");
		String sourceDir = params.sourceDir + apiNameSplit[apiNameSplit.length-1] + "/";
		String outDir = params.outDir + apiNameSplit[apiNameSplit.length-1] + "/";
		File outDirFile = new File(outDir);
		if (! outDirFile.exists()){
			outDirFile.mkdir();
		}

		File logFile = null;
		if (!params.verbose) {
			File LOG_DIR = new File(outDir);
			logFile = new File(outDir + "MARBLE_logs.log");
		}

		// Mine interesting API call sequences
		System.out.println("Processing " + FilenameUtils.getBaseName(params.arffFile) + "...");
		mineAPICallSequences(params.arffFile, outDir+"PAM_seqs.txt", inferenceAlg, params.maxStructureSteps,
				params.maxEMIterations, logFile, sourceDir);


	}

	/**
	 * Mine API call sequences using PAM
	 *
	 * @param arffFile
	 *            API calls in ARF Format. Attributes are fqCaller and fqCalls
	 *            as space separated string of API calls.
	 */

	/** dayen: added sourceDir param to output source code implementation of patterns */
	public static void mineAPICallSequences(final String arffFile, final String outFile,
											final InferenceAlgorithm inferenceAlgorithm, final int maxStructureSteps, final int maxEMIterations,
											final File logFile, final String sourceDir) throws Exception {

		final File fout = new File(outFile);
		if (fout.getParentFile() != null)
			fout.getParentFile().mkdirs();

		System.out.print("  Creating temporary transaction DB... ");
		final File transactionDB = File.createTempFile("APICallDB", ".txt");
		final BiMap<String, Integer> dictionary = HashBiMap.create();
		final ArrayList<TransactionMetaInfo> transactionMetaInfos = new ArrayList<TransactionMetaInfo>();
		generateTransactionDatabase(arffFile, dictionary, transactionDB, transactionMetaInfos);
		System.out.println("done.");

		System.out.print("  Mining interesting sequences... ");
		/** dayen: add last sequences into transaction meta data */
//		/** dayen: return sequences and filenames that the pattern occurred, instead of the probability */
		final Map<Sequence, Double> sequences = PAMCore.mineInterestingSequences(transactionDB, inferenceAlgorithm,
				maxStructureSteps, maxEMIterations, logFile, transactionMetaInfos, dictionary);
		transactionDB.delete();
		System.out.println("done.");

		decodeInterestingSequences(sequences, dictionary, outFile);
	}

	private static void generateTransactionDatabase(final String arffFile, final BiMap<String, Integer> dictionary,
													final File transactionDB, final ArrayList<TransactionMetaInfo> transactionMetaInfos) throws IOException {

		int mID = 0;
		boolean found = false;
		final PrintWriter out = new PrintWriter(transactionDB);
		final LineIterator it = FileUtils.lineIterator(new File(arffFile));

		/** dayen: save meta_data of each transaction */
		int tID = 0;
//		boolean isOdd = true;

		while (it.hasNext()) {
			final String line = it.nextLine();

			if (found) {
//				/** dayen: only read 1/2 to reduce the runtime */
//				if (isOdd) {
//					isOdd = false;
//					continue;
//				}
//				else {
//					isOdd = true;
//				}

				/** dayen: save meta_data of each transaction */
				transactionMetaInfos.add(new TransactionMetaInfo(line));

//				for (final String raw_call : line.split(",")[1].replace("\'", "").split(" ")) {
				for (final String raw_call : line.split("','")[3].replace("|", "").replace("'", "").split(" ")) {
					final String call = raw_call.trim();
					if (call.isEmpty()) // skip empty strings
						continue;
					if (dictionary.containsKey(call)) {
						final int ID = dictionary.get(call);
						out.print(ID + " -1 ");
					} else {
						out.print(mID + " -1 ");
						dictionary.put(call, mID);
						mID++;
					}
				}
				/** dayen: add meta_data tID to the end of each line */
//				out.println("-2");
				out.println(String.format("-2 %d", tID));
				tID++;
			}

			if (line.contains("@data"))
				found = true;

		}
		it.close();
		out.close();
	}

	private static void decodeInterestingSequences(final Map<Sequence, Double> sequences,
												   final BiMap<String, Integer> dictionary, final String outFile) throws IOException {

		final PrintWriter out = new PrintWriter(outFile);
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			out.println(String.format("prob: %1.5f", entry.getValue()));
			out.print("[");
			String prefix = "";
			for (final int item : entry.getKey()) {
				out.print(prefix + dictionary.inverse().get(item));
				prefix = ", ";
			}
			out.print("]");
			/** dayen: print meta information of transactions which contain this pattern */

			out.println();
			out.println();
		}
		out.close();

	}

	private static void decodeInterestingSequencesWithFiles(final Map<Sequence, List<String>> sequences,
															final BiMap<String, Integer> dictionary, final String outDir, final String sourceDir) throws IOException {

		final PrintWriter out = new PrintWriter(String.join("/", outDir, "PAM_eg_seqs.txt"));
		int patternIndex = -1;
		for (final Entry<Sequence, List<String>> entry : sequences.entrySet()) {
			patternIndex++;
			out.println(String.format("pattern %d", patternIndex));
			out.print("[");
			String prefix = "";
			for (final int item : entry.getKey()) {
				out.print(prefix + dictionary.inverse().get(item));
				prefix = ", ";
			}
			out.print("]");
			out.println();
			out.println();
			/** dayen: when there are more than two files for a pattern */
			if (entry.getValue().size() < 2) {
				continue;
			}
			/** dayen: TODO: toString parameter unresolved error fix */
			/** dayen: TODO: support based on project level */
			/** dayen: print meta information of transactions which contain this pattern */
			for (final String file_caller: entry.getValue()) {
				final String fileName = file_caller.split("-----")[0];
				final String callerName = file_caller.split("-----")[1];

				String outputfile_dir = outDir + "/" + Integer.toString(patternIndex) + "/" + file_caller + ".java";
				if(outputfile_dir.length() > 200) {
					outputfile_dir = outputfile_dir.substring(0, 200) + ".java";
				}
				final File patternEgFile = new File(outputfile_dir);
				patternEgFile.getParentFile().mkdirs();
				final PrintWriter fileOut = new PrintWriter(patternEgFile);

				final File methodFile = new File(sourceDir + "/" + fileName + ".java");
				CompilationUnit ast = ASTVisitors.getAST(methodFile);
				final MethodPrintVisitor mpv = new MethodPrintVisitor(ast);
				mpv.process(ast);
				final Map<String, String> methodImplementation = mpv.methodImplementation;

				for (final Entry<String, String> mi : methodImplementation.entrySet()) {
					if (callerName.contains(mi.getKey())) {
						fileOut.print("//");
						fileOut.println(callerName);
						fileOut.print(mi.getValue());
						fileOut.println();
					}
				}
				fileOut.close();

			}
			out.println();
			out.println();

		}
		out.close();

	}

	/** Convert string level to level class */
	public static class LogLevelConverter implements IStringConverter<Level> {
		@Override
		public Level convert(final String value) {
			if (value.equals("SEVERE"))
				return Level.SEVERE;
			else if (value.equals("WARNING"))
				return Level.WARNING;
			else if (value.equals("INFO"))
				return Level.INFO;
			else if (value.equals("CONFIG"))
				return Level.CONFIG;
			else if (value.equals("FINE"))
				return Level.FINE;
			else if (value.equals("FINER"))
				return Level.FINER;
			else if (value.equals("FINEST"))
				return Level.FINEST;
			else
				throw new RuntimeException("Incorrect Log Level.");
		}
	}

}
