package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.HashMap;
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
		public Map<String, Map<String, Double>> preferences;
		public Map<String, Map<String, Double>> discordances;
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
		checkPreferences(inputs, xmcda, errors);
		return inputs;
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

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		ComparisonWithParam comparisonWith = null;
		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one list of parameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("List of parameters not found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 1) {
			errors.addError("Exactly one parameter is expected");
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
	}

	private static void checkPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("List of preferences has not been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 2) {
			errors.addError("List of preferences and list of discordances are expected");
			return;
		}
		if (xmcda.alternativesMatricesList.get(0).isEmpty()) {
			errors.addError("Preferences table is empty");
		}
		if (xmcda.alternativesMatricesList.get(1).isEmpty()) {
			errors.addError("Discordances table is empty");
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
		extractPreferences(inputs, xmcda, xmcda_execution_results);
		checkExtractedPreferences(inputs, xmcda_execution_results);
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

	@SuppressWarnings("unchecked")
	private static void extractPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		AlternativesMatrix<Double> preferencesMatrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);
		AlternativesMatrix<Double> discordancesMatrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(1);
		
		inputs.preferences = new LinkedHashMap<String, Map<String, Double>>();
		inputs.discordances = new LinkedHashMap<String, Map<String, Double>>();

		importMatrix(inputs, preferencesMatrix, errors, true);
		importMatrix(inputs, discordancesMatrix, errors, false);

	}

	private static void importMatrix(Inputs inputs, AlternativesMatrix<Double> matrix, ProgramExecutionResult errors,
			Boolean preferences) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			mainExtractionLoop(inputs.alternatives_ids, inputs.profiles_ids, inputs, matrix, errors, preferences);
			mainExtractionLoop(inputs.profiles_ids, inputs.alternatives_ids, inputs, matrix, errors, preferences);
			mainExtractionLoop(inputs.profiles_ids, inputs.profiles_ids, inputs, matrix, errors, preferences);
		} else {
			mainExtractionLoop(inputs.alternatives_ids, inputs.alternatives_ids, inputs, matrix, errors, preferences);
		}
	}

	private static void mainExtractionLoop(List<String> list1, List<String> list2, Inputs inputs,
			AlternativesMatrix<Double> matrix, ProgramExecutionResult errors, Boolean preferences) {
		for (String a : list1) {
			for (String b : list2) {
				Alternative altA = new Alternative(a);
				Alternative altB = new Alternative(b);
				Coord<Alternative, Alternative> coord = new Coord<Alternative, Alternative>(altA, altB);
				if (matrix.containsKey(coord)) {
					Double value = matrix.get(coord).get(0).getValue().doubleValue();
					if (preferences) {
						inputs.preferences.putIfAbsent(a, new HashMap<>());
						inputs.preferences.get(a).put(b, value);
					} else {
						inputs.discordances.putIfAbsent(a, new HashMap<>());
						inputs.discordances.get(a).put(b, value);
					}
				} else {
					if (preferences) {
						errors.addError(
								"List of preferences does not contain preference index of pair :" + a + ", " + b);
					} else {
						errors.addError(
								"List of discordances does not contain discordance index of pair :" + a + ", " + b);
					}
					return;
				}
			}
		}
	}

	private static void checkExtractedPreferences(Inputs inputs, ProgramExecutionResult errors) {
		if (inputs.preferences.size() == 0) {
			errors.addError("Preferences table is empty");
			return;
		}
		if (inputs.discordances.size() == 0) {
			errors.addError("Discordances table is empty");
			return;
		}
	}

}
