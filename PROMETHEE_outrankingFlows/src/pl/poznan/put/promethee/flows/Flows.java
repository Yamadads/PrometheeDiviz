package pl.poznan.put.promethee.flows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import pl.poznan.put.promethee.xmcda.InputsHandler.ComparisonWithParam;
import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Flows {

	public static Map<String, Map<String, Double>> calcFlows(Inputs inputs) {
		Map<String, Map<String, Double>> flows = new LinkedHashMap<String, Map<String, Double>>();
		Map<String, Double> positiveFlows = new LinkedHashMap<String, Double>();
		Map<String, Double> negativeFlows = new LinkedHashMap<String, Double>();

		if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
			for (String alternative : inputs.alternatives_ids) {
				ArrayList<String> alternativesToCompare = new ArrayList<String>(inputs.alternatives_ids);
				alternativesToCompare.remove(alternative);
				positiveFlows.put(alternative, calcPositiveFlow(alternative, alternativesToCompare, inputs));
				negativeFlows.put(alternative, calcNegativeFlow(alternative, alternativesToCompare, inputs));
			}
		} else {
			for (String alternative : inputs.alternatives_ids) {
				positiveFlows.put(alternative, calcPositiveFlow(alternative, inputs.profiles_ids, inputs));
				negativeFlows.put(alternative, calcNegativeFlow(alternative, inputs.profiles_ids, inputs));
			}
			for (String profile : inputs.profiles_ids){
				ArrayList<String> profilesToCompare = new ArrayList<String>(inputs.profiles_ids);
				profilesToCompare.remove(profile);
				positiveFlows.put(profile, calcPositiveFlow(profile, profilesToCompare, inputs));
				negativeFlows.put(profile, calcNegativeFlow(profile, profilesToCompare, inputs));
			}
		}

		flows.put("positive_flows", sortMapByKey(positiveFlows));
		flows.put("negative_flows", sortMapByKey(negativeFlows));
		return flows;
	}

	private static Double calcPositiveFlow(String alternative, List<String> compareWith, Inputs inputs) {
		Double flow = 0.0;
		for (String alternative2 : compareWith) {
			flow += inputs.preferences.get(alternative).get(alternative2).doubleValue();
		}
		flow /= compareWith.size();
		return flow;
	}

	private static Double calcNegativeFlow(String alternative, List<String> compareWith, Inputs inputs) {
		Double flow = 0.0;
		for (String alternative2 : compareWith) {
			flow += inputs.preferences.get(alternative2).get(alternative).doubleValue();
		}
		flow /= compareWith.size();
		return flow;
	}

	private static Map<String, Double> sortMapByKey(Map<String, Double> map) {
		Map<String, Double> sortedMap = map.entrySet().stream().sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
}
