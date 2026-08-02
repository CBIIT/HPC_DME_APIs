/**
 * HpcAutoTieringDAO.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Auto Tiering DAO Interface.
 *
 * <p>Implementations of this interface can be used to identify files in external/internal archives
 * that are candidates for auto-tiering migration to S3 Glacier Deep Archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcAutoTieringDAO {
	/**
	 * Query for files that are candidates for auto-tiering migration to S3 Glacier Deep Archive.
	 * These are files that have not been accessed within the specified inactivity period and have
	 * been archived for at least the specified archived period.
	 *
	 * @param searchPath The search path to scan for files.
	 * @param inactivityMonths The time period in months during which files were not accessed.
	 *                          Files with last access time older than this will be returned.
	 * @param archivedMonths The minimum time period in months a file must have been archived.
	 *                       Only files archived at least this long ago will be returned.
	 * @param s3ArchiveConfigurationId The S3 archive configuration ID to exclude. Only files
	 *                                  NOT already in this S3 archive configuration are returned.
	 * @return A list of file paths that are candidates for auto-tiering.
	 * @throws HpcException on service failure.
	 */
	List<String> getFilesForAutoTiering(String searchPath, Integer inactivityMonths, Integer archivedMonths, String s3ArchiveConfigurationId) throws HpcException;
}
