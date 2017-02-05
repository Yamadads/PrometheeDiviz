package pl.poznan.put.promethee.veto;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.xmcda.Threshold;

import pl.poznan.put.promethee.exceptions.WrongPreferenceDirectionException;
import pl.poznan.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;
import pl.poznan.put.promethee.xmcda.InputsHandler.WeightsParam;

public class Veto {

	public static Map<String, Map<String, Double>> calcTotalVeto(Inputs inputs)
			throws WrongPreferenceDirectionException {
		Map<String, Map<String, Map<String, Double>>> partialVeto = calcPartialVeto(inputs);
		Map<String, Map<String, Double>> preferences = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalVetoOnPair(a, b, inputs, partialVeto));
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalVetoOnPair(a, b, inputs, partialVeto));
					preferences.putIfAbsent(b, new HashMap<>());
					preferences.get(b).put(a, calcTotalVetoOnPair(b, a, inputs, partialVeto));
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					preferences.putIfAbsent(a, new HashMap<>());
					preferences.get(a).put(b, calcTotalVetoOnPair(a, b, inputs, partialVeto));
				}
			}
		}
		return sortMapByKey(preferences);
	}

	public static Map<String, Map<String, Map<String, Double>>> calcPartialVeto(Inputs inputs)
			throws WrongPreferenceDirectionException {
		Map<String, Map<String, Map<String, Double>>> vetoMap = new LinkedHashMap<>();
		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.alternatives_ids) {
					for (String c : inputs.criteria_ids) {
						vetoMap.putIfAbsent(a, new LinkedHashMap<>());
						vetoMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						vetoMap.get(a).get(b).put(c,
								calcVetoOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.performanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.vetoThresholds.get(c)));
					}
				}
			}
		} else {
			for (String a : inputs.alternatives_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						vetoMap.putIfAbsent(a, new LinkedHashMap<>());
						vetoMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						vetoMap.get(a).get(b).put(c,
								calcVetoOnOneCriterion(inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.vetoThresholds.get(c)));

						vetoMap.putIfAbsent(b, new LinkedHashMap<>());
						vetoMap.get(b).putIfAbsent(a, new LinkedHashMap<>());
						vetoMap.get(b).get(a).put(c,
								calcVetoOnOneCriterion(inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.performanceTable.get(a).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.vetoThresholds.get(c)));
					}
				}
			}
			for (String a : inputs.profiles_ids) {
				for (String b : inputs.profiles_ids) {
					for (String c : inputs.criteria_ids) {
						vetoMap.putIfAbsent(a, new LinkedHashMap<>());
						vetoMap.get(a).putIfAbsent(b, new LinkedHashMap<>());
						vetoMap.get(a).get(b).put(c,
								calcVetoOnOneCriterion(inputs.profilesPerformanceTable.get(a).get(c).doubleValue(),
										inputs.profilesPerformanceTable.get(b).get(c).doubleValue(),
										inputs.preferenceDirections.get(c), inputs.vetoThresholds.get(c)));
					}
				}
			}
		}
		return vetoMap;
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
	 * @return calculated final threshold value (get constant if exist or
	 *         calculate value if defined as linear)
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

	private static Double calcVetoOnOneCriterion(Double ga, Double gb, String direction,
			Threshold<Double> vetoThreshold) throws WrongPreferenceDirectionException {
		Double diff = calcDifferenceBetweenEvaluations(direction, ga, gb);
		Double v = calcThreshold(direction, ga, gb, vetoThreshold);

		Double veto = 0.0;
		if (v != null) {
			if ((diff * -1) >= v) {
				veto = 1.0;
			}
		}
		return veto;
	}

	private static Double calcTotalVetoOnPair(String alternative1, String alternative2, Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialVeto) {
		Double veto = 0.0;
		if (inputs.weightsParam == WeightsParam.SPECIFIED) {
			Double totalWeight = 0.0;
			for (String criterion : inputs.criteria_ids) {
				Double weight = inputs.weights.get(criterion);
				totalWeight += weight;
				veto += (partialVeto.get(alternative1).get(alternative2).get(criterion).doubleValue() * weight);
			}
			veto = veto / totalWeight;
		} else {
			for (String criterion : inputs.criteria_ids) {
				if (partialVeto.get(alternative1).get(alternative2).get(criterion).doubleValue() == 1.0) {
					veto = 1.0;
					break;
				}
			}
		}
		return veto;
	}
}
