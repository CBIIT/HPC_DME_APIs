/**
 * HpcCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * <p>
 * HPC Codec abstract base class. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public abstract class HpcCodec<T> implements Codec<T>
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // HpcUser Document keys.
	public final static String USER_CREATED_KEY = "created"; 
	public final static String USER_LAST_UPDATED_KEY = "last_updated"; 
	public final static String USER_NIH_ACCOUNT_KEY = "nih_account"; 
	public final static String USER_DATA_TRANSFER_ACCOUNT_KEY = 
			                   "data_transfer_account"; 
	
	// HpcNihAccount Document keys.
	public final static String NIH_ACCOUNT_USER_ID_KEY = "user_id"; 
	public final static String NIH_ACCOUNT_FIRST_NAME_KEY = "first_name"; 
	public final static String NIH_ACCOUNT_LAST_NAME_KEY = "last_name"; 
	
	// HpcDataTransferAccount Document keys.
	public final static String DATA_TRANSFER_ACCOUNT_USERNAME_KEY = "username";
	public final static String DATA_TRANSFER_ACCOUNT_PASSWORD_KEY = "password";
	public final static String DATA_TRANSFER_ACCOUNT_ACCOUNT_TYPE_KEY = 
			                   "account_type";
	// HpcProject Document keys.
	public final static String PROJECT_ID_KEY = "id"; 
	public final static String PROJECT_DATASET_KEY = "dataset_id";
	public final static String PROJECT_METADATA_KEY = "metadata";
	public final static String PROJECT_NAME_KEY = "name";
	public final static String PROJECT_DESCRIPTION_KEY = "description";
	public final static String PROJECT_INTERNAL_PROJECT_ID_KEY = "internal_project_id";
	public final static String PROJECT_EXPERIMENT_ID_KEY = "experiment_id";
	public final static String NAME_KEY = "name"; 
	public final static String DESCRIPTION_KEY = "description";
    public final static String PRIMARY_METADATA_KEY = 
            "primary_metadata";
    public final static String FUNDING_ORGANIZATION_KEY = 
            "funding_organization";
	public final static String DIVISION_KEY = "division";
	public final static String CENTER_KEY = "center";
	public final static String ORGANIZATION_KEY = "organization";
	public final static String PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY = "primary_investigator_nih_user_id";
	public final static String CREATOR_NIH_USER_ID_KEY = "creator_nih_user_id";
	public final static String REGISTRATOR_NIH_USER_ID_KEY = "registrator_nih_user_id";
	public final static String LAB_BRANCH_KEY = "lab_branch";
	public final static String METADATA_ITEMS_KEY = "metadata_items";

	// HpcDataset Document keys.
	public final static String DATASET_ID_KEY = "id"; 
	public final static String DATASET_FILE_SET_KEY = "file_set";
	public final static String DATASET_UPLOAD_REQUESTS_KEY = 
			                   "upload_requests";
	public final static String DATASET_DOWNLOAD_REQUESTS_KEY = 
                               "download_requests";
	
    // HpcdFileSet Document keys.
	public final static String FILE_SET_NAME_KEY = "name"; 
	public final static String FILE_SET_DESCRIPTION_KEY = "description";
	public final static String FILE_SET_COMMENTS_KEY = "comments";
	public final static String FILE_SET_CREATED_KEY = "created"; 
	public final static String FILE_SET_FILES_KEY = "files";
    
    // HpcFile Document keys.
    public final static String FILE_ID_KEY = "id"; 
    public final static String FILE_TYPE_KEY = "type"; 
    public final static String FILE_SIZE_KEY = "size"; 
    public final static String FILE_SOURCE_KEY = "source";
    public final static String FILE_LOCATION_KEY = "location";
    public final static String FILE_METADATA_KEY = "metadata";
    
    // HpcFileLocation Document keys.
    public final static String FILE_LOCATION_ENDPOINT_KEY = "endpoint"; 
    public final static String FILE_LOCATION_PATH_KEY = "path"; 
    
    // HpcFileMetadata Document keys.
    public final static String FILE_METADATA_PRIMARY_METADATA_KEY = 
    		                   "primary_metadata";
    
    // HpcFilePrimaryMetadata Document keys.
    public final static String FILE_PRIMARY_METADATA_DATA_CONTAINS_PII_KEY = 
    		                   "data_contains_pii";
    public final static String FILE_PRIMARY_METADATA_DATA_CONTAINS_PHI_KEY = 
                               "data_contains_phi";
    public final static String FILE_PRIMARY_METADATA_DATA_ENCRYPTED_KEY = 
            				   "data_encrypted";
    public final static String FILE_PRIMARY_METADATA_DATA_COMPRESSED_KEY = 
			                   "data_compressed";
    public final static String FILE_PRIMARY_METADATA_FUNDING_ORGANIZATION_KEY = 
                               "funding_organization";
    public final static String 
                 FILE_PRIMARY_METADATA_PRIMARY_INVESTIGATOR_NIH_USER_ID_KEY = 
            				   "primary_investigator_nih_user_id"; 
    public final static String FILE_PRIMARY_METADATA_CREATOR_NIH_USER_ID_KEY = 
    		                   "creator_nih_user_id"; 
    public final static String FILE_PRIMARY_METADATA_REGISTRATOR_NIH_USER_ID_KEY = 
    		                   "registrator_nih_user_id"; 
    public final static String FILE_PRIMARY_METADATA_LAB_BRANCH_KEY = 
    		                   "lab_branch"; 
    public final static String FILE_PRIMARY_METADATA_DESCRIPTION_KEY = 
    		                   "description";
    public final static String FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY = 
                               "metadata_items";
    
    // HpcMetadataItem Document keys.
    public final static String METADATA_ITEM_KEY_KEY = "key";
    public final static String METADATA_ITEM_VALUE_KEY = "value";
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The codec registry.
	private CodecRegistry codecRegistry;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
    public HpcCodec()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get Codec Registry
     *
     * @return The codec registry.
     */
    protected CodecRegistry getRegistry()                              
    {
    	return codecRegistry;
    }   
    
    /**
     * Set Codec Registry
     *
     * @param codecRegistry The codec registry.
     */
    protected void setRegistry(CodecRegistry codecRegistry)                            
    {
    	this.codecRegistry = codecRegistry;
    }   
}

 