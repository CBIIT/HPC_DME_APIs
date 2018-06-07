package gov.nih.nci.hpc.archivescleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class CustomRestErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    System.out.println("Client error response occurred ... details to follow");
    System.out.println("Response Status Code : " + response.getRawStatusCode() +
      "/" + response.getStatusText());

    System.out.println("Response Headers (see below lines)");
    HttpHeaders responseHeaders = response.getHeaders();
    Set<String> keys = responseHeaders.keySet();
    for (String someKey : keys) {
      System.out.println("  <header, as name : value> " + someKey + " : " +
        responseHeaders.get(someKey) );
    }
    String bodyContent = readInputStream(response.getBody());
    System.out.println("Response Body: " + bodyContent);
  }


  @Override
  public boolean hasError(ClientHttpResponse response) {
    return false;
  }


  private String readInputStream(InputStream pInputStream) throws IOException {
    InputStreamReader isReader = new InputStreamReader(pInputStream);
    BufferedReader bufReader = new BufferedReader(isReader);
    return bufReader.lines().collect(Collectors.joining("\n"));
  }

}
