package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

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
		case "preferences":
			return "alternativesMatrix";
		case "messages":
			return "programExecutionResult";
		default:
			throw new IllegalArgumentException(String.format("Unknown output name '%s'", outputName));
		}
	}

	/**
	 * Converts the results of the computation step into XMCDA objects.
	 * 
	 * @param alternativesValues
	 * @param executionResult
	 * @return a map with keys being xmcda objects' names and values their
	 *         corresponding XMCDA object
	 */
	public static Map<String, XMCDA> convert(Map<String, Map<String, Double>> alternativesMatrix,
			ProgramExecutionResult executionResult) {
		final HashMap<String, XMCDA> x_results = new HashMap<>();
		XMCDA xmcda = new XMCDA();
		AlternativesMatrix<Double> result = new AlternativesMatrix<Double>();

		for (String alternative1 : alternativesMatrix.keySet()) {
			for (String alternative2 : alternativesMatrix.get(alternative1).keySet()) {
				Double value = alternativesMatrix.get(alternative1).get(alternative2).doubleValue();
				Alternative alt1 = new Alternative(alternative1);
				Alternative alt2 = new Alternative(alternative2);
				Coord<Alternative, Alternative> coord = new Coord<Alternative, Alternative>(alt1, alt2);
				QualifiedValues<Double> values = new QualifiedValues<Double>(new QualifiedValue<Double>(value));
				result.put(coord, values);
			}
		}
		xmcda.alternativesMatricesList.add(result);
		x_results.put("preferences", xmcda);

		return x_results;
	}
}
