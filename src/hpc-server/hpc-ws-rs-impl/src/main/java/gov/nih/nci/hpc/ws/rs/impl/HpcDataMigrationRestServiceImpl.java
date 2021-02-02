/**
 * HpcDataManagementRestServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataMigrationRestService;

/**
 * HPC Data Management REST Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataMigrationRestServiceImpl extends HpcRestServiceImpl implements HpcDataMigrationRestService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Management Business Service instance.
	@Autowired
	private HpcDataMigrationBusService dataMigrationBusService = null;

	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataMigrationRestServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataMigrationRestService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Response migrateDataObject(String path, HpcMigrationRequestDTO migrationRequest) {
		HpcMigrationResponseDTO migrationResponse = null;
		try {
			migrationResponse = dataMigrationBusService.migrateDataObject(toNormalizedPath(path), migrationRequest);

		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(migrationResponse, false);
	}

	@Override
	public Response migrateCollection(String path, HpcMigrationRequestDTO migrationRequest) {
		HpcMigrationResponseDTO migrationResponse = null;
		try {
			migrationResponse = dataMigrationBusService.migrateCollection(toNormalizedPath(path), migrationRequest);

		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(migrationResponse, false);
	}
}
