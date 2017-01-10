package pl.poznan.put.weights.xmcda;

import org.xmcda.CriteriaValues;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.XMCDA;

import pl.poznan.put.weights.exceptions.WrongParamValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {
	public enum DecimalPlacesParam {
		P0("0"), P1("1"), P2("2");

		private String label;

		private DecimalPlacesParam(String paramLabel) {
			label = paramLabel;
		}

		/**
		 * Return the label for this DecimalPlaces Parameter
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
		 * Returns the {@link DecimalPlacesParam} with the specified label. It
		 * behaves like {@link #valueOf(String)} with the exception
		 *
		 * @param parameterLabel
		 *            the label of the constant to return
		 * @return the enum constant with the specified label
		 * @throws IllegalArgumentException
		 *             if there is no DecimalPlacesParam with this label
		 * @throws NullPointerException
		 *             if parameterLabel is null
		 */
		public static DecimalPlacesParam fromString(String parameterLabel) {
			if (parameterLabel == null)
				throw new NullPointerException("parameterLabel is null");
			for (DecimalPlacesParam op : DecimalPlacesParam.values()) {
				if (op.toString().equals(parameterLabel))
					return op;
			}
			throw new IllegalArgumentException(
					"Enum DecimalPlacesParam with label " + parameterLabel + "was not found");
		}
	}

	public static class Inputs {
		public Integer decimalPlaces;
		public Double criteriaWeightRatio;
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
	 * @return
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkParameters(inputs, xmcda, errors);
		checkCriteriaValues(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		DecimalPlacesParam decimalPlaces = null;
		Double criteriaWeightRatio = null;

		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one list of parameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("List of parameters was not found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 2) {
			errors.addError("Exactly two parameters are expected");
			return;
		}

		final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(1);

		if (!"decimal_places".equals(prgParam.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam.id()));
			return;
		}

		if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
			errors.addError("Parameter \"decimal_places\" must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam.getValues().get(0).getValue();
			decimalPlaces = DecimalPlacesParam.fromString((String) parameterValue);
		} catch (Throwable throwable) {
			StringBuffer valid_values = new StringBuffer();
			for (DecimalPlacesParam op : DecimalPlacesParam.values()) {
				valid_values.append(op.getLabel()).append(", ");
			}
			String err = "Invalid value for parameter \"decimal_places\", it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			decimalPlaces = null;
		}
		if (decimalPlaces != null) {

			inputs.decimalPlaces = Integer.parseInt(decimalPlaces.getLabel());
		}

		final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(0);

		if (!"criteria_weight_ratio".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("Parameter \"criteria_weight_ratio\" must have a single float value only");
			return;
		}

		try {
			final Double parameterValue = (Double) prgParam2.getValues().get(0).getValue();
			if (parameterValue <= 0)
				throw new WrongParamValue();
			criteriaWeightRatio = parameterValue;
		} catch (Throwable throwable) {
			String err = "Invalid value for parameter \"criteria_weight_ratio\", it must be a float value greater than 0 ";
			errors.addError(err);
			criteriaWeightRatio = null;
		}
		if (criteriaWeightRatio != null) {
			inputs.criteriaWeightRatio = criteriaWeightRatio;
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
						"Criteria ranking position must be integers greater than 0");
				break;
			}
		}
	}

}
