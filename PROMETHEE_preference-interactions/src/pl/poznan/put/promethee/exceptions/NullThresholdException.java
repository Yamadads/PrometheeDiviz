package pl.poznan.put.promethee.exceptions;

public class NullThresholdException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -589872866780711505L;

	public NullThresholdException() {
		super("The threshold value needed to properly compute preference is null");
	}

	public NullThresholdException(String message) {
		super(message);
	}

	public NullThresholdException(Throwable cause) {
		super(cause);
	}

	public NullThresholdException(String message, Throwable cause) {
		super(message, cause);
	}

	public NullThresholdException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace); 
	}

}
