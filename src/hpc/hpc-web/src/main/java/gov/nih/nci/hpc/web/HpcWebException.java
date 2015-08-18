package gov.nih.nci.hpc.web;

public class HpcWebException extends RuntimeException {

	public HpcWebException()
	{
		super();
	}
	
	public HpcWebException(String message)
	{
		super(message);
	}

	public HpcWebException(String message, Throwable e)
	{
		super(message, e);
	}

	public HpcWebException(Throwable e)
	{
		super(e);
	}
}
