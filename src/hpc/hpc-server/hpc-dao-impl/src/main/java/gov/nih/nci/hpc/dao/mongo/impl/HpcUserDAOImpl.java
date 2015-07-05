/**
 * HpcUserDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import static com.mongodb.client.model.Filters.eq;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

import com.mongodb.async.client.MongoCollection;


/**
 * <p>
 * HPC User DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserDAOImpl implements HpcUserDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Dataset ID field name.
	public final static String NIH_USER_ID_FIELD_NAME = 
							       HpcCodec.USER_NIH_ACCOUNT_KEY + "." + 
                                   HpcCodec.NIH_ACCOUNT_USER_ID_KEY;
	
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
    private HpcUserDAOImpl() throws HpcException
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
    private HpcUserDAOImpl(HpcMongoDB mongoDB) throws HpcException
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
	public void add(HpcUser user) throws HpcException
    {
		HpcSingleResultCallback<Void> callback = 
				                      new HpcSingleResultCallback<Void>();
		getCollection().insertOne(user, callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcUser get(String nihUserId) throws HpcException
	{
		HpcSingleResultCallback<HpcUser> callback = 
                                         new HpcSingleResultCallback<HpcUser>();
		getCollection().find(
		   eq(NIH_USER_ID_FIELD_NAME, nihUserId)).first(callback);
		
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
    private MongoCollection<HpcUser> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcUser.class);
    }  
}

 