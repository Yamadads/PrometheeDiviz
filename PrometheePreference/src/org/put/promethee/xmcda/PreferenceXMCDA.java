/**
 *
 */
package org.put.promethee.xmcda;

import org.put.promethee.xmcda.InputFile;
import org.put.promethee.preference.Preference;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_2_1_v3_0.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_2_2_1.XMCDAParser;

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
	public static void main(String[] args) throws Utils.InvalidCommandLineException {
		Map<String, InputFile> files = initFiles();

		final Utils.XMCDA_VERSION version = readVersion(args);
		final Utils.Arguments params = readParams(args);

		final String inputDirectory = params.inputDirectory;
		final String outputDirectory = params.outputDirectory;

		final File prgExecResultsFile = new File(outputDirectory, "messages.xml");

		final ProgramExecutionResult executionResult = new ProgramExecutionResult();

		final XMCDA xmcda = loadFiles(files, inputDirectory, executionResult, prgExecResultsFile, version);

		final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);
		if (!checkErrors(executionResult, inputs))
			exitProgram(executionResult, prgExecResultsFile, version);
	
		final Map<String, Map<String, Double>> results = calcResults(inputs, executionResult);
		if (!checkErrors(executionResult, results))
			exitProgram(executionResult, prgExecResultsFile, version);

		final Map<String, XMCDA> xmcdaResults = OutputsHandler.convert(results, executionResult);

		writeResultFiles(xmcdaResults, executionResult, outputDirectory, version);

		exitProgram(executionResult, prgExecResultsFile, version);
	}

	private static XMCDA loadFiles(Map<String, InputFile> files, String inputDirectory,
			ProgramExecutionResult executionResult, File prgExecResultsFile, Utils.XMCDA_VERSION version) {
		XMCDA xmcda = null;
		if (version.equals(Utils.XMCDA_VERSION.v2)) {
			org.xmcda.v2_2_1.XMCDA xmcda_v2 = loadFilesV2(executionResult, inputDirectory, files);
			if (!checkErrors(executionResult))
				exitProgram(executionResult, prgExecResultsFile, version);
			xmcda = convertToXMCDA_v3(xmcda_v2, executionResult);
			if (!checkErrors(executionResult))
				exitProgram(executionResult, prgExecResultsFile, version);
		} else if (version == Utils.XMCDA_VERSION.v3) {
			xmcda = loadFilesV3(executionResult, inputDirectory, files);
			if (!checkErrors(executionResult))
				exitProgram(executionResult, prgExecResultsFile, version);
		} else {
			executionResult.addError("XMCDA_VERSION not specified");
			exitProgram(executionResult, prgExecResultsFile, version);
		}
		return xmcda;
	}

	private static Utils.Arguments readParams(String[] args) throws Utils.InvalidCommandLineException {
		Utils.Arguments params;
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		argsList.remove("--v2");
		argsList.remove("--v3");
		params = Utils.parseCmdLineArguments((String[]) argsList.toArray(new String[] {}));
		return params;
	}

	private static Utils.XMCDA_VERSION readVersion(String[] args) throws Utils.InvalidCommandLineException {
		Utils.XMCDA_VERSION version = Utils.XMCDA_VERSION.v2;
		;
		final ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		if (argsList.remove("--v2")) {
			version = Utils.XMCDA_VERSION.v2;
		} else if (argsList.remove("--v3")) {
			version = Utils.XMCDA_VERSION.v3;
		} else {
			System.err.println("missing mandatory option --v2 or --v3");
			System.exit(-1);
		}
		return version;
	}

	private static Map<String, InputFile> initFiles() {
		Map<String, InputFile> files = new LinkedHashMap<>();
		files.put("methodParameters", new InputFile("methodParameters", "method_parameters.xml", true));
		files.put("criteria", new InputFile("criteria", "criteria.xml", true));
		files.put("criteriaScales", new InputFile("criteriaScales", "criteria.xml", true));
		files.put("criteriaThresholds", new InputFile("criteriaThresholds", "criteria.xml", true));
		files.put("performanceTable", new InputFile("performanceTable", "performance_table.xml", true));
		files.put("criteriaWeights", new InputFile("criteriaValues", "weights.xml", true));
		files.put("generalisedCriteria", new InputFile("criteriaValues", "generalised_criteria.xml", false));
		files.put("profilesPerformanceTable",
				new InputFile("performanceTable", "profiles_performance_table.xml", false));
		files.put("preferenceThresholds", new InputFile("criteriaThresholds", "criteria.xml", false));
		return files;
	}

	private static org.xmcda.v2_2_1.XMCDA loadFilesV2(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		org.xmcda.v2_2_1.XMCDA xmcda_v2 = new org.xmcda.v2_2_1.XMCDA();
		for (InputFile file : files.values()) {
			Utils.loadXMCDAv2(xmcda_v2, new File(indir, file.filename), file.mandatory, executionResult, file.loadTag);
		}
		return xmcda_v2;
	}

	private static XMCDA loadFilesV3(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		XMCDA xmcda = new XMCDA();
		for (InputFile file : files.values()) {
			Utils.loadXMCDAv3(xmcda, new File(indir, file.filename), file.mandatory, executionResult, file.loadTag);
		}
		return xmcda;
	}

	private static void exitProgram(ProgramExecutionResult executionResult, File prgExecResultsFile,
			Utils.XMCDA_VERSION version) {
		Utils.writeProgramExecutionResultsAndExit(prgExecResultsFile, executionResult, version);
	}

	private static Boolean checkErrors(ProgramExecutionResult executionResult) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning())) {
			success = false;
		}
		return success;
	}

	private static Boolean checkErrors(ProgramExecutionResult executionResult, InputsHandler.Inputs inputs) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning()) || inputs == null) {
			success = false;
		}
		return success;
	}

	private static Boolean checkErrors(ProgramExecutionResult executionResult, Map<String, Map<String, Double>> results) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning()) || results == null) {
			success = false;
		}
		return success;
	}

	private static XMCDA convertToXMCDA_v3(org.xmcda.v2_2_1.XMCDA xmcda_v2, ProgramExecutionResult executionResult) {
		XMCDA xmcda = null;
		try {
			xmcda = XMCDAConverter.convertTo_v3(xmcda_v2);
		} catch (Throwable t) {
			executionResult.addError(Utils.getMessage("Could not convert inputs to XMCDA v3, reason: ", t));
		}
		return xmcda;
	}

	private static Map<String, Map<String, Double>> calcResults(InputsHandler.Inputs inputs,
			ProgramExecutionResult executionResult) {
		Map<String, Map<String, Double>> results = null;
		try {
			results = Preference.calculatePreferences(inputs);
		} catch (Throwable t) {
			executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
			return results;
		}
		return results;
	}

	private static void writeResultFiles(Map<String, XMCDA> xmcdaResults, ProgramExecutionResult executionResult,
			String outputDirectory, Utils.XMCDA_VERSION version) {
		if (version == Utils.XMCDA_VERSION.v2) {
			writeResultFilesV2(xmcdaResults, executionResult, outputDirectory);
		} else if (version == Utils.XMCDA_VERSION.v3) {
			writeResultFilesV3(xmcdaResults, executionResult, outputDirectory);
		} else {
			executionResult.addError("XMCDA_VERSION not specified");
		}
	}

	private static void writeResultFilesV2(Map<String, XMCDA> xmcdaResults, ProgramExecutionResult executionResult,
			String outputDirectory) {
		org.xmcda.v2_2_1.XMCDA results_v2;
		for (String outputName : xmcdaResults.keySet()) {
			File outputFile = new File(outputDirectory, String.format("%s.xml", outputName));
			try {
				results_v2 = XMCDAConverter.convertTo_v2(xmcdaResults.get(outputName));
				if (results_v2 == null)
					throw new IllegalStateException("Conversion from v3 to v2 returned a null value");
			} catch (Throwable t) {
				final String err = String.format("Could not convert %s into XMCDA_v2, reason: ", outputName);
				executionResult.addError(Utils.getMessage(err, t));
				continue;
			}
			try {
				XMCDAParser.writeXMCDA(results_v2, outputFile, OutputsHandler.xmcdaV2Tag(outputName));
			} catch (Throwable t) {
				final String err = String.format("Error while writing %s.xml, reason: ", outputName);
				executionResult.addError(Utils.getMessage(err, t));
				outputFile.delete();
			}
		}
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
				executionResult.addError(Utils.getMessage(err, throwable)); //
				outputFile.delete();
			}
		}
	}
}
