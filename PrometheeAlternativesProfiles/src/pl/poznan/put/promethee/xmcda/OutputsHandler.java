package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.Criterion;
import org.xmcda.PerformanceTable;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.utils.PerformanceTableCoord;

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
		case "alternatives_profiles":
			return "performanceTable";
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
		case "alternatives_profiles":
			return "performanceTable";
		case "messages":
			return "methodMessages";
		default:
			throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
		}
	}

	/**
	 * @param performanceTable
	 * @param executionResult
	 * @return a map with keys being xmcda objects' names and values their
	 *         corresponding XMCDA object
	 */
	public static Map<String, XMCDA> convert(Map<String, Map<String, Double>> performanceTable,
			ProgramExecutionResult executionResult) {
		final HashMap<String, XMCDA> x_results = new HashMap<>();
		XMCDA xmcda = new XMCDA();
		PerformanceTable<Double> result = new PerformanceTable<Double>();

		for (String alternative : performanceTable.keySet()) {
			for (String criterion : performanceTable.get(alternative).keySet()) {
				Double value = performanceTable.get(alternative).get(criterion).doubleValue();
				Alternative a = new Alternative(alternative);
				Criterion c = new Criterion(criterion);
				PerformanceTableCoord coord = new PerformanceTableCoord(a, c);
				QualifiedValues<Double> values = new QualifiedValues<Double>(new QualifiedValue<Double>(value));
				result.put(coord, values);
			}
		}
		xmcda.performanceTablesList.add(result);
		x_results.put("alternatives_profiles", xmcda);

		return x_results;
	}
}
