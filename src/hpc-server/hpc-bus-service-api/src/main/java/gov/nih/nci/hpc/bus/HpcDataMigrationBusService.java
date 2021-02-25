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
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	public HpcMigrationResponseDTO migrateDataObject(String path, HpcMigrationRequestDTO migrationRequest)
			throws HpcException;

	/**
	 * Migrate a cllection to another archive.
	 *
	 * @param path             The collection path.
	 * @param migrationRequest The migration request DTO.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	public HpcMigrationResponseDTO migrateCollection(String path, HpcMigrationRequestDTO migrationRequest)
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
	 * Complete in-progress collection migration tasks.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeCollectionMigrationInProgress() throws HpcException;

	/**
	 * Restart data object migration tasks that are in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void restartDataObjectMigrationTasks() throws HpcException;
}
