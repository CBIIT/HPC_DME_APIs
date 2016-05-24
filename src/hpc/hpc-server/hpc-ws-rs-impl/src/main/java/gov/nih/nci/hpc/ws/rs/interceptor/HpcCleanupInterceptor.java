/**
 * HpcCleanupInterceptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import java.io.File;

import javax.ws.rs.core.Response;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Cleanup Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcCleanupInterceptor 
             extends AbstractPhaseInterceptor<Message> 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default Constructor.
     * 
     */
    private HpcCleanupInterceptor()
    {
    	super(Phase.SEND_ENDING);
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // AbstractPhaseInterceptor<Message> Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public void handleMessage(Message message) 
    {
    	// Close the connection to Data Management.
    	dataManagementBusService.closeConnection();
    	
    	// Clean up files returned by the data object download service.
    	OperationResourceInfo resourceInfo = message.getExchange().get(OperationResourceInfo.class);
    	if(resourceInfo.getClassResourceInfo().getServiceClass().equals(HpcDataManagementBusService.class) &&
    	   resourceInfo.getMethodToInvoke().getName().equals("downloadDataObject")) {
    		Response response = message.getExchange().get(Response.class);
    		File file = (File) response.getEntity();
    	}
    }
} 