package pl.poznan.put.promethee.xmcda;

import java.io.File;
import java.util.Map;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.converters.v2_v3.XMCDAConverter;
import org.xmcda.Referenceable;

public class InputFileLoader {

	public static XMCDA loadFiles(Map<String, InputFile> files, String inputDirectory,
			ProgramExecutionResult executionResult, File prgExecResultsFile, Utils.XMCDA_VERSION version) {
		XMCDA xmcda = null;
		if (version.equals(Utils.XMCDA_VERSION.v2)) {
			xmcda = loadFilesV2(executionResult, inputDirectory, files);
			if (!ErrorChecker.checkErrors(executionResult, xmcda))
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

	private static XMCDA loadFilesV2(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		XMCDA xmcda = new org.xmcda.XMCDA();	
		for (InputFile file : files.values()) {
			if ("".equals(file.filenameV2)) continue;
			org.xmcda.v2.XMCDA xmcda_v2 = new org.xmcda.v2.XMCDA();
			Referenceable.DefaultCreationObserver.currentMarker=file.filenameV2;
			Utils.loadXMCDAv2(xmcda_v2, new File(indir, file.filenameV2), file.mandatory, executionResult,
					file.loadTagV2);
			try {				
	            XMCDAConverter.convertTo_v3(xmcda_v2, xmcda);
	        } catch (Exception e) {
	            executionResult.addError(Utils.getMessage("Could not convert " + file.filenameV2 + " to XMCDA v3, reason: ", e));
	        }
		}
		return xmcda;
	}

	private static XMCDA loadFilesV3(ProgramExecutionResult executionResult, String indir,
			Map<String, InputFile> files) {
		XMCDA xmcda = new XMCDA();
		for (InputFile file : files.values()) {
			if ("".equals(file.filenameV3)) continue;
			Referenceable.DefaultCreationObserver.currentMarker=file.filenameV3;
			Utils.loadXMCDAv3(xmcda, new File(indir, file.filenameV3), file.mandatory, executionResult, file.loadTagV3);
		}
		return xmcda;
	}
}
