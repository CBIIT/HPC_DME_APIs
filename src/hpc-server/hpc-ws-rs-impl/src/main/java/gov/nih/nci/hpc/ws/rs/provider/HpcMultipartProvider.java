/**
 * HpcMultipartProvider.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.provider;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.provider.MultipartProvider;

/**
 * <p>
 * Multipart Provider
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMultipartProvider extends MultipartProvider
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The temporary directory to store attachments.
    private String tempDirectory = null;
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the temporary directory.
     * 
     * @return The temporary directory.
     */
	public String getTempDirectory()
	{
		return tempDirectory;
	}
	
    //---------------------------------------------------------------------//
    // MultipartProvider Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public void setAttachmentDirectory(String directory)
    {
    	// Create the directory.
    	try {
    	     FileUtils.forceMkdir(new File(directory));
    	     
    	} catch(IOException e) {
    		    throw new IllegalArgumentException("Faield to create tmp dir: " + directory, e);
    	}
    	
    	tempDirectory = directory;
    	super.setAttachmentDirectory(directory);
    }
}
