package org.put.promethee.xmcda;

import java.util.Map;

import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;

public class ErrorChecker {

	public static Boolean checkErrors(ProgramExecutionResult executionResult) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning())) {
			success = false;
		}
		return success;
	}

	public static Boolean checkErrors(ProgramExecutionResult executionResult, XMCDA xmcda) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning() || xmcda == null)) {
			success = false;
		}
		return success;
	}

	public static Boolean checkErrors(ProgramExecutionResult executionResult, InputsHandler.Inputs inputs) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning()) || inputs == null) {
			success = false;
		}
		return success;
	}

	public static Boolean checkErrors(ProgramExecutionResult executionResult,
			Map<String, Map<String, Double>> results) {
		Boolean success = true;
		if (!(executionResult.isOk() || executionResult.isWarning()) || results == null) {
			success = false;
		}
		return success;
	}
}
