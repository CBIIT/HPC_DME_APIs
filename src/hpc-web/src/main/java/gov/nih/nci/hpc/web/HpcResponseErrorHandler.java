package gov.nih.nci.hpc.web;

import java.io.IOException;
import java.io.InputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HpcResponseErrorHandler implements ResponseErrorHandler {
	private static final Logger log = LoggerFactory.getLogger(HpcResponseErrorHandler.class);

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		boolean hasError = false;
		int rawStatusCode = response.getRawStatusCode();
		if (rawStatusCode != 200) {
			hasError = true;
		}
		return hasError;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		InputStream stream = response.getBody();
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(HpcExceptionDTO.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			HpcExceptionDTO dto = (HpcExceptionDTO) jaxbUnmarshaller.unmarshal(stream);
			HpcWebException exception = new HpcWebException(
					"Error Code: " + dto.getErrorType() + " Reason: " + dto.getMessage());
			throw exception;
		} catch (JAXBException e) {
		}

	}

	// @Override
	// public void handleError(ClientHttpResponse response) throws
	// HpcWebException {
	// String body = IOUtils.toString(response.getBody());
	// throw new HpcWebException(response.getRawStatusCode() + ":" +
	// response.getBody().toString());
	// }
}