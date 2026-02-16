/**
 * HpcExternalArchiveDAO.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC External Archive DAO Interface.
 *
 * This interface provides data access methods for querying files in external archives
 * (VAST managed archives mounted via NFS on DME server) to support auto-tiering functionality.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcExternalArchiveDAO {
	/**
	 * Query for files in the external archive that have not been accessed within
	 * the specified time period. These files are candidates for auto-tiering migration
	 * to S3 Glacier Deep Archive.
	 *
	 * @param searchPath The external archive search path to scan for files.
	 * @param monthsNotAccessed The time period in months during which files were not accessed.
	 *                          Files with last access time older than this will be returned.
	 * @return A list of file paths that have not been accessed within the specified time period.
	 * @throws HpcException on service failure.
	 */
	List<String> getFilesNotAccessed(String searchPath, Integer monthsNotAccessed) throws HpcException;
}
