/**
 * HpcDataTieringRestServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataTieringBusService;
import gov.nih.nci.hpc.dto.datatiering.HpcBulkDataObjectTierRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataTieringRestService;


/**
 * HPC Data Tiering REST Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcDataTieringRestServiceImpl extends HpcRestServiceImpl implements HpcDataTieringRestService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Tiering Business Service instance.
	@Autowired
	private HpcDataTieringBusService dataTieringBusService = null;

	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataTieringRestServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTieringRestService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Response tierDataObject(String path) {
		try {
			dataTieringBusService.tierDataObject(path);
			
		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(null, false);
	}

	@Override
	public Response tierCollection(String path) {
		try {
			dataTieringBusService.tierCollection(path);
			
		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(null, false);
	}

	@Override
	public Response tierDataObjectsOrCollections(HpcBulkDataObjectTierRequestDTO tierRequest) {
		try {
			dataTieringBusService.tierDataObjectsOrCollections(tierRequest);
			
		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(null, false);
	}
	
}
