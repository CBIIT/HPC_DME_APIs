/**
 * HpcCatalogServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcCatalogDAO;
import gov.nih.nci.hpc.domain.catalog.HpcCatalog;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogEntry;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcCatalogService;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * HPC Catalog Application Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcCatalogServiceImpl implements HpcCatalogService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Catalog DAO.
  @Autowired private HpcCatalogDAO catalogDAO = null;

  //The Data Management Proxy instance.
  @Autowired private HpcDataManagementProxy dataManagementProxy = null;

  // Pagination support.
  @Autowired
  @Qualifier("hpcDataSearchPagination")
  private HpcPagination pagination = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcCatalogServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcCatalogService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public List<HpcCatalog> getCatalog(HpcCatalogCriteria catalogCriteria)
      throws HpcException {

    //If pageSize is specified, replace the default defined
    int finalPageSize = pagination.getPageSize();
    int finalOffset = pagination.getOffset(catalogCriteria.getPage());
    if(catalogCriteria.getPageSize() != 0) {
      finalPageSize = (catalogCriteria.getPageSize() <= pagination.getMaxPageSize() ? catalogCriteria.getPageSize() : pagination.getMaxPageSize());
      finalOffset = (catalogCriteria.getPage() - 1) * finalPageSize;
    }
    
    List<HpcCatalogMetadataEntry> catalogMetadataEntries = catalogDAO.getCatalog(catalogCriteria, finalOffset, finalPageSize);
    List<HpcCatalog> hpcCatalog = new ArrayList<HpcCatalog>();
    HpcCatalog catalog = null;
    String prevPath = null;
    for(HpcCatalogMetadataEntry catalogMetadataEntry: catalogMetadataEntries) {
      if(prevPath == null || !prevPath.equals(catalogMetadataEntry.getPath())) {
        if(prevPath != null)
          hpcCatalog.add(catalog);
        catalog = new HpcCatalog();
        catalog.setDoc(catalogMetadataEntry.getDoc());
        catalog.setBasePath(catalogMetadataEntry.getBasePath());
        catalog.setPath(toRelativePath(catalogMetadataEntry.getPath()));
      }
      HpcCatalogEntry entry = new HpcCatalogEntry();
      entry.setAttribute(catalogMetadataEntry.getAttribute());
      entry.setValue(catalogMetadataEntry.getValue());
      catalog.getCatalogEntries().add(entry);
      prevPath = catalogMetadataEntry.getPath();
      
    }
    if(prevPath != null) {
      hpcCatalog.add(catalog);
    }
    return hpcCatalog;
  }

  @Override
  public int getCatalogCount(HpcCatalogCriteria catalogCriteria)
      throws HpcException {
    
    return catalogDAO.getCatalogCount(catalogCriteria);
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//
  
  /**
   * Convert an absolute path, to relative path.
   *
   * @param path The absolute paths.
   * @return The relative paths.
   */
  public String toRelativePath(String path) {
    return dataManagementProxy.getRelativePath(path);
  }
}
