/**
 * HpcManagedDatasetBsonDocument.java
 *
 *  Copyright SVG, Inc.
 *  Copyright Leidos Biomedical Research, Inc
 * 
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Managed Dataset BSON Document. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetBsonDocument
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The BSON object id.
	private ObjectId objectId = new ObjectId();
	
	// The Metadata DTO instance.
	HpcDatasetsRegistrationInputDTO dto = new HpcDatasetsRegistrationInputDTO();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcManagedDatasetBsonDocument() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Properties getters/setters
    //---------------------------------------------------------------------//
    
    /**
     * Get the object id.
     *
     * @return The object id.
     */
    public ObjectId getObjectId()
    {
        return objectId;
    }
    
    /**
     * Set the Object id.
     *
     * @param objectId The object id.
     */
    public void setObjectId(ObjectId objectId)
    {
        this.objectId = objectId;
    }     
    
    /**
     * Get the metadata DTO.
     *
     * @return The metadata DTO.
     */
    public HpcDatasetsRegistrationInputDTO getDTO()
    {
        return dto;
    }
    
    /**
     * Set the metadata DTO.
     *
     * @param metadataDTO The metadata DTO.
     */
    public void setDTO(HpcDatasetsRegistrationInputDTO dto)
    {
        this.dto = dto;
    }      
}

 