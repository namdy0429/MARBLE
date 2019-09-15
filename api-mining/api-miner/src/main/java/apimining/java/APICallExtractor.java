package apimining.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.LinkedListMultimap;

import apimining.pam.main.PAM.LogLevelConverter;
import apimining.pam.main.PAM.Parameters;
import apimining.pam.util.Logging;

import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Extract API calls into ARF Format. Attributes are fqCaller and fqCalls as
 * space separated string of API calls.
 *
 * @author Jaroslav Fowkes <jaroslav.fowkes@ed.ac.uk>
 * @author Anonymous for submission
 */
public class APICallExtractor {
	
	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-sd", "--sourceDir" }, description = "Source Folder", required = true)
		String libFolder = "";
		
		@Parameter(names = { "-nf", "--namespaceFolder" }, description = "Namespace Folder")
		String namespaceFolder = "";

		@Parameter(names = { "-ia", "--interestingAPIs" }, description = "Interesting APIs")
		ArrayList<String> interestingAPIs = new ArrayList<String>();

		@Parameter(names = { "-pn", "--packageNames" }, description = "Package Names", required = true)
		String packageName = "";

		@Parameter(names = { "-od", "--outDir" }, description = "Output Directory", required = true)
		String outFolder = "";
		
		@Parameter(names = { "-sn", "--sampleNumber"}, description = "Number of Samples")
		Integer numSample = -1;

		@Parameter(names = { "-p", "--process" }, description = "Number of Processes")
		String numThread = "8";

	}


	public static void main(final String[] args) throws IOException {
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


		String[] interestingAPIs = params.interestingAPIs.toArray(new String[0]);
		String packageName = params.packageName;
		String projFolder = packageName.replace(".", "_");
		Integer numSample = params.numSample;

		System.out.println("===== Processing " + projFolder);

		File outDir = new File(params.outFolder);
		if (! outDir.exists()){
			outDir.mkdir();
		}

		final PrintWriter out = new PrintWriter(new File(params.outFolder + projFolder + ".arff"), "UTF-8");

		out.println("@relation " + projFolder);
		out.println();
		out.println("@attribute callerFile string");
		out.println("@attribute callerPackage string");
		out.println("@attribute fqCaller string");
		out.println("@attribute fqCalls string");
		out.println();
		out.println("@data");

		// Get all java files in source folder
		List<File> files = (List<File>) FileUtils.listFiles(new File(params.libFolder + projFolder),
				new String[] { "java" }, true);

		Collections.sort(files);

		// Randomly sample some files
		if (numSample != -1) {
			List<File> sampled_files = new ArrayList<File>();
			Random random = new Random();
			for (int si=0; si < numSample; si++) {
				File random_file = files.get(random.nextInt(files.size()));
				sampled_files.add(random_file);
				files.remove(random_file);
			}
			files = sampled_files;
		}

		int count = 0;
		for (final File file : files) {
			String fileNameWithOutExt = FilenameUtils.removeExtension(file.getName());

			System.out.println("\nFile: " + file);

			// Ignore empty files
			if (file.length() == 0)
				continue;

			if (count % 50 == 0)
				System.out.println("At file " + count + " of " + files.size());
			count++;

			CompilationUnit ast = ASTVisitors.getAST(file);
			/** dayen: added interesting API that should be identified */
			final APICallVisitor acv = new APICallVisitor(ast, params.namespaceFolder, interestingAPIs);
			acv.process();
			final LinkedListMultimap<String, String> fqAPICalls = acv.getAPINames(packageName);

			String callerPackage = "";
			if (ast.getPackage() != null) {
				callerPackage = ast.getPackage().getName().toString();
			}

			for (final String fqCaller : fqAPICalls.keySet()) {
				out.print("'" + fileNameWithOutExt + "',");
				out.print("'" + callerPackage + "',");
				out.print("'" + fqCaller + "','");
				String prefix = "";
				for (final String fqCall : fqAPICalls.get(fqCaller)) {
					out.print(prefix + fqCall);
					prefix = " ";
				}
				out.println("'");
			}

		}

		out.close();
	}

}
