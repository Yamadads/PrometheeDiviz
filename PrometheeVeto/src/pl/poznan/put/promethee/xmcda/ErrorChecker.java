package pl.poznan.put.promethee.xmcda;

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
		if (xmcda == null) {
			executionResult.addError("xmcda is null, loading or converting files was wrong");
		}
		success = checkErrors(executionResult);
		return success;
	}

	public static Boolean checkErrors(ProgramExecutionResult executionResult, InputsHandler.Inputs inputs) {
		Boolean success = true;
		if (inputs == null) {
			executionResult.addError("inputs is null");
		}
		success = checkErrors(executionResult);
		return success;
	}

	public static Boolean checkPartialResultsErrors(ProgramExecutionResult executionResult,
			Map<String, Map<String, Map<String, Double>>> results) {
		Boolean success = true;
		if (results == null) {
			executionResult.addError("results is null");
		}
		success = checkErrors(executionResult);
		return success;
	}

	public static Boolean checkResultsErrors(ProgramExecutionResult executionResult,
			Map<String, Map<String, Double>> results) {
		Boolean success = true;
		if (results == null) {
			executionResult.addError("results is null");
		}
		success = checkErrors(executionResult);
		return success;
	}
}
