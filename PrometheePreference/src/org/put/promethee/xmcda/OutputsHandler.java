package org.put.promethee.xmcda;

import org.xmcda.AlternativesValues;
import org.xmcda.Alternative;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class OutputsHandler
{
	/**
	 * Returns the xmcda v3 tag for a given output
	 * @param outputName the output's name
	 * @return the associated XMCDA v2 tag
	 * @throws NullPointerException if outputName is null
	 * @throws IllegalArgumentException if outputName is not known
	 */
	public static final String xmcdaV3Tag(String outputName)
	{
		switch(outputName)
		{
			case "alternativesValues":
				return "alternativesValues";
			case "messages":
				return "programExecutionResult";
			default:
				throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
		}
	}

	/**
	 * Returns the xmcda v2 tag for a given output
	 * @param outputName the output's name
	 * @return the associated XMCDA v2 tag
	 * @throws NullPointerException if outputName is null
	 * @throws IllegalArgumentException if outputName is not known
	 */
	public static final String xmcdaV2Tag(String outputName)
	{
		switch(outputName)
		{
			case "alternativesValues":
				return "alternativesValues";
			case "messages":
				return "methodMessages";
			default:
				throw new IllegalArgumentException(String.format("Unknown output name '%s'",outputName));
		}
	}


	/**
	 * Converts the results of the computation step into XMCDA objects.
	 * @param alternativesValues
	 * @param executionResult
	 * @return a map with keys being xmcda objects' names and values their corresponding XMCDA object
	 */
	public static Map<String, XMCDA> convert(Map<String, Double> alternativesValues, ProgramExecutionResult executionResult)
	{
		final HashMap<String, XMCDA> x_results = new HashMap<>();

		/* alternativesValues */
		XMCDA x_weighted_sum = new XMCDA();
		AlternativesValues<Double> x_alternatives_values = new AlternativesValues<Double>();

		for (String alternative_id : alternativesValues.keySet())
			x_alternatives_values.put(new Alternative(alternative_id), alternativesValues.get(alternative_id));

		x_weighted_sum.alternativesValuesList.add(x_alternatives_values);

		x_results.put("alternativesValues", x_weighted_sum);

		return x_results;
	}
}
