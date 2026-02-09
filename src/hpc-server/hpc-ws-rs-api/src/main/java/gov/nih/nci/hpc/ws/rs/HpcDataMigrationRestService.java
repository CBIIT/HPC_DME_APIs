/**
 * HpcDataMigrationRestService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import gov.nih.nci.hpc.dto.datamigration.HpcBulkMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMetadataMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationRequestDTO;

/**
 * HPC Data Migration REST Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Path("/")
public interface HpcDataMigrationRestService {
	/**
	 * Migrate a data object to another archive.
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/dataObject/{path:.*}/migrate")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response migrateDataObject(@PathParam("path") String path, HpcMigrationRequestDTO migrationRequest);

	/**
	 * Migrate a collection to another archive.
	 *
	 * @param path             The collection path.
	 * @param migrationRequest The migration request.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/collection/{path:.*}/migrate")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response migrateCollection(@PathParam("path") String path, HpcMigrationRequestDTO migrationRequest);

	/**
	 * Migrate a list of data objects or a list of collections.
	 *
	 * @param migrationRequest The migration request.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/migrate")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response migrateDataObjectsOrCollections(HpcBulkMigrationRequestDTO migrationRequest);

	/**
	 * Retry a data object migration task.
	 *
	 * @param taskId The migration task ID to retry
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/dataObject/migrate/{taskId}/retry")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response retryDataObjectMigrationTask(@PathParam("taskId") String taskId);

	/**
	 * Retry a collection migration task.
	 *
	 * @param taskId          The migration task ID to retry
	 * @param failedItemsOnly (Optional) if set to true, only failed items of
	 *                        'taskId' will be retried. Otherwise the collection
	 *                        will be re-scanned for a new migration to include any
	 *                        items added since the previous migration attempt.
	 *                        Default to true.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/collection/migrate/{taskId}/retry")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response retryCollectionMigrationTask(@PathParam("taskId") String taskId,
			@QueryParam("failedItemsOnly") Boolean failedItemsOnly);

	/**
	 * Retry migration task of a list of data objects or a list of collections.
	 *
	 * @param taskId          The migration task ID to retry
	 * @param failedItemsOnly (Optional) if set to true, only failed items of
	 *                        'taskId' will be retried. Otherwise the collection
	 *                        will be re-scanned for a new migration to include any
	 *                        items added since the previous migration attempt.
	 *                        Default to true.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/migrate/{taskId}/retry")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response retryDataObjectsOrCollectionsMigrationTask(@PathParam("taskId") String taskId,
			@QueryParam("failedItemsOnly") Boolean failedItemsOnly);

	/**
	 * Migrate metadata.
	 *
	 * @param metadataMigrationRequest The metadata migration request.
	 * @return The REST service response w/ HpcMigrationResponseDTO entity.
	 */
	@POST
	@Path("/migrateMetadata")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response migrateMetadata(HpcMetadataMigrationRequestDTO metadataMigrationRequest);

	/**
	 * Process auto-tiering for a specific data management configuration.
	 *
	 * @param configurationId The data management configuration ID.
	 * @return The REST service response.
	 */
	@POST
	@Path("/autoTiering/{configurationId}")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response processAutoTiering(@PathParam("configurationId") String configurationId);
}
