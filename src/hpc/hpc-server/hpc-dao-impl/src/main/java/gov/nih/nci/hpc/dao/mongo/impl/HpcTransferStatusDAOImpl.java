/**
 * HpcTransferStatusDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import static com.mongodb.client.model.Filters.eq;
import gov.nih.nci.hpc.dao.HpcTransferStatusDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import com.mongodb.async.client.MongoCollection;


/**
 * <p>
 * HPC HpcTransferStatus DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcTransferStatusDAOImpl implements HpcTransferStatusDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Dataset ID field name.
	public final static String NCI_USER_ID_FIELD_NAME = 
							       HpcCodec.USER_NCI_ACCOUNT_KEY + "." + 
                                   HpcCodec.NCI_ACCOUNT_USER_ID_KEY;
	
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
    private HpcTransferStatusDAOImpl() throws HpcException
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
    private HpcTransferStatusDAOImpl(HpcMongoDB mongoDB) throws HpcException
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
    // HpcManagedUserDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void add(HpcDataTransferRequest hpcDataTransferRequest) throws HpcException
    {
		HpcSingleResultCallback<Void> callback = 
				                      new HpcSingleResultCallback<Void>();
		getCollection().insertOne(hpcDataTransferRequest, callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcDataTransferRequest get(String submissionId) throws HpcException
	{
		HpcSingleResultCallback<HpcDataTransferRequest> callback = 
                                         new HpcSingleResultCallback<HpcDataTransferRequest>();
		getCollection().find(
		   eq(NCI_USER_ID_FIELD_NAME, submissionId)).first(callback);
		
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
    private MongoCollection<HpcDataTransferRequest> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcDataTransferRequest.class);
    }  
}

 