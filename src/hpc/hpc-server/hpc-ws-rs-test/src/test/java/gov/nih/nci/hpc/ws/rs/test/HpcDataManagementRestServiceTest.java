/**
 * HpcDataManagementRestServiceTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.interceptor.HpcAPIVersionInterceptor;
import gov.nih.nci.hpc.ws.rs.provider.HpcExceptionMapper;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Message;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
 * HPC Collection REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcDataManagementRestServiceImpl.java 1623 2016-11-16 02:58:37Z rosenbergea $
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:META-INF/spring/hpc-ws-rs-test-configuration.xml",
		                           "classpath:META-INF/spring/hpc-ws-rs-test-mock.xml"})
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)

public class HpcDataManagementRestServiceTest extends Assert
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
	
	// Services under test.
	@Autowired 
	@InjectMocks
	private HpcDataManagementRestService dataManagementRestService = null;
	
	// Mock Integration & DAO 
	@Mock
	private HpcDataManagementProxy dataManagementProxyMock = null;
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	
    //---------------------------------------------------------------------//
    // Unit Tests
    //---------------------------------------------------------------------//
    
    @Test
    public void testRegistration() 
    {
    	logger.info("*** ERAN test 1 ***");
    }
    
    @Test
    public void testRegistration1() 
    {
    	logger.info("*** ERAN test 2 ***");
    }
    
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
	@Before
    public void initialize() throws Exception 
    {
		// Initialize the server once.
		if(server != null && server.isStarted()) {
		   return;
		}
		logger.info("*** ERAN server init ***");
		// Initialize the mock objects.
		MockitoAnnotations.initMocks(this);
		
		// Initialize the JAX-RS server.
        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
        serverFactory.setAddress(ENDPOINT_ADDRESS);
        
        // Attach providers
        List<Object> providers = new ArrayList<>();
        providers.add(exceptionMapper);
        providers.add(multipartProvider);
        serverFactory.setProviders(providers);
        
        // Attach interceptors.
        List<Interceptor<? extends Message>> outInterceptors = new ArrayList<>();
        outInterceptors.add(apiVersionInterceptor);
        serverFactory.setOutInterceptors(outInterceptors);
        
        // Set Rest Services.
        serverFactory.setResourceClasses(HpcDataManagementRestService.class);
        serverFactory.setResourceProvider(HpcDataManagementRestService.class,
                                          new SingletonResourceProvider(dataManagementRestService, true));
        
        server = serverFactory.create();
        
    }
    
    @AfterClass
    public static void destroy() throws Exception 
    {
       server.stop();
       server.destroy();
    }
}

 
