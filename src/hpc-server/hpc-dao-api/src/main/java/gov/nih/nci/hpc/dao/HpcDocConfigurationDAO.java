/**
 * HpcDocConfigurationDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC DOC Configuration DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDocConfigurationDAO 
{    
    /**
     * Get DOC configuration for all supported DOC.
     *
     * @return A list of DOC configurations.
     * @throws HpcException on service failure.
     */
    public List<HpcDocConfiguration> getDocConfigurations() throws HpcException;
}

 