package pl.poznan.put.promethee.xmcda;

import java.io.File;
import java.util.Map;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.parsers.xml.xmcda_v2.XMCDAParser;

public class OutputFileWriter {

	public static void writeResultFiles(Map<String, XMCDA> xmcdaResults, ProgramExecutionResult executionResult,
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
		org.xmcda.v2.XMCDA results_v2;
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
		final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();

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
