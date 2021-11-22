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

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearch;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * HPC Data Management configuration locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementConfigurationLocator extends HashMap<String, HpcDataManagementConfiguration> {
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

	// A map of all supported base paths (to allow quick search).
	private Map<String, HpcDataManagementConfiguration> basePathConfigurations = new HashMap<>();

	// A set of all supported docs (to allow quick search).
	private Set<String> docs = new HashSet<>();

	// A map of all supported S3 archive configurations (to allow quick search by.
	// ID).
	private Map<String, HpcDataTransferConfiguration> s3ArchiveConfigurations = new HashMap<>();

	// A map of all supported distinguished name searches.
	private Map<String, HpcDistinguishedNameSearch> distinguishedNameSearches = new HashMap<>();

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
	 * Get Data Transfer configuration to access a file in the archive (upload,
	 * download , delete, etc)
	 *
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) S3 archive configuration ID.
	 * @param dataTransferType         The data transfer type.
	 * @return The data transfer configuration for the requested configuration ID
	 *         and data transfer type for uploading.
	 * @throws HpcException if the configuration was not found.
	 */
	public HpcDataTransferConfiguration getDataTransferConfiguration(String configurationId,
			String s3ArchiveConfigurationId, HpcDataTransferType dataTransferType) throws HpcException {
		logger.error("ERAN: 4: {}", configurationId);
		HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
		if (dataManagementConfiguration != null) {
			return dataTransferType.equals(HpcDataTransferType.S_3)
					? getS3ArchiveConfiguration(
							!StringUtils.isEmpty(s3ArchiveConfigurationId) ? s3ArchiveConfigurationId
									: dataManagementConfiguration.getS3DefaultDownloadConfigurationId())
					: dataManagementConfiguration.getGlobusConfiguration();
		}

		// Configuration was not found.
		throw new HpcException("Could not locate data transfer configuration: " + configurationId + ", "
				+ dataTransferType.value() + ", S3 Archive: " + s3ArchiveConfigurationId,
				HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get the Archive Data Transfer type.
	 *
	 * @param configurationId The data management configuration ID.
	 * @return The archive data transfer type for this configuration ID.
	 * @throws HpcException if the configuration was not found.
	 */
	public HpcDataTransferType getArchiveDataTransferType(String configurationId) throws HpcException {
		HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
		if (dataManagementConfiguration != null) {
			return dataManagementConfiguration.getArchiveDataTransferType();
		}

		// Configuration was not found.
		throw new HpcException("Could not locate configuration: " + configurationId, HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Load the data management configurations from the DB. Called by spring as
	 * init-method.
	 *
	 * @throws HpcException On configuration error.
	 */
	public void reload() throws HpcException {
		clear();
		basePathConfigurations.clear();
		docs.clear();
		s3ArchiveConfigurations.clear();
		distinguishedNameSearches.clear();

		// Load the distinguished name search bases.
		dataManagementConfigurationDAO.getDistinguishedNameSearches()
				.forEach(distinguishedNameSearch -> distinguishedNameSearches
						.put(toNormalizedPath(distinguishedNameSearch.getBasePath()), distinguishedNameSearch));

		// Load the S3 archive configurations.
		dataManagementConfigurationDAO.getS3ArchiveConfigurations()
				.forEach(s3ArchiveConfiguration -> s3ArchiveConfigurations.put(s3ArchiveConfiguration.getId(),
						s3ArchiveConfiguration));

		// Load the data management configurations.
		for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementConfigurationDAO
				.getDataManagementConfigurations()) {
			// Ensure the base path is in the form of a relative path, and one level deep
			// (i.e.
			// /base-path).
			String basePath = dataManagementProxy.getRelativePath(dataManagementConfiguration.getBasePath());
			if (basePath.split("/").length != 2) {
				throw new HpcException("Invalid base path [" + basePath + "]. Only one level path supported.",
						HpcErrorType.UNEXPECTED_ERROR);
			}
			dataManagementConfiguration.setBasePath(basePath);

			// Ensure base path is unique (i.e. no 2 configurations share the same base
			// path).
			if (basePathConfigurations.put(basePath, dataManagementConfiguration) != null) {
				throw new HpcException("Duplicate base-path in data management configurations:"
						+ dataManagementConfiguration.getBasePath(), HpcErrorType.UNEXPECTED_ERROR);
			}

			// Determine the archive data transfer type.
			// Note: Currently the transfer into the archive can be supported by either S3
			// or Globus. We use Globus data-transfer proxy to transfer into POSIX archive.
			HpcArchiveType globusArchiveType = dataManagementConfiguration.getGlobusConfiguration()
					.getBaseArchiveDestination().getType();
			boolean s3Archive = !StringUtils.isEmpty(dataManagementConfiguration.getS3UploadConfigurationId());
			if (s3Archive
					&& (globusArchiveType == null || globusArchiveType.equals(HpcArchiveType.TEMPORARY_ARCHIVE))) {
				dataManagementConfiguration.setArchiveDataTransferType(HpcDataTransferType.S_3);
			} else if ((globusArchiveType != null && globusArchiveType.equals(HpcArchiveType.ARCHIVE)) && !s3Archive) {
				dataManagementConfiguration.setArchiveDataTransferType(HpcDataTransferType.GLOBUS);
			} else {
				throw new HpcException(
						"Invalid S3/Globus archive type configuration: " + dataManagementConfiguration.getBasePath(),
						HpcErrorType.UNEXPECTED_ERROR);
			}

			// Validate S3 Archive configurations.
			if (s3Archive) {
				// If the upload S3 configuration or default S3 download configuration not
				// found, an
				// exception will be thrown.
				getS3ArchiveConfiguration(dataManagementConfiguration.getS3UploadConfigurationId());
				getS3ArchiveConfiguration(dataManagementConfiguration.getS3DefaultDownloadConfigurationId());
			}

			// Populate the DOCs list.
			docs.add(dataManagementConfiguration.getDoc());

			// Populate the configurationId -> configuration map.
			put(dataManagementConfiguration.getId(), dataManagementConfiguration);
		}

		logger.info("Data Management Configurations: " + toString());
	}

	/**
	 * Get S3 Archive configuration by ID.
	 *
	 * @param s3ArchiveConfigurationId The S3 archive configuration ID.
	 * @return The S3 configuration for the requested S3 configuration ID.
	 * @throws HpcException if the configuration was not found.
	 */
	public HpcDataTransferConfiguration getS3ArchiveConfiguration(String s3ArchiveConfigurationId) throws HpcException {
		HpcDataTransferConfiguration s3ArchiveConfiguration = s3ArchiveConfigurations.get(s3ArchiveConfigurationId);

		if (s3ArchiveConfiguration == null) {
			throw new HpcException("Could not locate S3 archive configuration: " + s3ArchiveConfigurationId,
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Ensure encryption algorithm and key are both defined or both omitted.
		if ((StringUtils.isEmpty(s3ArchiveConfiguration.getEncryptionAlgorithm())
				&& !StringUtils.isEmpty(s3ArchiveConfiguration.getEncryptionKey()))
				|| (!StringUtils.isEmpty(s3ArchiveConfiguration.getEncryptionAlgorithm())
						&& StringUtils.isEmpty(s3ArchiveConfiguration.getEncryptionKey()))) {
			throw new HpcException(
					"Misconfigured encryption key/algorithm for S3 archive configuration: " + s3ArchiveConfigurationId,
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return s3ArchiveConfiguration;
	}

	/**
	 * Get Distinguished Name Search.
	 *
	 * @param basePath The base path of the mounted disk on DME server to get the
	 *                 search base for.
	 * @return The distinguished name search base, or null if not found.
	 */
	public HpcDistinguishedNameSearch getDistinguishedNameSearch(String basePath) {
		return distinguishedNameSearches.get(basePath);
	}

	/**
	 * Get all supported DN search base paths.
	 *
	 * @return A list of all supported DN search base paths.
	 */
	public Set<String> getDistinguishedNameSearchBasePaths() {
		return distinguishedNameSearches.keySet();
	}
}
