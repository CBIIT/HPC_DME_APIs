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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

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
    
    // Field name to query by Dataset ID.
	public final static String DATASET_ID_FIELD_NAME = 
						       HpcCodec.DATASET_ID_KEY; 
	
	// Field name to query by Dataset name.
	public final static String DATASET_NAME_FIELD_NAME = 
							   HpcCodec.DATASET_FILE_SET_KEY + "." + 
	                           HpcCodec.FILE_SET_NAME_KEY;
	
	// Field names to query by NIH user id.
	public final static String PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME = 
                 HpcCodec.DATASET_FILE_SET_KEY + "." + 
                 HpcCodec.FILE_SET_FILES_KEY + "." + 
                 HpcCodec.FILE_METADATA_KEY + "." + 
                 HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
                 HpcCodec.FILE_PRIMARY_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY;
	public final static String CREATOR_NAME_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
	             HpcCodec.FILE_SET_FILES_KEY + "." + 
	             HpcCodec.FILE_METADATA_KEY + "." + 
	             HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
	             HpcCodec.FILE_PRIMARY_METADATA_CREATOR_NAME_KEY;
	public final static String REGISTRAR_NIH_USER_ID_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
		         HpcCodec.FILE_SET_FILES_KEY + "." + 
		         HpcCodec.FILE_METADATA_KEY + "." + 
		         HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
		         HpcCodec.FILE_PRIMARY_METADATA_REGISTRAR_NIH_USER_ID_KEY;
	
	// Field names to query by Primary Metadata.
	public final static String DATA_CONTAINS_PII_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_DATA_CONTAINS_PII_KEY;
	public final static String DATA_CONTAINS_PHI_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_DATA_CONTAINS_PHI_KEY;
	public final static String DATA_ENCRYPTED_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_DATA_ENCRYPTED_KEY;
	public final static String DATA_COMPRESSED_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_DATA_COMPRESSED_KEY;
	public final static String FUNDING_ORGANIZATION_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_FUNDING_ORGANIZATION_KEY;
	public final static String DESCRIPTION_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_DESCRIPTION_KEY;
	public final static String LAB_BRANCH_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_LAB_BRANCH_KEY;
	public final static String METADATA_ITEMS_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY;
	
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
	public void updateReplace(HpcDataset dataset) throws HpcException
    {
		SingleResultCallback<UpdateResult> callback = 
				                      new HpcSingleResultCallback<UpdateResult>();
		getCollection().replaceOne(eq(DATASET_ID_FIELD_NAME, dataset.getId()), dataset, callback);
       
		// Throw the callback exception (if any).
		((HpcSingleResultCallback<UpdateResult>) callback).throwException();
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
		       case PRIMARY_INVESTIGATOR:
		            fieldName = PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME;
		            break;
		            
		       case REGISTRAR:
		            fieldName = REGISTRAR_NIH_USER_ID_FIELD_NAME;
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
	
	public List<HpcDataset> getDatasets(HpcFilePrimaryMetadata primaryMetadata) 
                                       throws HpcException
    {
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(
				        and(getFilters(primaryMetadata))).into(datasets, callback); 
		
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
    
    /**
     * Get a collection of filters to query by primary metadata.
     *
     * @param primaryMetadata The metadata to query.
     * @return A collection of filters.
     */
    private List<Bson> getFilters(HpcFilePrimaryMetadata primaryMetadata) 
    {
    	List<Bson> filters = new ArrayList<Bson>();
    	
    	if(primaryMetadata.getDataContainsPII() != null) {
    	   filters.add(eq(DATA_CONTAINS_PII_FIELD_NAME, 
    			          primaryMetadata.getDataContainsPII()));
    	}
    	if(primaryMetadata.getDataContainsPHI() != null) {
     	   filters.add(eq(DATA_CONTAINS_PHI_FIELD_NAME, 
     			          primaryMetadata.getDataContainsPHI()));
     	}
    	if(primaryMetadata.getDataEncrypted() != null) {
       	   filters.add(eq(DATA_ENCRYPTED_FIELD_NAME, 
       			          primaryMetadata.getDataEncrypted()));
       	}
    	if(primaryMetadata.getDataCompressed() != null) {
      	   filters.add(eq(DATA_COMPRESSED_FIELD_NAME, 
      			          primaryMetadata.getDataCompressed()));
      	}
    	if(primaryMetadata.getFundingOrganization() != null) {
       	   filters.add(eq(FUNDING_ORGANIZATION_FIELD_NAME, 
       			          primaryMetadata.getFundingOrganization()));
       	}
    	if(primaryMetadata.getPrimaryInvestigatorNihUserId() != null) {
           filters.add(eq(PRIMARY_INVESTIGATOR_NIH_USER_ID_FIELD_NAME, 
        		          primaryMetadata.getPrimaryInvestigatorNihUserId()));
        }
    	if(primaryMetadata.getCreatorName() != null) {
     	   filters.add(eq(CREATOR_NAME_FIELD_NAME, 
     			          primaryMetadata.getCreatorName()));
    	}
    	if(primaryMetadata.getRegistrarNihUserId() != null) {
      	   filters.add(eq(REGISTRAR_NIH_USER_ID_FIELD_NAME, 
      			          primaryMetadata.getRegistrarNihUserId()));
     	}
    	if(primaryMetadata.getDescription() != null) {
       	   filters.add(regex(DESCRIPTION_FIELD_NAME, 
       			             primaryMetadata.getDescription(), "i"));
      	}
    	if(primaryMetadata.getLabBranch() != null) {
           filters.add(eq(LAB_BRANCH_FIELD_NAME, 
        		          primaryMetadata.getLabBranch()));
       	}
    	if(primaryMetadata.getMetadataItems() != null) {
     	   filters.add(all(METADATA_ITEMS_FIELD_NAME, 
     			           primaryMetadata.getMetadataItems()));
    	}
    	
    	return filters;
    }  
}

 