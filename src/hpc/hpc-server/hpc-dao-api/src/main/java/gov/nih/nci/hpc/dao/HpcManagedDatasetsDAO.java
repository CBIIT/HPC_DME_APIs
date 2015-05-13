/**
 * HpcManagedDatasetsDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.HpcManagedDatasets;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed Datasets DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDatasetsDAO 
{         
    /**
     * Add managed datasets to the repository.
     *
     * @param managedDatasets The datasets to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcManagedDatasets managedDatasets) throws HpcException;
}

 