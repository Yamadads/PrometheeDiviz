package org.put.promethee.exceptions;

public class WrongParamValue extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3553747262446985964L;

	public WrongParamValue() {
		super("The preferenceDirection value is wrong");
	}

	public WrongParamValue(String message) {
		super(message);
	}

	public WrongParamValue(Throwable cause) {
		super(cause);
	}

	public WrongParamValue(String message, Throwable cause) {
		super(message, cause);
	}

	public WrongParamValue(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
