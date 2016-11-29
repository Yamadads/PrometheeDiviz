package pl.poznan.put.promethee.exceptions;

public class InvalidZFunctionParamException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3122525551645248966L;

	public InvalidZFunctionParamException() {
		super("ZFUnction param is invlid");
	}

	public InvalidZFunctionParamException(String message) {
		super(message);
	}

	public InvalidZFunctionParamException(Throwable cause) {
		super(cause);
	}

	public InvalidZFunctionParamException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidZFunctionParamException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
