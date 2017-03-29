package gov.nih.nci.hpc.web.controller;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;

public abstract class AbstractHpcController {
	@Value("${gov.nih.nci.hpc.ssl.cert}")
	protected String sslCertPath;
	@Value("${gov.nih.nci.hpc.ssl.cert.password}")
	protected String sslCertPassword;

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@ExceptionHandler({ Exception.class, java.net.ConnectException.class })
	public @ResponseBody HpcResponse handleUncaughtException(Exception ex, WebRequest request,
			HttpServletResponse response) {
		log.info("Converting Uncaught exception to RestResponse : " + ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return new HpcResponse("Error occurred", ex.toString());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public @ResponseBody HpcResponse handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request,
			HttpServletResponse response) {
		log.info("Converting IllegalArgumentException to RestResponse : " + ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return new HpcResponse("Error occurred", ex.toString());
	}
}