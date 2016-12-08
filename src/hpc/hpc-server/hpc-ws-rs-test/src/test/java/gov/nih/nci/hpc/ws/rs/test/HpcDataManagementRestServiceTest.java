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

import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestServiceImpl;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>
 * HPC Collection REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcDataManagementRestServiceImpl.java 1623 2016-11-16 02:58:37Z rosenbergea $
 */

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:META-INF/spring/hpc-ws-rs-beans-configuration.xml"})
public class HpcDataManagementRestServiceTest extends Assert
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
	private final static String ENDPOINT_ADDRESS = "local://hpc-server";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	private static Server server;
	
	//@Autowired 
	//HpcDataManagementRestService dataManagementServiceImpl = null;
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    @Before
    public void initialize() throws Exception 
    {
//        JAXRSServerFactoryBean serverFactory = new JAXRSServerFactoryBean();
//        serverFactory.setResourceClasses(HpcDataManagementRestService.class);
//            
//        List<Object> providers = new ArrayList<Object>();
//        // add custom providers if any
//        serverFactory.setProviders(providers);
//        
//            
//        serverFactory.setResourceProvider(HpcDataManagementRestService.class,
//                               new SingletonResourceProvider(dataManagementServiceImpl, true));
//        serverFactory.setAddress(ENDPOINT_ADDRESS);
//    
//        server = serverFactory.create();
//        System.err.println("ERAN");
        
    }
    
    @AfterClass
    public static void destroy() throws Exception 
    {
       //server.stop();
       //server.destroy();
    }
    
    @Test
    public void test() 
    {
    	
    }
    
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
   
}

 
