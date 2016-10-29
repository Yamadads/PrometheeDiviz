package org.put.promethee.xmcda;

import java.io.File;
import java.util.Map;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_2_1_v3_0.XMCDAConverter;

public class InputFileLoader {

	public static XMCDA loadFiles(Map<String, InputFile> files, String inputDirectory,
			ProgramExecutionResult executionResult, File prgExecResultsFile, Utils.XMCDA_VERSION version) {
		XMCDA xmcda = null;
		if (version.equals(Utils.XMCDA_VERSION.v2)) {
			org.xmcda.v2_2_1.XMCDA xmcda_v2 = loadFilesV2(executionResult, inputDirectory, files);
			if (!ErrorChecker.checkErrors(executionResult))
				return null;
			xmcda = convertToXMCDA_v3(xmcda_v2, executionResult);
			if (!ErrorChecker.checkErrors(executionResult))
				return null;
		} else if (version == Utils.XMCDA_VERSION.v3) {
			xmcda = loadFilesV3(executionResult, inputDirectory, files);
			if (!ErrorChecker.checkErrors(executionResult))
				return null;
		} else {
			executionResult.addError("XMCDA_VERSION not specified");
			return null;
		}
		return xmcda;
	}
	
	private static org.xmcda.v2_2_1.XMCDA loadFilesV2(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		org.xmcda.v2_2_1.XMCDA xmcda_v2 = new org.xmcda.v2_2_1.XMCDA();
		for (InputFile file : files.values()) {
			Utils.loadXMCDAv2(xmcda_v2, new File(indir, file.filename), file.mandatory, executionResult,
					file.loadTagV2);
		}
		return xmcda_v2;
	}

	private static XMCDA loadFilesV3(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		XMCDA xmcda = new XMCDA();
		for (InputFile file : files.values()) {
			Utils.loadXMCDAv3(xmcda, new File(indir, file.filename), file.mandatory, executionResult, file.loadTagV3);
		}
		return xmcda;
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
}
