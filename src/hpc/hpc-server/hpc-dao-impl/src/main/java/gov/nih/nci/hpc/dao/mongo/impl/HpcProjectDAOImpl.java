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

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcProjectDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

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
	public final static String NAME_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_NAME_KEY;
	public final static String TYPE_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_TYPE_KEY;
	public final static String INTERNAL_PROJECT_ID_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_INTERNAL_PROJECT_ID_KEY;
	public final static String PRINCIPAL_INVESTIGATOR_NIH_USER_ID_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_PRINCIPAL_INVESTIGATOR_NIH_USER_ID_KEY;
	public final static String REGISTRAR_NIH_USER_ID_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_REGISTRAR_NIH_USER_ID_KEY;
	public final static String LAB_BRANCH_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_LAB_BRANCH_KEY;
	public final static String DOC_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_DOC_KEY;
	public final static String CREATED_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_CREATED_KEY;
	public final static String ORGANIZATIONAL_STRUCTURE_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_ORGANIZATIONAL_STRUCTURE_KEY;
	public final static String DESCRIPTION_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_DESCRIPTION_KEY;
	public final static String METADATA_ITEMS_FIELD_NAME = 
            HpcCodec.PROJECT_METADATA_KEY + "." + 
            HpcCodec.PROJECT_METADATA_METADATA_ITEMS_KEY;
	
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
		       case PRINCIPAL_INVESTIGATOR:
		            fieldName = PRINCIPAL_INVESTIGATOR_NIH_USER_ID_FIELD_NAME;
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
	
	@Override
	public List<HpcProject> getProjects(HpcProjectMetadata metadata) 
                                       throws HpcException
    {
		List<HpcProject> projects = new ArrayList<HpcProject>();
		HpcSingleResultCallback<List<HpcProject>> callback = 
                       new HpcSingleResultCallback<List<HpcProject>>();
		getCollection().find(
				        and(getFilters(metadata))).into(projects, callback); 
		
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
    
    /**
     * Get a collection of filters to query by metadata.
     *
     * @param metadata The metadata to query.
     * @return A collection of filters.
     */
    private List<Bson> getFilters(HpcProjectMetadata metadata) 
    {
    	List<Bson> filters = new ArrayList<Bson>();
    	
    	if(metadata.getName() != null) {
      	   filters.add(eq(NAME_FIELD_NAME, 
      			          metadata.getName()));
      	}
    	if(metadata.getType() != null) {
      	   filters.add(eq(TYPE_FIELD_NAME, 
      			          metadata.getType().value()));
      	}
    	if(metadata.getInternalProjectId() != null) {
           filters.add(eq(INTERNAL_PROJECT_ID_FIELD_NAME, 
        		          metadata.getInternalProjectId()));
        }
    	if(metadata.getPrincipalInvestigatorNihUserId() != null) {
           filters.add(eq(PRINCIPAL_INVESTIGATOR_NIH_USER_ID_FIELD_NAME, 
          		          metadata.getPrincipalInvestigatorNihUserId()));
        }
     	if(metadata.getRegistrarNihUserId() != null) {
           filters.add(eq(REGISTRAR_NIH_USER_ID_FIELD_NAME, 
        		          metadata.getRegistrarNihUserId()));
       	}
    	if(metadata.getLabBranch() != null) {
           filters.add(eq(LAB_BRANCH_FIELD_NAME, 
          		          metadata.getLabBranch()));
        }
    	if(metadata.getDoc() != null) {
       	   filters.add(eq(DOC_FIELD_NAME, 
       			          metadata.getDoc()));
       	}
    	if(metadata.getCreated() != null) {
           filters.add(eq(CREATED_FIELD_NAME, 
        		          metadata.getCreated().getTime()));
        }
    	if(metadata.getOrganizationalStructure() != null) {
           filters.add(eq(ORGANIZATIONAL_STRUCTURE_FIELD_NAME, 
         			      metadata.getOrganizationalStructure()));
        }
    	if(metadata.getDescription() != null) {
           filters.add(regex(DESCRIPTION_FIELD_NAME, 
        		             metadata.getDescription(), "i"));
       	}
    	if(metadata.getMetadataItems() != null && 
    	   metadata.getMetadataItems().size() > 0 ) {
     	   filters.add(all(METADATA_ITEMS_FIELD_NAME, 
     			           metadata.getMetadataItems()));
    	}
    	
    	return filters;
    }
}

 