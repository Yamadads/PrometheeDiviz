package pl.poznan.put.promethee.exceptions;

public class WrongPreferenceDirectionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3553747262446985964L;

	public WrongPreferenceDirectionException() {
		super("The preferenceDirection value is wrong");
	}

	public WrongPreferenceDirectionException(String message) {
		super(message);
	}

	public WrongPreferenceDirectionException(Throwable cause) {
		super(cause);
	}

	public WrongPreferenceDirectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public WrongPreferenceDirectionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
