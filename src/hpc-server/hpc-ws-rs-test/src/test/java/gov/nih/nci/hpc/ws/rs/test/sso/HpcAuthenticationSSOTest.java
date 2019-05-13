package gov.nih.nci.hpc.ws.rs.test.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.SamlHeaderOutInterceptor;
import org.apache.cxf.rt.security.SecurityConstants;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;

public class HpcAuthenticationSSOTest extends AbstractBusClientServerTestBase {
    public static final String PORT = "7738";

    @Test
    public void testValidBasicAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        WebClient wc = createWebClient(address, new BasicAuthHeaderOutInterceptor(), null);
        
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
            
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
          if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            fail(ex.getCause().getMessage());
          } else {
            fail(ex.getMessage());
          }
        }
        
    }

    @Test
    public void testInvalidBasicAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Basic invalid_basic");
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(401, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }

    @Test
    public void testValidTokenAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        String validToken = getValidToken();
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Bearer " + validToken);
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }

    @Test
    public void testInvalidTokenAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Bearer invalid_token");
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(401, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testValidSAMLAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        WebClient wc = createWebClient(address, new SamlHeaderOutInterceptor(), null, true);
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testInvalidSAMLAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "SAML invalid_grant");
        Response r = wc.get();
        assertEquals(401, r.getStatus());
    }
    
    @Test
    public void testIdpInvalidSAMLAuth() {
        String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
        
        WebClient wc = 
            createWebClientForExistingToken(address, new SamlHeaderOutInterceptor(), null);
        
        try {
            Response response = wc.get(Response.class);
            assertEquals(401, response.getStatus()); // Token is currently expired.
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }

    @Test
    public void testValidBasicApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        WebClient wc = createWebClient(address, new BasicAuthHeaderOutInterceptor(), null);
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testInvalidBasicApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Basic invalid_basic");
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(401, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }

    @Test
    public void testValidTokenApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        String validToken = getValidToken();
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Bearer " + validToken);
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }

    @Test
    public void testInvalidTokenApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "Bearer invalid_token");
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(401, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testValidSAMLApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        WebClient wc = createWebClient(address, new SamlHeaderOutInterceptor(), null, true);
        
        try {
          Response response = wc.get(Response.class);
            assertEquals(200, response.getStatus());
        } catch (WebApplicationException ex) {
            fail(ex.getMessage());
        } catch (ProcessingException ex) {
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                fail(ex.getCause().getMessage());
            } else {
                fail(ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testInvalidSAMLApi() {
        String address = "https://localhost:" + PORT + "/hpc-server/dataObject/TEST_NO_HIER_Archive/Sample_Collection_Yuri/test-archive/TEST0001.tar";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.header("Authorization", "SAML invalid_grant");
        Response r = wc.get();
        assertEquals(401, r.getStatus());
    }
    
    private WebClient createWebClient(String address, 
                                      Interceptor<Message> outInterceptor,
                                      Object provider,
                                      boolean selfSign) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.CALLBACK_HANDLER, 
                       "gov.nih.nci.hpc.ws.rs.test.sso.KeystorePasswordCallback");
        properties.put(SecurityConstants.SAML_CALLBACK_HANDLER, 
                       "gov.nih.nci.hpc.ws.rs.test.sso.SamlCallbackHandler");
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "alice");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, 
                       "src/test/resources/alice.properties");
        if (selfSign) {
            properties.put("security.self-sign-saml-assertion", "true");
        }
        bean.setProperties(properties);
        
        bean.getOutInterceptors().add(outInterceptor);
        if (provider != null) {
            bean.setProvider(provider);
        }
        return bean.createWebClient();
    }
    
    private WebClient createWebClientForExistingToken(String address, 
                                      Interceptor<Message> outInterceptor,
                                      Object provider) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        bean.getOutInterceptors().add(outInterceptor);
        bean.getOutInterceptors().add(new SamlRetrievalInterceptor());
        if (provider != null) {
            bean.setProvider(provider);
        }
        return bean.createWebClient();
    }
    
    private WebClient createWebClient(String address, 
        Interceptor<Message> outInterceptor,
        Object provider) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = HpcAuthenticationSSOTest.class.getClassLoader().getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        
        bean.getOutInterceptors().add(outInterceptor);
        if (provider != null) {
        bean.setProvider(provider);
        }
        return bean.createWebClient();
        }

    private String getValidToken() {
      JsonParser parser = null;
      String address = "https://localhost:" + PORT + "/hpc-server/authenticate";
      
      WebClient wc = createWebClient(address, new BasicAuthHeaderOutInterceptor(), null);
      
      
      try {
        Response response = wc.get(Response.class);
          MappingJsonFactory factory = new MappingJsonFactory();
          parser = factory.createParser((InputStream) response.getEntity());
          HpcAuthenticationResponseDTO authResponse = parser.readValueAs(HpcAuthenticationResponseDTO.class);
          return authResponse.getToken();
      } catch (WebApplicationException ex) {
          fail(ex.getMessage());
      } catch (ProcessingException | IOException ex) {
          if (ex.getCause() != null && ex.getCause().getMessage() != null) {
              fail(ex.getCause().getMessage());
          } else {
              fail(ex.getMessage());
          }
      } finally {
        try {
          if(parser != null)
            parser.close();
        } catch (IOException e) {
          //silent exception
        }
      }
      return null;
      
  }

}
