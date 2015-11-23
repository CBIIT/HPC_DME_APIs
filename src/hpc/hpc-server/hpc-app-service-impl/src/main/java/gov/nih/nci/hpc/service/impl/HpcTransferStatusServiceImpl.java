/**
 * HpcTransferStatusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcTransferStatusDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcTransferStatusService;

/**
 * <p>
 * HPC Data Transfer Service Implementation.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcTransferStatusServiceImpl.java 291 2015-07-05 14:05:20Z narram $
 */

public class HpcTransferStatusServiceImpl implements HpcTransferStatusService
{            

    private HpcTransferStatusDAO transferStatusDao = null;
    private HpcDataTransferProxy dataTransferProxy = null;

    

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcTransferStatusServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param transferStatusDao The transfer status DAO instance.
     */
    private HpcTransferStatusServiceImpl(HpcTransferStatusDAO transferStatusDao,
    								HpcDataTransferProxy dataTransferProxy) 
    		   throws HpcException
    {
    	if(transferStatusDao == null || dataTransferProxy == null) {
     	   throw new HpcException("Null Integration beans",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.transferStatusDao = transferStatusDao;
    	this.dataTransferProxy = dataTransferProxy;
    }      
     
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataTransferRequest addUpdateStatus(HpcDataTransferRequest hpcDataTransferRequest) 
	                             throws HpcException
    {   
//    	try{
//		    	HpcDataTransferReport hpcDataTransferReport = dataTransferProxy.getTaskStatusReport(hpcDataTransferRequest.getDataTransferId());
//		    	hpcDataTransferRequest.setReport(hpcDataTransferReport);
//		    	// Persist to Mongo.
//		    	transferStatusDao.add(hpcDataTransferRequest);
//		    	//hpcDataTransferRequest.setDataTransferId(statusId);
//		    	return hpcDataTransferRequest;
//			} 
//    		catch(Exception ex) {
//			    throw new HpcException("Error while retriving status",
//			    		               HpcErrorType.DATA_TRANSFER_ERROR);
//		}
    	return null;
    }    
}
 