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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileUploadRequest;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcDatasetService;
import gov.nih.nci.hpc.service.HpcTransferStatusService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private HpcDatasetDAO datasetDAO = null;
    
    // Key Generator.
    private HpcKeyGenerator keyGenerator = null;
    
    private HpcUserService userService = null;
    private HpcDataTransferService dataTransferService = null;
    private HpcTransferStatusService transferStatusService = null;    
	@SuppressWarnings("rawtypes")
	private Map<String, String> collections = null;
    
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
     * @param datasetDAO The dataset DAO instance.
     * @param keyGenerator The key generator.
     */
    private HpcDatasetServiceImpl(HpcDatasetDAO datasetDAO, 
    		                      HpcKeyGenerator keyGenerator,
    		    		          HpcUserService userService,
    		    		          HpcDataTransferService dataTransferService,
    		    		          HpcTransferStatusService transferStatusService,
    		    		          Map<String, String> collections)
    		                     throws HpcException
    {
    	if(datasetDAO == null || keyGenerator == null) {
     	   throw new HpcException("Null DatasetDAO / HpcKeyGenerator",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.datasetDAO = datasetDAO;
    	this.keyGenerator = keyGenerator;
    	this.userService = userService;
    	this.dataTransferService = dataTransferService;
    	this.transferStatusService = transferStatusService;
    	this.collections = collections;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(String name, String description, String comments,
		              List<HpcFileUploadRequest> uploadRequests) 
		             throws HpcException
    {
    	// Input validation.
    	if(name == null || description == null ||
    	   uploadRequests == null || uploadRequests.size() == 0) {
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
    	
       	// Attach the files to this dataset.
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		// Validate the upload file request.
    		if(!isValidFileUploadRequest(uploadRequest)) {
    		   throw new HpcException("Invalid file upload request: " + 
    		                          uploadRequest, 
		                              HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    		
    		// Map the upload request to a managed file instance.
    		HpcFile file = new HpcFile();
    		file.setId(keyGenerator.generateKey());
    		file.setType(uploadRequest.getType());
    		file.setSize(0);
    		file.setSource(uploadRequest.getLocations().getSource());
    		file.setLocation(uploadRequest.getLocations().getDestination());
    		
    		// Set the metadata.
    		HpcFileMetadata metadata = new HpcFileMetadata();
    		metadata.setPrimaryMetadata(uploadRequest.getMetadata());
    		file.setMetadata(metadata);
    		
    		// Add the managed file to the dataset.
    		fileSet.getFiles().add(file);
    		
    		//Transfer file 
    		HpcDataTransferAccount dataTransferAccount = 
    	    		   userService.get(
    	    			   uploadRequest.getMetadata().getRegistrarNihUserId()).
    	    			                               getDataTransferAccount();
    	        	
    	        	// Submit data transfer request for this file.
    	    		logger.info("Submiting Data Transfer Request: "+ 
    	        	            uploadRequest.getLocations());
    	    		
    		HpcDataTransferReport hpcDataTransferReport = 
    				dataTransferService.transferDataset(
    				                    uploadRequest.getLocations(), 
    				                    dataTransferAccount,
    				                    uploadRequest.getMetadata().getRegistrarNihUserId());
    		logger.info("Data Transfer Report : " + hpcDataTransferReport);    	    		
    		//Add status 
    		
     		HpcDataTransferRequest hpcDataTransferRequest = new HpcDataTransferRequest(); 
     		
     		hpcDataTransferRequest.setReport(hpcDataTransferReport);
     		hpcDataTransferRequest.setFileId(file.getId());
     		hpcDataTransferRequest.setStatus(HpcDataTransferStatus.valueOf(collections.get(hpcDataTransferReport.getStatus())));
     		hpcDataTransferRequest.setDataTransferId(hpcDataTransferReport.getTaskID());
     		//hpcDataTransferRequest.setLocations(uploadRequest.getLocations());
     		dataset.getUploadRequests().add(hpcDataTransferRequest);
     		// add status to dataset 
     		//transferStatusService.addUpdateStatus(hpcDataTransferRequest);
        	
    	}
    	dataset.setFileSet(fileSet);
    	
    	datasetDAO.add(dataset);
    	logger.debug("Dataset added: " + dataset);
    	
    	return dataset.getId();
    }
    
    @Override
    public HpcDataset getDataset(String id) throws HpcException
    {
    	// Input validation.
    	if(!keyGenerator.validateKey(id)) {
    	   throw new HpcException("Invalid dataset ID: " + id, 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	HpcDataset hpcDataset  = datasetDAO.getDataset(id);
    	if(!hpcDataset.getFileSet().getFiles().isEmpty())
    	{
    		for(HpcDataTransferRequest uploadRequest : hpcDataset.getUploadRequests())
    		{
        		HpcDataTransferAccount dataTransferAccount = 
     	    		   userService.get(
     	    				  hpcDataset.getFileSet().getFiles().get(1).getMetadata().getPrimaryMetadata().getRegistrarNihUserId()).
     	    			                               getDataTransferAccount();
        		
    			HpcDataTransferReport hpcDataTransferReport = dataTransferService.retriveTransferStatus(uploadRequest.getDataTransferId(),dataTransferAccount);
    			uploadRequest.setReport(hpcDataTransferReport);
    			uploadRequest.setStatus(HpcDataTransferStatus.valueOf(collections.get(hpcDataTransferReport.getStatus())));
    			
    			datasetDAO.updateReplace(hpcDataset);
    		}
    	}
    	return hpcDataset;
    }
    
    @Override
    public List<HpcDataset> getDatasets(String userId, HpcDatasetUserAssociation association) 
 	                                   throws HpcException
 	{
    	return datasetDAO.getDatasets(userId, association);
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

	public List<HpcDataset> getDatasetsByStatus(String transferStatus) throws HpcException{
		return datasetDAO.getDatasetsByStatus(transferStatus);
	}
}

 