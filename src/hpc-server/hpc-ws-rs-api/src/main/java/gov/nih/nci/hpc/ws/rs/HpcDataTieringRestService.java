/**
 * HpcDataTieringRestService.java
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

import gov.nih.nci.hpc.dto.datatiering.HpcBulkDataObjectTierRequestDTO;


/**
 * HPC Data Tiering REST Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
@Path("/")
public interface HpcDataTieringRestService {
	/**
	 * Data object tiering request to Glacier.
	 *
	 * @param path The data object path to tier.
	 * @return The REST service response.
	 */
	@POST
	@Path("/dataObject/{path:.*}/tier")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8, application/octet-stream")
	public Response tierDataObject(@PathParam("path") String path);
	
	/**
	 * Tiering a collection to Glacier.
	 *
	 * @param path            The collection path.
	 * @return The REST service response.
	 */
	@POST
	@Path("/collection/{path:.*}/tier")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response tierCollection(@PathParam("path") String path);
	
	/**
	 * Tier a list of data objects or a list of collections to Glacier.
	 *
	 * @param tierRequest The tiering request.
	 * @return The REST service response.
	 */
	@POST
	@Path("/tier")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response tierDataObjectsOrCollections(HpcBulkDataObjectTierRequestDTO tierRequest);

}
