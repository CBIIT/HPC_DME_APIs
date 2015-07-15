/**
 * HpcTransferStatusBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcTransferStatusBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcDatasetService;
import gov.nih.nci.hpc.service.HpcTransferStatusService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC TransferStatus Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcDatasetBusServiceImpl.java 292 2015-07-05 21:26:40Z narram $
 */

public class HpcTransferStatusBusServiceImpl implements HpcTransferStatusBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    private HpcTransferStatusService transferStatusService = null;
    
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
    private HpcTransferStatusBusServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataService The dataset application service.
     * @param userService The user application service.
     * @param dataTransferService The data transfer application service.
     * 
     * @throws HpcException If any application service provided is null.
     */
    private HpcTransferStatusBusServiceImpl(
    		HpcTransferStatusService transferStatusService)
                      throws HpcException
    {
    	if(transferStatusService == null) {
     	   throw new HpcException("Null App Service(s) instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}

    	this.transferStatusService = transferStatusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    public String retriveSaveTransferStatus(String datasetId)  
    		                     throws HpcException
    {
    	logger.info("Invoking retriveSaveTransferStatus(datasetId): " + 
    			datasetId);
//    	
//    	for(HpcFile hpcFile : 
////   		   datasetService.getDataset(datasetId).getFileSet().getFiles()) { 
////      		HpcDataTransferRequest hpcDataTransferRequest = new HpcDataTransferRequest(); 
////          	
////      		hpcDataTransferRequest.setReport(hpcDataTransferReport);
////      		hpcDataTransferRequest.setFileId(hpcFile.getId());
////      		hpcDataTransferRequest.setLocations(uploadRequest.getLocations());
////
////         	HpcDataTransferRequest statusId = 
////          		   transferStatusService.addUpdateStatus(hpcDataTransferRequest);
////          	logger.info("statusId  = " + statusId);
//      	}

    	return datasetId;
    }
}

 