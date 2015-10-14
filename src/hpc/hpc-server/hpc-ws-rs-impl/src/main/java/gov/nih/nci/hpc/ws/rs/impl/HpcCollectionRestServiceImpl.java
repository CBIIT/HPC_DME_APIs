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

    // The Project Registration Business Service instance.
	//@Autowired
    //private HpcProjectBusService projectBusService = null;
    
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
    public Response addCollection(final String path, 
    		                      final HpcCollectionRegistrationDTO collectionRegistrationDTO)
    {	
		logger.info("Invoking RS: PUT /collection" + path);
		
		IRODSAccessObjectFactory irodsAccessObjectFactory = null;
    	try {
	   		 // Account credentials.
	   		 IRODSAccount irodsAccount = 
	   		              IRODSAccount.instance("52.7.244.225", 1247, "rods", 
	   			         		                "irods", "/tempZone/home/rods", 
	   			    	    	                "tempZone", "dsnetresource");
	   		 
	   		 // iRODs factories.
	   		IRODSFileFactory irodsFileFactory = 
	   				          irodsFileSystem.getIRODSFileFactory(irodsAccount);
	   		irodsAccessObjectFactory = 
	   				                  irodsFileSystem.getIRODSAccessObjectFactory();
	   		CollectionAO collectionAO = irodsAccessObjectFactory.getCollectionAO(irodsAccount);
	   		 
	   		 // Create the directory in iRODs file system.
	   		 
	   		 //String decodedPath = DataUtils.buildDecodedPathFromURLPathInfo(
		     //	                                   path, retrieveEncoding());
	   		String calcPath = "/" + path;
			IRODSFile collectionFile = irodsFileFactory.instanceIRODSFile(calcPath);
			collectionFile.mkdirs();
			
			// Register the collection metadata.
			List<AvuData> avuDatas = new ArrayList<AvuData>();

			for (HpcMetadataEntry metadataEntry : collectionRegistrationDTO.getMetadataEntries()) {
				avuDatas.add(AvuData.instance(metadataEntry.getAttribute(),
						metadataEntry.getValue(), metadataEntry.getUnit()));
			}

			List<BulkAVUOperationResponse> bulkAVUOperationResponses = collectionAO
					.addBulkAVUMetadataToCollection(calcPath, avuDatas);
			
	   		
	   	} catch(JargonException e) {
	   	        return errorResponse(new HpcException("iRODs error: " + e.getMessage(),
		                                                  HpcErrorType.UNEXPECTED_ERROR, e));
	   	} finally {
	   		       irodsAccessObjectFactory.closeSessionAndEatExceptions();
	   		    
	   	}
    	
    	HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
    	HpcMetadataEntry e = new HpcMetadataEntry();
    	e.setAttribute("Attr");
    	e.setValue("Val");
    	e.setUnit("Uni");
    	dto.getMetadataEntries().add(e);
    	dto.getMetadataEntries().add(e);
	   	return okResponse(dto, false);
	}
    
    
}

 