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

	public enum GeneralisedCriterionParam {
		SPECIFIED("specified"), F1("1"), F2("2"), F3("3"), F4("4"), F5("5");

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
			throw new IllegalArgumentException("No enum GeneralisedCriterionParam with label " + parameterLabel);
		}
	}

	/**
	 * This class contains every element which are needed to compute the
	 * weighted sum. It is populated by
	 * {@link InputsHandler#checkAndExtractInputs(XMCDA, ProgramExecutionResult)}.
	 */
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
		public Map<String, Double> reinforcementFactors;
		public Map<String, Threshold<Double>> preferenceThresholds;
		public Map<String, Threshold<Double>> indifferenceThresholds;
		public Map<String, Threshold<Double>> reinforcedPreferenceThresholds;

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
		checkCriteriaValues(inputs, xmcda, errors);
		checkCriteriaScales(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		ComparisonWithParam comparisonWith = null;
		GeneralisedCriterionParam generalisedCriterion = null;
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

		if (!"generalised_criterion".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("Parameter operator must have a single (label) value only");
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
			String err = "Invalid value for parameter operator, it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			generalisedCriterion = null;
		}
		inputs.generalisedCriterion = generalisedCriterion;
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

	private static void checkCriteriaValues(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.criteriaValuesList.size() == 0) {
			errors.addError("No criteria values has been supplied");
			return;
		} else if ((inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED)
				&& (xmcda.criteriaValuesList.size() != 3)) {
			errors.addError(
					"Improper input data. Criteria weights, Generalised Criteria, and Reinforcement Factors are expected");
			return;
		}
		if ((inputs.generalisedCriterion != GeneralisedCriterionParam.SPECIFIED)
				&& (xmcda.criteriaValuesList.size() != 2)) {
			errors.addError("Improper input data. Criteria weights and Reinforcement Factors are expected");
			return;
		}
		@SuppressWarnings("rawtypes")
		CriteriaValues weights = xmcda.criteriaValuesList.get(0);
		if (!weights.isNumeric()) {
			errors.addError("The weights table must contain numeric values only");
		} else {
			try {
				@SuppressWarnings("unchecked")
				CriteriaValues<Double> weightsDouble = weights.asDouble();
				xmcda.criteriaValuesList.set(0, weightsDouble);
			} catch (ValueConverters.ConversionException e) {
				final String msg = "Error when converting the weights table's value to Double, reason:";
				errors.addError(Utils.getMessage(msg, e));
				return;
			}
		}
		@SuppressWarnings("rawtypes")
		CriteriaValues reinforcementFactors = xmcda.criteriaValuesList.get(1);
		if (!reinforcementFactors.isNumeric()) {
			errors.addError("The Reinforcement Factor table must contain numeric values only");
		} else {
			try {
				@SuppressWarnings("unchecked")
				CriteriaValues<Double> reinforcementFactorsDouble = reinforcementFactors.asDouble();
				xmcda.criteriaValuesList.set(1, reinforcementFactorsDouble);
			} catch (ValueConverters.ConversionException e) {
				final String msg = "Error when converting the Reinforcement Factor table's value to Double, reason:";
				errors.addError(Utils.getMessage(msg, e));
				return;
			}
		}
		if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
			@SuppressWarnings("unchecked")
			CriteriaValues<Integer> generalisedCriteria = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(2);
			if (!generalisedCriteria.isNumeric()) {
				errors.addError("The generalised criteria table must contain numeric values only");
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
		extractPreferenceTables(inputs, xmcda);
		extractProfilesPreferenceTables(inputs, xmcda);
		extractWeights(inputs, xmcda);
		extractGeneralisedCriteria(inputs, xmcda, xmcda_execution_results);
		extractCriteriaDirection(inputs, xmcda);
		extractReinforcementFactors(inputs, xmcda);
		extractThresholds(inputs, xmcda, xmcda_execution_results);
		if (!thresholdsCompatibleWithGeneralisedCriteria(inputs, xmcda_execution_results)) {
			return null;
		}
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
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and performance_table.xml file");
			}
			if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
				if (!xmcda.performanceTablesList.get(1).getCriteria().contains(criterion)) {
					criteriaIdentical = false;
					xmcda_execution_results.addError(
							"Criteria are not identical in criteria.xml file and profiles_performance_table.xml file");
				}
			}
			if (!xmcda.criteriaValuesList.get(0).getCriteria().contains(criterion)) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and weights.xml file");
			}
			if (!xmcda.criteriaValuesList.get(1).getCriteria().contains(criterion)) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and weights.xml file");
			}
			if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
				if (!xmcda.criteriaValuesList.get(2).getCriteria().contains(criterion)) {
					criteriaIdentical = false;
					xmcda_execution_results.addError(
							"Criteria are not identical in criteria.xml file and generalised_criteria.xm file");
				}
			}

			if (!criteriaIdentical)
				return criteriaIdentical;
		}
		for (Criterion criterion : xmcda.performanceTablesList.get(0).getCriteria()) {
			if ((!inputs.criteria_ids.contains(criterion.id())) && (criterion.isActive())) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and performance_table.xml file");
				break;
			}
		}
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			for (Criterion criterion : xmcda.performanceTablesList.get(1).getCriteria()) {
				if ((!inputs.criteria_ids.contains(criterion.id())) && (criterion.isActive())) {
					criteriaIdentical = false;
					xmcda_execution_results.addError(
							"Criteria are not identical in criteria.xml file and profiles_performance_table.xml file");
					break;
				}
			}
		}
		for (Criterion criterion : xmcda.criteriaValuesList.get(0).getCriteria()) {
			if ((!inputs.criteria_ids.contains(criterion.id())) && (criterion.isActive())) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and weights.xml file");
				break;
			}
		}
		for (Criterion criterion : xmcda.criteriaValuesList.get(1).getCriteria()) {
			if ((!inputs.criteria_ids.contains(criterion.id())) && (criterion.isActive())) {
				criteriaIdentical = false;
				xmcda_execution_results
						.addError("Criteria are not identical in criteria.xml file and reinforcement_factors.xml file");
				break;
			}
		}
		if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
			for (Criterion criterion : xmcda.criteriaValuesList.get(2).getCriteria()) {
				if ((!inputs.criteria_ids.contains(criterion.id())) && (criterion.isActive())) {
					criteriaIdentical = false;
					xmcda_execution_results.addError(
							"Criteria are not identical in criteria.xml file and generalised_criteria.xm file");
					break;
				}
			}
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
							.addError("U-Shape function (2) specified in generalised_criteria.xml  on Criterion "
									+ criterion + "requires indifference threshold");
					compatible = false;
				}
				break;
			case 3:
				if ((inputs.preferenceThresholds == null) || (inputs.preferenceThresholds.get(criterion) == null)) {
					xmcda_execution_results
							.addError("V-Shape function (3) specified in generalised_criteria.xml  on Criterion "
									+ criterion + "requires preference threshold");
					compatible = false;
				}
				break;
			case 4:
				if (((inputs.indifferenceThresholds == null) || (inputs.indifferenceThresholds.get(criterion) == null))
						|| ((inputs.preferenceThresholds == null)
								|| (inputs.preferenceThresholds.get(criterion) == null))) {
					xmcda_execution_results
							.addError("Level function (4) specified in generalised_criteria.xml  on Criterion "
									+ criterion + "requires indifference and preference thresholds");
					compatible = false;
				}
				break;
			case 5:
				if (((inputs.indifferenceThresholds == null) || (inputs.indifferenceThresholds.get(criterion) == null))
						|| ((inputs.preferenceThresholds == null)
								|| (inputs.preferenceThresholds.get(criterion) == null))) {
					xmcda_execution_results.addError(
							"V-Shape With Indifference function (5) specified in generalised_criteria.xml  on Criterion "
									+ criterion + "requires indifference and preference thresholds");
					compatible = false;
				}
				break;
			}
		}
		return compatible;
	}

	private static void extractPreferenceTables(Inputs inputs, XMCDA xmcda) {
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

	private static void extractProfilesPreferenceTables(Inputs inputs, XMCDA xmcda) {
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

	private static void extractWeights(Inputs inputs, XMCDA xmcda) {
		inputs.weights = new HashMap<>();
		@SuppressWarnings("unchecked")
		CriteriaValues<Double> weights_table = (CriteriaValues<Double>) xmcda.criteriaValuesList.get(0);
		for (Criterion criterion : weights_table.getCriteria()) {
			inputs.weights.put(criterion.id(), weights_table.get(criterion).get(0).getValue());
		}
	}

	private static void extractReinforcementFactors(Inputs inputs, XMCDA xmcda) {
		inputs.reinforcementFactors = new HashMap<>();
		@SuppressWarnings("unchecked")
		CriteriaValues<Double> reinforcement_factors_table = (CriteriaValues<Double>) xmcda.criteriaValuesList.get(1);
		for (Criterion criterion : reinforcement_factors_table.getCriteria()) {
			inputs.reinforcementFactors.put(criterion.id(),
					reinforcement_factors_table.get(criterion).get(0).getValue());
		}
	}

	private static void extractGeneralisedCriteria(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		inputs.generalisedCriteria = new HashMap<>();
		if (inputs.generalisedCriterion == GeneralisedCriterionParam.SPECIFIED) {
			@SuppressWarnings("unchecked")
			CriteriaValues<Integer> generalisedCriteria = (CriteriaValues<Integer>) xmcda.criteriaValuesList.get(2);
			for (Criterion criterion : generalisedCriteria.getCriteria()) {
				Integer value = generalisedCriteria.get(criterion).get(0).getValue();
				if ((value >= 1) && (value <= 5)) {
					inputs.generalisedCriteria.put(criterion.id(), value);
				} else {
					xmcda_execution_results.addError(
							"Generalised criteria must be integers between 1 and 5 in file generalised_criteria.xml");
					break;
				}
			}
		} else {
			for (String criterion : inputs.criteria_ids) {
				inputs.generalisedCriteria.put(criterion, Integer.parseInt(inputs.generalisedCriterion.label));
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

	@SuppressWarnings("unchecked")
	private static void extractThresholds(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results) {
		CriteriaThresholds thresholds = (CriteriaThresholds) xmcda.criteriaThresholdsList.get(0);

		inputs.indifferenceThresholds = new HashMap<>();
		inputs.reinforcedPreferenceThresholds = new HashMap<>();
		inputs.preferenceThresholds = new HashMap<>();

		for (Criterion criterion : thresholds.keySet()) {
			CriterionThresholds critThresholds = thresholds.get(criterion);

			if (critThresholds == null)
				continue;
			int prefThresholdID = -1;
			int indiffThresholdID = -1;
			int reinforcedPreferenceThresholdID = -1;

			for (int i = 0; i < critThresholds.size(); i++) {
				if (critThresholds.get(i).mcdaConcept().equals("indifference")) {
					indiffThresholdID = i;
				}
				if (critThresholds.get(i).mcdaConcept().equals("preference")) {
					prefThresholdID = i;
				}
				if (critThresholds.get(i).mcdaConcept().equals("reinforced_preference")) {
					reinforcedPreferenceThresholdID = i;
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
			if (reinforcedPreferenceThresholdID != -1) {
				inputs.reinforcedPreferenceThresholds.put(criterion.id(),
						(Threshold<Double>) critThresholds.get(reinforcedPreferenceThresholdID));
			} else {
				xmcda_execution_results
						.addError("Reinforced preference threshold is not specified on criterion " + criterion.id());
				inputs.reinforcedPreferenceThresholds.put(criterion.id(), null);
			}
		}
	}
}
