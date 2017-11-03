/**
 * HpcGlobusDirectorySizeFileVisitor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.globus.impl;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * HPC Globus Directory Size (calculator) File Visitor implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGlobusDirectorySizeFileVisitor implements HpcGlobusFileVisitor
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Directory size.
	private long size = 0;
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the calculated directory size.
     * @return The directory size.
     */
    public long getSize()
	{
		return size;
	}
    		          
	//---------------------------------------------------------------------//
	// HpcDataTransferProxy Interface Implementation
	//---------------------------------------------------------------------//  
    
    @Override
    public void onFile(String path, JSONObject jsonFile) throws JSONException
    {
    	size += jsonFile.getLong("size");
    }
}
