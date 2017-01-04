package pl.poznan.put.promethee.preference;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

import org.xmcda.Threshold;

import pl.poznan.put.promethee.exceptions.NullThresholdException;
import pl.poznan.put.promethee.exceptions.WrongPreferenceDirectionException;
import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Preference {

	public static Map<String, Map<String, Double>> calculatePreferences(InputsHandler.Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialResult)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Map<String, Map<String, Double>>> pp = calcPartialPreferences(inputs);
		Map<String, Map<String, Map<String, Double>>> rfpc = calcReinforcementPreferenceCrossed(inputs);
		Map<String, Map<String, Double>> preferences = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					preferences.putIfAbsent(a, new LinkedHashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new LinkedHashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
					preferences.putIfAbsent(b, new LinkedHashMap<>());
					preferences.get(b).put(a, calcTotalPreference(b, a, inputs, pp, rfpc));
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new LinkedHashMap<>());
					preferences.get(a).put(b, calcTotalPreference(a, b, inputs, pp, rfpc));
				}
			}
		}
		calcPartialPreferencesReinforcedPreference(partialResult, rfpc, pp);
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
	 * @return matrix of preferences - all alternatives with all
	 *         alternatives(profiles) on all criteria
	 * @throws WrongPreferenceDirectionException
	 * @throws NullThresholdException
	 */
	private static Map<String, Map<String, Map<String, Double>>> calcPartialPreferences(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Map<String, Map<String, Double>>> preferenceMap = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.putIfAbsent(a, new LinkedHashMap<>());
						preferenceMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						preferenceMap.get(a).get(b).put(c, calcPreferenceOnOneCriterion(
								inputs.performanceTable.get(a).get(c).doubleValue(),
								inputs.performanceTable.get(b).get(c).doubleValue(), inputs.preferenceDirections.get(c),
								inputs.generalisedCriteria.get(c).intValue(), inputs.preferenceThresholds.get(c),
								inputs.indifferenceThresholds.get(c), inputs.sigmaThresholds.get(c)));
					}
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.putIfAbsent(a, new LinkedHashMap<>());
						preferenceMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						preferenceMap.get(a).get(b).put(c,
								calcPreferenceOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c),
										inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
										inputs.sigmaThresholds.get(c)));
						preferenceMap.putIfAbsent(b, new LinkedHashMap<>());
						preferenceMap.get(b).putIfAbsent(a, new LinkedHashMap<>());
						preferenceMap.get(b).get(a).put(c, calcPreferenceOnOneCriterion(
								inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
								inputs.performanceTable.get(a).get(c).doubleValue(), inputs.preferenceDirections.get(c),
								inputs.generalisedCriteria.get(c).intValue(), inputs.preferenceThresholds.get(c),
								inputs.indifferenceThresholds.get(c), inputs.sigmaThresholds.get(c)));
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						preferenceMap.putIfAbsent(a, new LinkedHashMap<>());
						preferenceMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						preferenceMap.get(a).get(b).put(c, calcPreferenceOnOneCriterion(
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

	/**
	 * @param inputs
	 * @return matrix of reinforcement preferences - all alternatives with all
	 *         alternatives(profiles) on all criteria
	 * @throws WrongPreferenceDirectionException
	 * @throws NullThresholdException
	 */
	private static Map<String, Map<String, Map<String, Double>>> calcReinforcementPreferenceCrossed(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Map<String, Map<String, Double>>> reinforcementPreferenceCrossed = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					for (String c : inputs.criteria_ids) {
						Double ga = inputs.performanceTable.get(a).get(c).doubleValue();
						Double gb = inputs.performanceTable.get(b).get(c).doubleValue();
						if (checkIfCrossed(ga, gb, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c,
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						} else {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c, 1.0);
						}
					}
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						Double ga = inputs.performanceTable.get(a).get(c).doubleValue();
						Double gb = inputs.profilesPerformanceTable.get(b).get(c).doubleValue();
						if (checkIfCrossed(ga, gb, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c,
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						} else {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c, 1.0);
						}
						if (checkIfCrossed(gb, ga, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(b).putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(b).get(a).put(c,
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						} else {
							reinforcementPreferenceCrossed.putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(b).putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(b).get(a).put(c, 1.0);
						}
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						Double ga = inputs.profilesPerformanceTable.get(a).get(c).doubleValue();
						Double gb = inputs.profilesPerformanceTable.get(b).get(c).doubleValue();
						if (checkIfCrossed(ga, gb, inputs.preferenceDirections.get(c),
								inputs.reinforcedPreferenceThresholds.get(c))) {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c,
									inputs.reinforcementFactors.getOrDefault(c, 1.0));
						} else {
							reinforcementPreferenceCrossed.putIfAbsent(a, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).putIfAbsent(b, new LinkedHashMap<>());
							reinforcementPreferenceCrossed.get(a).get(b).put(c, 1.0);
						}
					}
				}
			}
		}
		return reinforcementPreferenceCrossed;
	}

	private static Double calcTotalPreference(String alternative1, String alternative2, Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialPreferences,
			Map<String, Map<String, Map<String, Double>>> reinforcementPreferenceCrossed) {
		Double sumOfWeights = sumOfWeightRPCrossed(alternative1, alternative2, inputs, reinforcementPreferenceCrossed);
		Double sum = sumOfPreference(alternative1, alternative2, inputs, reinforcementPreferenceCrossed,
				partialPreferences);
		Double preference = sum / sumOfWeights;
		return preference;
	}

	private static Double sumOfWeightRPCrossed(String a, String b, Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> reinforcementPreferenceCrossed) {
		Double sum = 0.0;
		for (String criterion : inputs.criteria_ids) {
			sum += inputs.weights.get(criterion) * reinforcementPreferenceCrossed.get(a).get(b).get(criterion);
		}
		return sum;
	}

	private static Double sumOfPreference(String a, String b, Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> reinforcementPreferenceCrossed,
			Map<String, Map<String, Map<String, Double>>> partialPreferences) {
		Double sum = 0.0;
		for (String criterion : inputs.criteria_ids) {
			sum += inputs.weights.get(criterion)
					* reinforcementPreferenceCrossed.get(a).get(b).get(criterion).doubleValue()
					* partialPreferences.get(a).get(b).get(criterion);
		}
		return sum;
	}

	public static void calcPartialPreferencesReinforcedPreference(
			Map<String, Map<String, Map<String, Double>>> preference,
			Map<String, Map<String, Map<String, Double>>> reinforcementPreferenceCrossed,
			Map<String, Map<String, Map<String, Double>>> partialPreferences) {
		for (String a : partialPreferences.keySet()) {
			for (String b : partialPreferences.get(a).keySet()) {
				for (String c : partialPreferences.get(a).get(b).keySet()) {
					preference.putIfAbsent(a, new LinkedHashMap<>());
					preference.get(a).putIfAbsent(b, new LinkedHashMap<>());
					Double prefValue = partialPreferences.get(a).get(b).get(c).doubleValue();
					Double reinfValue = reinforcementPreferenceCrossed.get(a).get(b).get(c).doubleValue();
					Double resultValue = prefValue * reinfValue;
					preference.get(a).get(b).put(c, resultValue);
				}
			}
		}
	}
}
