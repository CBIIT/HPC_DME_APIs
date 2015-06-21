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
import gov.nih.nci.hpc.exception.HpcErrorType;

import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserType;

import com.mongodb.async.client.MongoCollection;
import static com.mongodb.client.model.Filters.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
		String fieldName = HpcCodec.MANAGED_DATASET_DATASET_KEY + "." + 
				           HpcCodec.DATASET_ID_KEY;
		getCollection().find(eq(fieldName, id)).first(callback);
		
		return callback.getResult();
	}
	
	public List<HpcManagedDataset> get(String userId, 
                                       HpcDatasetUserType datasetUserType) 
                                      throws HpcException
    {
		return null;
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

 