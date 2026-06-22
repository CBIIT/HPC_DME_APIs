/**
 * HpcStaleFilesServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcStaleFilesDAO;
import gov.nih.nci.hpc.domain.stalefiles.HpcStalePieChartEntry;
import gov.nih.nci.hpc.domain.stalefiles.HpcStaleBarChartEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcStaleFilesService;

/**
 * HPC Stale Files Application Service Implementation.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public class HpcStaleFilesServiceImpl implements HpcStaleFilesService {

    // -------------------------------------------------------------------------//
    // Instance members
    // -------------------------------------------------------------------------//

    // Stale Files DAO.
    @Autowired
    private HpcStaleFilesDAO staleFilesDAO = null;

    // -------------------------------------------------------------------------//
    // Constructors
    // -------------------------------------------------------------------------//

    /** Constructor for Spring Dependency Injection. */
    private HpcStaleFilesServiceImpl() {
    }

    // -------------------------------------------------------------------------//
    // Methods
    // -------------------------------------------------------------------------//

    // -------------------------------------------------------------------------//
    // HpcStaleFilesService Interface Implementation
    // -------------------------------------------------------------------------//

    @Override
    public List<HpcStalePieChartEntry> getPieChartData(String basePath, String currentPath)
            throws HpcException {
        return staleFilesDAO.getPieChartData(basePath, currentPath);
    }

    @Override
    public List<HpcStaleBarChartEntry> getBarChartData(String basePath, String currentPath)
            throws HpcException {
        return staleFilesDAO.getBarChartData(basePath, currentPath);
    }
}
