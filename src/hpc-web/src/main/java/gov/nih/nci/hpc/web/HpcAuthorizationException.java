package gov.nih.nci.hpc.web;

public class HpcAuthorizationException extends RuntimeException {

    public HpcAuthorizationException() {
        super();
    }

    public HpcAuthorizationException(String message) {
        super(message);
    }

    public HpcAuthorizationException(String message, Throwable e) {
        super(message, e);
    }

    public HpcAuthorizationException(Throwable e) {
        super(e);
    }
}
