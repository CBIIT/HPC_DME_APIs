/**
 * HpcManagedDatasetDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import gov.nih.nci.hpc.dao.HpcManagedDatasetDAO;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;

import gov.nih.nci.hpc.exception.HpcException;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;

import com.mongodb.async.client.MongoCollection;
import static com.mongodb.client.model.Filters.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * <p>
 * HPC Managed Dataset DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetDAOImpl implements HpcManagedDatasetDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Dataset ID field name.
	public final static String DATASET_ID_FIELD_NAME = 
			                   HpcCodec.MANAGED_DATASET_DATASET_KEY + "." + 
	                           HpcCodec.DATASET_ID_KEY; 
	public final static String PRIMARY_INVESTIGATOR_ID_FIELD_NAME = 
                               HpcCodec.MANAGED_DATASET_DATASET_KEY + "." + 
                               HpcCodec.DATASET_PRIMARY_INVESTIGATOR_ID_KEY; 
	public final static String CREATOR_ID_FIELD_NAME = 
                               HpcCodec.MANAGED_DATASET_DATASET_KEY + "." + 
                               HpcCodec.DATASET_CREATOR_ID_KEY; 
	public final static String REGISTRATOR_ID_FIELD_NAME = 
                               HpcCodec.MANAGED_DATASET_DATASET_KEY + "." + 
                               HpcCodec.DATASET_REGISTRATOR_ID_KEY; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
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
    private HpcManagedDatasetDAOImpl() throws HpcException
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
    private HpcManagedDatasetDAOImpl(HpcMongoDB mongoDB) throws HpcException
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
	public void add(HpcManagedDataset managedDataset) throws HpcException
    {
		HpcSingleResultCallback<Void> callback = 
				                      new HpcSingleResultCallback<Void>();
		getCollection().insertOne(managedDataset, callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcManagedDataset get(String id) throws HpcException
	{
		HpcSingleResultCallback<HpcManagedDataset> callback = 
                       new HpcSingleResultCallback<HpcManagedDataset>();
		getCollection().find(eq(DATASET_ID_FIELD_NAME, id)).first(callback);
		
		return callback.getResult();
	}
	
	public List<HpcManagedDataset> get(String userId, 
                                       HpcDatasetUserAssociation association) 
                                      throws HpcException
    {
		// Determine the field name needed to query for the requested association.
		String fieldName = null;
		switch(association) {
		       case CREATOR:
		            fieldName = CREATOR_ID_FIELD_NAME;
		            break;
		            
		       case PRIMARY_INVESTIGATOR:
		            fieldName = PRIMARY_INVESTIGATOR_ID_FIELD_NAME;
		            break;
		            
		       case REGISTRATOR:
		            fieldName = REGISTRATOR_ID_FIELD_NAME;
		            break;
		            
		       default:
		    	   throw new HpcException("Invalid Association Value: " + 
		                                  association.value(), 
		                                  HpcErrorType.UNEXPECTED_ERROR);
		}
		
		// Invoke the query.
		List<HpcManagedDataset> managedDatasets = 
				                new ArrayList<HpcManagedDataset>();
		HpcSingleResultCallback<List<HpcManagedDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcManagedDataset>>();
		getCollection().find(
		                eq(fieldName, userId)).into(managedDatasets, callback); 
		
		return callback.getResult();
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the managed dataset Mongo collection.
     *
     * @return A The managed dataset Mongo collection.
     */
    private MongoCollection<HpcManagedDataset> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcManagedDataset.class);
    }  
}

 