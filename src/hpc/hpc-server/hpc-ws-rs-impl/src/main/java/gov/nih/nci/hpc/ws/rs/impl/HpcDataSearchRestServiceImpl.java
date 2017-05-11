/**
 * HpcDataSearchRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataSearchRestService;

import java.net.URLDecoder;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Search REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataSearchRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataSearchBusService dataSearchBusService = null;
	
	@Autowired
    private HpcSystemBusService systemBusService = null;
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataSearchRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQuery)
    {
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(compoundMetadataQuery);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response queryCollections(String queryName, Boolean detailedResponse, Integer page, 
    		                         Boolean totalCount)
    {
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(queryName, detailedResponse, 
					                                           page, totalCount);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response queryDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQuery)
    {
    	HpcDataObjectListDTO dataObjects = null;
		try {
			 dataObjects = dataSearchBusService.getDataObjects(compoundMetadataQuery);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(!dataObjects.getDataObjects().isEmpty() ||
				          !dataObjects.getDataObjectPaths().isEmpty() ? dataObjects : null, true);
    }
    
    @Override
    public Response queryDataObjects(String queryName, Boolean detailedResponse, Integer page,
    		                         Boolean totalCount)
    {
    	HpcDataObjectListDTO dataObjects = null;
		try {
			 dataObjects = dataSearchBusService.getDataObjects(queryName, detailedResponse,
					                                           page, totalCount);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(!dataObjects.getDataObjects().isEmpty() ||
				          !dataObjects.getDataObjectPaths().isEmpty() ? dataObjects : null, true);
    }
    
    @Override
    public Response addQuery(String queryName,
    		                 HpcCompoundMetadataQueryDTO compoundMetadataQuery)
    {
		try {
			 dataSearchBusService.addQuery(URLDecoder.decode(queryName), compoundMetadataQuery);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return createdResponse(null);
    }
    
    @Override
    public Response updateQuery(String queryName,
    		                    HpcCompoundMetadataQueryDTO compoundMetadataQuery)
    {
		try {
			 dataSearchBusService.updateQuery(URLDecoder.decode(queryName), compoundMetadataQuery);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(null, false);
    }
    
    @Override
    public Response deleteQuery(String queryName)
    {
		try {
			 dataSearchBusService.deleteQuery(URLDecoder.decode(queryName));
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(null, false);
    }
    
    @Override
    public Response getQuery(String queryName)
    {
    	HpcNamedCompoundMetadataQueryDTO query = null;
		try {
			 query = dataSearchBusService.getQuery(URLDecoder.decode(queryName));
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(query.getNamedCompoundQuery() != null ? query : null, true);
    }    

    @Override
    public Response getQueries()
    {
    	HpcNamedCompoundMetadataQueryListDTO queries = null;
		try {
			 queries = dataSearchBusService.getQueries();
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(!queries.getNamedCompoundQueries().isEmpty() ? queries : null, true);
    }
    
    @Override
    public Response getMetadataAttributes(String levelLabel)
    {
    	HpcMetadataAttributesListDTO metadataAttributes = null;
		try {
		     metadataAttributes = dataSearchBusService.getMetadataAttributes(levelLabel);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return  okResponse(!metadataAttributes.getCollectionMetadataAttributes().isEmpty() || 
				           !metadataAttributes.getDataObjectMetadataAttributes().isEmpty() ? 
				           metadataAttributes : null, true);
    }
    
    @Override
    public Response refreshMetadataViews()
    {
		try {
		     systemBusService.refreshMetadataViews();
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
    }
}

 
