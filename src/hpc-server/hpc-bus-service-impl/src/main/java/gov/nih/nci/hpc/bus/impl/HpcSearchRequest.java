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
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.service.HpcDataSearchService;

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
	
	private final String dataManagementUsername;
	
	private final String path;
	
	private final int page;
	
	private final int limit;
	
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
    HpcSearchRequest(HpcDataSearchService dataSearchService, String dataManagementUsername, String path, int page, int limit) 
    {
    	this.dataSearchService = dataSearchService;
    	this.dataManagementUsername = dataManagementUsername;
    	this.path = path;
    	this.page = page;
    	this.limit = limit;
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
	@Override
	public HpcDataObjectListDTO call() throws Exception {
		
		return toDetailedDataObjectListDTO(dataSearchService.getAllDataObjectPaths(dataManagementUsername, path, page, limit));
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
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(dataObjectPath, entry);
				dataObject.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(dataObjectPath, entry);
				dataObject.getMetadataEntries().getParentMetadataEntries().add(entry);
			}
			prevId = dataObjectPath.getId();
		}
		return dataObjectsDTO;
	}
}

 