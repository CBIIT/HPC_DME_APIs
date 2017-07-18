/**
 * HpcScheduledTask.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.scheduler.impl;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Scheduled task interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcScheduledTask 
{   
    /**
     * Execute the scheduled task
     *
     * @throws HpcException on task execution failure.
     */
    public void execute() throws HpcException;
}

 