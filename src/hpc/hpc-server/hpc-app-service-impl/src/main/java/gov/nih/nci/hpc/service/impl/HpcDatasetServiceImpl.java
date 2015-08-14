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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidDataTransferLocations;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileUploadRequest;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
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
    	
    	// Associate the FileSet.
    	HpcFileSet fileSet = new HpcFileSet();

    	// Populate attributes.
    	fileSet.setName(name);
    	fileSet.setDescription(description);
    	fileSet.setComments(comments);
    	fileSet.setCreated(Calendar.getInstance());
    	dataset.setFileSet(fileSet);
    	
    	// Persist if requested.
    	if(persist) {
    	   datasetDAO.upsert(dataset);
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
			   if(!file.getProjectIds().contains(projectId)) {
				  file.getProjectIds().add(projectId);
			   }
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
     	   datasetDAO.upsert(dataset);
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
    public void persist(HpcDataset dataset) throws HpcException
    {
    	if(dataset != null) {
    	   datasetDAO.upsert(dataset);
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
    	if(primaryMetadata == null ||
    	   (primaryMetadata.getDataContainsPII() == null && 	
    		primaryMetadata.getDataContainsPHI() == null &&
    		primaryMetadata.getDataEncrypted() == null &&
    		primaryMetadata.getDataCompressed() == null &&
    		primaryMetadata.getFundingOrganization() == null && 
    		primaryMetadata.getPrimaryInvestigatorNihUserId() == null &&
    		primaryMetadata.getCreatorName() == null &&
    		primaryMetadata.getRegistrarNihUserId() == null &&
    		primaryMetadata.getDescription() == null &&
    		primaryMetadata.getLabBranch() == null &&
    		primaryMetadata.getMetadataItems() == null)) {
    		throw new HpcException("Invalid primary metadata", 
                                   HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return datasetDAO.getDatasets(primaryMetadata);
    }
    
    @Override
    public boolean exists(String name, String nihUserId, HpcDatasetUserAssociation association) 
                         throws HpcException
    {
    	return datasetDAO.exists(name, nihUserId, association);
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
}

 