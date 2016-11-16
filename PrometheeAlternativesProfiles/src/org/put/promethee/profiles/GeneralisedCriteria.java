package org.put.promethee.profiles;

import java.util.HashMap;
import java.util.Map;

import org.put.promethee.exceptions.NullThresholdException;

public class GeneralisedCriteria {
	Map<Integer, GeneralisedCriterion> generalisedCriteria;

	public GeneralisedCriteria() {
		generalisedCriteria = new HashMap<>();
		generalisedCriteria.put(1, new UsualCriterion());
		generalisedCriteria.put(2, new UShapeCriterion());
		generalisedCriteria.put(3, new VShapeCriterion());
		generalisedCriteria.put(4, new LevelCriterion());
		generalisedCriteria.put(5, new VShapeWithIndifferenceCriterion());
		generalisedCriteria.put(6, new GaussianCriterion());
	}

	/**
	 * @param functionNumber
	 * @param differenceBetweenEvaluations
	 * @param p preferenceThreshold
	 * @param q indefferenceThreshold
	 * @param s sigmaThreshold
	 * @return
	 * @throws NullThresholdException
	 */
	public Double calculate(Integer functionNumber, Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		return generalisedCriteria.get(functionNumber).calculate(differenceBetweenEvaluations, p, q, s);
	}
}

abstract class GeneralisedCriterion {
	public abstract Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException;
}

class UsualCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s) {
		if (differenceBetweenEvaluations <= 0) {
			return 0.0;
		} else {
			return 1.0;
		}
	}
}

class UShapeCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		if (q == null)
			throw new NullThresholdException();
		if (differenceBetweenEvaluations <= q) {
			return 0.0;
		} else {
			return 1.0;
		}
	}
}

class VShapeCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		if (p == null)
			throw new NullThresholdException();
		if (differenceBetweenEvaluations <= 0) {
			return 0.0;
		}
		if (differenceBetweenEvaluations > p) {
			return 1.0;
		}
		return differenceBetweenEvaluations / p;
	}
}

class LevelCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		if ((p == null) || (q == null)) {
			throw new NullThresholdException();
		}
		if (differenceBetweenEvaluations <= q) {
			return 0.0;
		}
		if (differenceBetweenEvaluations > p) {
			return 1.0;
		}
		return 0.5;
	}
}

class VShapeWithIndifferenceCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		if ((p == null) || (q == null)) {
			throw new NullThresholdException();
		}
		if (differenceBetweenEvaluations <= q) {
			return 0.0;
		}
		if (differenceBetweenEvaluations > p) {
			return 1.0;
		}
		return (differenceBetweenEvaluations - q) / (p - q);
	}
}

class GaussianCriterion extends GeneralisedCriterion {

	@Override
	public Double calculate(Double differenceBetweenEvaluations, Double p, Double q, Double s)
			throws NullThresholdException {
		if (s == null)
			throw new NullThresholdException();
		if (differenceBetweenEvaluations <= 0) {
			return 0.0;
		}
		return 1 - Math.exp(-(differenceBetweenEvaluations * differenceBetweenEvaluations / 2 * s * s));
	}
}