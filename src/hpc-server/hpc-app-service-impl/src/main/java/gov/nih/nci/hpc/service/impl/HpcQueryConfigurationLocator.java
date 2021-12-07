/**
 * HpcQueryConfigurationLocator.java
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
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.dao.HpcQueryConfigDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * HPC Query configuration locator.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcQueryConfigurationLocator extends HashMap<String, HpcQueryConfiguration> {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	private static final long serialVersionUID = 1849668198146866432L;

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;
		
	// The Query Configuration DAO instance.
	@Autowired
	private HpcQueryConfigDAO queryConfigDAO = null;

	// A map of all base paths that has entries in query config
	private Map<String, HpcQueryConfiguration> queryConfigurations = new HashMap<>();

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Default constructor for Spring Dependency Injection. */
	private HpcQueryConfigurationLocator() {
		super();
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Load the query configurations from the DB. Called by spring as
	 * init-method.
	 *
	 * @throws HpcException On configuration error.
	 */
	public void reload() throws HpcException {
		clear();
		queryConfigurations.clear();

		// Load the query configurations.
		for (HpcQueryConfiguration queryConfiguration : queryConfigDAO
				.getQueryConfigurations()) {
			// Ensure the base path is in the form of a relative path, and one level deep
			// (i.e.
			// /base-path).
			String basePath = dataManagementProxy.getRelativePath(queryConfiguration.getBasePath());
			if (basePath.split("/").length != 2) {
				throw new HpcException("Invalid base path in query config [" + basePath + "]. Only one level path supported.",
						HpcErrorType.UNEXPECTED_ERROR);
			}
			queryConfiguration.setBasePath(basePath);

			// Ensure base path is unique (i.e. no 2 configurations share the same base
			// path).
			if (queryConfigurations.put(basePath, queryConfiguration) != null) {
				throw new HpcException("Duplicate base-path in query configurations:"
						+ queryConfiguration.getBasePath(), HpcErrorType.UNEXPECTED_ERROR);
			}
		}

	}

	/**
	 * Get query configuration for base path.
	 *
	 * @return Query configuration for the base path.
	 */
	public HpcQueryConfiguration getConfig(String basePath) {
		return queryConfigurations.get(basePath);
	}
}
