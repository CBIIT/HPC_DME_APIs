/**
 * HpcDatasetDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.async.client.MongoCollection;

/**
 * <p>
 * HPC Dataset DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetDAOImpl implements HpcDatasetDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Dataset ID field name.
	public final static String DATASET_ID_FIELD_NAME = 
						       HpcCodec.DATASET_ID_KEY; 
	public final static String DATASET_NAME_FIELD_NAME = 
							   HpcCodec.DATASET_FILE_SET_KEY + "." + 
	                           HpcCodec.FILE_SET_NAME_KEY;
	public final static String PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME = 
                 HpcCodec.DATASET_FILE_SET_KEY + "." + 
                 HpcCodec.FILE_SET_FILES_KEY + "." + 
                 HpcCodec.FILE_METADATA_KEY + "." + 
                 HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
                 HpcCodec.FILE_PRIMARY_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY;
	public final static String CREATOR_NIH_USER_ID_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
	             HpcCodec.FILE_SET_FILES_KEY + "." + 
	             HpcCodec.FILE_METADATA_KEY + "." + 
	             HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
	             HpcCodec.FILE_PRIMARY_METADATA_CREATOR_NIH_USER_ID_KEY;
	public final static String REGISTRATOR_NIH_USER_ID_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
		         HpcCodec.FILE_SET_FILES_KEY + "." + 
		         HpcCodec.FILE_METADATA_KEY + "." + 
		         HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
		         HpcCodec.FILE_PRIMARY_METADATA_REGISTRATOR_NIH_USER_ID_KEY;
	
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
    private HpcDatasetDAOImpl() throws HpcException
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
    private HpcDatasetDAOImpl(HpcMongoDB mongoDB) throws HpcException
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
    // HpcManagedDatasetDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void add(HpcDataset dataset) throws HpcException
    {
		HpcSingleResultCallback<Void> callback = 
				                      new HpcSingleResultCallback<Void>();
		getCollection().insertOne(dataset, callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcDataset getDataset(String id) throws HpcException
	{
		HpcSingleResultCallback<HpcDataset> callback = 
                       new HpcSingleResultCallback<HpcDataset>();
		getCollection().find(eq(DATASET_ID_FIELD_NAME, id)).first(callback);
		
		return callback.getResult();
	}
	
	@Override
	public List<HpcDataset> getDatasets(String nihUserId, 
                                        HpcDatasetUserAssociation association) 
                                       throws HpcException
    {
		// Determine the field name needed to query for the requested association.
		String fieldName = null;
		switch(association) {
		       case CREATOR:
		            fieldName = CREATOR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       case PRIMARY_INVESTIGATOR:
		            fieldName = PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       case REGISTRATOR:
		            fieldName = REGISTRATOR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       default:
		    	   throw new HpcException("Invalid Association Value: " + 
		                                  association.value(), 
		                                  HpcErrorType.UNEXPECTED_ERROR);
		}
		
		// Invoke the query.
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(
		                eq(fieldName, nihUserId)).into(datasets, callback); 
		
		return callback.getResult();
    }
	
	@Override
	public List<HpcDataset> getDatasets(String name) throws HpcException
	{
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(
		                regex(DATASET_NAME_FIELD_NAME, 
		                	  name, "i")).into(datasets, callback); 
		
		return callback.getResult();
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the managed dataset Mongo collection.
     *
     * @return A The dataset Mongo collection.
     */
    private MongoCollection<HpcDataset> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcDataset.class);
    }  
}

 