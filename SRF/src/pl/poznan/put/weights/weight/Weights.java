package pl.poznan.put.weights.weight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import pl.poznan.put.weights.xmcda.InputsHandler.Inputs;

public class Weights {

	public static Map<String, Double> calcWeights(Inputs inputs) {
		Map<String, Double> weights = new LinkedHashMap<>();

		Map<String, Double> nonNormalizedWeights = calcNonNormalizedWeights(inputs);
		Map<String, Double> normalizedWeights = calcNormalizedWeights(nonNormalizedWeights);
		Map<String, Double> roundedWeights = calcRoundedWeights(normalizedWeights, inputs.decimalPlaces);
		Map<String, Double> ratioLminus = calcLminus(normalizedWeights, roundedWeights, inputs.decimalPlaces);
		Map<String, Double> ratioLplus = calcLplus(normalizedWeights, roundedWeights, inputs.decimalPlaces);
		Set<String> ratioLplusGreaterThanLminus = calcLplusGreaterThanLminus(ratioLplus, ratioLminus);
		weights = calcNormalizedWeightsUpTo100(ratioLplus, ratioLminus, ratioLplusGreaterThanLminus, normalizedWeights,
				roundedWeights, inputs.decimalPlaces);
		return sortMapByKey(weights);
	}

	private static Map<String, Double> calcNonNormalizedWeights(Inputs inputs) {
		Map<String, Double> weights = new LinkedHashMap<>();
		Double z = inputs.criteriaWeightRatio;
		Integer maxValue = Collections.max(inputs.criteriaRanking.entrySet(), Map.Entry.comparingByValue()).getValue();
		Double e = maxValue.doubleValue() - 1;
		Double u = (z - 1) / e;
		for (String criterion : inputs.criteriaRanking.keySet()) {
			weights.put(criterion, 1 + u * (inputs.criteriaRanking.get(criterion) - 1));
		}
		return weights;
	}

	private static Map<String, Double> calcNormalizedWeights(Map<String, Double> nonNormalizedWeights) {
		Double weightsSum = 0.0;
		for (Double value : nonNormalizedWeights.values())
			weightsSum += value;
		Map<String, Double> normalizedWeights = new LinkedHashMap<>();
		for (String criterion : nonNormalizedWeights.keySet()) {
			normalizedWeights.put(criterion, nonNormalizedWeights.get(criterion) * 100 / weightsSum);
		}
		return normalizedWeights;
	}

	private static Map<String, Double> calcRoundedWeights(Map<String, Double> normalizedWeights,
			Integer decimalPlaces) {
		Map<String, Double> roundedWeights = new LinkedHashMap<>();
		for (String criterion : normalizedWeights.keySet()) {
			roundedWeights.put(criterion, customRound(normalizedWeights.get(criterion), decimalPlaces, true));
		}
		return roundedWeights;
	}

	private static Double customRound(Double num, Integer places, Boolean floor) {
		Double base = Math.pow(10.0, places);
		Double result = 0.0;
		if (floor) {
			result = Math.floor(num * base) / base;
		} else {
			result = Math.ceil(num * base) / base;
		}
		return result;
	}

	private static Map<String, Double> calcLplus(Map<String, Double> normalizedWeights,
			Map<String, Double> roundedWeights, Integer decimalPlaces) {
		Map<String, Double> ratioLplus = new LinkedHashMap<>();
		Double w = Math.pow(10.0, -decimalPlaces);
		for (String criterion : normalizedWeights.keySet()) {
			Double value = (w - (normalizedWeights.get(criterion) - roundedWeights.get(criterion)))
					/ normalizedWeights.get(criterion);
			ratioLplus.put(criterion, value);
		}
		return ratioLplus;
	}

	private static Map<String, Double> calcLminus(Map<String, Double> normalizedWeights,
			Map<String, Double> roundedWeights, Integer decimalPlaces) {
		Map<String, Double> ratioLminus = new LinkedHashMap<>();
		for (String criterion : normalizedWeights.keySet()) {
			Double value = (normalizedWeights.get(criterion) - roundedWeights.get(criterion))
					/ normalizedWeights.get(criterion);
			ratioLminus.put(criterion, value);
		}
		return ratioLminus;
	}

	private static Set<String> calcLplusGreaterThanLminus(Map<String, Double> Lplus, Map<String, Double> Lminus) {
		Set<String> ratio = new LinkedHashSet<>();
		for (String criterion : Lplus.keySet()) {
			if (Lplus.get(criterion) > Lminus.get(criterion)) {
				ratio.add(criterion);
			}
		}
		return ratio;
	}

	private static Map<String, Double> calcNormalizedWeightsUpTo100(Map<String, Double> ratioLplus,
			Map<String, Double> ratioLminus, Set<String> ratioLplusGTLmins, Map<String, Double> normalizedWeights,
			Map<String, Double> roundedWeights, Integer decimalPlaces) {
		Map<String, Double> normalizedWeightsUpTo100 = new LinkedHashMap<>();
		ArrayList<String> sortedLplus = sortMapKeysByValue(ratioLplus, true);
		ArrayList<String> sortedLminus = sortMapKeysByValue(ratioLminus, false);
		Double v = calcV(roundedWeights, decimalPlaces);
		if (ratioLplusGTLmins.size() + v > normalizedWeights.size()) {
			Integer i = sortedLplus.size() - 1;
			Integer lastCriterion = v.intValue();
			while ((i >= 0) && (i >= lastCriterion)) {
				String criterion = sortedLplus.get(i);
				if (!ratioLplusGTLmins.contains(criterion)) {
					normalizedWeightsUpTo100.put(criterion,
							customRound(normalizedWeights.get(criterion), decimalPlaces, true));
				}else{
					lastCriterion--;
				}
				i--;
			}
			i = 0;
			while (i < normalizedWeights.size()) {
				String criterion = sortedLplus.get(i);
				if (!normalizedWeightsUpTo100.containsKey(criterion)) {
					normalizedWeightsUpTo100.put(criterion,
							customRound(normalizedWeights.get(criterion), decimalPlaces, false));
				}
				i++;
			}
			if (checkSum(normalizedWeightsUpTo100)<100){
				String criterion = sortedLminus.get(i);
				normalizedWeightsUpTo100.put(criterion,
						customRound(normalizedWeights.get(criterion), decimalPlaces, false));
			}	
		} else {
			Integer i = 0;
			Integer lastCriterion = v.intValue();
			while ((i < sortedLminus.size()) && (i <= lastCriterion)) {
				String criterion = sortedLminus.get(i);
				if (!ratioLplusGTLmins.contains(criterion)) {
					normalizedWeightsUpTo100.put(criterion,
							customRound(normalizedWeights.get(criterion), decimalPlaces, false));
				}else{
					lastCriterion++;
				}
				i++;
			}
			i = 0;
			while (i < normalizedWeights.size()) {
				String criterion = sortedLplus.get(i);
				if (!normalizedWeightsUpTo100.containsKey(criterion)) {
					normalizedWeightsUpTo100.put(criterion,
							customRound(normalizedWeights.get(criterion), decimalPlaces, true));
				}
				i++;
			}
			if (checkSum(normalizedWeightsUpTo100)>100){
				String criterion = sortedLminus.get(lastCriterion);
				normalizedWeightsUpTo100.put(criterion,
						customRound(normalizedWeights.get(criterion), decimalPlaces, true));
			}						
		}
		return normalizedWeightsUpTo100;
	}
	
	private static Double checkSum(Map<String, Double> sumUpTo100){
		Double sum=0.0;
		for (Double value: sumUpTo100.values()){
			sum+=value;
		}		
		return sum;
	}

	private static ArrayList<String> sortMapKeysByValue(Map<String, Double> map, Boolean ascending) {
		Map<String, Double> sortedMap = map.entrySet().stream().sorted(Entry.comparingByValue())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		ArrayList<String> sortedList = new ArrayList<String>(sortedMap.keySet());

		if (!ascending)
			Collections.reverse(sortedList);
		return sortedList;
	}
	
	private static Map<String, Double> sortMapByKey(Map<String, Double> map) {
		Map<String, Double> sortedMap = map.entrySet().stream().sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return sortedMap;
	}

	private static Double calcV(Map<String, Double> roundedWeights, Integer decimalPlaces) {
		Double v = 0.0;
		for (Double value : roundedWeights.values()) {
			v += value;
		}
		v = 100.0 - v;
		v *= Math.pow(10.0, decimalPlaces);
		return v;
	}

}
