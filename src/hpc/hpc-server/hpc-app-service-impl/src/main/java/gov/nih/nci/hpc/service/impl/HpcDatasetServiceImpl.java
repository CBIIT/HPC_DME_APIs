/**
 * HpcDatasetServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isEmptyFilePrimaryMetadata;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidDataTransferLocations;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileUploadRequest;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataItems;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isOverlapping;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.dao.HpcFileMetadataHistoryDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataHistory;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataVersion;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDatasetService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Dataset Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetServiceImpl implements HpcDatasetService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Dataset DAO instance.
	@Autowired
    private HpcDatasetDAO datasetDAO = null;
	
    // The Managed Dataset DAO instance.
	@Autowired
    private HpcFileMetadataHistoryDAO fileMetadataHistoryDAO = null;
    
    // Key Generator.
	@Autowired
    private HpcKeyGenerator keyGenerator = null;
    
	// Map data transfer status strings to enum values.
	private Map<String, HpcDataTransferStatus> dataTransferStatusMap = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDatasetServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferStatusMap The data transfer status string-<enum map.
     */
    private HpcDatasetServiceImpl(Map<String, HpcDataTransferStatus> dataTransferStatusMap)
    	                         throws HpcException
    {
    	this.dataTransferStatusMap = dataTransferStatusMap;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataset addDataset(String name, String description, 
    		                     String comments, boolean persist) 
		                        throws HpcException
    {
    	// Input validation.
    	if(name == null || description == null) {
    	   throw new HpcException("Invalid add dataset input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Create the Dataset domain object.
    	HpcDataset dataset = new HpcDataset();
    	
    	// Generate and set its ID.
    	dataset.setId(keyGenerator.generateKey());
    	
    	// Set created date.
    	dataset.setCreated(Calendar.getInstance());
    	
    	// Associate the FileSet.
    	HpcFileSet fileSet = new HpcFileSet();

    	// Populate attributes.
    	fileSet.setName(name);
    	fileSet.setDescription(description);
    	fileSet.setComments(comments);
    	dataset.setFileSet(fileSet);
    	
    	// Persist if requested.
    	if(persist) {
    	   persist(dataset);
    	}
    	
    	logger.debug("Dataset added: " + dataset);
    	return dataset;
    }
    
    @Override
    public HpcFile addFile(HpcDataset dataset,
                           HpcFileUploadRequest uploadRequest,
                           boolean persist) 
                          throws HpcException
    {
       	// Input validation.
       	if(dataset == null || uploadRequest == null || 
       	   !isValidFileUploadRequest(uploadRequest)) {
       	   throw new HpcException("Invalid add file input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       
		// Map the upload request to a managed file instance.
		HpcFile file = new HpcFile();
		file.setId(keyGenerator.generateKey());
		file.setType(uploadRequest.getType());
		file.setSize(0);
		file.setSource(uploadRequest.getLocations().getSource());
		file.setLocation(uploadRequest.getLocations().getDestination());
		if(uploadRequest.getProjectIds() != null) {
		   // Add the associated projects if not already associated.
		   for(String projectId : uploadRequest.getProjectIds()) {
			   associateProject(file, projectId);
		   }
		}
		
		// Set the metadata.
		HpcFileMetadata metadata = new HpcFileMetadata();
		metadata.setPrimaryMetadata(uploadRequest.getMetadata());
		file.setMetadata(metadata);
		
		// Add the managed file to the dataset.
		dataset.getFileSet().getFiles().add(file);
		
		// Persist if requested.
    	if(persist) {
     	   persist(dataset);
     	}
     	
     	logger.debug("File added: " + file);
     	return file;
    }
    
    @Override
    public HpcDataTransferRequest addDataTransferUploadRequest(
                                  HpcDataset dataset, String requesterNihUserId, 
                                  String fileId, HpcDataTransferLocations locations,
                                  HpcDataTransferReport report, boolean persist) 
                                  throws HpcException
    {
    	HpcDataTransferRequest dataTransferRequest = 
    	   newDataTransferRequest(requesterNihUserId, fileId, locations, report);
    	   dataset.getUploadRequests().add(dataTransferRequest);
    	   return dataTransferRequest;
    }
    
    @Override
    public HpcDataTransferRequest addDataTransferDownloadRequest(
                                  HpcDataset dataset, String requesterNihUserId, 
                                  String fileId, HpcDataTransferLocations locations,
                                  HpcDataTransferReport report, boolean persist) 
                                  throws HpcException
    {
    	HpcDataTransferRequest dataTransferRequest = 
    	   newDataTransferRequest(requesterNihUserId, fileId, locations, report);
    	   dataset.getDownloadRequests().add(dataTransferRequest);
    	   return dataTransferRequest;
    }
    
    @Override
    public HpcFilePrimaryMetadata   
           addPrimaryMetadataItems(HpcDataset dataset, String fileId,
                                   List<HpcMetadataItem> metadataItems,
                                   boolean persist) 
                                  throws HpcException
    {
       	// Input validation.
       	if(dataset == null || !keyGenerator.validateKey(fileId) || 
       	   !isValidMetadataItems(metadataItems)) {
       	   throw new HpcException("Invalid add metadata items input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Locate the file to attach the metadata items.
       	HpcFile file = getFile(dataset, fileId);
       	if(file == null) {
       	   throw new HpcException("File not found: " + fileId, 
       			                  HpcRequestRejectReason.FILE_NOT_FOUND);
       	}
       	
       	// Validate the request doesn't include an existing metadata item.
       	if(isOverlapping(file.getMetadata().getPrimaryMetadata().getMetadataItems(),
       			         metadataItems)) {
           throw new HpcException("At least one metadata item already exists", 
		                          HpcRequestRejectReason.METADATA_ITEM_ALREADY_EXISTS);
       	}
       	
       	// Add the metadata items.
       	file.getMetadata().getPrimaryMetadata().getMetadataItems().addAll(metadataItems);
       	
		// Persist if requested.
    	if(persist) {
     	   persist(dataset);
     	}
    	
    	return file.getMetadata().getPrimaryMetadata();
    }
    @Override
    public HpcFilePrimaryMetadata 
           updatePrimaryMetadata(HpcDataset dataset, String fileId,
 		                         HpcFilePrimaryMetadata primaryMetadata, 
 		                         boolean persist) 
                                throws HpcException
    {
       	// Input validation.
       	if(dataset == null || !keyGenerator.validateKey(fileId) || 
       	   primaryMetadata == null ||
       	   isEmptyFilePrimaryMetadata(primaryMetadata) ||
       	   (primaryMetadata.getMetadataItems() != null &&
 	        !isValidMetadataItems(primaryMetadata.getMetadataItems()))) {
       	   throw new HpcException("Invalid update primary metadata input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
    	
       	// Locate the file to attach the metadata items.
       	HpcFile file = getFile(dataset, fileId);
       	if(file == null) {
       	   throw new HpcException("File not found: " + fileId, 
       			                  HpcRequestRejectReason.FILE_NOT_FOUND);
       	}
    	
    	// Update the metadata domain object.
    	updatePrimaryMetadata(file, primaryMetadata);
       	
		// Persist if requested.
    	if(persist) {
     	   persist(dataset);
     	}
    	
    	return file.getMetadata().getPrimaryMetadata();    	
    }
    
    @Override
    public void associateProject(HpcDataset dataset, String fileId,
                                 String projectId, boolean persist) 
                                throws HpcException
    {
       	// Input validation.
       	if(dataset == null || !keyGenerator.validateKey(fileId) || 
       	   !keyGenerator.validateKey(projectId)) {
       	   throw new HpcException("Invalid project association input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
    	
       	// Locate the file to attach the metadata items.
       	HpcFile file = getFile(dataset, fileId);
       	if(file == null) {
       	   throw new HpcException("File not found: " + fileId, 
       			                  HpcRequestRejectReason.FILE_NOT_FOUND);
       	}
       	
       	// Associate the project to this file. 
       	associateProject(file, projectId);
       	
		// Persist if requested.
    	if(persist) {
     	   persist(dataset);
     	}
    }
    
    @Override
    public void addFileMetadataVersion(String fileId, 
    		                            HpcFileMetadata metadata,
    		                            boolean persist)
    		                           throws HpcException
	{
    	// Get the file metadata history from DB. If not found, create a new one.
    	HpcFileMetadataHistory metadataHistory = 
    			               fileMetadataHistoryDAO.getFileMetadataHistory(fileId);
    	if(metadataHistory == null) {
    	   metadataHistory = new HpcFileMetadataHistory();
    	   metadataHistory.setFileId(fileId);
    	   metadataHistory.setMaxVersion(0);
    	}
    	
    	// Calculate the new version.
    	int newVersion = metadataHistory.getMaxVersion() + 1;
    	
    	// Create a new metadata version.
    	HpcFileMetadataVersion metadataVersion = new HpcFileMetadataVersion();
    	metadataVersion.setMetadata(metadata);
    	metadataVersion.setVersion(newVersion);
    	metadataVersion.setCreated(Calendar.getInstance());
    	
    	// Attach the new version and update the max version.
    	metadataHistory.getVersions().add(metadataVersion);
    	metadataHistory.setMaxVersion(newVersion);
    	
    	// Persist.
    	if(persist) {
    	   persist(metadataHistory);
    	}
	}
           
    @Override
    public boolean setDataTransferRequestStatus(HpcDataTransferRequest dataTransferRequest, 
    		                                    HpcDataTransferReport dataTransferReport)
    {
    	// Attach the new report to the transfer request.
    	dataTransferRequest.setReport(dataTransferReport);
    
    	// Calculate and set the data transfer status.
    	HpcDataTransferStatus currentTransferStatus = dataTransferRequest.getStatus();
    	HpcDataTransferStatus newTransferStatus = HpcDataTransferStatus.ERROR;
    	if(dataTransferReport != null) {
    	   newTransferStatus = dataTransferReport.getStatus() != null ?
    			               dataTransferStatusMap.get(dataTransferReport.getStatus()) :
    			               HpcDataTransferStatus.UNKONWN; 	 
    	} 
    	dataTransferRequest.setStatus(newTransferStatus);
    	
    	// Return true if the transfer status has changed.
    	return (newTransferStatus != currentTransferStatus); 
    }
    
    @Override
    public HpcDataset getDataset(String id) throws HpcException
    {
    	// Input validation.
    	if(!keyGenerator.validateKey(id)) {
    	   throw new HpcException("Invalid dataset ID: " + id, 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	HpcDataset hpcDataset = datasetDAO.getDataset(id);

    	return hpcDataset;
    }
    
    @Override
    public HpcFile getFile(String id) throws HpcException
    {
    	// Input validation.
    	if(!keyGenerator.validateKey(id)) {
    	   throw new HpcException("Invalid dataset ID: " + id, 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return datasetDAO.getFile(id);
    }
    
    @Override
    public List<HpcDataset> getDatasets(List<String> userIds, HpcDatasetUserAssociation association) 
 	                                   throws HpcException
 	{
    	return datasetDAO.getDatasets(userIds, association);
 	}
    
    @Override
    public List<HpcDataset> getDatasets(String name) throws HpcException
 	{
    	return datasetDAO.getDatasets(name);
 	}
    
    @Override
    public List<HpcDataset> getDatasets(HpcFilePrimaryMetadata primaryMetadata) 
                                       throws HpcException
    {
    	// Input Validation. At least one metadata element needs to be provided
    	// to query for.
    	if(isEmptyFilePrimaryMetadata(primaryMetadata)) { 
    	   throw new HpcException("Invalid primary metadata", 
                                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Validate metada items if not null.
    	if(primaryMetadata.getMetadataItems() != null &&
    	   !isValidMetadataItems(primaryMetadata.getMetadataItems())) {
    	   throw new HpcException("Invalid metadata items", 
                                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return datasetDAO.getDatasets(primaryMetadata);
    }
    
    @Override
	public List<HpcDataset> getDatasets(HpcDataTransferStatus dataTransferStatus,
                                        Boolean uploadRequests, 
                                        Boolean downloadRequests) throws HpcException
	{
    	// Input Validation. Both upload/download requests can't be null
    	if((uploadRequests == null || uploadRequests == false) &&
    	   (downloadRequests == null || downloadRequests == false)) {
    	   throw new HpcException("Both upload/download Requests can't be null", 
                     HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	List<HpcDataset> datasets = new ArrayList<HpcDataset>();
    	
    	// Search the upload requests.
    	if(uploadRequests != null && uploadRequests) {
    	   datasets.addAll(datasetDAO.getDatasets(dataTransferStatus, true));
    	}
    	
    	// Search the download requests.
    	if(downloadRequests != null && downloadRequests) {
    	   datasets.addAll(datasetDAO.getDatasets(dataTransferStatus, false));
    	}
    	
    	return datasets;
	}

	@Override
	public List<HpcDataset> getDatasetsByProjectId(String projectId)
			throws HpcException {
		return datasetDAO.getDatasetsByProjectId(projectId);
	}
	
	@Override
    public HpcFile getFile(HpcDataset dataset, String fileId)
	{
    	for(HpcFile file : dataset.getFileSet().getFiles()) {
    		if(file.getId().equals(fileId)) {
    		   return file;
    		}
    	}
    	
    	return null;
	}
	
    @Override
    public boolean exists(String name, String nihUserId, HpcDatasetUserAssociation association) 
                         throws HpcException
    {
    	return datasetDAO.exists(name, nihUserId, association);
    }
    
    @Override
    public void persist(HpcDataset dataset) throws HpcException
    {
    	if(dataset != null) {
    	   dataset.setLastUpdated(Calendar.getInstance());
    	   datasetDAO.upsert(dataset);
    	}
    }
    
    @Override
    public void persist(HpcFileMetadataHistory metadataHistory) throws HpcException
    {
    	if(metadataHistory != null) {
    	   fileMetadataHistoryDAO.upsert(metadataHistory);
    	}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Instantiate a data transfer request object.
     *
     * @param requesterNihUserId The user-id initiated the upload.
     * @param fileId The uploaded file ID.
     * @param locations The transfer source and destination.
     * @param report The data transfer report.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new data transfer request
     * 
     * @throws HpcException
     */
    private HpcDataTransferRequest newDataTransferRequest(
                           String requesterNihUserId, String fileId, 
                           HpcDataTransferLocations locations,
                           HpcDataTransferReport report) throws HpcException
	{
       	// Input validation.
       	if(requesterNihUserId == null || fileId == null ||
       	   !isValidDataTransferLocations(locations)) {
       	   throw new HpcException("Invalid add data transfer request", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
		HpcDataTransferRequest dataTransferRequest = new HpcDataTransferRequest(); 
		dataTransferRequest.setReport(report);
		dataTransferRequest.setFileId(fileId);
		dataTransferRequest.setRequesterNihUserId(requesterNihUserId);
		dataTransferRequest.setLocations(locations);
		dataTransferRequest.setDataTransferId(report != null && report.getTaskID() != null ?
				                              report.getTaskID() : "UNKNOWN");
		setDataTransferRequestStatus(dataTransferRequest, report);
		
		return dataTransferRequest;
	}
  
    /**
     * Update a primary metadata object of a file.
     *
     * @param file The file to apply the update.
     * @param update The updated metadata.
     * 
     * @throws HpcException If the metadata to update was not found.
     */
    private void updatePrimaryMetadata(HpcFile file, HpcFilePrimaryMetadata update)
                                      throws HpcException
    {
    	HpcFilePrimaryMetadata metadata = file.getMetadata().getPrimaryMetadata();
    	if(update.getCreatorName() != null) {
    	   metadata.setCreatorName(update.getCreatorName());	
    	}
    	if(update.getDataCompressed() != null) {
     	   metadata.setDataCompressed(update.getDataCompressed());	
     	}
    	if(update.getDataContainsPHI() != null) {
      	   metadata.setDataContainsPHI(update.getDataContainsPHI());	
      	}
    	if(update.getDataContainsPII() != null) {
       	   metadata.setDataContainsPII(update.getDataContainsPII());	
       	}
    	if(update.getDataEncrypted() != null) {
           metadata.setDataEncrypted(update.getDataEncrypted());	
        }
    	if(update.getDescription() != null) {
           metadata.setDescription(update.getDescription());	
        }
    	if(update.getFundingOrganization() != null) {
           metadata.setFundingOrganization(update.getFundingOrganization());	
        }
    	if(update.getLabBranch() != null) {
           metadata.setLabBranch(update.getLabBranch());	
        }
    	if(update.getOriginallyCreated() != null) {
           metadata.setOriginallyCreated(update.getOriginallyCreated());	
        }
    	if(update.getPrincipalInvestigatorDOC() != null) {
           metadata.setPrincipalInvestigatorDOC(update.getPrincipalInvestigatorDOC());	
        }
    	if(update.getPrincipalInvestigatorNihUserId() != null) {
           metadata.setPrincipalInvestigatorNihUserId(update.getPrincipalInvestigatorNihUserId());	
        }
    	if(update.getRegistrarDOC() != null) {
           metadata.setRegistrarDOC(update.getRegistrarDOC());	
        }
    	if(update.getRegistrarNihUserId() != null) {
           metadata.setRegistrarNihUserId(update.getRegistrarNihUserId());	
        }
    	if(update.getMetadataItems() != null && !update.getMetadataItems().isEmpty()) {
    	   for(HpcMetadataItem metadataItem : update.getMetadataItems()) {
    		   updateMetadataItem(metadata.getMetadataItems(), metadataItem);
    	   }
        }
    }
    
    /**
     * Upsert a metadata item.
     *
     * @param metadataItems The metadata items to update.
     * @param metadataItem The item to update.
     * 
     * @throws HpcException If the metadata to update was not found.
     */
    private void updateMetadataItem(List<HpcMetadataItem> metadataItems, 
    		                        HpcMetadataItem metadataItem)
    		                       throws HpcException
    {
    	for(HpcMetadataItem item : metadataItems) {
    		if(item.getKey().equals(metadataItem.getKey())) {
    		   item.setValue(metadataItem.getValue());	
    		   return;
    		}
    	}
    	
    	throw new HpcException("Metadata item not found: " + metadataItem.getKey(), 
	                           HpcRequestRejectReason.METADATA_ITEM_NOT_FOUND);
    }
    
    /**
     * Associate project with a file
     *
     * @param file The file to associate the project with.
     * @param projectId The project ID
     */
    private void associateProject(HpcFile file, String projectId)
    {
    	if(file != null && projectId != null) {
		   // Associate the project to the file. No dups allowed.
		   if(!file.getProjectIds().contains(projectId)) {
			  file.getProjectIds().add(projectId);
		   }
		}
    }
}

 