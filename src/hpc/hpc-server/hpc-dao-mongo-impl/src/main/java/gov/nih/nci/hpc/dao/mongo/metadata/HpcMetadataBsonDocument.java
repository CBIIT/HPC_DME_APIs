/**
 * HpcMetadataBsonDocument.java
 *
 *  Copyright SVG, Inc.
 *  Copyright Leidos Biomedical Research, Inc
 * 
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.metadata;

import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Metadata BSON Document. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataBsonDocument
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
	HpcMetadataDTO metadataDTO = new HpcMetadataDTO();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcMetadataBsonDocument() 
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
    public HpcMetadataDTO getMetadataDTO()
    {
        return metadataDTO;
    }
    
    /**
     * Set the metadata DTO.
     *
     * @param metadataDTO The metadata DTO.
     */
    public void setMetadataDTO(HpcMetadataDTO metadataDTO)
    {
        this.metadataDTO = metadataDTO;
    }      
}

 