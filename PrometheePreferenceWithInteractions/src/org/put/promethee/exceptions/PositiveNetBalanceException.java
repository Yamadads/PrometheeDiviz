package org.put.promethee.exceptions;

public class PositiveNetBalanceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7500379416068600L;

	public PositiveNetBalanceException() {
		super("Positive net balance condition is not fulfilled");
	}

	public PositiveNetBalanceException(String message) {
		super("Positive net balance condition is not fulfilled by criterion " +message);
	}

	public PositiveNetBalanceException(Throwable cause) {
		super(cause);
	}

	public PositiveNetBalanceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PositiveNetBalanceException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
