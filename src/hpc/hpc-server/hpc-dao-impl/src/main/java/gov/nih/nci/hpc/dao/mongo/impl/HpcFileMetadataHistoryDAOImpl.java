/**
 * HpcFileMetadataHistoryDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import static com.mongodb.client.model.Filters.eq;
import gov.nih.nci.hpc.dao.HpcFileMetadataHistoryDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataHistory;
import gov.nih.nci.hpc.exception.HpcException;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

/**
 * <p>
 * HPC Dataset DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcFileMetadataHistoryDAOImpl implements HpcFileMetadataHistoryDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Field name to query by Dataset ID.
	public final static String FILE_ID_FIELD_NAME = 
						       HpcCodec.FILE_METADATA_HISTORY_FILE_ID_KEY; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// HpcMongoDB instance.
	private HpcMongoDB mongoDB = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcFileMetadataHistoryDAOImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param mongoDB HPC Mongo DB driver instance.
     * 
     * @throws HpcException If a HpcMongoDB instance was not provided.
     */
    private HpcFileMetadataHistoryDAOImpl(HpcMongoDB mongoDB) throws HpcException
    {
    	if(mongoDB == null) {
    	   throw new HpcException("Null HpcMongoDB instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.mongoDB = mongoDB;
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcFileMetadataHistoryDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsert(HpcFileMetadataHistory metadataHistory) throws HpcException
    {
		HpcSingleResultCallback<UpdateResult> callback = 
				                new HpcSingleResultCallback<UpdateResult>();
		getCollection().replaceOne(eq(FILE_ID_FIELD_NAME, metadataHistory.getFileId()), 
				                      metadataHistory, new UpdateOptions().upsert(true), 
				                      callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
		
	@Override
	public HpcFileMetadataHistory getFileMetadataHistory(String fileId) throws HpcException
	{
		HpcSingleResultCallback<HpcFileMetadataHistory> callback = 
                       new HpcSingleResultCallback<HpcFileMetadataHistory>();
		getCollection().find(eq(FILE_ID_FIELD_NAME, fileId)).first(callback);
		
		return callback.getResult();
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the file metadata history Mongo collection.
     *
     * @return A The file metadata history Mongo collection.
     */
    private MongoCollection<HpcFileMetadataHistory> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcFileMetadataHistory.class);
    }  
}

 