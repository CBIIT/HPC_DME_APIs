package gov.nih.nci.hpc.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;


public class HpcResponseErrorHandler implements ResponseErrorHandler {
	  private static final Logger log = LoggerFactory.getLogger(HpcResponseErrorHandler.class);

	  @Override
	    public boolean hasError(ClientHttpResponse response) throws IOException {
	    boolean hasError = false;
	    int rawStatusCode = response.getRawStatusCode();if (rawStatusCode != 200){
	    hasError = true;
	    }
	    return hasError;
	    }

	    @Override
	    public void handleError(ClientHttpResponse response) throws IOException {
	    	log.error("Response error: {} {}", response.getStatusCode(), response.getStatusText());
	    }
	    
//	    @Override
//	    public void handleError(ClientHttpResponse response) throws HpcWebException {
	    //String body = IOUtils.toString(response.getBody());
//	    throw new HpcWebException(response.getRawStatusCode() + ":" + response.getBody().toString());
//	    }
}