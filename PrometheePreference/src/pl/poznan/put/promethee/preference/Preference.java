package pl.poznan.put.promethee.preference;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.xmcda.Threshold;

import pl.poznan.put.promethee.exceptions.NullThresholdException;
import pl.poznan.put.promethee.exceptions.WrongPreferenceDirectionException;
import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Preference {

	public static Map<String, Map<String, Double>> calculatePreferences(InputsHandler.Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Map<String, Map<String, Double>>> partialPreferences = calcPartialPreferences(inputs);
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

	public static Map<String, Map<String, Map<String, Double>>> calcPartialPreferences(Inputs inputs)
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
						preferenceMap.get(b).get(a).put(c,
								calcPreferenceOnOneCriterion(inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.preferenceDirections.get(c),
										inputs.generalisedCriteria.get(c).intValue(),
										inputs.preferenceThresholds.get(c), inputs.indifferenceThresholds.get(c),
										inputs.sigmaThresholds.get(c)));
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

	private static Double calcTotalPreference(String alternative1, String alternative2, Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialPreferences) {
		Double preference = 0.0;
		Double totalWeight = 0.0;
		for (String criterion : inputs.criteria_ids) {
			Double weight = inputs.weights.get(criterion);
			totalWeight += weight;
			preference += (partialPreferences.get(alternative1).get(alternative2).get(criterion).doubleValue() * weight);
		}
		preference = preference / totalWeight;
		return preference;
	}
}
