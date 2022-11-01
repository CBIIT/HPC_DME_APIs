/**
 * HpcDataMigrationBusService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.datamigration.HpcBulkMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Migration Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataMigrationBusService {
	/**
	 * Migrate a data object Object to another archive.
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request DTO.
	 * @param alignArchivePath If true, the file is moved within its current archive
	 *                         to align w/ the iRODs path.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	public HpcMigrationResponseDTO migrateDataObject(String path, HpcMigrationRequestDTO migrationRequest,
			boolean alignArchivePath) throws HpcException;

	/**
	 * Migrate a collection to another archive.
	 *
	 * @param path             The collection path.
	 * @param migrationRequest The migration request DTO.
	 * @param alignArchivePath If true, the file is moved within its current archive
	 *                         to align w/ the iRODs path.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	public HpcMigrationResponseDTO migrateCollection(String path, HpcMigrationRequestDTO migrationRequest,
			boolean alignArchivePath) throws HpcException;

	/**
	 * Migrate data objects or collections. Note: API doesn't support mixed, so user
	 * expected to provide a list of data objects or a list of collections, not
	 * both.
	 *
	 * @param migrationRequest The download request DTO.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	public HpcMigrationResponseDTO migrateDataObjectsOrCollections(HpcBulkMigrationRequestDTO migrationRequest)
			throws HpcException;

	/**
	 * Process received data object migration tasks.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataObjectMigrationReceived() throws HpcException;

	/**
	 * Process received collection migration tasks
	 *
	 * @throws HpcException on service failure.
	 */
	public void processCollectionMigrationReceived() throws HpcException;

	/**
	 * Process received data object list migration tasks
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataObjectListMigrationReceived() throws HpcException;

	/**
	 * Process received collection list migration tasks
	 *
	 * @throws HpcException on service failure.
	 */
	public void processCollectionListMigrationReceived() throws HpcException;

	/**
	 * Complete in-progress bulk migration tasks.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeBulkMigrationInProgress() throws HpcException;

	/**
	 * Assign a migration server to process migration tasks.
	 *
	 * @throws HpcException on service failure.
	 */
	public void assignMigrationServer() throws HpcException;

	/**
	 * Restart data object and collection migration tasks that are in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void restartDataMigrationTasks() throws HpcException;
}
