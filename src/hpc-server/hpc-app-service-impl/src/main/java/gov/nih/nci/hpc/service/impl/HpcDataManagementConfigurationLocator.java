/**
 * HpcDataManagementConfigurationLocator.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcArchiveDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * HPC Data Management configuration locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementConfigurationLocator
    extends HashMap<String, HpcDataManagementConfiguration> {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  private static final long serialVersionUID = -2233828633688868458L;

  // The Data Management Proxy instance.
  @Autowired
  private HpcDataManagementProxy dataManagementProxy = null;

  // The Data Management Configuration DAO instance.
  @Autowired
  private HpcDataManagementConfigurationDAO dataManagementConfigurationDAO = null;

  // A set of all supported base paths (to allow quick search).
  private Map<String, HpcDataManagementConfiguration> basePathConfigurations = new HashMap<>();

  // A set of all supported docs (to allow quick search).
  private Set<String> docs = new HashSet<>();

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Default constructor for Spring Dependency Injection. */
  private HpcDataManagementConfigurationLocator() {
    super();
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Get all supported base paths.
   *
   * @return A list of all supported base paths.
   */
  public Set<String> getBasePaths() {
    return basePathConfigurations.keySet();
  }

  /**
   * Get all supported DOCs.
   *
   * @return A list of all supported base paths.
   */
  public Set<String> getDocs() {
    return docs;
  }

  /**
   * Get configuration ID by base path.
   *
   * @param basePath The base path to get the config for.
   * @return A configuration ID, or null if not found.
   */
  public String getConfigurationId(String basePath) {
    HpcDataManagementConfiguration configuration = basePathConfigurations.get(basePath);
    return configuration != null ? configuration.getId() : null;
  }

  /**
   * Get Archive Data Transfer configuration.
   *
   * @param configurationId The data management configuration ID.
   * @param dataTransferType The data transfer type.
   * @return The archive data transfer configuration for the requested configuration ID and data transfer type.
   * @throws HpcException if the configuration was not found.
   */
  public HpcArchiveDataTransferConfiguration getArchiveDataTransferConfiguration(String configurationId,
      HpcDataTransferType dataTransferType) throws HpcException {
    HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
    if (dataManagementConfiguration != null) {
      return dataTransferType.equals(HpcDataTransferType.GLOBUS)
          ? dataManagementConfiguration.getArchiveGlobusConfiguration()
          : dataManagementConfiguration.getArchiveS3Configuration();
    }

    // Configuration was not found.
    throw new HpcException("Could not locate data transfer configuration: " + configurationId + " "
        + dataTransferType.value(), HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Get the Archive type.
   *
   * @param configurationId The data management configuration ID.
   * @return The archive type for this configuration ID.
   * @throws HpcException if the configuration was not found.
   */
  public HpcArchiveType getArchiveType(String configurationId) throws HpcException {
    HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
    if (dataManagementConfiguration != null) {
      return dataManagementConfiguration.getArchiveType();
    }

    // Configuration was not found.
    throw new HpcException("Could not locate configuration: " + configurationId,
        HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Load the data management configurations from the DB. Called by spring as init-method.
   *
   * @throws HpcException On configuration error.
   */
  public void reload() throws HpcException {
    clear();
    basePathConfigurations.clear();
    docs.clear();

    for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementConfigurationDAO
        .getDataManagementConfigurations()) {
      // Ensure the base path is in the form of a relative path, and one level deep (i.e.
      // /base-path).
      String basePath =
          dataManagementProxy.getRelativePath(dataManagementConfiguration.getBasePath());
      if (basePath.split("/").length != 2) {
        throw new HpcException(
            "Invalid base path [" + basePath + "]. Only one level path supported.",
            HpcErrorType.UNEXPECTED_ERROR);
      }
      dataManagementConfiguration.setBasePath(basePath);

      // Ensure base path is unique (i.e. no 2 configurations share the same base path).
      if (basePathConfigurations.put(basePath, dataManagementConfiguration) != null) {
        throw new HpcException("Duplicate base-path in data management configurations:"
            + dataManagementConfiguration.getBasePath(), HpcErrorType.UNEXPECTED_ERROR);
      }

      // Populate the DOCs list.
      docs.add(dataManagementConfiguration.getDoc());

      // Populate the configurationId -> configuration map.
      put(dataManagementConfiguration.getId(), dataManagementConfiguration);
    }

    logger.info("Data Management Configurations: " + toString());
  }
}
