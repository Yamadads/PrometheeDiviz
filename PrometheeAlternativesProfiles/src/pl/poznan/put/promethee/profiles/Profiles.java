package pl.poznan.put.promethee.profiles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import pl.poznan.put.promethee.exceptions.NullThresholdException;
import pl.poznan.put.promethee.exceptions.WrongPreferenceDirectionException;
import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Profiles {

	public static Map<String, Map<String, Double>> calculateProfiles(Inputs inputs)
			throws WrongPreferenceDirectionException, NullThresholdException {
		Map<String, Map<String, Double>> profiles = new LinkedHashMap<String, Map<String, Double>>();

		for (String alternative : inputs.alternatives_ids) {
			for (String criterion : inputs.criteria_ids) {
				Double value = calcProfileOfAlternative(alternative, criterion, inputs);
				profiles.putIfAbsent(alternative, new LinkedHashMap<>());
				profiles.get(alternative).put(criterion, value);
			}
		}
		return profiles;
	}

	private static Double calcProfileOfAlternative(String alternative, String criterion, Inputs inputs) {
		ArrayList<String> alternatives = new ArrayList<String>(inputs.alternatives_ids);
		alternatives.remove(alternative);
		Double sumOfPreference = 0.0;
		for (String alt : alternatives) {
			sumOfPreference += calcDiff(alternative, alt, criterion, inputs);
		}
		return sumOfPreference / alternatives.size();
	}

	private static Double calcDiff(String a, String x, String criterion, Inputs inputs) {
		return inputs.partialPreferences.get(a).get(x).get(criterion).doubleValue()
				- inputs.partialPreferences.get(x).get(a).get(criterion).doubleValue();
	}
}
