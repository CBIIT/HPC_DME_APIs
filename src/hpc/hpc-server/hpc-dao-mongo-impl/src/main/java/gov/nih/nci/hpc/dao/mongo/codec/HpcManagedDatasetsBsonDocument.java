/**
 * HpcManagedDatasetsBsonDocument.java
 *
 *  Copyright SVG, Inc.
 *  Copyright Leidos Biomedical Research, Inc
 * 
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.HpcManagedDatasets;
import org.bson.types.ObjectId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Managed Datasets BSON Document. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetsBsonDocument
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	// The BSON object id.
	private ObjectId objectId = new ObjectId();
	
	// The Managed Datasets domain object.
	HpcManagedDatasets managedDatasets = new HpcManagedDatasets();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcManagedDatasetsBsonDocument() 
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
     * Get the managed datasets.
     *
     * @return The managed datasets domain object.
     */
    public HpcManagedDatasets getManagedDatasets()
    {
        return managedDatasets;
    }
    
    /**
     * Set the managed datasets.
     *
     * @param managedDatasets The managed datasets domain object.
     */
    public void setManagedDatasets(HpcManagedDatasets managedDatasets)
    {
        this.managedDatasets = managedDatasets;
    }      
}

 