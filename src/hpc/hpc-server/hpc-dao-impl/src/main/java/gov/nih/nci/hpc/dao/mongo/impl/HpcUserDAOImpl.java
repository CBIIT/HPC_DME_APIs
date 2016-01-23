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
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

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

 