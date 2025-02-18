package gov.nih.nci.hpc.test.common;

public class ErrorMessage {
    public String errorType;
    public String message;
    public String stackTrace;

    public String getErrorType() {
    	return errorType;
    }

    public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

    public String getMessage() {
    	return message;
    }

    public void setMessage(String message) {
    	this.message = message;
    }

    public String getStackTrace() {
    	return stackTrace ;
    }

    public void setStackTrace(String stackTrace) {
    	this.stackTrace = stackTrace;
    }
}