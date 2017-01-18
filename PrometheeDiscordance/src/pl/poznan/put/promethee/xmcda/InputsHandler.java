package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.Criteria;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 
 */
public class InputsHandler {
	public enum ComparisonWithParam {
		ALTERNATIVES("alternatives"), BOUNDARY_PROFILES("boundary_profiles"), CENTRAL_PROFILES("central_profiles");

		private String label;

		private ComparisonWithParam(String paramLabel) {
			label = paramLabel;
		}

		/**
		 * Return the label for this ComparisonWith Parameter
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
		 * Returns the {@link ComparisonWithParam} with the specified label. It
		 * behaves like {@link #valueOf(String)} with the exception
		 *
		 * @param parameterLabel
		 *            the label of the constant to return
		 * @return the enum constant with the specified label
		 * @throws IllegalArgumentException
		 *             if there is no ComparisonWithParam with this label
		 * @throws NullPointerException
		 *             if parameterLabel is null
		 */
		public static ComparisonWithParam fromString(String parameterLabel) {
			if (parameterLabel == null)
				throw new NullPointerException("parameterLabel is null");
			for (ComparisonWithParam op : ComparisonWithParam.values()) {
				if (op.toString().equals(parameterLabel))
					return op;
			}
			throw new IllegalArgumentException("Enum ComparisonWithParam with label " + parameterLabel + "not found");
		}
	}

	public static class Inputs {
		public ComparisonWithParam comparisonWith;
		public List<String> alternatives_ids;
		public List<String> profiles_ids;
		public Integer technicalParam;
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
		checkParameters(inputs, xmcda, errors);
		checkAlternatives(inputs, xmcda, errors);
		checkProfiles(inputs, xmcda, errors);
		checkPartialPreferences(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		Integer technicalParam = null;
		ComparisonWithParam comparisonWith = null;
		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one list of parameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("List of parameters not found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 2) {
			errors.addError("Exactly two parameters are expected");
			return;
		}

		final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);

		if (!"comparison_with".equals(prgParam.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam.id()));
			return;
		}

		if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
			errors.addError("comparison_with patemater must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam.getValues().get(0).getValue();
			comparisonWith = ComparisonWithParam.fromString((String) parameterValue);
		} catch (Throwable throwable) {
			StringBuffer valid_values = new StringBuffer();
			for (ComparisonWithParam op : ComparisonWithParam.values()) {
				valid_values.append(op.getLabel()).append(", ");
			}
			String err = "Invalid value for parameter \"comparison_with\", it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			comparisonWith = null;
		}
		inputs.comparisonWith = comparisonWith;

		final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(1);

		if (!"technical_parameter".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("technical_parameter must have a single (Integer) value only");
			return;
		}

		try {
			technicalParam = (Integer) prgParam2.getValues().get(0).getValue();
		} catch (Throwable throwable) {
			String err = "Invalid value for technical_parameter, it must be an Integer value";
			errors.addError(err);
			technicalParam = null;
		}
		inputs.technicalParam = technicalParam;
	}

	private static void checkAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternatives.isEmpty()) {
			errors.addError("No alternatives list has been supplied.");
		}
	}

	private static void checkProfiles(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			if (!xmcda.categoriesProfilesList.isEmpty()) {
				errors.addError("Categories profiles list is not needed");
			}
		} else {
			if (xmcda.categoriesProfilesList.isEmpty()) {
				errors.addError("No categories profiles list has been supplied");
			}
			if (xmcda.categoriesProfilesList.size() > 1) {
				errors.addError("You can not supply more then 1 categories profiles list");
			}
		}
	}

	private static void checkPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
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
		extractProfiles(inputs, xmcda, xmcda_execution_results);
		extractCriteria(inputs, xmcda);
		if (!criteriaExists(inputs, xmcda, xmcda_execution_results)) {
			return null;
		}
		extractPartialPreferences(inputs, xmcda, xmcda_execution_results);
		return inputs;
	}

	private static void extractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream()
				.filter(a -> "alternatives.xml".equals(a.getMarker())).map(Alternative::id)
				.collect(Collectors.toList());
		if (alternativesIds.isEmpty())
			errors.addError("The alternatives list can not be empty.");
		inputs.alternatives_ids = alternativesIds;
	}

	private static void extractProfiles(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			List<String> profilesIds = xmcda.alternatives.getActiveAlternatives().stream()
					.filter(a -> "categories_profiles.xml".equals(a.getMarker())).map(Alternative::id)
					.collect(Collectors.toList());
			if (profilesIds.isEmpty())
				errors.addError("The alternatives list can not be empty.");
			inputs.profiles_ids = profilesIds;
		} else {
			inputs.profiles_ids = null;
		}
	}

	private static void extractCriteria(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = new ArrayList<>();
		Criteria criteria = (Criteria) xmcda.criteria;
		for (Criterion x_criterion : criteria)
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		inputs.criteria_ids = criteria_ids;
	}

	private static Boolean criteriaExists(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results) {
		Boolean allExists = true;
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

		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			mainExtractionLoop(inputs.alternatives_ids, inputs.profiles_ids, inputs, matrix, errors);
			mainExtractionLoop(inputs.profiles_ids, inputs.alternatives_ids, inputs, matrix, errors);
			mainExtractionLoop(inputs.profiles_ids, inputs.profiles_ids, inputs, matrix, errors);
		} else {
			mainExtractionLoop(inputs.alternatives_ids, inputs.alternatives_ids, inputs, matrix, errors);
		}
	}

	private static void mainExtractionLoop(List<String> list1, List<String> list2, Inputs inputs,
			AlternativesMatrix<Double> matrix, ProgramExecutionResult errors) {
		for (String a : list1) {
			for (String b : list2) {
				Alternative altA = new Alternative(a);
				Alternative altB = new Alternative(b);
				Coord<Alternative, Alternative> coord = new Coord<Alternative, Alternative>(altA, altB);
				if (matrix.containsKey(coord)) {
					if (!putPreferencesIntoMap(inputs, errors, matrix, a, b)) {
						return;
					}
				} else {
					errors.addError(
							"List of partial preferences does not contain preferences of pair :" + a + ", " + b);
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
			errors.addError("List of partial preferences does not contain value for pair of alternatives (" + a + ","
					+ b + ")");
			return false;
		}
		if (values.size() != inputs.criteria_ids.size()) {
			errors.addError("List of partial preferences does not contain correct criteria list");
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
				errors.addError("List of partial preferences contains unexpected criterion id " + value.id());
				return false;
			}
		}
		return true;
	}
}
