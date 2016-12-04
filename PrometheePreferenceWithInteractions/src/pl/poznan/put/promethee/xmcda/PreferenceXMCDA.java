/**
 *
 */
package pl.poznan.put.promethee.xmcda;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;

import pl.poznan.put.promethee.preference.Preference;
import pl.poznan.put.promethee.xmcda.InputFile;
import pl.poznan.put.promethee.xmcda.Utils.InvalidCommandLineException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
public class PreferenceXMCDA {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, InputFile> files = initFiles();

		final Utils.XMCDA_VERSION version = readVersion(args);
		final Utils.Arguments params = readParams(args);

		final String inputDirectory = params.inputDirectory;
		final String outputDirectory = params.outputDirectory;

		final File prgExecResultsFile = new File(outputDirectory, "messages.xml");

		final ProgramExecutionResult executionResult = new ProgramExecutionResult();

		final XMCDA xmcda = InputFileLoader.loadFiles(files, inputDirectory, executionResult, prgExecResultsFile,
				version);
		if (!ErrorChecker.checkErrors(executionResult, xmcda))
			exitProgram(executionResult, prgExecResultsFile, version);

		final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
		if (!ErrorChecker.checkErrors(executionResult, inputs))
			exitProgram(executionResult, prgExecResultsFile, version);

		Map<String, Map<String, Map<String, Double>>> partialResults = new LinkedHashMap<>();
		final Map<String, Map<String, Double>> results = calcResults(inputs, partialResults, executionResult);
		if (!ErrorChecker.checkErrors(executionResult, results))
			exitProgram(executionResult, prgExecResultsFile, version);
		if (!ErrorChecker.checkPartialResultsErrors(executionResult, partialResults))
			exitProgram(executionResult, prgExecResultsFile, version);

		final Map<String, XMCDA> xmcdaResults = OutputsHandler.convert(results, executionResult);

		OutputFileWriter.writeResultFiles(xmcdaResults, executionResult, outputDirectory, version);

		exitProgram(executionResult, prgExecResultsFile, version);
	}

	private static Utils.XMCDA_VERSION readVersion(String[] args) {
		Utils.XMCDA_VERSION version = Utils.XMCDA_VERSION.v2;
		;
		final ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		if (argsList.remove("--v2")) {
			version = Utils.XMCDA_VERSION.v2;
		} else if (argsList.remove("--v3")) {
			version = Utils.XMCDA_VERSION.v3;
		} else {
			System.err.println("Missing mandatory option --v2 or --v3");
			System.exit(-1);
		}
		return version;
	}

	private static Utils.Arguments readParams(String[] args) {
		Utils.Arguments params = null;
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		argsList.remove("--v2");
		argsList.remove("--v3");
		try {
			params = Utils.parseCmdLineArguments((String[]) argsList.toArray(new String[] {}));
		} catch (InvalidCommandLineException e) {
			System.err.println("Missing mandatory options. Required: [--v2|--v3] -i input_dir -o output_dir");
			System.exit(-1);
		}
		return params;
	}

	private static void exitProgram(ProgramExecutionResult executionResult, File prgExecResultsFile,
			Utils.XMCDA_VERSION version) {
		Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, version);
	}

	private static Map<String, InputFile> initFiles() {
		Map<String, InputFile> files = new LinkedHashMap<>();
		files.put("methodParameters",
				new InputFile("methodParameters", "programParameters", "method_parameters.xml", true));
		files.put("criteria", new InputFile("criteria", "criteria", "criteria.xml", true));
		files.put("criteriaScales", new InputFile("criteriaScales", "criteriaScales", "criteria.xml", true));
		files.put("performanceTable", new InputFile("performanceTable", "performanceTable", "performance_table.xml", true));
		files.put("criteriaWeights", new InputFile("criteriaValues", "criteriaValues", "weights.xml", true));
		files.put("generalisedCriteria", new InputFile("criteriaValues", "criteriaValues", "generalised_criteria.xml", false));
		files.put("profilesPerformanceTable",
				new InputFile("performanceTable", "performanceTable", "profiles_performance_table.xml", false));
		files.put("preferenceThresholds", new InputFile("criteriaThresholds", "criteriaThresholds", "criteria.xml", false));
		files.put("interactions", new InputFile("criteriaValues", "criteriaMatrix", "interactions.xml", true));
		return files;
	}

	private static Map<String, Map<String, Double>> calcResults(InputsHandler.Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialResult, ProgramExecutionResult executionResult) {
		Map<String, Map<String, Double>> results = null;
		try {
			results = Preference.calculatePreferences(inputs, partialResult);
		} catch (Throwable t) {
			executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
			return results;
		}
		return results;
	}
}
