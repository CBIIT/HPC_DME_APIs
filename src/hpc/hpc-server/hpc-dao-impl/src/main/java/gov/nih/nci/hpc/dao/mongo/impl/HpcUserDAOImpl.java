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

import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.conversions.Bson;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;


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
    
    // User ID field name.
	public final static String NIH_USER_ID_FIELD_NAME = 
							       HpcCodec.USER_NIH_ACCOUNT_KEY + "." + 
                                   HpcCodec.NIH_ACCOUNT_USER_ID_KEY;
	
    // User first name field name.
	public final static String FIRST_NAME_FIELD_NAME = 
							         HpcCodec.USER_NIH_ACCOUNT_KEY + "." + 
                                     HpcCodec.NIH_ACCOUNT_FIRST_NAME_KEY;
	
    // User last name field name.
	public final static String LAST_NAME_FIELD_NAME = 
							        HpcCodec.USER_NIH_ACCOUNT_KEY + "." + 
                                    HpcCodec.NIH_ACCOUNT_LAST_NAME_KEY;
	
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
	public void upsert(HpcUser user) throws HpcException
    {
		HpcSingleResultCallback<UpdateResult> callback = 
				                      new HpcSingleResultCallback<UpdateResult>();
		getCollection().replaceOne(eq(NIH_USER_ID_FIELD_NAME, user.getNihAccount().getUserId()), 
				                   user, new UpdateOptions().upsert(true), callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcUser getUser(String nihUserId) throws HpcException
	{
		HpcSingleResultCallback<HpcUser> callback = 
                                         new HpcSingleResultCallback<HpcUser>();
		getCollection().find(
		   eq(NIH_USER_ID_FIELD_NAME, nihUserId)).first(callback);
		
		return callback.getResult();
	}
	
	@Override
	public List<HpcUser> getUsers(String firstName, String lastName) throws HpcException
	{
		List<HpcUser> users = new ArrayList<HpcUser>();
		HpcSingleResultCallback<List<HpcUser>> callback = 
                       new HpcSingleResultCallback<List<HpcUser>>();
		
		List<Bson> filters = new ArrayList<Bson>();
    	if(firstName != null && !firstName.isEmpty()) {
    	   filters.add(regex(FIRST_NAME_FIELD_NAME, 
	        		         "^" + Pattern.quote(firstName) + "$", "i"));
    	}
    	if(lastName != null && !lastName.isEmpty()) {
     	   filters.add(regex(LAST_NAME_FIELD_NAME, 
 	        		         "^" + Pattern.quote(lastName) + "$", "i"));
     	}
    	
		getCollection().find(or(filters)).into(users, callback); 
		
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

 