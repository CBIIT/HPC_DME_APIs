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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import gov.nih.nci.hpc.dto.datamigration.HpcBulkMigrationRequestDTO;
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

}
