/**
 * HpcCatalogService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.catalog.HpcCatalog;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Catalog Application Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
public interface HpcCatalogService {
  /**
   * Get catalog meta data by catalog query criteria.
   *
   * @param catalogCriteria The catalog query criteria.
   * @return A list of catalog entries.
   * @throws HpcException on service failure.
   */
  public List<HpcCatalog> getCatalog(HpcCatalogCriteria catalogCriteria)
      throws HpcException;

  /**
   * Get count of catalog matching the catalog query criteria.
   *
   * @param catalogCriteria The catalog query criteria.
   * @return The count of catalog entries matching the query.
   * @throws HpcException on service failure.
   */
  public int getCatalogCount(HpcCatalogCriteria catalogCriteria) throws HpcException;
}
