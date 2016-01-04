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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;

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
	public final static String NCI_USER_ID_FIELD_NAME = 
							       HpcCodec.USER_NCI_ACCOUNT_KEY + "." + 
                                   HpcCodec.NCI_ACCOUNT_USER_ID_KEY;
	
    // User first name field name.
	public final static String FIRST_NAME_FIELD_NAME = 
							         HpcCodec.USER_NCI_ACCOUNT_KEY + "." + 
                                     HpcCodec.NCI_ACCOUNT_FIRST_NAME_KEY;
	
    // User last name field name.
	public final static String LAST_NAME_FIELD_NAME = 
							        HpcCodec.USER_NCI_ACCOUNT_KEY + "." + 
                                    HpcCodec.NCI_ACCOUNT_LAST_NAME_KEY;
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// HpcMongoDB instance.
	@Autowired
	private HpcMongoDB mongoDB = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     */
    private HpcUserDAOImpl() throws HpcException
    {
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
		getCollection().replaceOne(eq(NCI_USER_ID_FIELD_NAME, user.getNciAccount().getUserId()), 
				                   user, new UpdateOptions().upsert(true), callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcUser getUser(String nciUserId) throws HpcException
	{
		HpcSingleResultCallback<HpcUser> callback = 
                                         new HpcSingleResultCallback<HpcUser>();
		getCollection().find(
		   eq(NCI_USER_ID_FIELD_NAME, nciUserId)).first(callback);
		
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
    	
		getCollection().find(and(filters)).into(users, callback); 
		
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

 