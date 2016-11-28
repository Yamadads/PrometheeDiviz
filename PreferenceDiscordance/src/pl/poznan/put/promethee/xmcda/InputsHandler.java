package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {
	/**
	 * This class contains every element which are needed to compute the
	 * weighted sum. It is populated by
	 * {@link InputsHandler#checkAndExtractInputs(XMCDA, ProgramExecutionResult)}.
	 */
	public static class Inputs {
		public Map<String, Map<String, Double>> preferences;
		public Map<String, Map<String, Double>> discordances;
	}

	/**
	 *
	 * @param xmcda
	 * @param xmcda_exec_results
	 * @return
	 */
	static public Inputs checkAndExtractInputs(XMCDA xmcda, ProgramExecutionResult xmcda_exec_results) {
		Inputs inputsDict = checkInputs(xmcda, xmcda_exec_results);

		if (xmcda_exec_results.isError())
			return null;

		return extractInputs(inputsDict, xmcda, xmcda_exec_results);
	}

	/**
	 * Checks the inputs
	 *
	 * @param xmcda
	 * @param errors
	 * @return a map containing a key "operator" with the appropriate
	 *         {@link AggregationOperator operator}
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkPreferences(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("No preference table has been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 2) {
			errors.addError("Exactly two performance tables are expected");
			return;
		}
		if (xmcda.alternativesMatricesList.get(0).isEmpty()) {
			errors.addError("Preferences table is empty");
		}
		if (xmcda.alternativesMatricesList.get(1).isEmpty()) {
			errors.addError("Discordances table is empty");
		}
	}

	/**
	 *
	 * @param inputs
	 * @param xmcda
	 * @param xmcda_execution_results
	 * @return
	 */
	protected static Inputs extractInputs(Inputs inputs, XMCDA xmcda, ProgramExecutionResult xmcda_execution_results) {
		extractPreferences(inputs, xmcda, xmcda_execution_results);
		checkExtractedPreferences(inputs, xmcda_execution_results);
		return inputs;
	}

	@SuppressWarnings("unchecked")
	private static void extractPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		AlternativesMatrix<Double> preferences = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);
		inputs.preferences = new LinkedHashMap<String, Map<String, Double>>();

		for (Coord<Alternative, Alternative> coord : preferences.keySet()) {
			String x = coord.x.id();
			String y = coord.y.id();
			Double value = preferences.get(coord).get(0).getValue().doubleValue();
			inputs.preferences.putIfAbsent(x, new HashMap<>());
			inputs.preferences.get(x).put(y, value);
		}

		AlternativesMatrix<Double> discordances = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(1);
		inputs.discordances = new LinkedHashMap<String, Map<String, Double>>();

		for (Coord<Alternative, Alternative> coord : discordances.keySet()) {
			String x = coord.x.id();
			String y = coord.y.id();
			Double value = discordances.get(coord).get(0).getValue().doubleValue();
			inputs.discordances.putIfAbsent(x, new HashMap<>());
			inputs.discordances.get(x).put(y, value);
		}
	}

	private static void checkExtractedPreferences(Inputs inputs, ProgramExecutionResult errors) {
		if (inputs.preferences.size()==0){
			errors.addError("Preferences table is empty");
			return;
		}
		if (inputs.discordances.size()==0){
			errors.addError("Discordances table is empty");
			return;
		}
		checkIfMapContainsAllKeysFromBase(inputs.preferences, inputs.discordances, errors, "preferences",
				"discordances");
		checkIfMapContainsAllKeysFromBase(inputs.discordances, inputs.preferences, errors, "discordances",
				"preferences");		
	}

	private static void checkIfMapContainsAllKeysFromBase(Map<String, Map<String, Double>> baseMap,
			Map<String, Map<String, Double>> mapToCheck, ProgramExecutionResult errors, String baseMapName,
			String mapToCheckName) {
		for (String key : baseMap.keySet()) {
			if (mapToCheck.containsKey(key)) {
				for (String key2 : baseMap.get(key).keySet()) {
					if (!mapToCheck.get(key).containsKey(key2)) {
						errors.addError("Alternative " + key2 + " from " + baseMapName + " table doesn't exist in "
								+ mapToCheckName + " table");
						return;
					}
				}
			} else {
				errors.addError("Alternative " + key + " from " + baseMapName + " table doesn't exist in "
						+ mapToCheckName + " table");
				return;
			}
		}
	}
}
