package pl.poznan.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.Criteria;
import org.xmcda.Criterion;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.QualifiedValue;
import org.xmcda.QualifiedValues;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {	
	public static class Inputs {		
		public Integer technicalParam;			
		public List<String> criteria_ids;					
		public Map<String, Map<String, Map<String, Double>>> partialPreferences;
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
	 * @param xmcda
	 * @param errors
	 * @return Inputs
	 */
	protected static Inputs checkInputs(XMCDA xmcda, ProgramExecutionResult errors) {
		Inputs inputs = new Inputs();
		checkParameters(inputs, xmcda, errors);			
		checkPartialPreferences(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {		
		Integer technicalParam = null;
		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one list of parameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("List of parameters not found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 1) {
			errors.addError("Exactly one parameter is expected");
			return;
		}

		final ProgramParameter<?> prgParam2 = xmcda.programParametersList.get(0).get(0);

		if (!"technical_parameter".equals(prgParam2.name())) {
			errors.addError(String.format("Invalid parameter '%s'", prgParam2.id()));
			return;
		}

		if (prgParam2.getValues() == null || (prgParam2.getValues() != null && prgParam2.getValues().size() != 1)) {
			errors.addError("technical_parameter must have a single (Integer) value only");
			return;
		}

		try {			
			technicalParam = (Integer) prgParam2.getValues().get(0).getValue();
		} catch (Throwable throwable) {
			String err = "Invalid value for technical_parameter, it must be an Integer value";
			errors.addError(err);
			technicalParam = null;
		}
		inputs.technicalParam = technicalParam;
	}

	private static void checkPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("Partial preferences has not been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 1) {
			errors.addError("Exactly one list of partial preferences is expected");
			return;
		}
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		if (matrix.isEmpty()) {
			errors.addError("List of partial preferences is empty");
			return;
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
		extractCriteria(inputs, xmcda);				
		if (!criteriaExists(inputs, xmcda, xmcda_execution_results)) {
			return null;
		}
		extractPartialPreferences(inputs, xmcda, xmcda_execution_results);
		return inputs;
	}
	

	private static void extractCriteria(Inputs inputs, XMCDA xmcda) {
		List<String> criteria_ids = new ArrayList<>();
		Criteria criteria = (Criteria) xmcda.criteria;
		for (Criterion x_criterion : criteria)
			if (x_criterion.isActive())
				criteria_ids.add(x_criterion.id());
		inputs.criteria_ids = criteria_ids;
	}

	private static Boolean criteriaExists(Inputs inputs, XMCDA xmcda,
			ProgramExecutionResult xmcda_execution_results) {
		Boolean allExists = true;		
		if (inputs.criteria_ids.size() == 0) {
			xmcda_execution_results.addError("No active criteria in criteria.xml");
			allExists = false;
		}
		return allExists;
	}	

		private static void extractPartialPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		inputs.partialPreferences = new LinkedHashMap<>();
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> matrix = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);

		for (Alternative a : matrix.getRows()){
			for (Alternative b : matrix.getColumns()){
				if (matrix.containsKey(new Coord<Alternative,Alternative>(a,b))){
					if (!putPreferencesIntoMap(inputs, errors, matrix, a.id(), b.id())){
						return;
					}
				}
			}		
		}

	}

	private static boolean putPreferencesIntoMap(Inputs inputs, ProgramExecutionResult errors,
			AlternativesMatrix<Double> matrix, String a, String b) {
		inputs.partialPreferences.putIfAbsent(a, new LinkedHashMap<>());
		inputs.partialPreferences.get(a).putIfAbsent(b, new LinkedHashMap<>());
		Alternative alt1 = new Alternative(a);
		Alternative alt2 = new Alternative(b);
		Coord<Alternative, Alternative> coord = new Coord<Alternative, Alternative>(alt1, alt2);
		QualifiedValues<Double> values = matrix.getOrDefault(coord, null);
		if (values == null) {
			errors.addError("List of partial preferences does not contain value for pair of alternatives (" + a + "," + b + ")");
			return false;
		}
		if (values.size() != inputs.criteria_ids.size()) {
			errors.addError("List of partial preferences does not contain correct criteria list");
			return false;
		}
		for (QualifiedValue<Double> value : values) {
			if (inputs.criteria_ids.contains(value.id())) {
				if (inputs.partialPreferences.get(a).get(b).containsKey(value.id())) {
					errors.addError("Partial Preferences list contains duplicates of criteria");
					return false;
				} else {
					inputs.partialPreferences.get(a).get(b).put(value.id(), value.getValue().doubleValue());
				}

			} else {
				errors.addError("List of partial preferences contains unexpected criterion id " + value.id());
				return false;
			}
		}
		return true;
	}
}
