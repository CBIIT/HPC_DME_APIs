package gov.nih.nci.hpc.cli.util;

public class HpcBatchException extends RuntimeException {

	public HpcBatchException()
	{
		super();
	}
	
	public HpcBatchException(String message)
	{
		super(message);
	}

	public HpcBatchException(String message, Throwable e)
	{
		super(message, e);
	}

	public HpcBatchException(Throwable e)
	{
		super(e);
	}
}