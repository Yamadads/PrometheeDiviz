package pl.poznan.put.promethee.xmcda;

import org.xmcda.CriteriaValues;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class OutputsHandler {
	/**
	 * Returns the xmcda v3 tag for a given output
	 * 
	 * @param outputName
	 *            the output's name
	 * @return the associated XMCDA v2 tag
	 * @throws NullPointerException
	 *             if outputName is null
	 * @throws IllegalArgumentException
	 *             if outputName is not known
	 */
	public static final String xmcdaV3Tag(String outputName) {
		switch (outputName) {
		case "weights":
			return "criteriaValues";
		case "messages":
			return "programExecutionResult";
		default:
			throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
		}
	}

	/**
	 * Returns the xmcda v2 tag for a given output
	 * 
	 * @param outputName
	 *            the output's name
	 * @return the associated XMCDA v2 tag
	 * @throws NullPointerException
	 *             if outputName is null
	 * @throws IllegalArgumentException
	 *             if outputName is not known
	 */
	public static final String xmcdaV2Tag(String outputName) {
		switch (outputName) {
		case "weights":
			return "criteriaValues";
		case "messages":
			return "methodMessages";
		default:
			throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
		}
	}

	/**
	 * Converts the results of the computation step into XMCDA objects.
	 * 
	 * @param weights
	 * @param executionResult
	 * @return a map with keys being xmcda objects' names and values their
	 *         corresponding XMCDA object
	 */
	public static Map<String, XMCDA> convert(Map<String, Double> weights,
			ProgramExecutionResult executionResult) {
		final HashMap<String, XMCDA> x_results = new HashMap<>();
		XMCDA xmcda = new XMCDA();
		CriteriaValues<Double> result = new CriteriaValues<Double>();

		for (String criterionID : weights.keySet()) {			
				Double value = weights.get(criterionID).doubleValue();
				Criterion criterion = new Criterion(criterionID);
				result.put(criterion, value);							
		}
		xmcda.criteriaValuesList.add(result);
		x_results.put("weights", xmcda);

		return x_results;
	}
}
