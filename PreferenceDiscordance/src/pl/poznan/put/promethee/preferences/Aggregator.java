package pl.poznan.put.promethee.preferences;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Aggregator {

	public static Map<String, Map<String, Double>> aggregate(Inputs inputs) {
		Map<String, Map<String, Double>> aggregatedPreferences = new LinkedHashMap<>();

		for (String key : inputs.preferences.keySet()) {
			for (String key2 : inputs.preferences.get(key).keySet()) {
				Double cp = inputs.preferences.get(key).get(key2).doubleValue();
				Double dp = inputs.discordances.get(key).get(key2).doubleValue();

				Double aggregatedValue = cp * (1 - dp);

				aggregatedPreferences.putIfAbsent(key, new HashMap<>());
				aggregatedPreferences.get(key).put(key2, aggregatedValue);
			}
		}

		return sortMapByKey(aggregatedPreferences);
	}

	private static Map<String, Map<String, Double>> sortMapByKey(Map<String, Map<String, Double>> map) {
		Map<String, Map<String, Double>> sortedMap = map.entrySet().stream().sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
}
