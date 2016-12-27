/**
 * HpcRestServiceTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.impl.HpcDataManagementAuthenticator;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.interceptor.HpcAPIVersionInterceptor;
import gov.nih.nci.hpc.ws.rs.provider.HpcExceptionMapper;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.message.Message;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * <p>
 * HPC REST Service Test base class.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:META-INF/spring/hpc-ws-rs-test-configuration.xml",
		                           "classpath:META-INF/spring/hpc-ws-rs-test-mock.xml"})
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)

public abstract class HpcRestServiceTest extends Assert
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
	private final static String ENDPOINT_ADDRESS = "local://hpc-server";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// JAX-RS server to deployed services under test.
	private static Server server = null;
	
	// JAX-RS providers / interceptors.
	@Autowired
	private HpcExceptionMapper exceptionMapper = null;
	
	@Autowired
	private HpcMultipartProvider multipartProvider = null;
	
	@Autowired
	private HpcAPIVersionInterceptor apiVersionInterceptor = null;
	
	@Autowired
	private HpcAuthenticationTestInterceptor authenticationInterceptor = null;
	
	@Autowired
	private JAXBElementProvider<?> jaxbProvider = null;
	
	@Autowired
	private JSONProvider<?> jsonProvider = null;
	
	// JAX-RS services.
	@Autowired 
	private HpcDataManagementRestService dataManagementRestService = null;
	
	// Beans with injected mocks.
	@Autowired
	@InjectMocks
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
	@Autowired
	@InjectMocks
    private HpcDataManagementService dataManagementService = null;
	
	// Mock Integration & DAO 
	@Mock
	protected HpcDataManagementProxy dataManagementProxyMock = null;
	
    // The logger instance.
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
	
    /**
     * Get a JAX-RS client of the data management service.
     * 
     * @return a client reference to the data management service.
     *
     */
	protected HpcDataManagementRestService getDataManagementClient()
	{
		List<Object> providers = new ArrayList<>();
        providers.add(jaxbProvider);
        providers.add(jsonProvider);
        
		return JAXRSClientFactory.create(ENDPOINT_ADDRESS, HpcDataManagementRestService.class, providers);
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Initialize the JAX-RS server.
     * @throws HpcException 
     */
	@Before
    public void initialize() throws HpcException 
    {
		// Initialize Mockito and the mock objects.
		initMockito();
		
		// Initialize the server once.
		if(server != null && server.isStarted()) {
		   return;
		}
		
		logger.info("Starting JAX-RS Test Server");
		
		// Initialize the JAX-RS server.
        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
        serverFactory.setAddress(ENDPOINT_ADDRESS);
        
        // Attach providers
        List<Object> providers = new ArrayList<>();
        providers.add(jaxbProvider);
        providers.add(jsonProvider);
        providers.add(exceptionMapper);
        providers.add(multipartProvider);
        serverFactory.setProviders(providers);
        
        // Attach in interceptors.
        List<Interceptor<? extends Message>> inInterceptors = new ArrayList<>();
        inInterceptors.add(authenticationInterceptor);
        serverFactory.setInInterceptors(inInterceptors);
        
        // Attach out interceptors.
        List<Interceptor<? extends Message>> outInterceptors = new ArrayList<>();
        outInterceptors.add(apiVersionInterceptor);
        serverFactory.setOutInterceptors(outInterceptors);
        
        // Set Rest Services.
        serverFactory.setResourceClasses(HpcDataManagementRestService.class);
        serverFactory.setResourceProvider(HpcDataManagementRestService.class,
                                          new SingletonResourceProvider(dataManagementRestService, true));
        
        server = serverFactory.create();
        
        logger.info("JAX-RS Test Server started");
    }
    
    /**
     * Destroy the JAX-RS server.
     */
    @AfterClass
    public static void destroy() throws Exception 
    {
       server.stop();
       server.destroy();
    }
    
    /**
     * Initialize Mockito and common mocked methods.
     * @throws HpcException 
     */
    private void initMockito() throws HpcException 
    {
    	MockitoAnnotations.initMocks(this);
    	
    	// Mock the IRODS authentication method.
    	when(dataManagementProxyMock.authenticate(anyObject(), anyBoolean())).thenReturn(new String("Authenticated"));
    }
}

 
