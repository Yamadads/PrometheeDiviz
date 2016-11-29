package pl.poznan.put.promethee.weight;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import pl.poznan.put.promethee.xmcda.InputsHandler.Inputs;

public class Weights {

	public static Map<String, Double> calcWeights(Inputs inputs) {
		WeightsMethods weightCalculator = new WeightsMethods();
		Map<String, Double> weights = weightCalculator.calculate(inputs.criteriaRanking,
				inputs.methodNameParam.getLabel());
		return sortMapByKey(weights);
	}

	private static Map<String, Double> sortMapByKey(Map<String, Double> map) {
		Map<String, Double> sortedMap = map.entrySet().stream().sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
}