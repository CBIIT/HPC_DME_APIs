/**
 * HpcSystemBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC System Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcSystemBusService 
{         
    /**
     * Update the data transfer upload status of all data objects that the transfer is 'in progress'.
     *
     * @throws HpcException.
     */
	public void updateDataTransferUploadStatus() throws HpcException;
	
    /**
     * Transfer data objects currently in temporary archive to the (permanent) archive, 
     * and complete the registration process.
     *
     * @throws HpcException
     */
	public void processTemporaryArchive() throws HpcException;
	
    /**
     * Detect all the completed (aync) 2nd hop download requests and delete the files 
     * in the download space.
     *
     * @throws HpcException
     */
	public void cleanupDataTransferDownloadFiles() throws HpcException;
	
    /**
     * Process all (active) events.
     *
     * @throws HpcException
     */
	public void processEvents() throws HpcException;
}

 