/**
 * HpcManagedDataDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.HpcManagedData;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed Data DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDataDAO 
{         
    /**
     * Add managed data to the repository.
     *
     * @param managedData The managed data to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcManagedData managedData) throws HpcException;
}

 