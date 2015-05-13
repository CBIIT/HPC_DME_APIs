/**
 * HpcManagedDatasetsService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Managed Datasets App Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDatasetsService 
{         
    /**
     * Add managed datasets.
     *
     * @param datasets The datasets to start manage.
     * 
     * @throws HpcException
     */
    public void add(List<HpcDataset> datasets) throws HpcException;
}

 