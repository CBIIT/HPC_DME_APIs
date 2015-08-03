package gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

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
	    	InputStream stream = response.getBody();
			try {

				JAXBContext jaxbContext = JAXBContext.newInstance(HpcExceptionDTO.class);
				 
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				HpcExceptionDTO dto = (HpcExceptionDTO) jaxbUnmarshaller.unmarshal(stream);
				System.out.println(dto);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
	    	
	    }
	    
//	    @Override
//	    public void handleError(ClientHttpResponse response) throws HpcWebException {
	    //String body = IOUtils.toString(response.getBody());
//	    throw new HpcWebException(response.getRawStatusCode() + ":" + response.getBody().toString());
//	    }
}