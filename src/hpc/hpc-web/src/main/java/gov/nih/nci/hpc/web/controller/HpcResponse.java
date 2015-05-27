package gov.nih.nci.hpc.web.controller;

public class HpcResponse {

	private String code;
	private String message;
	
	public HpcResponse(String code, String message)
	{
		this.code = code;
		this.message = message;
	}
}
