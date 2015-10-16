/**
 * HpcCollectionRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.hpc.bus.HpcCollectionBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.collection.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcCollectionRestService;

import javax.ws.rs.core.Response;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.BulkAVUOperationResponse;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.irods.jargon.core.pub.domain.AvuData;

/**
 * <p>
 * HPC Collection REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcCollectionRestServiceImpl extends HpcRestServiceImpl
             implements HpcCollectionRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Collection Business Service instance.
	@Autowired
    private HpcCollectionBusService collectionBusService = null;
    
    // The URI Info context instance.
    //private @Context UriInfo uriInfo;
	
	// IRODS file system.
	private IRODSFileSystem irodsFileSystem = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcCollectionRestServiceImpl() throws HpcException
    {
		try {
		     this.irodsFileSystem = IRODSFileSystem.instance();
		     
		} catch(JargonException e) {
			    throw new HpcException("Failed to instantiate iRODs file system",
	                                   HpcErrorType.UNEXPECTED_ERROR, e);
		}
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcCollectionRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response addCollection(
    		           String path, 
    		           HpcCollectionRegistrationDTO collectionRegistrationDTO)
    {	
    	path = toAbsolutePath(path);
		logger.info("Invoking RS: PUT /collection" + path);
		
		String collectionId = null;
		try {
			 collectionId = collectionBusService.registerCollection(
					                                     path,
					                                     collectionRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /collection" + path + " failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(collectionId);
	}
}

 