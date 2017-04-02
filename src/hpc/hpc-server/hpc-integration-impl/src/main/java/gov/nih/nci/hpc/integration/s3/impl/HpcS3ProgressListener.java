/**
 * HpcS3ProgressListener.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.s3.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

/**
 * <p>
 * HPC S3 Progress Listener. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcS3Connection.java 1717 2016-12-01 22:39:40Z rosenbergea $
 */

public class HpcS3ProgressListener implements ProgressListener
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// HPC progress listener
	HpcDataTransferProgressListener progressListener = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor.
     * 
     * @param progressListener The HPC progress listener.
     */
    public HpcS3ProgressListener(HpcDataTransferProgressListener progressListener)
                                throws HpcException
    {
    	if(progressListener == null) {
    	   throw new HpcException("Null progress listener", HpcErrorType.UNEXPECTED_ERROR);
    	}
        this.progressListener = progressListener;
    }
    
	/**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	@SuppressWarnings("unused")
	private HpcS3ProgressListener() throws HpcException
    {
    	throw new HpcException("Constructor Disabled", HpcErrorType.UNEXPECTED_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
	@Override
    public void progressChanged(ProgressEvent event)
    {
		switch(event.getEventType()) {
		       case TRANSFER_COMPLETED_EVENT:
		    	    progressListener.transferCompleted();
		    	    break;
		    	    
		       case TRANSFER_FAILED_EVENT:
		       case TRANSFER_CANCELED_EVENT:
		    	    progressListener.transferFailed();
		    	    break;
		    	   
		    	default:
		    		break;
		}
    }
}

 