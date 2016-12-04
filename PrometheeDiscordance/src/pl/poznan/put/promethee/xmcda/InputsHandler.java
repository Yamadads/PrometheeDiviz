package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.Criteria;
import org.xmcda.CriteriaScales;
import org.xmcda.Criterion;
import org.xmcda.PerformanceTable;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.QuantitativeScale;
import org.xmcda.utils.Coord;
import org.xmcda.utils.ValueConverters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			throw new IllegalArgumentException("No enum ComparisonWithParam with label " + parameterLabel);
		}
	}

	public static class Inputs {
		public ComparisonWithParam comparisonWith;
		public Double technicalParam;
		public List<String> alternatives_ids;
		public Map<String, Map<String, Double>> performanceTable;
		public List<String> criteria_ids;
		public Map<String, String> preferenceDirections;
		public List<String> profiles_ids;
		public Map<String, Map<String, Double>> profilesPerformanceTable;
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
		checkParameters(inputs, xmcda, errors);
		checkPerformanceTables(inputs, xmcda, errors);
		checkCriteriaScales(inputs, xmcda, errors);
		checkPartialPreferences(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		ComparisonWithParam comparisonWith = null;
		Double technicalParam = null;
		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one programParameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("No programParameter found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 2) {
			errors.addError("Exactly two programParameters are expected");
			return;
		}

		final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);

		if (!"comparison_with".equals(prgParam.name())) {
			errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam.id()));
			return;
		}

		if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
			errors.addError("Parameter operator must have a single (label) value only");
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
			String err = "Invalid value for parameter operator, it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			comparisonWith = null;
		}
		inputs.comparisonWith = comparisonWith;

		final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(1);

		if (!"technical_parameter".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("Parameter operator must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam2.getValues().get(0).getValue();
			technicalParam = Double.parseDouble(parameterValue);
		} catch (Throwable throwable) {
			String err = "Invalid value for parameter operator, it must be a Double value";
			errors.addError(err);
			technicalParam = null;
		}
		inputs.technicalParam = technicalParam;
	}

	private static void checkPerformanceTables(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.performanceTablesList.size() == 0) {
			errors.addError("No performance table has been supplied");
			return;
		} else if (xmcda.performanceTablesList.size() > 2) {
			errors.addError("More than two performance tables have been supplied");
			return;
		} else if ((inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES)
				&& (xmcda.performanceTablesList.size() != 1)) {
			errors.addError("Only one performance table is expected");
			return;
		}
		if ((inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) && (xmcda.performanceTablesList.size() != 2)) {
			errors.addError("Exactly two performance tables are expected");
			return;
		} else {
			@SuppressWarnings("rawtypes")
			PerformanceTable p = xmcda.performanceTablesList.get(0);

			if (p.hasMissingValues())
				errors.addError("The performance table has missing values");
			if (!p.isNumeric()) {
				errors.addError("The performance table must contain numeric values only");
			} else {
				try {
					@SuppressWarnings("unchecked")
					PerformanceTable<Double> perfTable = p.asDouble();
					xmcda.performanceTablesList.set(0, perfTable);
				} catch (ValueConverters.ConversionException e) {
					final String msg = "Error when converting the performance table's value to Double, reason:";
					errors.addError(Utils.getMessage(msg, e));
					return;
				}
			}
			if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
				@SuppressWarnings("rawtypes")
				PerformanceTable p2 = xmcda.performanceTablesList.get(1);

				if (p2.hasMissingValues())
					errors.addError("The performance table has missing values");
				if (!p2.isNumeric()) {
					errors.addError("The performance table must contain numeric values only");
				} else {
					try {
						@SuppressWarnings("unchecked")
						PerformanceTable<Double> perfTable = p2.asDouble();
						xmcda.performanceTablesList.set(1, perfTable);
					} catch (ValueConverters.ConversionException e) {
						final String msg = "Error when converting the performance table's value to Double, reason:";
						errors.addError(Utils.getMessage(msg, e));
					}
				}
			}
		}
	}

	private static void checkCriteriaScales(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteriaScalesList.size() == 0) {
			errors.addError("No scales list has been supplied");
			return;
		}
		if (xmcda.criteriaScalesList.size() != 1) {
			errors.addError("Exactly one scales list is expected");
			return;
		}
	}

	private static void checkPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
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
		extractProfiles(inputs, xmcda);
		if (!profilesIDsUnique(inputs, xmcda_execution_results)) {
			return null;
		}
		if (!criteriaAlternativesAndProfilesExists(inputs, xmcda, xmcda_execution_results)) {
			return null;
		}
		if (!criteriaIdenticalInAllFiles(inputs, xmcda, xmcda_execution_results)) {
			return null;
		}
		extractPerformanceTables(inputs, xmcda);
		extractProfilesPerformanceTables(inputs, xmcda);
		extractCriteriaDirection(inputs, xmcda);
		extractPartialPreferences(inputs, xmcda, xmcda_execution_results);
		return inputs;
	}

	private static void extractAlternatives(Inputs inputs, XMCDA xmcda) {
		@SuppressWarnings("unchecked")
		PerformanceTable<Double> xmcda_perf_table = (PerformanceTable<Double>) xmcda.performanceTablesList.get(0);
		inputs.alternatives_ids = new ArrayList<>();
		for (Alternative x_alternative : xmcda_perf_table.getAlternatives())
			if (x_alternative.isActive())
				inputs.alternatives_ids.add(x_alternative.id());
	}

	private static void extractCriteria(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = new ArrayList<>();
		Criteria criteria = (Criteria) xmcda.criteria;
		for (Criterion x_criterion : criteria)
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		inputs.criteria_ids = criteria_ids;
	}

	private static void extractProfiles(Inputs inputs, XMCDA xmcda) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			@SuppressWarnings("unchecked")
			PerformanceTable<Double> x_perf_table_profiles = (PerformanceTable<Double>) xmcda.performanceTablesList
					.get(1);
			inputs.profiles_ids = new ArrayList<>();
			for (Alternative x_alternative : x_perf_table_profiles.getAlternatives()) {
				if (x_alternative.isActive())
					inputs.profiles_ids.add(x_alternative.id());
			}
		} else {
			inputs.profiles_ids = null;
		}
	}

	private static Boolean profilesIDsUnique(Inputs inputs, ProgramExecutionResult errors) {
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			return true;
		}
		if (inputs.profiles_ids == null) {
			errors.addError("Profile IDs is null");
			return false;
		}
		Boolean unique = true;
		for (String profile : inputs.profiles_ids) {
			if (inputs.alternatives_ids.contains(profile)) {
				errors.addError("Profile IDs are not unique");
				unique = false;
				break;
			}
		}
		return unique;
	}

	private static Boolean criteriaAlternativesAndProfilesExists(Inputs inputs, XMCDA xmcda,
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
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			if (inputs.profiles_ids.size() == 0) {
				xmcda_execution_results.addError("No active profiles in profiles_performance_table.xml");
				allExists = false;
			}
		}
		return allExists;
	}

	private static Boolean criteriaIdenticalInAllFiles(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		Boolean criteriaIdentical = true;
		for (String criterionID : inputs.criteria_ids) {
			Criterion criterion = new Criterion(criterionID);

			if (!xmcda.performanceTablesList.get(0).getCriteria().contains(criterion)) {
				criteriaIdentical = false;
				xmcda_execution_results.addError(
						"Criteria are not identical, performance_table.xml doesn't contain criterion " + criterionID);
			}
			if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
				if (!xmcda.performanceTablesList.get(1).getCriteria().contains(criterion)) {
					criteriaIdentical = false;
					xmcda_execution_results.addError(
							"Criteria are not identical, profiles_performance_table.xml doesn't contain criterion "
									+ criterionID);
				}
			}
			if (!criteriaIdentical)
				return criteriaIdentical;
		}
		return criteriaIdentical;
	}

	private static void extractPerformanceTables(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = inputs.criteria_ids;
		inputs.performanceTable = new LinkedHashMap<>();
		@SuppressWarnings("unchecked")
		PerformanceTable<Double> x_perf_table = (PerformanceTable<Double>) xmcda.performanceTablesList.get(0);
		for (Alternative x_alternative : x_perf_table.getAlternatives()) {
			if (!inputs.alternatives_ids.contains(x_alternative.id()))
				continue;
			for (Criterion x_criterion : x_perf_table.getCriteria()) {
				if (!criteria_ids.contains(x_criterion.id()))
					continue;
				Double value = (Double) x_perf_table.getValue(x_alternative, x_criterion);
				inputs.performanceTable.putIfAbsent(x_alternative.id(), new HashMap<>());
				inputs.performanceTable.get(x_alternative.id()).put(x_criterion.id(), value);
			}
		}
	}

	private static void extractProfilesPerformanceTables(Inputs inputs, XMCDA xmcda) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			List<String> criteria_ids = inputs.criteria_ids;
			@SuppressWarnings("unchecked")
			PerformanceTable<Double> x_perf_table_profiles = (PerformanceTable<Double>) xmcda.performanceTablesList
					.get(1);
			inputs.profilesPerformanceTable = new LinkedHashMap<>();
			for (Alternative x_alternative : x_perf_table_profiles.getAlternatives()) {
				if (!inputs.profiles_ids.contains(x_alternative.id()))
					continue;
				for (Criterion x_criterion : x_perf_table_profiles.getCriteria()) {
					if (!criteria_ids.contains(x_criterion.id()))
						continue;
					Double value = x_perf_table_profiles.getValue(x_alternative, x_criterion);
					inputs.profilesPerformanceTable.putIfAbsent(x_alternative.id(), new HashMap<>());
					inputs.profilesPerformanceTable.get(x_alternative.id()).put(x_criterion.id(), value);
				}
			}
		}
	}

	private static void extractCriteriaDirection(Inputs inputs, XMCDA xmcda) {
		inputs.preferenceDirections = new HashMap<>();
		CriteriaScales criteriaDirection = (CriteriaScales) xmcda.criteriaScalesList.get(0);
		for (Criterion criterion : criteriaDirection.keySet()) {
			@SuppressWarnings("unchecked")
			QuantitativeScale<String> scale = (QuantitativeScale<String>) criteriaDirection.get(criterion).get(0);
			inputs.preferenceDirections.put(criterion.id(), scale.getPreferenceDirection().name());
		}
	}

	private static void extractPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		inputs.partialPreferences = new LinkedHashMap<>();
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					if (!putPreferencesIntoMap(inputs, errors, matrix, a, b)) {
						return;
					}
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					if (!putPreferencesIntoMap(inputs, errors, matrix, a, b)) {
						return;
					}
					if (!putPreferencesIntoMap(inputs, errors, matrix, b, a)) {
						return;
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					if (!putPreferencesIntoMap(inputs, errors, matrix, a, b)) {
						return;
					}
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
