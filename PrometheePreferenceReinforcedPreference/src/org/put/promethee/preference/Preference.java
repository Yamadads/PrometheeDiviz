package org.put.promethee.preference;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.put.promethee.exceptions.NullThresholdException;
import org.put.promethee.exceptions.WrongPreferenceDirectionException;
import org.put.promethee.xmcda.InputsHandler;
import org.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import org.put.promethee.xmcda.InputsHandler.Inputs;
import org.xmcda.Threshold;

public class Preference {

	public static Map<String, Map<String, Double>> calculatePreferences(InputsHandler.Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Double> pp = calcPartialPreferences(inputs);
		Map<String, Double> rfpc = calcReinforcementPreferenceCrossed(inputs);
		Map<String, Map<String, Double>> preferences = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
					preferences.putIfAbsent(b, new HashMap<>());
					preferences.get(b).put(a, calcTotalPreference(b, a, inputs, pp, rfpc));
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
				}
			}
		}
		return sortMapByKey(preferences);
	}

	private static Map<String, Map<String, Double>> sortMapByKey(Map<String, Map<String, Double>> map) {
		Map<String, Map<String, Double>> sortedMap = map.entrySet().stream().sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
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
			Threshold<Double> preferenceThreshold, Threshold<Double> indifferenceThreshold)
			throws WrongPreferenceDirectionException, NullThresholdException {
		GeneralisedCriteria generalisedCriteria = new GeneralisedCriteria();

		Double diff = calcDifferenceBetweenEvaluations(direction, ga, gb);
		Double p = calcThreshold(direction, ga, gb, preferenceThreshold);
		Double q = calcThreshold(direction, ga, gb, indifferenceThreshold);

		Double preference = generalisedCriteria.calculate(functionNumber, diff, p, q);
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
										inputs.preferenceDirections.get(c),
										inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c)));
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
										inputs.preferenceDirections.get(c),
										inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c)));
						preferenceMap.put(keyHash(b, a, c),
								calcPreferenceOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c),
										inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c)));
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.put(keyHash(a, b, c), calcPreferenceOnOneCriterion(
								inputs.profilesPerformanceTable.get(a).get(c).doubleValue(),
								inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
								inputs.preferenceDirections.get(c), inputs.generalisedCriteria.get(c).intValue(),
								inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c)));
					}
				}
			}
		}
		return preferenceMap;
	}

	/**
	 * @param inputs
	 * @return matrix of reinforcement preferences - all alternatives with all
	 *         alternatives(profiles) on all criteria
	 * @throws WrongPreferenceDirectionException
	 * @throws NullThresholdException
	 */
	private static Map<String, Double> calcReinforcementPreferenceCrossed(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Double> reinforcementPreferenceCrossed = new HashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
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
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						Double ga = inputs.performanceTable.get(a).get(c).doubleValue();
						Double gb = inputs.performanceTable.get(b).get(c).doubleValue();
						if (checkIfCrossed(ga, gb, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.put(keyHash(a, b, c),
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						}
						if (checkIfCrossed(gb, ga, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.put(keyHash(b, a, c),
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						}
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
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
		}
		return reinforcementPreferenceCrossed;
	}

	private static Double calcTotalPreference(String alternative1, String alternative2, Inputs inputs,
			Map<String, Double> partialPreferences, Map<String, Double> reinforcementPreferenceCrossed) {
		Double sumOfWeights = sumOfWeightRPCrossed(alternative1, alternative2, inputs, reinforcementPreferenceCrossed);
		Double sum = sumOfPreference(alternative1, alternative2, inputs, reinforcementPreferenceCrossed,
				partialPreferences);
		Double preference = sum / sumOfWeights;
		return preference;
	}

	private static Double sumOfWeightRPCrossed(String a, String b, Inputs inputs,
			Map<String, Double> reinforcementPreferenceCrossed) {
		Double sum = 0.0;
		for (String criterion : inputs.criteria_ids) {
			sum += inputs.weights.get(criterion)
					* reinforcementPreferenceCrossed.getOrDefault(keyHash(a, b, criterion), 1.0);
		}
		return sum;
	}

	private static Double sumOfPreference(String a, String b, Inputs inputs,
			Map<String, Double> reinforcementPreferenceCrossed, Map<String, Double> partialPreferences) {
		Double sum = 0.0;
		for (String criterion : inputs.criteria_ids) {
			sum += inputs.weights.get(criterion)
					* reinforcementPreferenceCrossed.getOrDefault(keyHash(a, b, criterion), 1.0)
					* partialPreferences.get(keyHash(a, b, criterion));
		}
		return sum;
	}

	private static String keyHash(String a, String b, String c) {
		return "_" + a + "_*|*_" + b + "_*|*_" + c + "_";
	}
}
