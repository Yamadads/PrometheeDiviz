package org.put.promethee.preference;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.put.promethee.exceptions.NullThresholdException;
import org.put.promethee.exceptions.WrongPreferenceDirectionException;
import org.put.promethee.xmcda.InputsHandler;
import org.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import org.put.promethee.xmcda.InputsHandler.Inputs;
import org.xmcda.Threshold;

public class Preference {

	public static Map<String, Map<String, Double>> calculatePreferences(InputsHandler.Inputs inputs) throws WrongPreferenceDirectionException, NullThresholdException {		
		Map<String , Double> partialPreferences = calcPartialPreferences(inputs);
		Map<String, Map<String, Double>> preferences = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, partialPreferences));
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {					
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, partialPreferences));
					preferences.putIfAbsent(b, new HashMap<>());
					preferences.get(b).put(a, calcTotalPreference(b, a, inputs, partialPreferences));
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, partialPreferences));
				}
			}
		}
		return preferences;
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
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
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
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.put(keyHash(a, b, c),
								calcPreferenceOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
										inputs.sigmaThresholds.get(c)));
						preferenceMap.put(keyHash(b, a, c),
								calcPreferenceOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
										inputs.sigmaThresholds.get(c)));
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.put(keyHash(a, b, c),
								calcPreferenceOnOneCriterion(
										inputs.profilesPerformanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
										inputs.sigmaThresholds.get(c)));
					}
				}
			}
		}
		return preferenceMap;
	}

	private static Double calcTotalPreference(String alternative1, String alternative2, Inputs inputs,
			Map<String, Double> partialPreferences) {
		Double preference = 0.0;
		Double totalWeight = 0.0;
		for (String criterion : inputs.criteria_ids) {
			Double weight = inputs.weights.get(criterion);
			totalWeight += weight;
			preference += (partialPreferences.get(keyHash(alternative1, alternative2 , criterion)) * weight);
		}
		preference = preference/totalWeight;
		return preference;
	}
	
	private static String keyHash(String a, String b, String c){
		return "_"+a+"_*|*_"+b+"_*|*_"+c+"_";
	}
}
