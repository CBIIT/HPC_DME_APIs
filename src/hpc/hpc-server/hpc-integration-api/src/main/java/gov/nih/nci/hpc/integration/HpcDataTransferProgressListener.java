/**
 * HpcDataTransferProgressListener.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

/**
 * <p>
 * HPC Data Transfer Completion Listener Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$ 
 */

public interface HpcDataTransferProgressListener 
{    
    /**
     * Called when data transfer request (upload or download) completed.
     */
    public void transferCompleted();
    
    /**
     * Called when data transfer request (upload or download) failed.
     */
    public void transferFailed();
}
