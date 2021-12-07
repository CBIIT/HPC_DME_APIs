/**
 * HpcSearchRequest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataSearchService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * HPC HpcSearchRequest. 
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcSearchRequest implements Callable<HpcDataObjectListDTO>
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Data Search Application Service instance.
	private HpcDataSearchService dataSearchService = null;
	
	// Security Application Service instance.
	private HpcSecurityService securityService = null;
		
	private final String dataManagementUsername;
	
	private final String path;
	
	private final int offset;
	
	private final int pageSize;
	
	private final boolean encrypt;
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
	
    /**
     * Default Constructor.
     * 
     */
    HpcSearchRequest(HpcDataSearchService dataSearchService, HpcSecurityService securityService, String dataManagementUsername, String path, int offset, int pageSize, boolean encrypt) 
    {
    	this.dataSearchService = dataSearchService;
    	this.securityService = securityService;
    	this.dataManagementUsername = dataManagementUsername;
    	this.path = path;
    	this.offset = offset;
    	this.pageSize = pageSize;
    	this.encrypt = encrypt;
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
	@Override
	public HpcDataObjectListDTO call() throws Exception {
		
		return toDetailedDataObjectListDTO(dataSearchService.getAllDataObjectPaths(dataManagementUsername, path, offset, pageSize));
	}
	
	private HpcDataObjectListDTO toDetailedDataObjectListDTO(List<HpcSearchMetadataEntry> dataObjectPaths) {	
		
		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
		if (!CollectionUtils.isEmpty(dataObjectPaths)) {
			dataObjectPaths
					.sort(Comparator.comparing(HpcSearchMetadataEntry::getAbsolutePath, String::compareToIgnoreCase)
							.thenComparing(HpcSearchMetadataEntry::getLevel));
		}
		int prevId = 0;
		HpcDataObjectDTO dataObject = null;
		for (HpcSearchMetadataEntry dataObjectPath : dataObjectPaths) {
			HpcMetadataEntry entry = new HpcMetadataEntry();
			BeanUtils.copyProperties(dataObjectPath, entry);
			HpcQueryConfiguration queryConfig = null;
			HpcEncryptor encryptor = null;
			if (encrypt) {
				try {
					String basePath = dataObjectPath.getAbsolutePath().substring(0, dataObjectPath.getAbsolutePath().indexOf('/', 1));
					queryConfig = securityService.getQueryConfig(basePath);
					if(queryConfig != null)
						encryptor = new HpcEncryptor(queryConfig.getEncryptionKey());
				} catch (HpcException e) {
					// Failed to get encryptor so don't return the value
					entry.setValue("");
				}
			}
			if (dataObject == null || dataObjectPath.getId() != prevId) {
				dataObject = new HpcDataObjectDTO();
				HpcDataObject dataObj = new HpcDataObject();
				BeanUtils.copyProperties(dataObjectPath, dataObj);
				dataObject.setDataObject(dataObj);
				dataObjectsDTO.getDataObjects().add(dataObject);
			}
			if (dataObject.getMetadataEntries() == null) {
				HpcMetadataEntries entries = new HpcMetadataEntries();
				dataObject.setMetadataEntries(entries);
			}
			if (dataObjectPath.getLevel().intValue() == 1) {
				if(encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
				dataObject.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				if(encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
				dataObject.getMetadataEntries().getParentMetadataEntries().add(entry);
			}
			prevId = dataObjectPath.getId();
		}
		return dataObjectsDTO;
	}
}

 