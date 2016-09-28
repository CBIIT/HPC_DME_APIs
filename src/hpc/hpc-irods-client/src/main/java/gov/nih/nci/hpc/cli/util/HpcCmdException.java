package gov.nih.nci.hpc.cli.util;

public class HpcCmdException extends RuntimeException {

	public HpcCmdException()
	{
		super();
	}
	
	public HpcCmdException(String message)
	{
		super(message);
	}

	public HpcCmdException(String message, Throwable e)
	{
		super(message, e);
	}

	public HpcCmdException(Throwable e)
	{
		super(e);
	}
}