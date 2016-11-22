/**
 *
 */
package org.put.promethee.xmcda;

import org.put.promethee.preferences.Aggregator;
import org.put.promethee.xmcda.InputFile;
import org.put.promethee.xmcda.Utils.InvalidCommandLineException;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
public class PreferencesAggregatorXMCDA {
	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, InputFile> files = initFiles();

		final Utils.Arguments params = readParams(args);

		final String inputDirectory = params.inputDirectory;
		final String outputDirectory = params.outputDirectory;

		final File prgExecResultsFile = new File(outputDirectory, "messages.xml");

		final ProgramExecutionResult executionResult = new ProgramExecutionResult();

		final XMCDA xmcda = loadFiles(files, inputDirectory, executionResult);
		if (!ErrorChecker.checkErrors(executionResult, xmcda))
			exitProgram(executionResult, prgExecResultsFile);

		InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
		if (!ErrorChecker.checkErrors(executionResult, inputs))
			exitProgram(executionResult, prgExecResultsFile);

		final Map<String, Map<String, Double>> results = calcResults(inputs, executionResult);
		if (!ErrorChecker.checkErrors(executionResult, results))
			exitProgram(executionResult, prgExecResultsFile);

		final Map<String, XMCDA> xmcdaResults = OutputsHandler.convert(results, executionResult);

		writeResultFilesV3(xmcdaResults, executionResult, outputDirectory);

		exitProgram(executionResult, prgExecResultsFile);
	}

	private static Utils.Arguments readParams(String[] args) {
		Utils.Arguments params = null;
		try {
			params = Utils.parseCmdLineArguments(args);
		} catch (InvalidCommandLineException e) {
			System.err.println("Missing mandatory options. Required: -i input_dir -o output_dir");
			System.exit(-1);
		}
		return params;
	}

	private static Map<String, InputFile> initFiles() {
		Map<String, InputFile> files = new LinkedHashMap<>();
		files.put("preferences", new InputFile("alternativesMatrix", "preferences.xml", true));
		files.put("discordances", new InputFile("alternativesMatrix", "discordances.xml", true));
		return files;
	}

	private static void exitProgram(ProgramExecutionResult executionResult, File prgExecResultsFile) {
		Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, Utils.XMCDA_VERSION.v3);
	}

	private static Map<String, Map<String, Double>> calcResults(InputsHandler.Inputs inputs,
			ProgramExecutionResult executionResult) {
		Map<String, Map<String, Double>> results = null;
		try {
			results = Aggregator.aggregate(inputs);
		} catch (Throwable t) {
			executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
			return results;
		}
		return results;
	}

	private static XMCDA loadFiles(Map<String, InputFile> files, String indir, ProgramExecutionResult executionResult) {
		XMCDA xmcda = new XMCDA();
		for (InputFile file : files.values()) {
			Utils.loadXMCDAv3(xmcda, new File(indir, file.filename), file.mandatory, executionResult, file.loadTagV3);
		}
		return xmcda;
	}

	private static void writeResultFilesV3(Map<String, XMCDA> xmcdaResults, ProgramExecutionResult executionResult,
			String outputDirectory) {
		final org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_3_0.XMCDAParser();

		for (String key : xmcdaResults.keySet()) {
			File outputFile = new File(outputDirectory, String.format("%s.xml", key));
			try {
				parser.writeXMCDA(xmcdaResults.get(key), outputFile, OutputsHandler.xmcdaV3Tag(key));
			} catch (Throwable throwable) {
				final String err = String.format("Error while writing %s.xml, reason: ", key);
				executionResult.addError(Utils.getMessage(err, throwable));
				outputFile.delete();
			}
		}
	}
}