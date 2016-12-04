package pl.poznan.put.promethee.discordance;

import java.util.LinkedHashMap;
import java.util.Map;

import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Discordance {

	public static Map<String, Map<String, Double>> calcResult(Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> partialResult) {
		getPartialDiscordances(inputs, partialResult);
		Map<String, Map<String, Double>> results = new LinkedHashMap<>();
		for (String a : partialResult.keySet()) {
			for (String b : partialResult.get(a).keySet()) {
				results.putIfAbsent(a, new LinkedHashMap<>());
				results.get(a).put(b, getTotalDiscordance(inputs, a, b, partialResult));
			}
		}
		return results;
	}

	private static void getPartialDiscordances(Inputs inputs,
			Map<String, Map<String, Map<String, Double>>> discordances) {
		for (String a : inputs.partialPreferences.keySet()) {
			for (String b : inputs.partialPreferences.get(a).keySet()) {
				for (String c : inputs.partialPreferences.get(a).get(b).keySet()) {
					discordances.putIfAbsent(a, new LinkedHashMap<>());
					discordances.get(a).putIfAbsent(b, new LinkedHashMap<>());
					discordances.get(a).get(b).put(c, inputs.partialPreferences.get(b).get(a).get(c).doubleValue());
				}
			}
		}
	}

	private static Double getTotalDiscordance(Inputs inputs, String alternative1, String alternative2,
			Map<String, Map<String, Map<String, Double>>> discordances) {
		Double result = 1.0;
		Boolean discordantSetEmpty = true;
		Double power = inputs.technicalParam / inputs.criteria_ids.size();
		for (String criterion : inputs.criteria_ids) {
			Double ga = getAlternativeEvaluation(alternative1, criterion, inputs);
			Double gb = getAlternativeEvaluation(alternative2, criterion, inputs);
			if (ga < gb) {
				discordantSetEmpty = false;
				Double disc = discordances.get(alternative1).get(alternative2).get(criterion);
				result *= Math.pow((1.0 - disc), power);
			}
		}
		result = 1.0 - result;
		if (discordantSetEmpty) {
			return 0.0;
		}
		return result;
	}

	private static Double getAlternativeEvaluation(String alternative, String criterion, Inputs inputs) {
		if (inputs.alternatives_ids.contains(alternative)) {
			return inputs.performanceTable.get(alternative).get(criterion).doubleValue();
		}
		if (inputs.profiles_ids.contains(alternative)) {
			return inputs.profilesPerformanceTable.get(alternative).get(criterion).doubleValue();
		}
		return null;
	}
}