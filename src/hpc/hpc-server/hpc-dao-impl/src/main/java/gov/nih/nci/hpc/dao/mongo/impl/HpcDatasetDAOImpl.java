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

import static com.mongodb.client.model.Filters.all;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.regex;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.dao.mongo.codec.HpcCodec;
import gov.nih.nci.hpc.dao.mongo.driver.HpcMongoDB;
import gov.nih.nci.hpc.dao.mongo.driver.HpcSingleResultCallback;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.async.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
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

	// Field name to query by Project Id.
	public final static String DATASET_PROJECT_IDS_FIELD_NAME = 
							   HpcCodec.DATASET_FILE_SET_KEY + "." + 
							   HpcCodec.FILE_SET_FILES_KEY + "." +
	                           HpcCodec.FILE_PROJECT_IDS_KEY;
	
	// Field names to query by NCI user id.
	public final static String PRINCIPAL_INVESTIGATOR_NCI_USER_ID_FIELD_NAME = 
                 HpcCodec.DATASET_FILE_SET_KEY + "." + 
                 HpcCodec.FILE_SET_FILES_KEY + "." + 
                 HpcCodec.FILE_METADATA_KEY + "." + 
                 HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
                 HpcCodec.FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_NCI_USER_ID_KEY;
	public final static String CREATOR_NAME_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
	             HpcCodec.FILE_SET_FILES_KEY + "." + 
	             HpcCodec.FILE_METADATA_KEY + "." + 
	             HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
	             HpcCodec.FILE_PRIMARY_METADATA_CREATOR_NAME_KEY;
	public final static String REGISTRAR_NCI_USER_ID_FIELD_NAME = 
			     HpcCodec.DATASET_FILE_SET_KEY + "." + 
		         HpcCodec.FILE_SET_FILES_KEY + "." + 
		         HpcCodec.FILE_METADATA_KEY + "." + 
		         HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
		         HpcCodec.FILE_PRIMARY_METADATA_REGISTRAR_NCI_USER_ID_KEY;
	
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
	public final static String PRINCIPAL_INVESTIGATOR_DOC_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_DOC_KEY;
	public final static String REGISTRAR_DOC_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_REGISTRAR_DOC_KEY;
	public final static String ORIGINALLY_CREATED_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_ORIGINALLY_CREATED_KEY;
	public final static String METADATA_ITEMS_FIELD_NAME = 
            HpcCodec.DATASET_FILE_SET_KEY + "." + 
            HpcCodec.FILE_SET_FILES_KEY + "." + 
            HpcCodec.FILE_METADATA_KEY + "." + 
            HpcCodec.FILE_METADATA_PRIMARY_METADATA_KEY + "." + 
            HpcCodec.FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY;
	
	// Field name to query by upload requests data transfer status.
	public final static String DATASET_UPLOAD_TRANSFER_STATUS_NAME =
							   HpcCodec.DATASET_UPLOAD_REQUESTS_KEY + "." + 
	                           HpcCodec.DATA_TRANSFER_REQUEST_STATUS_KEY;
	
	// Field name to query by download requests data transfer status.
	public final static String DATASET_DOWNLOAD_TRANSFER_STATUS_NAME =
							   HpcCodec.DATASET_DOWNLOAD_REQUESTS_KEY + "." + 
	                           HpcCodec.DATA_TRANSFER_REQUEST_STATUS_KEY;
	
	// Field name to query a file by id.
	public final static String FILE_ID_FIELD_NAME = 
							   HpcCodec.DATASET_FILE_SET_KEY + "." + 
	                           HpcCodec.FILE_SET_FILES_KEY + "." +
	                           HpcCodec.FILE_ID_KEY;
	
	// Field name to query by date range.
	public final static String DATASET_CREATED_FIELD_NAME = 
			                   HpcCodec.DATASET_CREATED_KEY;
	
	// Projection for query a file by id.
	public final static String FILE_PROJECTION = 
							   HpcCodec.DATASET_FILE_SET_KEY + "." + 
	                           HpcCodec.FILE_SET_FILES_KEY + ".$";
	
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
    // HpcDatasetDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsert(HpcDataset dataset) throws HpcException
    {
		HpcSingleResultCallback<UpdateResult> callback = 
				                new HpcSingleResultCallback<UpdateResult>();
		getCollection().replaceOne(eq(DATASET_ID_FIELD_NAME, dataset.getId()), 
				                   dataset, new UpdateOptions().upsert(true), 
				                   callback);
       
		// Throw the callback exception (if any).
		callback.throwException();
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
	public HpcFile getFile(String id) throws HpcException
	{
		HpcSingleResultCallback<HpcDataset> callback = 
                       new HpcSingleResultCallback<HpcDataset>();
		
		getCollection().find(eq(FILE_ID_FIELD_NAME, id)).projection(new Document(FILE_PROJECTION, "1")).first(callback);
		if(callback.getResult() != null && callback.getResult().getFileSet() != null) {
		   return callback.getResult().getFileSet().getFiles().get(0);
		}
		
		return null;
	}
	
	@Override
	public List<HpcDataset> getDatasets() throws HpcException
    {
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find().into(datasets, callback); 
		
		return callback.getResult();
    }
	
	@Override
	public List<HpcDataset> getDatasets(List<String> nciUserIds, 
                                        HpcDatasetUserAssociation association) 
                                       throws HpcException
    {
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(in(getFieldName(association), nciUserIds)).into(datasets, callback); 
		
		return callback.getResult();
    }
	
	@Override
	public List<HpcDataset> getDatasets(String name, boolean regex) 
			                           throws HpcException
	{
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(
		   regex(DATASET_NAME_FIELD_NAME, 
		         regex ? name : Pattern.quote(name), "i")).into(datasets, callback); 
		
		return callback.getResult();
	}
	
	@Override
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
	
	@Override
    public boolean exists(String name, String nciUserId, 
                          HpcDatasetUserAssociation association) 
                         throws HpcException
    {
		HpcSingleResultCallback<Long> callback = new HpcSingleResultCallback<Long>();
    	getCollection().count(and(eq(getFieldName(association), nciUserId),
    			                  regex(DATASET_NAME_FIELD_NAME, 
	                	                "^" + Pattern.quote(name) + "$", "i")), callback);
    	return callback.getResult() != null ? callback.getResult() > 0 : false;
    }
	
	@Override
   	public List<HpcDataset> getDatasets(HpcDataTransferStatus dataTransferStatus,
                                        boolean uploadDownloadRequests)
			                           throws HpcException
	{
   		// Determine the search in the upload or download requests. 
   		String fieldName = uploadDownloadRequests ?
   				           DATASET_UPLOAD_TRANSFER_STATUS_NAME : 
   				           DATASET_DOWNLOAD_TRANSFER_STATUS_NAME;
   		
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(eq(fieldName, 
				                dataTransferStatus.value())).into(datasets, 
				                		                          callback); 
		
		return callback.getResult();
	}  
	
	@Override
	public List<HpcDataset> getDatasets(Calendar from, Calendar to) 
                                       throws HpcException
    {
		// Create the filters.
    	List<Bson> filters = new ArrayList<Bson>();
    	if(from != null) {
    	   filters.add(gte(DATASET_CREATED_FIELD_NAME, from.getTime()));
    	}
    	if(to != null) {
     	   filters.add(lte(DATASET_CREATED_FIELD_NAME, to.getTime()));
     	}
    	
    	// Query the data.
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(and(filters)).into(datasets, callback); 
		
		return callback.getResult();
    }
	
	@Override
	public List<HpcDataset> getDatasetsByProjectId(String projectId)
			                                      throws HpcException 
	{
		List<HpcDataset> datasets = new ArrayList<HpcDataset>();
		HpcSingleResultCallback<List<HpcDataset>> callback = 
                       new HpcSingleResultCallback<List<HpcDataset>>();
		getCollection().find(in(DATASET_PROJECT_IDS_FIELD_NAME, projectId)).
		                into(datasets, callback); 
		
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
    			          primaryMetadata.getDataContainsPII().value()));
    	}
    	if(primaryMetadata.getDataContainsPHI() != null) {
     	   filters.add(eq(DATA_CONTAINS_PHI_FIELD_NAME, 
     			          primaryMetadata.getDataContainsPHI().value()));
     	}
    	if(primaryMetadata.getDataEncrypted() != null) {
       	   filters.add(eq(DATA_ENCRYPTED_FIELD_NAME, 
       			          primaryMetadata.getDataEncrypted().value()));
       	}
    	if(primaryMetadata.getDataCompressed() != null) {
      	   filters.add(eq(DATA_COMPRESSED_FIELD_NAME, 
      			          primaryMetadata.getDataCompressed().value()));
      	}
    	if(primaryMetadata.getFundingOrganization() != null) {
       	   filters.add(eq(FUNDING_ORGANIZATION_FIELD_NAME, 
       			          primaryMetadata.getFundingOrganization()));
       	}
    	if(primaryMetadata.getPrincipalInvestigatorNciUserId() != null) {
           filters.add(eq(PRINCIPAL_INVESTIGATOR_NCI_USER_ID_FIELD_NAME, 
        		          primaryMetadata.getPrincipalInvestigatorNciUserId()));
        }
    	if(primaryMetadata.getCreatorName() != null) {
     	   filters.add(eq(CREATOR_NAME_FIELD_NAME, 
     			          primaryMetadata.getCreatorName()));
    	}
    	if(primaryMetadata.getRegistrarNciUserId() != null) {
      	   filters.add(eq(REGISTRAR_NCI_USER_ID_FIELD_NAME, 
      			          primaryMetadata.getRegistrarNciUserId()));
     	}
    	if(primaryMetadata.getDescription() != null) {
       	   filters.add(regex(DESCRIPTION_FIELD_NAME, 
       			             Pattern.quote(primaryMetadata.getDescription()), "i"));
      	}
    	if(primaryMetadata.getLabBranch() != null) {
           filters.add(eq(LAB_BRANCH_FIELD_NAME, 
        		          primaryMetadata.getLabBranch()));
       	}
    	if(primaryMetadata.getPrincipalInvestigatorDOC() != null) {
            filters.add(eq(PRINCIPAL_INVESTIGATOR_DOC_FIELD_NAME, 
         		           primaryMetadata.getPrincipalInvestigatorDOC()));
        }
    	if(primaryMetadata.getRegistrarDOC() != null) {
            filters.add(eq(REGISTRAR_DOC_FIELD_NAME, 
         		           primaryMetadata.getRegistrarDOC()));
        }
    	if(primaryMetadata.getOriginallyCreated() != null) {
            filters.add(eq(ORIGINALLY_CREATED_FIELD_NAME, 
            		primaryMetadata.getOriginallyCreated().getTime()));
        }
    	if(primaryMetadata.getMetadataItems() != null && 
    	   !primaryMetadata.getMetadataItems().isEmpty()) {
     	   filters.add(all(METADATA_ITEMS_FIELD_NAME, 
     			           primaryMetadata.getMetadataItems()));
    	}
    	
    	return filters;
    }
    
    /**
     * Get a query field name from a dataset/user associartion.
     *
     * @param association The dataset to user association..
     * @return A field name to include in a filter.
     * 
     * @throws HpcException id the association value is unexpected.
     */
    private String getFieldName(HpcDatasetUserAssociation association) 
                               throws HpcException
    {
		switch(association) {
	           case PRINCIPAL_INVESTIGATOR:
	                return PRINCIPAL_INVESTIGATOR_NCI_USER_ID_FIELD_NAME;
	         
	           case REGISTRAR:
	                return REGISTRAR_NCI_USER_ID_FIELD_NAME;
	         
	           default:
	 	            throw new HpcException("Invalid Association Value: " + 
	                                       association.value(), 
	                                       HpcErrorType.UNEXPECTED_ERROR);
		}
    }
}

 