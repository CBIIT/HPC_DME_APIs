/**
 * HpcDataManagementConfigurationDAO.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Management Configuration DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataManagementConfigurationDAO {
  /**
   * Get all supported configurations.
   *
   * @return A list of data management configurations.
   * @throws HpcException on service failure.
   */
  public List<HpcDataManagementConfiguration> getDataManagementConfigurations() throws HpcException;

  /**
   * Get all S3 Archive configurations.
   *
   * @return A list of data management configurations.
   * @throws HpcException on service failure.
   */
  public List<HpcDataTransferConfiguration> getS3ArchiveConfigurations() throws HpcException;
}

