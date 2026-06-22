/**
 * HpcStaleFilesRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import jakarta.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcStaleFilesBusService;
import gov.nih.nci.hpc.dto.stalefiles.HpcStalePieChartDTO;
import gov.nih.nci.hpc.dto.stalefiles.HpcStaleBarChartDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcStaleFilesRestService;

/**
 * <p>
 * HPC Stale Files REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public class HpcStaleFilesRestServiceImpl extends HpcRestServiceImpl
        implements HpcStaleFilesRestService {

    // -------------------------------------------------------------------------//
    // Instance members
    // -------------------------------------------------------------------------//

    // The Stale Files Business Service instance.
    @Autowired
    private HpcStaleFilesBusService staleFilesBusService = null;

    // -------------------------------------------------------------------------//
    // Constructors
    // -------------------------------------------------------------------------//

    /** Constructor for Spring Dependency Injection. */
    private HpcStaleFilesRestServiceImpl() throws HpcException {
    }

    // -------------------------------------------------------------------------//
    // Methods
    // -------------------------------------------------------------------------//

    // -------------------------------------------------------------------------//
    // HpcStaleFilesRestService Interface Implementation
    // -------------------------------------------------------------------------//

    @Override
    public Response getPieChartData(String basePath, String currentPath) {
        HpcStalePieChartDTO dto = null;
        try {
            dto = staleFilesBusService.getPieChartData(basePath, currentPath);
        } catch (HpcException e) {
            return errorResponse(e);
        }
        return okResponse(dto, true);
    }

    @Override
    public Response getBarChartData(String basePath, String currentPath) {
        HpcStaleBarChartDTO dto = null;
        try {
            dto = staleFilesBusService.getBarChartData(basePath, currentPath);
        } catch (HpcException e) {
            return errorResponse(e);
        }
        return okResponse(dto, true);
    }
}
