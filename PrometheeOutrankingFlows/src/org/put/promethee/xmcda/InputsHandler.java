package org.put.promethee.xmcda;

import org.xmcda.Alternative;
import org.xmcda.AlternativesMatrix;
import org.xmcda.CategoryProfile;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.ProgramParameter;
import org.xmcda.XMCDA;
import org.xmcda.utils.Coord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InputsHandler {
	public enum ComparisonWithParam {
		ALTERNATIVES("alternatives"), BOUNDARY_PROFILES("boundary_profiles"), CENTRAL_PROFILES("central_profiles");

		private String label;

		private ComparisonWithParam(String paramLabel) {
			label = paramLabel;
		}

		/**
		 * Return the label for this ComparisonWith Parameter
		 *
		 * @return the parameter's label
		 */
		public final String getLabel() {
			return label;
		}

		/**
		 * Returns the parameter's label
		 *
		 * @return the parameter's label
		 */
		@Override
		public String toString() {
			return label;
		}

		/**
		 * Returns the {@link ComparisonWithParam} with the specified label. It
		 * behaves like {@link #valueOf(String)} with the exception
		 *
		 * @param parameterLabel
		 *            the label of the constant to return
		 * @return the enum constant with the specified label
		 * @throws IllegalArgumentException
		 *             if there is no ComparisonWithParam with this label
		 * @throws NullPointerException
		 *             if parameterLabel is null
		 */
		public static ComparisonWithParam fromString(String parameterLabel) {
			if (parameterLabel == null)
				throw new NullPointerException("parameterLabel is null");
			for (ComparisonWithParam op : ComparisonWithParam.values()) {
				if (op.toString().equals(parameterLabel))
					return op;
			}
			throw new IllegalArgumentException("No enum ComparisonWithParam with label " + parameterLabel);
		}
	}

	/**
	 * This class contains every element which are needed to compute the
	 * weighted sum. It is populated by
	 * {@link InputsHandler#checkAndExtractInputs(XMCDA, ProgramExecutionResult)}.
	 */
	public static class Inputs {
		public ComparisonWithParam comparisonWith;
		public List<String> alternatives_ids;
		public List<String> profiles_ids;
		public Map<String, Map<String, Double>> preferences;
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
		checkParameters(inputs, xmcda, errors);
		checkAlternatives(inputs, xmcda, errors);
		checkPreferences(inputs, xmcda, errors);
		checkCategoriesProfiles(inputs, xmcda, errors);
		return inputs;
	}

	private static void checkParameters(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		ComparisonWithParam comparisonWith = null;

		if (xmcda.programParametersList.size() > 1) {
			errors.addError("Only one programParameters is expected");
			return;
		}
		if (xmcda.programParametersList.size() == 0) {
			errors.addError("No programParameter found");
			return;
		}
		if (xmcda.programParametersList.get(0).size() != 1) {
			errors.addError("Exactly one programParameters are expected");
			return;
		}

		final ProgramParameter<?> prgParam = xmcda.programParametersList.get(0).get(0);

		if (!"comparison_with".equals(prgParam.name())) {
			errors.addError(String.format("Invalid parameter w/ id '%s'", prgParam.id()));
			return;
		}

		if (prgParam.getValues() == null || (prgParam.getValues() != null && prgParam.getValues().size() != 1)) {
			errors.addError("Parameter operator must have a single (label) value only");
			return;
		}

		try {
			final String parameterValue = (String) prgParam.getValues().get(0).getValue();
			comparisonWith = ComparisonWithParam.fromString((String) parameterValue);
		} catch (Throwable throwable) {
			StringBuffer valid_values = new StringBuffer();
			for (ComparisonWithParam op : ComparisonWithParam.values()) {
				valid_values.append(op.getLabel()).append(", ");
			}
			String err = "Invalid value for parameter operator, it must be a label, ";
			err += "possible values are: " + valid_values.substring(0, valid_values.length() - 2);
			errors.addError(err);
			comparisonWith = null;
		}
		inputs.comparisonWith = comparisonWith;
	}

	private static void checkAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternatives.size() == 0) {
			errors.addError("No alternatives found");
			return;
		}
		if (xmcda.alternatives.getActiveAlternatives().size() == 0) {
			errors.addError("No active alternatives found");
			return;
		}
	}

	private static void checkPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (xmcda.alternativesMatricesList.size() == 0) {
			errors.addError("No preference table has been supplied");
			return;
		}
		if (xmcda.alternativesMatricesList.size() != 1) {
			errors.addError("Exactly one performance table is expected");
			return;
		}
		if (xmcda.alternativesMatricesList.get(0).isEmpty()) {
			errors.addError("Preferences table is empty");
		}
	}

	private static void checkCategoriesProfiles(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			if (xmcda.categoriesProfilesList.size() == 0) {
				errors.addError("No preference table has been supplied");
				return;
			}
			if (xmcda.categoriesProfilesList.size() != 1) {
				errors.addError("Exactly one performance table is expected");
				return;
			}
			if (xmcda.categoriesProfilesList.get(0).isEmpty()) {
				errors.addError("Categories Profiles list is empty");
				return;
			}
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
		extractProfiles(inputs, xmcda, xmcda_execution_results);
		extractAlternatives(inputs, xmcda, xmcda_execution_results);
		extractPreferences(inputs, xmcda, xmcda_execution_results);
		checkExtractedPreferences(inputs, xmcda_execution_results);
		return inputs;
	}

	@SuppressWarnings("rawtypes")
	private static void extractProfiles(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		if (inputs.comparisonWith != ComparisonWithParam.ALTERNATIVES) {
			inputs.profiles_ids = new ArrayList<String>();
			for (CategoryProfile catProf : xmcda.categoriesProfilesList.get(0)) {
				if (inputs.comparisonWith == ComparisonWithParam.BOUNDARY_PROFILES) {
					if (catProf.getLowerBound() != null) {
						if (catProf.getLowerBound().getAlternative() != null) {
							inputs.profiles_ids.add(catProf.getLowerBound().getAlternative().id());
						} else {
							errors.addError("Alternative in Category Profile must be specified");
							return;
						}
					} else {
						errors.addError("Lower Bound Profile in categories profiles must be specified");
						return;
					}

					if (catProf.getUpperBound() != null) {
						if (catProf.getUpperBound().getAlternative() != null) {
							inputs.profiles_ids.add(catProf.getUpperBound().getAlternative().id());
						} else {
							errors.addError("Alternative in Category Profile must be specified");
							return;
						}
					} else {
						errors.addError("Upper Bound Profile in categories profiles must be specified");
						return;
					}
				}
				if (inputs.comparisonWith == ComparisonWithParam.CENTRAL_PROFILES) {
					if (catProf.getCentralProfile() != null) {
						if (catProf.getCentralProfile().getAlternative() != null) {
							inputs.profiles_ids.add(catProf.getCentralProfile().getAlternative().id());
						} else {
							errors.addError("Alternative in Category Profile must be specified");
							return;
						}
					} else {
						errors.addError("Central Profile in categories profiles must be specified");
						return;
					}
				}
			}
			if (inputs.profiles_ids.isEmpty()) {
				errors.addError("Profiles IDs is empty");
			}
		}
	}

	private static void extractAlternatives(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		List<String> alternatives_ids = new ArrayList<>();
		for (Alternative alternative : xmcda.alternatives) {
			if (alternative.isActive()) {
				alternatives_ids.add(alternative.id());
			}
		}
		if (inputs.profiles_ids != null) {
			for (String profile : inputs.profiles_ids) {
				inputs.alternatives_ids.remove(profile);
			}
		}
		inputs.alternatives_ids = alternatives_ids;
		if (alternatives_ids.isEmpty()) {
			errors.addError("Profiles IDs is empty");
		}
	}

	private static void extractPreferences(Inputs inputs, XMCDA xmcda, ProgramExecutionResult errors) {
		@SuppressWarnings("unchecked")
		AlternativesMatrix<Double> preferences = (AlternativesMatrix<Double>) xmcda.alternativesMatricesList.get(0);
		inputs.preferences = new LinkedHashMap<String, Map<String, Double>>();

		for (Coord<Alternative, Alternative> coord : preferences.keySet()) {
			String x = coord.x.id();
			String y = coord.y.id();
			Double value = preferences.get(coord).get(0).getValue().doubleValue();
			inputs.preferences.putIfAbsent(x, new HashMap<>());
			inputs.preferences.get(x).put(y, value);
		}
	}

	private static void checkExtractedPreferences(Inputs inputs, ProgramExecutionResult errors) {
		if ((inputs.alternatives_ids != null) && (inputs.preferences != null)) {
			if (inputs.comparisonWith == ComparisonWithParam.ALTERNATIVES) {
				for (String alternative : inputs.alternatives_ids) {
					if (inputs.preferences.containsKey(alternative)) {
						for (String alternative2 : inputs.alternatives_ids) {
							if (alternative != alternative2) {
								if (!inputs.preferences.get(alternative).containsKey(alternative2)) {
									errors.addError("In preferences table doesn't exist alternative: " + alternative2);
									return;
								}
							}
						}
					} else {
						errors.addError("In preferences table doesn't exist alternative: " + alternative);
						return;
					}
				}
			} else {
				if (inputs.preferences != null) {
					for (String profile : inputs.profiles_ids) {
						if (inputs.preferences.containsKey(profile)) {
							for (String profile2 : inputs.profiles_ids) {
								if (profile != profile2) {
									if (!inputs.preferences.get(profile).containsKey(profile2)) {
										errors.addError("In preferences table doesn't exist profile: " + profile2);
										return;
									}
								}
							}
						} else {
							errors.addError("In preferences table doesn't exist profile: " + profile);
							return;
						}
					}
					for (String alternative : inputs.alternatives_ids) {
						if (inputs.preferences.containsKey(alternative)) {
							for (String profile : inputs.profiles_ids) {
								if (!inputs.preferences.get(alternative).containsKey(profile)) {
									errors.addError("In preferences table doesn't exist profile: " + profile);
									return;
								}
							}
						} else {
							errors.addError("In preferences table doesn't exist alternative: " + alternative);
							return;
						}
					}
					for (String profile : inputs.profiles_ids) {
						if (inputs.preferences.containsKey(profile)) {
							for (String alternative : inputs.alternatives_ids) {
								if (!inputs.preferences.get(profile).containsKey(alternative)) {
									errors.addError("In preferences table doesn't exist alternative: " + alternative);
									return;
								}
							}
						} else {
							errors.addError("In preferences table doesn't exist profile: " + profile);
							return;
						}
					}
				}
			}
		}
	}
}
