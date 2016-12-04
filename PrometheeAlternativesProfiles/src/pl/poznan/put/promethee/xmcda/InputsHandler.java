package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.Criteria;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {

	/**
	 * This class contains every element which are needed to compute the
	 * weighted sum. It is populated by
	 * {@link InputsHandler#checkAndExtractInputs(XMCDA, ProgramExecutionResult)}.
	 */
	public static class Inputs {
		public List<String> alternatives_ids;
		public List<String> criteria_ids;
		public Map<String, Map<String, Map<String, Double>>> partialPreferences;
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
	 * Checks the inputs
	 *
	 * @param xmcda
	 * @param errors
	 * @return a map containing a key "operator" with the appropriate
	 *         {@link AggregationOperator operator}
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkCriteria(inputs, xmcda, errors);
		checkPartialPreferences(xmcda, errors);
		return inputs;
	}

	private static void checkCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteria.isEmpty()) {
			errors.addError("Criteria set is empty");
		}
		if (xmcda.criteria.getActiveCriteria().isEmpty()) {
			errors.addError("No active criteria");
		}
	}

	private static void checkPartialPreferences(XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("No partial preferences has been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 1) {
			errors.addError("Exactly one partial preferences list is expected");
			return;
		}
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		if (matrix.isEmpty()) {
			errors.addError("Partial preferences list is empty");
			return;
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
		extractAlternatives(inputs, xmcda);
		extractCriteria(inputs, xmcda);
		if (!criteriaAndAlternatives(inputs, xmcda, xmcda_execution_results)) {
			return null;
		}
		extractPartialPreferences(inputs, xmcda, xmcda_execution_results);
		return inputs;
	}

	private static void extractAlternatives(Inputs inputs, XMCDA xmcda) {
		inputs.alternatives_ids = new ArrayList<>();
		for (Alternative x_alternative : xmcda.alternatives)
			if (x_alternative.isActive())
				inputs.alternatives_ids.add(x_alternative.id());
		Collections.sort(inputs.alternatives_ids);
	}

	private static void extractCriteria(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = new ArrayList<>();
		Criteria criteria = (Criteria) xmcda.criteria;
		for (Criterion x_criterion : criteria)
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		Collections.sort(criteria_ids);
		inputs.criteria_ids = criteria_ids;

	}

	private static Boolean criteriaAndAlternatives(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		Boolean allExists = true;
		if (inputs.alternatives_ids.size() == 0) {
			xmcda_execution_results.addError("No active alternatives in performance_table.xml");
			allExists = false;
		}
		if (inputs.criteria_ids.size() == 0) {
			xmcda_execution_results.addError("No active criteria in criteria.xml");
			allExists = false;
		}
		return allExists;
	}

	private static void extractPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		inputs.partialPreferences = new LinkedHashMap<>();
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		for (String a : inputs.alternatives_ids) {
			for (String b : inputs.alternatives_ids) {
				if (!putPreferencesIntoMap(inputs, errors, matrix, a, b)) {
					return;
				}
			}
		}
	}

	private static boolean putPreferencesIntoMap(Inputs inputs, ProgramExecutionResult errors,
			AlternativesMatrix<Double> matrix, String a, String b) {
		inputs.partialPreferences.putIfAbsent(a, new LinkedHashMap<>());
		inputs.partialPreferences.get(a).putIfAbsent(b, new LinkedHashMap<>());
		Alternative alt1 = new Alternative(a);
		Alternative alt2 = new Alternative(b);
		Coord<Alternative, Alternative> coord = new Coord<Alternative, Alternative>(alt1, alt2);
		QualifiedValues<Double> values = matrix.getOrDefault(coord, null);
		if (values == null) {
			errors.addError("Partial Preferences list does not contain value for coord (" + a + "," + b + ")");
			return false;
		}
		if (values.size() != inputs.criteria_ids.size()) {
			errors.addError("Partial Preferences list does not contain correct criteria list");
			return false;
		}
		for (QualifiedValue<Double> value : values) {
			if (inputs.criteria_ids.contains(value.id())) {
				if (inputs.partialPreferences.get(a).get(b).containsKey(value.id())) {
					errors.addError("Partial Preferences list contains duplicates of criteria");
					return false;
				} else {
					inputs.partialPreferences.get(a).get(b).put(value.id(), value.getValue().doubleValue());
				}

			} else {
				errors.addError("Partial Preferences list contains unexpected criterion id " + value.id());
				return false;
			}
		}
		return true;
	}
}
