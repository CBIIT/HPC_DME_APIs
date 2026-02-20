/**
 * HpcExternalArchiveDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.trino.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.dao.HpcExternalArchiveDAO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC External Archive DAO Implementation.
 *
 * This implementation queries external archives (VAST managed archives mounted via NFS)
 * to identify files that have not been accessed within a specified time period for
 * auto-tiering migration to S3 Glacier Deep Archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcExternalArchiveDAOImpl implements HpcExternalArchiveDAO {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	public HpcExternalArchiveDAOImpl() {}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcExternalArchiveDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<String> getFilesNotAccessed(String searchPath, Integer monthsNotAccessed) throws HpcException {
		logger.info("getFilesNotAccessed called with searchPath: {}, monthsNotAccessed: {}",
				searchPath, monthsNotAccessed);

		// TODO: Implement actual query logic using Trino to scan external archive
		// For now, return dummy paths for testing
		List<String> filePaths = new ArrayList<>();
		filePaths.add("/Eran/eran-file-1.txt");
		filePaths.add("/Eran/eran-file-2.txt");
		filePaths.add("/Eran/eran-file-3.txt");

		logger.info("Returning {} file paths", filePaths.size());
		return filePaths;
	}
}
