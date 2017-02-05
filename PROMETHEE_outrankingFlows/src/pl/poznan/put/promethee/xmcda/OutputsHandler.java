package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesValues;
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
		case "positive_flows":
			return "alternativesValues";
		case "negative_flows":
			return "alternativesValues";
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
		case "positive_flows":
			return "alternativesValues";
		case "negative_flows":
			return "alternativesValues";
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
	public static Map<String, XMCDA> convert(Map<String, Map<String, Double>> flows,
			ProgramExecutionResult executionResult) {
		final HashMap<String, XMCDA> x_results = new HashMap<>();
		for (String flowName : flows.keySet()) {
			XMCDA xmcda = new XMCDA();
			Map<String, Double> flow = flows.get(flowName);
			AlternativesValues<Double> result = new AlternativesValues<Double>();
			for (String alternativeID : flow.keySet()) {
				Double value = flow.get(alternativeID).doubleValue();
				Alternative alternative = new Alternative(alternativeID);
				result.put(alternative, value);
			}
			xmcda.alternativesValuesList.add(result);
			x_results.put(flowName, xmcda);
		}
		return x_results;
	}
}
