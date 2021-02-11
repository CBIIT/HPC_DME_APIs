/**
 * HpcDataTieringDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;

import gov.nih.nci.hpc.domain.datatransfer.HpcTieringRequestType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Tiering DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public interface HpcDataTieringDAO 
{    
    /**
     * Store a new lifecycle record.
     *
     * @param userId The user who applied the lifecycle rule.
     * @param requestType The request type of the lifecycle rule.
     * @param s3ArchiveConfigurationId The S3 archive configuration ID.
     * @param completed The time the request was completed.
     * @param filterPrefix The lifecycle filter prefix that was applied.
     * @throws HpcException on database error.
     */
    public void insert(String userId, HpcTieringRequestType requestType,
    		           String s3ArchiveConfigurationId, Calendar completed,
    		           String filterPrefix) 
    		          throws HpcException;
}

 