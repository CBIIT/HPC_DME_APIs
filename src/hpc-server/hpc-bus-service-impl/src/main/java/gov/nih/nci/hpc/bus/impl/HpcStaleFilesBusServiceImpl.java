/**
 * HpcStaleFilesBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcStaleFilesBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.stalefiles.HpcStalePieChartEntry;
import gov.nih.nci.hpc.domain.stalefiles.HpcStaleBarChartEntry;
import gov.nih.nci.hpc.dto.stalefiles.HpcStalePieChartDTO;
import gov.nih.nci.hpc.dto.stalefiles.HpcStaleBarChartDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcStaleFilesService;

/**
 * HPC Stale Files Business Service Implementation.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public class HpcStaleFilesBusServiceImpl implements HpcStaleFilesBusService {

    // -------------------------------------------------------------------------//
    // Instance members
    // -------------------------------------------------------------------------//

    // Stale Files Application Service instance.
    @Autowired
    private HpcStaleFilesService staleFilesService = null;

    // The security service instance.
    @Autowired
    private HpcSecurityService securityService = null;

    // -------------------------------------------------------------------------//
    // Constructors
    // -------------------------------------------------------------------------//

    /** Constructor for Spring Dependency Injection. */
    private HpcStaleFilesBusServiceImpl() {
    }

    // -------------------------------------------------------------------------//
    // Methods
    // -------------------------------------------------------------------------//

    // -------------------------------------------------------------------------//
    // HpcStaleFilesBusService Interface Implementation
    // -------------------------------------------------------------------------//

    @Override
    public HpcStalePieChartDTO getPieChartData(String basePath, String currentPath)
            throws HpcException {
        HpcRequestInvoker invoker = securityService.getRequestInvoker();
        if (invoker == null) {
            throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
        }

        if (basePath == null || basePath.isEmpty()) {
            throw new HpcException("basePath is required", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (currentPath == null || currentPath.isEmpty()) {
            throw new HpcException("currentPath is required", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        List<HpcStalePieChartEntry> entries =
                staleFilesService.getPieChartData(basePath, currentPath);

        HpcStalePieChartDTO dto = new HpcStalePieChartDTO();
        dto.getPieChartEntries().addAll(entries);
        return dto;
    }

    @Override
    public HpcStaleBarChartDTO getBarChartData(String basePath, String currentPath)
            throws HpcException {
        HpcRequestInvoker invoker = securityService.getRequestInvoker();
        if (invoker == null) {
            throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
        }

        if (basePath == null || basePath.isEmpty()) {
            throw new HpcException("basePath is required", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (currentPath == null || currentPath.isEmpty()) {
            throw new HpcException("currentPath is required", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        List<HpcStaleBarChartEntry> entries =
                staleFilesService.getBarChartData(basePath, currentPath);

        HpcStaleBarChartDTO dto = new HpcStaleBarChartDTO();
        dto.getBarChartEntries().addAll(entries);
        return dto;
    }
}
