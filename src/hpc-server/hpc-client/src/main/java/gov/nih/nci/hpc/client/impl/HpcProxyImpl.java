/**
 * HpcProxyImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.client.impl;

import gov.nih.nci.hpc.client.HpcProxy;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.HpcSecurityRestService;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

/**
 * <p>
 * HPC Proxy Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcProxyImpl implements HpcProxy
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Registration Proxy instance.
    private String baseAddress = null;
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor.
     * 
     */
    private HpcProxyImpl()
    {
    	throw new RuntimeException("Constructor Disabled");
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param baseAddress The service base address.
     */
    private HpcProxyImpl(String baseAddress)
    {
    	if(baseAddress == null || baseAddress.isEmpty()) {
    	   throw new RuntimeException("Null or empty base address");
    	}
    	
    	this.baseAddress = baseAddress;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcProxy Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public HpcSecurityRestService getUserRestServiceProxy()
    {
    	return JAXRSClientFactory.create(baseAddress, 
    			                         HpcSecurityRestService.class);
	}
    
    @Override
    public HpcDataManagementRestService getDataManagementRestServiceProxy()
    {
    	return JAXRSClientFactory.create(baseAddress, 
    			                         HpcDataManagementRestService.class);
	}
}

 