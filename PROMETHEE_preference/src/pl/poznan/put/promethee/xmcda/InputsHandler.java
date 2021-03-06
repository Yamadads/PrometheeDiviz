package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.Criteria;
import org.xmcda.CriteriaScales;
import org.xmcda.CriteriaThresholds;
import org.xmcda.CriteriaValues;
import org.xmcda.Criterion;
import org.xmcda.CriterionThresholds;
import org.xmcda.PerformanceTable;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.Threshold;
import org.xmcda.XMCDA;
import org.xmcda.QuantitativeScale;
import org.xmcda.utils.ValueConverters;

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

	public enum GeneralisedCriterionParam {
		SPECIFIED("specified"), F1("usual"), F2("u-shape"), F3("v-shape"), F4("level"), F5("v-shape-ind"), F6(
				"gaussian");

		private String label;

		private GeneralisedCriterionParam(String operatorLabel) {
			label = operatorLabel;
		}

		/**
		 * Return the label for this GeneralisedCriterion Parameter
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
		 * Returns the {@link GeneralisedCriterionParam} with the specified
		 * label. It behaves like {@link #valueOf(String)} with the exception
		 *
		 * @param parameterLabel
		 *            the label of the constant to return
		 * @return the enum constant with the specified label
		 * @throws IllegalArgumentException
		 *             if there is no GeneralisedCriterionParam with this label
		 * @throws NullPointerException
		 *             if parameterLabel is null
		 */
		public static GeneralisedCriterionParam fromString(String parameterLabel) {
			if (parameterLabel == null)
				throw new NullPointerException("operatorLabel is null");
			for (GeneralisedCriterionParam op : GeneralisedCriterionParam.values()) {
				if (op.toString().equals(parameterLabel))
					return op;
			}
			throw new IllegalArgumentException(
					"Enum GeneralisedCriterionParam with label " + parameterLabel + " not found");
		}
	}

	public static class Inputs {
		public ComparisonWithParam comparisonWith;
		public GeneralisedCriterionParam generalisedCriterion;
		public List<String> alternatives_ids;
		public Map<String, Map<String, Double>> performanceTable;
		public List<String> criteria_ids;
		public Map<String, Integer> generalisedCriteria;
		public Map<String, String> preferenceDirections;
		public List<String> profiles_ids;
		public Map<String, Map<String, Double>> profilesPerformanceTable;
		public Map<String, Double> weights;
		public Map<String, Threshold<Double>> preferenceThresholds;
		public Map<String, Threshold<Double>> indifferenceThresholds;
		public Map<String, Threshold<Double>> sigmaThresholds;

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
		checkPerformanceTables(inputs, xmcda, errors);
		checkCriteriaValues(inputs, xmcda, errors);
		checkCriteriaScales(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternatives.isEmpty()) {
			errors.addError("No alternatives list has been supplied.");
		}
	}
		
	private static void checkProfiles(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			if (!xmcda.categoriesProfilesList.isEmpty()){
				errors.addError("Categories profiles list is not needed");
			}
		}else{
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
		GeneralisedCriterionParam generalisedCriterion = null;
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

		if (!"generalised_criterion".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("Parameter \"generalised_criterion\" must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam2.getValues().get(0).getValue();
			generalisedCriterion = GeneralisedCriterionParam.fromString((String) parameterValue);
		} catch (Throwable throwable) {
			StringBuffer valid_values = new StringBuffer();
			for (GeneralisedCriterionParam op : GeneralisedCriterionParam.values()) {
				valid_values.append(op.getLabel()).append(", ");
			}
			String err = "Invalid value for parameter \"generalised_criterion\", it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			generalisedCriterion = null;
		}
		inputs.generalisedCriterion = generalisedCriterion;
	}

	private static void checkPerformanceTables(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.performanceTablesList.size() == 0) {
			errors.addError("Performance table has not been supplied");
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

	@SuppressWarnings("unchecked")
	private static void checkCriteriaValues(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteriaValuesList.size() == 0) {
			errors.addError("Criteria values has not been supplied");
			return;
		} else if ((inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED)
				&& (xmcda.criteriaValuesList.size() != 2)) {
			errors.addError("Criteria weights and Generalised Criteria are expected");
			return;
		}
		if ((inputs.generalisedCriterion != GeneralisedCriterionParam.SPECIFIED)
				&& (xmcda.criteriaValuesList.size() != 1)) {
			errors.addError(
					"More than one criteria values have been supplied. Only weights of criteria are expected. ");
			return;
		}
		@SuppressWarnings("rawtypes")
		CriteriaValues weights = xmcda.criteriaValuesList.get(0);
		if (!weights.isNumeric()) {
			errors.addError("The table of weights must contain numeric values only");
		} else {
			try {
				CriteriaValues<Double> weightsDouble = weights.asDouble();
				xmcda.criteriaValuesList.set(0, weightsDouble);
			} catch (ValueConverters.ConversionException e) {
				final String msg = "Error when converting the value of weight to Double, reason:";
				errors.addError(Utils.getMessage(msg, e));
				return;
			}
		}
		if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
			if (xmcda.criteriaValuesList.get(0) == null) {
				errors.addError("generalised_criteria.xml file not found");
				return;
			}
			CriteriaValues<Integer> generalisedCriteria = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(1);
			if (!generalisedCriteria.isNumeric()) {
				errors.addError("The table of generalised criteria must contain numeric values only");
			}
		}
	}

	private static void checkCriteriaScales(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteriaScalesList.size() == 0) {
			errors.addError("List of scales has not been supplied");
			return;
		}
		if (xmcda.criteriaScalesList.size() != 1) {
			errors.addError("Exactly one list of scales is expected");
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
		checkAlternativesPerformanceTable(inputs, xmcda, xmcda_execution_results);
		extractCriteria(inputs, xmcda);
		extractProfiles(inputs, xmcda, xmcda_execution_results);
		checkProfilesPerformanceTable(inputs, xmcda, xmcda_execution_results);
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
		extractWeights(inputs, xmcda);
		extractGeneralisedCriteria(inputs, xmcda, xmcda_execution_results);
		extractCriteriaDirection(inputs, xmcda);
		extractThresholds(inputs, xmcda);
		if (!thresholdsCompatibleWithGeneralisedCriteria(inputs, xmcda_execution_results)) {
			return null;
		}
		return inputs;
	}

	private static void extractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		List<String> alternativesIds = xmcda.alternatives.getActiveAlternatives().stream()
				.filter(a -> "alternatives.xml".equals(a.getMarker())).map(Alternative::id).collect(Collectors.toList());
		if (alternativesIds.isEmpty())
			errors.addError("The alternatives list can not be empty.");
		inputs.alternatives_ids = alternativesIds;
	}

	private static void checkAlternativesPerformanceTable(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		@SuppressWarnings("unchecked")
		PerformanceTable<Double> xmcda_perf_table = (PerformanceTable<Double>) xmcda.performanceTablesList.get(0);
		List<String> alternativesIds = new ArrayList<>();
		for (Alternative x_alternative : xmcda_perf_table.getAlternatives())
			if (x_alternative.isActive())
				alternativesIds.add(x_alternative.id());
		for (String alternative : inputs.alternatives_ids) {
			if (!alternativesIds.contains(alternative)) {
				errors.addError("The performance table does not contain alternative: " + alternative);
			}
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

	private static void checkProfilesPerformanceTable(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			@SuppressWarnings("unchecked")
			PerformanceTable<Double> x_perf_table_profiles = (PerformanceTable<Double>) xmcda.performanceTablesList
					.get(1);
			List<String> profilesIds = new ArrayList<>();
			for (Alternative x_alternative : x_perf_table_profiles.getAlternatives()) {
				if (x_alternative.isActive())
					profilesIds.add(x_alternative.id());
			}
			for (String profile : inputs.profiles_ids) {
				if (!profilesIds.contains(profile)) {
					errors.addError("The performance table does not contain profile: " + profile);
				}
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
			errors.addError("List of profiles is null");
			return false;
		}
		Boolean unique = true;
		for (String profile : inputs.profiles_ids) {
			if (inputs.alternatives_ids.contains(profile)) {
				errors.addError("Ids of profiles are not unique");
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
			xmcda_execution_results.addError("List of active alternatives is empty");
			allExists = false;
		}
		if (inputs.criteria_ids.size() == 0) {
			xmcda_execution_results.addError("List of active criteria is empty");
			allExists = false;
		}
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			if (inputs.profiles_ids.size() == 0) {
				xmcda_execution_results.addError("List of active profiles is empty");
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
			if (!xmcda.criteriaValuesList.get(0).getCriteria().contains(criterion)) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical, weights.xml doesn't contain criterion " + criterionID);
			}
			if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
				if (!xmcda.criteriaValuesList.get(1).getCriteria().contains(criterion)) {
					criteriaIdentical = false;
					xmcda_execution_results
							.addError("Criteria are not identical, generalised_criteria.xm doesn't contain criterion "
									+ criterionID);
				}
			}
			if (!criteriaIdentical)
				return criteriaIdentical;
		}
		return criteriaIdentical;
	}

	private static Boolean thresholdsCompatibleWithGeneralisedCriteria(Inputs inputs,
			ProgramExecutionResult xmcda_execution_results) {
		Boolean compatible = true;
		if (inputs.generalisedCriteria == null)
			return false;
		for (String criterion : inputs.generalisedCriteria.keySet()) {
			switch (inputs.generalisedCriteria.get(criterion)) {
			case 2:
				if ((inputs.indifferenceThresholds == null) || (inputs.indifferenceThresholds.get(criterion) == null)) {
					xmcda_execution_results
							.addError("U-Shape function (2) specified in generalised_criteria.xml on criterion "
									+ criterion + "requires indifference threshold");
					compatible = false;
				}
				break;
			case 3:
				if ((inputs.preferenceThresholds == null) || (inputs.preferenceThresholds.get(criterion) == null)) {
					xmcda_execution_results
							.addError("V-Shape function (3) specified in generalised_criteria.xml on criterion "
									+ criterion + "requires preference threshold");
					compatible = false;
				}
				break;
			case 4:
				if (((inputs.indifferenceThresholds == null) || (inputs.indifferenceThresholds.get(criterion) == null))
						|| ((inputs.preferenceThresholds == null)
								|| (inputs.preferenceThresholds.get(criterion) == null))) {
					xmcda_execution_results
							.addError("Level function (4) specified in generalised_criteria.xml on criterion "
									+ criterion + "requires indifference and preference thresholds");
					compatible = false;
				}
				break;
			case 5:
				if (((inputs.indifferenceThresholds == null) || (inputs.indifferenceThresholds.get(criterion) == null))
						|| ((inputs.preferenceThresholds == null)
								|| (inputs.preferenceThresholds.get(criterion) == null))) {
					xmcda_execution_results.addError(
							"V-Shape With Indifference function (5) specified in generalised_criteria.xml on criterion "
									+ criterion + "requires indifference and preference thresholds");
					compatible = false;
				}
				break;
			case 6:
				if ((inputs.sigmaThresholds == null) || (inputs.sigmaThresholds.get(criterion) == null)) {
					xmcda_execution_results
							.addError("Gaussian function (6) specified in generalised_criteria.xml on criterion "
									+ criterion + "requires sigma threshold");
					compatible = false;
				}
				break;
			}
		}
		return compatible;
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
				inputs.performanceTable.putIfAbsent(x_alternative.id(), new LinkedHashMap<>());
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
					inputs.profilesPerformanceTable.putIfAbsent(x_alternative.id(), new LinkedHashMap<>());
					inputs.profilesPerformanceTable.get(x_alternative.id()).put(x_criterion.id(), value);
				}
			}
		}
	}

	private static void extractWeights(Inputs inputs, XMCDA xmcda) {
		inputs.weights = new LinkedHashMap<>();
		@SuppressWarnings("unchecked")
		CriteriaValues<Double> weights_table = (CriteriaValues<Double>) xmcda.criteriaValuesList.get(0);
		for (Criterion criterion : weights_table.getCriteria()) {
			inputs.weights.put(criterion.id(), weights_table.get(criterion).get(0).getValue());
		}
	}

	private static void extractGeneralisedCriteria(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		inputs.generalisedCriteria = new LinkedHashMap<>();
		if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
			@SuppressWarnings("unchecked")
			CriteriaValues<Integer> generalisedCriteria = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(1);
			for (Criterion criterion : generalisedCriteria.getCriteria()) {
				Integer value = generalisedCriteria.get(criterion).get(0).getValue();
				if ((value >= 1) && (value <= 6)) {
					inputs.generalisedCriteria.put(criterion.id(), value);
				} else {
					xmcda_execution_results.addError("Generalised criteria must be integers between 1 and 6");
					break;
				}
			}
		} else {
			for (String criterion : inputs.criteria_ids) {
				inputs.generalisedCriteria.put(criterion, inputs.generalisedCriterion.ordinal());
			}
		}
	}

	private static void extractCriteriaDirection(Inputs inputs, XMCDA xmcda) {
		inputs.preferenceDirections = new LinkedHashMap<>();
		CriteriaScales criteriaDirection = (CriteriaScales) xmcda.criteriaScalesList.get(0);
		for (Criterion criterion : criteriaDirection.keySet()) {
			@SuppressWarnings("unchecked")
			QuantitativeScale<String> scale = (QuantitativeScale<String>) criteriaDirection.get(criterion).get(0);
			inputs.preferenceDirections.put(criterion.id(), scale.getPreferenceDirection().name());
		}
	}

	@SuppressWarnings("unchecked")
	private static void extractThresholds(Inputs inputs, XMCDA xmcda) {
		CriteriaThresholds thresholds = (CriteriaThresholds) xmcda.criteriaThresholdsList.get(0);

		inputs.indifferenceThresholds = new LinkedHashMap<>();
		inputs.sigmaThresholds = new LinkedHashMap<>();
		inputs.preferenceThresholds = new LinkedHashMap<>();

		for (Criterion criterion : thresholds.keySet()) {
			CriterionThresholds critThresholds = thresholds.get(criterion);

			if (critThresholds == null)
				continue;
			int prefThresholdID = -1;
			int indiffThresholdID = -1;
			int sigmaThresholdID = -1;

			for (int i = 0; i < critThresholds.size(); i++) {
				if (critThresholds.get(i).mcdaConcept().equals("indifference")) {
					indiffThresholdID = i;
				}
				if (critThresholds.get(i).mcdaConcept().equals("preference")) {
					prefThresholdID = i;
				}
				if (critThresholds.get(i).mcdaConcept().equals("sigma")) {
					sigmaThresholdID = i;
				}
			}
			if (prefThresholdID != -1) {
				inputs.preferenceThresholds.put(criterion.id(),
						(Threshold<Double>) critThresholds.get(prefThresholdID));
			} else {
				inputs.preferenceThresholds.put(criterion.id(), null);
			}
			if (indiffThresholdID != -1) {
				inputs.indifferenceThresholds.put(criterion.id(),
						(Threshold<Double>) critThresholds.get(indiffThresholdID));
			} else {
				inputs.indifferenceThresholds.put(criterion.id(), null);
			}
			if (sigmaThresholdID != -1) {
				inputs.sigmaThresholds.put(criterion.id(), (Threshold<Double>) critThresholds.get(sigmaThresholdID));
			} else {
				inputs.sigmaThresholds.put(criterion.id(), null);
			}
		}
	}
}
