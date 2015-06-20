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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    // HpcManagedUser Document keys.
	public final static String MANAGED_USER_ID_KEY = "id"; 
	public final static String MANAGED_USER_CREATED_KEY = "created"; 
	public final static String MANAGED_USER_LAST_UPDATED_KEY = "last_updated"; 
	public final static String MANAGED_USER_USER_KEY = "user"; 
	
	// HpcUser Document keys.
	public final static String USER_NIH_USER_ID_KEY = "nih_user_id"; 
	public final static String USER_FIRST_NAME_KEY = "first_name"; 
	public final static String USER_LAST_NAME_KEY = "last_name"; 
	public final static String USER_DATA_TRANSFER_ACCOUNT_KEY = 
			                   "data_transfer_account";
	
	// HpcDataTransferAccount Document keys.
	public final static String DATA_TRANSFER_ACCOUNT_USERNAME_KEY = "username";
	public final static String DATA_TRANSFER_ACCOUNT_PASSWORD_KEY = "password";
	public final static String DATA_TRANSFER_ACCOUNT_DATA_TRANSFER_TYPE_KEY = 
			                   "data_transfer_type";
	
    // HpcManagedDataset Document keys.
	public final static String MANAGED_DATASET_ID_KEY = "id"; 
	public final static String MANAGED_DATASET_NAME_KEY = "name"; 
	public final static String MANAGED_DATASET_PRIMARY_INVESTIGATOR_ID_KEY = 
			                   "primary_investigator_id"; 
	public final static String MANAGED_DATASET_CREATOR_ID_KEY = "creator_id"; 
	public final static String MANAGED_DATASET_REGISTRATOR_ID_KEY = 
			                   "registrator_id"; 
	public final static String MANAGED_DATASET_LAB_BRANCH_KEY = "lab_branch"; 
	public final static String MANAGED_DATASET_DESCRIPTION_KEY = "description";
	public final static String MANAGED_DATASET_COMMENTS_KEY = "comments";
	public final static String MANAGED_DATASET_CREATED_KEY = "created"; 
	public final static String MANAGED_DATASET_FILES_KEY = "files";
	public final static String MANAGED_DATASET_UPLOAD_REQUESTS_KEY = 
			                   "upload_requests";
	public final static String MANAGED_DATASET_DOWNLOAD_REQUESTS_KEY = 
                               "download_requests";
    
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
    
    // HpcFileMetadata Document keys.
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
    public final static String FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY = 
                               "metadata_items";
    
    // HpcMetadataItem Document keys.
    public final static String METADATA_ITEM_KEY_KEY = "key";
    public final static String METADATA_ITEM_VALUE_KEY = "value";
    
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
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

 