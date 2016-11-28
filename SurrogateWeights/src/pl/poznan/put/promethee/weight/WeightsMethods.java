package pl.poznan.put.promethee.weight;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WeightsMethods {
	Map<String, Method> methods;

	public WeightsMethods() {
		methods = new HashMap<>();
		methods.put("equal_weights", new EqualWeights());
		methods.put("rank_sum", new RankSum());
		methods.put("rank_reciprocal", new RankReciprocal());
		methods.put("rank_ordered_centroid", new RankOrderedCentroid());
	}

	/**
	 * @param ranking
	 * @param methodName
	 * @return
	 */
	public Map<String, Double> calculate(Map<String, Integer> ranking, String methodName) {
		return methods.get(methodName).calculate(ranking);
	}
}

abstract class Method {
	public abstract Map<String, Double> calculate(Map<String, Integer> ranking);
}

class EqualWeights extends Method {

	@Override
	public Map<String, Double> calculate(Map<String, Integer> ranking) {
		Map<String, Double> weights = new LinkedHashMap<>();
		Double result = 1.0 / ranking.size();
		for (String criterion : ranking.keySet()) {
			weights.put(criterion, result);
		}
		return weights;
	}
}

class RankSum extends Method {

	@Override
	public Map<String, Double> calculate(Map<String, Integer> ranking) {
		Map<String, Double> weights = new LinkedHashMap<>();
		Integer n = ranking.size();
		Integer d = n * (n + 1);
		for (String criterion : ranking.keySet()) {
			weights.put(criterion, (2 * (n.doubleValue() + 1 - ranking.get(criterion))) / d.doubleValue());
		}
		return weights;
	}
}

class RankReciprocal extends Method {

	@Override
	public Map<String, Double> calculate(Map<String, Integer> ranking) {
		Map<String, Double> weights = new LinkedHashMap<>();
		Double d = 0.0;
		for (Integer rank : ranking.values()) {
			d += 1 / rank.doubleValue();
		}
		for (String criterion : ranking.keySet()) {
			weights.put(criterion, (1.0 / ranking.get(criterion)) / d);
		}
		return weights;
	}
}

class RankOrderedCentroid extends Method {

	@Override
	public Map<String, Double> calculate(Map<String, Integer> ranking) {
		Map<String, Double> weights = new LinkedHashMap<>();
		Integer n = ranking.size();
		Double d = 0.0;
		for (Integer rank : ranking.values()) {
			d += 1.0 / rank;
		}
		Double result = (1.0 / n) * d;
		for (String criterion : ranking.keySet()) {
			weights.put(criterion, result);
		}
		return weights;
	}
}
