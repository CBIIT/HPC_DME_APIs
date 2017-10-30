/**
 * HpcGlobusDirectoryScanFileVisitor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.globus.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;

/**
 * <p>
 * HPC Globus Directory Scan File Visitor implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGlobusDirectoryScanFileVisitor implements HpcGlobusFileVisitor
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Scan items.
	private List<HpcDirectoryScanItem> scanItems = new ArrayList<>();
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the scanned items.
     * @return A list of files in the directory tree that was scanned.
     */
    public List<HpcDirectoryScanItem> getScanItems()
	{
		return scanItems;
	}
    		          
	//---------------------------------------------------------------------//
	// HpcDataTransferProxy Interface Implementation
	//---------------------------------------------------------------------//  
    
    @Override
    public void onFile(String path, JSONObject jsonFile) throws JSONException
    {
    	HpcDirectoryScanItem scanItem = new HpcDirectoryScanItem();
    	scanItem.setFileName(jsonFile.getString("name"));
    	scanItem.setFilePath((path + scanItem.getFileName()).replaceAll("//", "/"));
    	scanItem.setLastModified(jsonFile.getString("last_modified"));
    	
    	scanItems.add(scanItem);
    }
}
