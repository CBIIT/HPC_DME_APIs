/**
 * HpcProjectDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.impl;

import static com.mongodb.client.model.Filters.eq;
import gov.nih.nci.hpc.dao.HpcProjectDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

/**
 * <p>
 * HPC Project DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectDAOImpl implements HpcProjectDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Project ID field name.
	public final static String PROJECT_ID_FIELD_NAME = HpcCodec.PROJECT_ID_KEY; 
	public final static String REGISTRAR_NIH_USER_ID_FIELD_NAME = 
		                       HpcCodec.PROJECT_METADATA_KEY + "." + 
	                           HpcCodec.PROJECT_METADATA_REGISTRAR_NIH_USER_ID_KEY;
	public final static String PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME = 
		                HpcCodec.PROJECT_METADATA_KEY + "." + 
	                    HpcCodec.PROJECT_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY;
	
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
    private HpcProjectDAOImpl() throws HpcException
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
    private HpcProjectDAOImpl(HpcMongoDB mongoDB) throws HpcException
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
    // HpcManagedProjectDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsert(HpcProject project) throws HpcException
    {
		HpcSingleResultCallback<UpdateResult> callback = 
                new HpcSingleResultCallback<UpdateResult>();
		getCollection().replaceOne(eq(PROJECT_ID_FIELD_NAME, project.getId()), 
                                   project, new UpdateOptions().upsert(true), 
                                   callback);

		// Throw the callback exception (if any).
		callback.throwException();
    }
	
	@Override
	public HpcProject getProject(String id) throws HpcException
	{
		HpcSingleResultCallback<HpcProject> callback = 
                       new HpcSingleResultCallback<HpcProject>();
		getCollection().find(eq(PROJECT_ID_FIELD_NAME, id)).first(callback);
		
		return callback.getResult();
	}
	
	@Override
	public List<HpcProject> getProjects(String nihUserId, 
                                        HpcDatasetUserAssociation association) 
                                       throws HpcException
    {
		// Determine the field name needed to query for the requested association.
		String fieldName = null;
		switch(association) {
		       case PRIMARY_INVESTIGATOR:
		            fieldName = PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       case REGISTRAR:
		            fieldName = REGISTRAR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       case CREATOR:
		       default:
		    	   throw new HpcException("Invalid Association Value: " + 
		                                  association.value(), 
		                                  HpcErrorType.UNEXPECTED_ERROR);
		}
		
		// Invoke the query.
		List<HpcProject> projects = new ArrayList<HpcProject>();
		HpcSingleResultCallback<List<HpcProject>> callback = 
                       new HpcSingleResultCallback<List<HpcProject>>();
		getCollection().find(
		                eq(fieldName, nihUserId)).into(projects, callback); 
		
		return callback.getResult();
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the managed project Mongo collection.
     *
     * @return A The project Mongo collection.
     */
    private MongoCollection<HpcProject> getCollection() throws HpcException
    {
    	return mongoDB.getCollection(HpcProject.class);
    }  
}

 