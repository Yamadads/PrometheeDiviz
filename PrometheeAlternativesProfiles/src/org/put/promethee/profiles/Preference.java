package org.put.promethee.profiles;

import java.util.Map;
import java.util.HashMap;

import org.put.promethee.exceptions.NullThresholdException;
import org.put.promethee.exceptions.WrongPreferenceDirectionException;
import org.put.promethee.xmcda.InputsHandler;
import org.put.promethee.xmcda.InputsHandler.Inputs;
import org.put.promethee.xmcda.InputsHandler.OperatingModeParam;
import org.xmcda.Threshold;

public class Preference {

	public static Map<String, Double> calculatePreferences(InputsHandler.Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Double> partialPreferences = calcPartialPreferences(inputs);

		if (inputs.mode == OperatingModeParam.REINFORCED_PREFERENCE) {
			Map<String, Double> reinforcedPreferenceCrossed = calcReinforcementPreferenceCrossed(inputs);
			for (String key : partialPreferences.keySet()) {
				partialPreferences.put(key,
						partialPreferences.get(key) * reinforcedPreferenceCrossed.getOrDefault(key, 1.0));
			}
		}
		return partialPreferences;
	}

	/**
	 * @param direction
	 *            direction of function on criterion ('MIN' or 'MAX')
	 * @param ga
	 *            evaluation of alternative on specified criterion
	 * @param gb
	 *            evaluation of alternative on specified criterion
	 * @return difference between evaluations
	 * @throws WrongPreferenceDirectionException
	 */
	private static Double calcDifferenceBetweenEvaluations(String direction, Double ga, Double gb)
			throws WrongPreferenceDirectionException {
		Double differenceBetweenEvaluations = 0.0;
		if (direction.equals("MAX")) {
			differenceBetweenEvaluations = ga - gb;
		} else if (direction.equals("MIN")) {
			differenceBetweenEvaluations = gb - ga;
		} else {
			throw new WrongPreferenceDirectionException();
		}
		return differenceBetweenEvaluations;
	}

	/**
	 * @param direction
	 * @param ga
	 * @param gb
	 * @param threshold
	 * @return calculated final threshold value (get constant if exist or calc
	 *         value if defined as linear)
	 * @throws WrongPreferenceDirectionException
	 */
	private static Double calcThreshold(String direction, Double ga, Double gb, Threshold<Double> threshold)
			throws WrongPreferenceDirectionException {
		if (threshold == null) {
			return null;
		}
		Double thresholdValue = 0.0;
		if (threshold.isConstant()) {
			thresholdValue = threshold.getConstant().getValue();
		} else {
			Double baseEvaluation = 0.0;
			if (direction.equals("MAX")) {
				baseEvaluation = ga > gb ? gb : ga;
			} else if (direction.equals("MIN")) {
				baseEvaluation = ga > gb ? ga : gb;
			} else {
				throw new WrongPreferenceDirectionException();
			}
			Double slope = threshold.getSlope().getValue();
			Double intercept = threshold.getIntercept().getValue();
			thresholdValue = slope * baseEvaluation + intercept;
		}
		return thresholdValue;
	}

	private static Double calcPreferenceOnOneCriterion(Double ga, Double gb, String direction, Integer functionNumber,
			Threshold<Double> preferenceThreshold, Threshold<Double> indifferenceThreshold,
			Threshold<Double> sigmaThreshold) throws WrongPreferenceDirectionException, NullThresholdException {
		GeneralisedCriteria generalisedCriteria = new GeneralisedCriteria();

		Double diff = calcDifferenceBetweenEvaluations(direction, ga, gb);
		Double p = calcThreshold(direction, ga, gb, preferenceThreshold);
		Double q = calcThreshold(direction, ga, gb, indifferenceThreshold);
		Double s = calcThreshold(direction, ga, gb, sigmaThreshold);

		Double preference = generalisedCriteria.calculate(functionNumber, diff, p, q, s);
		return preference;
	}

	private static Boolean checkIfCrossed(Double ga, Double gb, String direction,
			Threshold<Double> reinforcedPreferenceThreshold) throws WrongPreferenceDirectionException {
		Double diff = calcDifferenceBetweenEvaluations(direction, ga, gb);
		Double r = calcThreshold(direction, ga, gb, reinforcedPreferenceThreshold);
		Boolean crossed = false;
		if (r != null) {
			if (diff > r) {
				crossed = true;
			}
		}
		return crossed;
	}

	/**
	 * @param inputs
	 * @return
	 * @throws WrongPreferenceDirectionException
	 * @throws NullThresholdException
	 */
	private static Map<String, Double> calcReinforcementPreferenceCrossed(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Double> reinforcementPreferenceCrossed = new HashMap<>();
		for (String a : inputs.alternatives_ids) {
			for (String b : inputs.alternatives_ids) {
				for (String c : inputs.criteria_ids) {
					Double ga = inputs.performanceTable.get(a).get(c).doubleValue();
					Double gb = inputs.performanceTable.get(b).get(c).doubleValue();
					if (checkIfCrossed(ga, gb, inputs.preferenceDirections.get(c),
							inputs.reinforcedPreferenceThresholds.get(c))) {
						reinforcementPreferenceCrossed.put(keyHash(a, b, c),
								inputs.reinforcementFactors.getOrDefault(c, 1.0));
					}
				}
			}
		}
		return reinforcementPreferenceCrossed;
	}

	/**
	 * @param inputs
	 * @return matrix of preferences - all alternatives with all
	 *         alternatives(profiles) on all criteria
	 * @throws WrongPreferenceDirectionException
	 * @throws NullThresholdException
	 */
	private static Map<String, Double> calcPartialPreferences(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Double> preferenceMap = new HashMap<>();
		for (String a : inputs.alternatives_ids) {
			for (String b : inputs.alternatives_ids) {
				for (String c : inputs.criteria_ids) {
					preferenceMap.put(keyHash(a, b, c),
							calcPreferenceOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
									inputs.performanceTable.get(b).get(c).doubleValue(),
									inputs.preferenceDirections.get(c), inputs.generalisedCriteria.get(c).intValue(),
									inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
									inputs.sigmaThresholds.get(c)));
				}
			}
		}
		return preferenceMap;
	}

	public static String keyHash(String a, String b, String c) {
		return "_" + a + "_*|*_" + b + "_*|*_" + c + "_";
	}
}
