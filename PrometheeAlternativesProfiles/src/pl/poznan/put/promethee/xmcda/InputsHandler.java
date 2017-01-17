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
import java.util.stream.Collectors;

/**
 * 
 */
public class InputsHandler {

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
	 * @param xmcda
	 * @param errors
	 * @return Inputs
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkCriteria(inputs, xmcda, errors);
		checkAlternatives(inputs, xmcda, errors);
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
	
	private static void checkAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternatives.isEmpty()) {
			errors.addError("No alternatives list has been supplied.");
		}
	}

	private static void checkPartialPreferences(XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("Partial preferences has not been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 1) {
			errors.addError("Exactly one list of partial preferences is expected");
			return;
		}
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		if (matrix.isEmpty()) {
			errors.addError("List of partial preferences is empty");
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
		extractAlternatives(inputs, xmcda, xmcda_execution_results);		
		extractCriteria(inputs, xmcda, xmcda_execution_results);
		extractPartialPreferences(inputs, xmcda, xmcda_execution_results);
		return inputs;
	}
	
	private static void extractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream()
				.filter(a -> "alternatives.xml".equals(a.getMarker())).map(Alternative::id).collect(Collectors.toList());
		if (alternativesIds.isEmpty())
			errors.addError("The alternatives list can not be empty.");
		inputs.alternatives_ids = alternativesIds;
	}

	private static void extractCriteria(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results) {
		List<String> criteria_ids = new ArrayList<>();
		Criteria criteria = (Criteria) xmcda.criteria;
		for (Criterion x_criterion : criteria)
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		Collections.sort(criteria_ids);
		inputs.criteria_ids = criteria_ids;

		if (inputs.criteria_ids.size() == 0) {
			xmcda_execution_results.addError("No active criteria in criteria.xml");			
		}
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
			errors.addError("List of partial preferences does not contain value for coord (" + a + "," + b + ")");
			return false;
		}
		if (values.size() != inputs.criteria_ids.size()) {
			errors.addError("List of partial preferences does not contain correct criteria list");
			return false;
		}
		for (QualifiedValue<Double> value : values) {
			if (inputs.criteria_ids.contains(value.id())) {
				if (inputs.partialPreferences.get(a).get(b).containsKey(value.id())) {
					errors.addError("List of partial preferences contains duplicates of criteria");
					return false;
				} else {
					inputs.partialPreferences.get(a).get(b).put(value.id(), value.getValue().doubleValue());
				}

			} else {
				errors.addError("List of partial preferences contains unexpected criterion id " + value.id());
				return false;
			}
		}
		return true;
	}
}
