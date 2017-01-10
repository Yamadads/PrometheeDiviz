package pl.poznan.put.weights.xmcda;

import org.xmcda.CriteriaValues;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.XMCDA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {
	public enum MethodNameParam {
		EQUAL_WEIGHTS("equal_weights"), RANK_SUM("rank_sum"), RANK_RECIPROCAL("rank_reciprocal"), RANK_ORDERED_CENTROID(
				"rank_ordered_centroid");

		private String label;

		private MethodNameParam(String paramLabel) {
			label = paramLabel;
		}

		/**
		 * Return the label for this Parameter
		 *
		 * @return the parameter's label
		 */
		public final String getLabel() {
			return label;
		}

		/**
		 * Returns the parameter's label
		 *
		 * @return the parameter's label
		 */
		@Override
		public String toString() {
			return label;
		}

		/**
		 * Returns the {@link MethodNameParam} with the specified label. It
		 * behaves like {@link #valueOf(String)} with the exception
		 *
		 * @param parameterLabel
		 *            the label of the constant to return
		 * @return the enum constant with the specified label
		 * @throws IllegalArgumentException
		 *             if there is no MethodNameParam with this label
		 * @throws NullPointerException
		 *             if parameterLabel is null
		 */
		public static MethodNameParam fromString(String parameterLabel) {
			if (parameterLabel == null)
				throw new NullPointerException("parameterLabel is null");
			for (MethodNameParam op : MethodNameParam.values()) {
				if (op.toString().equals(parameterLabel))
					return op;
			}
			throw new IllegalArgumentException("Enum MethodNameParam with label " + parameterLabel + "was not found");
		}
	}

	public static class Inputs {
		public MethodNameParam methodNameParam;
		public List<String> criteria_ids;
		public Map<String, Integer> criteriaRanking;
	}

	/**
	 *
	 * @param xmcda
	 * @param xmcda_exec_results
	 * @return
	 */
	static public Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcda_exec_results) {
		Inputs inputsDict = checkInputs(xmcda, xmcda_exec_results);

		if (xmcda_exec_results.isError())
			return null;

		return extractInputs(inputsDict, xmcda, xmcda_exec_results);
	}

	/**
	 * @param xmcda
	 * @param errors
	 * @return Inputs
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkParameters(inputs, xmcda, errors);
		checkCriteriaValues(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		MethodNameParam methodNameParam = null;

		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one list of parameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("List of parameters was not found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 1) {
			errors.addError("Exactly one parameter is expected");
			return;
		}

		final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);

		if (!"method".equals(prgParam.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam.id()));
			return;
		}

		if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
			errors.addError("Parameter \"method\" must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam.getValues().get(0).getValue();
			methodNameParam = MethodNameParam.fromString((String) parameterValue);
		} catch (Throwable throwable) {
			StringBuffer valid_values = new StringBuffer();
			for (MethodNameParam op : MethodNameParam.values()) {
				valid_values.append(op.getLabel()).append(", ");
			}
			String err = "Invalid value for parameter \"method\", it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			methodNameParam = null;
		}
		if (methodNameParam != null) {

			inputs.methodNameParam = methodNameParam;
		}
	}

	private static void checkCriteriaValues(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteriaValuesList.size() == 0) {
			errors.addError("Criteria values has not been supplied");
			return;
		} else if (xmcda.criteriaValuesList.size() != 1) {
			errors.addError("Criteria ranking is expected");
			return;
		}
		@SuppressWarnings("unchecked")
		CriteriaValues<Integer> generalisedCriteria = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(0);
		if (!generalisedCriteria.isNumeric()) {
			errors.addError("The criteria ranking must contain numeric values only");
		}
	}

	/**
	 *
	 * @param inputs
	 * @param xmcda
	 * @param xmcda_execution_results
	 * @return
	 */
	protected static Inputs extractInputs(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results) {
		extractCriteria(inputs, xmcda);
		extractCriteriaRanking(inputs, xmcda, xmcda_execution_results);
		checkRanking(inputs, xmcda_execution_results);
		return inputs;
	}

	private static void extractCriteria(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = new ArrayList<>();
		for (Criterion x_criterion : xmcda.criteriaValuesList.get(0).getCriteria())
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		inputs.criteria_ids = criteria_ids;
	}

	private static void extractCriteriaRanking(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		inputs.criteriaRanking = new HashMap<>();
		@SuppressWarnings("unchecked")
		CriteriaValues<Integer> criteriaRanking = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(0);
		for (Criterion criterion : criteriaRanking.getCriteria()) {
			Integer value = criteriaRanking.get(criterion).get(0).getValue();
			if (value >= 1) {
				inputs.criteriaRanking.put(criterion.id(), value);
			} else {
				xmcda_execution_results.addError(
						"Criteria ranking positions must be integers greater than 0");
				break;
			}
		}
	}

	private static void checkRanking(Inputs inputs, ProgramExecutionResult xmcda_execution_results) {
		for (int i = 1; i <= inputs.criteriaRanking.size(); i++) {
			if (!inputs.criteriaRanking.values().contains(i)) {
				xmcda_execution_results.addError(
						"Ranking is incorrect. It should start from 1 and have one criterion on each next position ");
				return;
			}
		}
	}

}
