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
	public final static String USER_NCI_ACCOUNT_KEY = "nci_account"; 
	public final static String USER_DATA_TRANSFER_ACCOUNT_KEY = 
			                   "data_transfer_account"; 
	public final static String USER_DATA_MANAGEMENT_ACCOUNT_KEY = 
                               "data_management_account"; 
	
	// HpcNciAccount Document keys.
	public final static String NCI_ACCOUNT_USER_ID_KEY = "user_id"; 
	public final static String NCI_ACCOUNT_FIRST_NAME_KEY = "first_name"; 
	public final static String NCI_ACCOUNT_LAST_NAME_KEY = "last_name"; 
	
	// HpcIntegratedSystemAccount Document keys.
	public final static String INTEGRATED_SYSTEM_ACCOUNT_USERNAME_KEY = "username";
	public final static String INTEGRATED_SYSTEM_ACCOUNT_PASSWORD_KEY = "password";
	public final static String INTEGRATED_SYSTEM_ACCOUNT_INTEGRATED_SYSTEM_KEY = 
			                   "integrated_system";
	// HpcProject Document keys.
	public final static String PROJECT_CREATED_KEY = "created"; 
	public final static String PROJECT_LAST_UPDATED_KEY = "last_updated"; 
	public final static String PROJECT_ID_KEY = "id"; 
	public final static String PROJECT_DATASET_IDS_KEY = "dataset_ids";
	public final static String PROJECT_METADATA_KEY = "metadata";
	public final static String PROJECT_NAME_KEY = "name";
	public final static String PROJECT_DESCRIPTION_KEY = "description";
	
	// HpcProjectMetadata Document Keys.
	public final static String PROJECT_METADATA_NAME_KEY = "name"; 
	public final static String PROJECT_METADATA_TYPE_KEY = "type";
	public final static String PROJECT_METADATA_INTERNAL_PROJECT_ID_KEY = 
			                   "internal_project_id";
	public final static String PROJECT_METADATA_PRINCIPAL_INVESTIGATOR_NCI_USER_ID_KEY = 
                               "principal_investigator_nci_user_id";
	public final static String PROJECT_METADATA_REGISTRAR_NCI_USER_ID_KEY = 
                               "registrar_nci_user_id";
    public final static String PROJECT_METADATA_PRINCIPAL_INVESTIGATOR_DOC_KEY = 
                               "principal_investigator_doc";
    public final static String PROJECT_METADATA_REGISTRAR_DOC_KEY = 
                               "registrar_doc";
	public final static String PROJECT_METADATA_LAB_BRANCH_KEY = "lab_branch";
	public final static String PROJECT_METADATA_CREATED_KEY = "created";
	public final static String PROJECT_METADATA_DESCRIPTION_KEY = "description";
	public final static String PROJECT_METADATA_METADATA_ITEMS_KEY = "metadata_items";

	
	// HpcDataset Document keys.
	public final static String DATASET_CREATED_KEY = "created"; 
	public final static String DATASET_LAST_UPDATED_KEY = "last_updated"; 
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
	public final static String FILE_SET_FILES_KEY = "files";
    
    // HpcFile Document keys.
    public final static String FILE_ID_KEY = "id"; 
    public final static String FILE_TYPE_KEY = "type"; 
    public final static String FILE_SIZE_KEY = "size"; 
    public final static String FILE_SOURCE_KEY = "source";
    public final static String FILE_LOCATION_KEY = "location";
    public final static String FILE_METADATA_KEY = "metadata";
    public final static String FILE_PROJECT_IDS_KEY = "project_ids";
    
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
                 FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_NCI_USER_ID_KEY = 
            				   "principal_investigator_nci_user_id"; 
    public final static String FILE_PRIMARY_METADATA_CREATOR_NAME_KEY = 
    		                   "creator_name"; 
    public final static String FILE_PRIMARY_METADATA_REGISTRAR_NCI_USER_ID_KEY = 
    		                   "registrar_nci_user_id"; 
    public final static String FILE_PRIMARY_METADATA_LAB_BRANCH_KEY = 
    		                   "lab_branch"; 
    public final static String FILE_PRIMARY_METADATA_DESCRIPTION_KEY = 
    		                   "description";
    public final static String FILE_PRIMARY_METADATA_PRINCIPAL_INVESTIGATOR_DOC_KEY = 
                               "principal_investigator_doc";
    public final static String FILE_PRIMARY_METADATA_REGISTRAR_DOC_KEY = 
                               "registrar_doc";
    public final static String FILE_PRIMARY_METADATA_ORIGINALLY_CREATED_KEY = 
                               "originally_created";
    public final static String FILE_PRIMARY_METADATA_METADATA_ITEMS_KEY = 
                               "metadata_items";
    
    // HpcMetadataItem Document keys.
    public final static String METADATA_ITEM_KEY_KEY = "key";
    public final static String METADATA_ITEM_VALUE_KEY = "value";

    // HpcDataTransferRequest Document keys.
    public final static String DATA_TRANSFER_REQUEST_REQUESTER_NCI_USER_ID_KEY = 
                               "requester_nci_user_id";
    public final static String DATA_TRANSFER_REQUEST_FILE_ID_KEY = 
                               "file_id";
    public final static String DATA_TRANSFER_REQUEST_DATA_TRANSFER_ID_KEY = 
                               "data_transfer_id";
    public final static String DATA_TRANSFER_REQUEST_LOCATIONS_KEY = "locations";
    public final static String DATA_TRANSFER_REQUEST_STATUS_KEY = "status";
    public final static String DATA_TRANSFER_REQUEST_REPORT_KEY = "report";
    
    // HpcDataTransferLocations Document keys.
    public final static String DATA_TRANSFER_LOCATIONS_SOURCE_KEY = "source";
    public final static String DATA_TRANSFER_LOCATIONS_DESTINATION_KEY = "destination";
    
    // HpcDataTransferReport Document Keys.
    public final static String DATA_TRANSFER_REPORT_TASK_ID_KEY = "task_id"; 
    public final static String DATA_TRANSFER_REPORT_TASK_TYPE_KEY = "task_type"; 
    public final static String DATA_TRANSFER_REPORT_STATUS_KEY = "status";
    public final static String DATA_TRANSFER_REPORT_REQUEST_TIME_KEY = 
                               "request_time"; 
    public final static String DATA_TRANSFER_REPORT_DEADLINE_KEY = "deadline"; 
    public final static String DATA_TRANSFER_REPORT_COMPLETION_TIME_KEY = 
                               "completion_time"; 
    public final static String DATA_TRANSFER_REPORT_TOTAL_TASKS_KEY = "total_tasks"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_SUCCESSFUL_KEY = 
    		                   "tasks_successful"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_EXPIRED_KEY = 
                               "tasks_expired"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_CANCELED_KEY = 
                               "tasks_canceled"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_FAILED_KEY = 
                               "tasks_failed"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_PENDING_KEY = 
                               "tasks_pending"; 
    public final static String DATA_TRANSFER_REPORT_TASKS_RETRYING_KEY = 
                               "tasks_retrying"; 
    public final static String DATA_TRANSFER_REPORT_COMMAND_KEY = "command"; 
    public final static String DATA_TRANSFER_REPORT_SOURCE_ENDPOINT_KEY = 
    		                   "source_endpoint"; 
    public final static String DATA_TRANSFER_REPORT_DESTINATION_ENDPOINT_KEY = 
                               "destination_endpoint"; 
    public final static String DATA_TRANSFER_REPORT_DATA_ENCRYPTION_KEY = 
                               "data_encryption"; 
    public final static String DATA_TRANSFER_REPORT_CHECKSUM_VERIFICATION_KEY = 
                               "checksum_verification"; 
    public final static String DATA_TRANSFER_REPORT_DELETE_KEY = "delete"; 
    public final static String DATA_TRANSFER_REPORT_FILES_KEY = "files"; 
    public final static String DATA_TRANSFER_REPORT_FILES_SKIPPED_KEY = 
    		                   "files_skipped";
    public final static String DATA_TRANSFER_REPORT_DIRECTORIES_KEY = "directories"; 
    public final static String DATA_TRANSFER_REPORT_EXPANSIONS_KEY = "expansions"; 
    public final static String DATA_TRANSFER_REPORT_BYTES_TRANSFERRED_KEY = 
    		                   "bytes_transferred";
    public final static String DATA_TRANSFER_REPORT_BYTES_CHECKSUMMED_KEY = 
                               "bytes_checksummed";
    public final static String DATA_TRANSFER_REPORT_EFFECTIVE_MBITS_PER_SEC_KEY = 
                               "effective_mbits_per_sec";
    public final static String DATA_TRANSFER_REPORT_FAULTS_KEY = "faults"; 
    
    // HpcFileMetadataVersion Document Keys.
    public final static String FILE_METADATA_VERSION_METADATA_KEY = "metadata"; 
    public final static String FILE_METADATA_VERSION_CREATED_KEY = "created"; 
    public final static String FILE_METADATA_VERSION_VERSION_KEY = "version"; 
    
    // HpcFileMetadataHistory Document Keys.
    public final static String FILE_METADATA_HISTORY_FILE_ID_KEY = "file_id"; 
    public final static String FILE_METADATA_HISTORY_MAX_VERSION_KEY = "max_version"; 
    public final static String FILE_METADATA_HISTORY_VERSIONS_KEY = "versions"; 
    
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

 