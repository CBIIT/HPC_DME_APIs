/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HpcResponseErrorHandler implements ResponseErrorHandler {
	private static final Logger log = LoggerFactory.getLogger(HpcResponseErrorHandler.class);

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		boolean hasError = false;
		int rawStatusCode = response.getRawStatusCode();
		if (rawStatusCode != 200 || rawStatusCode != 201) {
			hasError = true;
		}
		return hasError;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		String body = IOUtils.toString(response.getBody());

		if (response.getStatusCode().equals(HttpStatus.CREATED) || response.getStatusCode().equals(HttpStatus.OK))
			return;
		// InputStream stream = response.getBody();
		JsonObjectMapper mapper = new JsonObjectMapper();
		HpcExceptionDTO dto = mapper.readValue(body, HpcExceptionDTO.class);

		String message = "Failed to process record due to: " + dto.getMessage();
		if (dto.getErrorType() != null)
			message = message + ": Error Type:" + dto.getErrorType().value();
		if (dto.getRequestRejectReason() != null)
			message = message + ": Request reject reason: " + dto.getRequestRejectReason().value();
		if (dto.getStackTrace() != null)
			message = message + ": Stacktrace: " + dto.getStackTrace();

		HpcBatchException exception = new HpcBatchException(
				"Error Code: " + response.getStatusCode() + " Reason: " + message);
		throw exception;

	}
}
